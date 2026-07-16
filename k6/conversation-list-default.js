/**
 * 대화방 목록 조회 기본 부하 테스트
 *
 * 서비스 전체 목표:
 * - 평시 트래픽: 100~300 RPS
 * - 피크 트래픽: 500~1,500 RPS
 * - 최대 부하 테스트: 2,000+ RPS
 *
 * 이 스크립트는 서비스 전체가 아니라 GET /api/conversations 단일 API를 측정한다.
 * 대화방 목록조회가 전체 트래픽의 일부라고 보고 기본값은 30 RPS -> 200 RPS로 설정한다.
 * 5개 애플리케이션 인스턴스 분산 환경에서는 BASE_URL을 로드밸런서 주소로 지정하고,
 * 이 RPS는 인스턴스당 값이 아니라 로드밸런서로 들어가는 전체 API RPS로 해석한다.
 *
 * 목적:
 * - 로그인/토큰 발급 부하는 setup 단계로 분리한다.
 * - 테스트 본문에서는 GET /api/conversations 목록 조회만 arrival-rate 기반으로 측정한다.
 * - 여러 사용자 Access Token을 순환 사용하여 특정 사용자 캐시/데이터에 부하가 몰리지 않게 한다.
 *
 * 데이터 전제:
 * - USER_PREFIX-000001@moduply.test 형식의 테스트 사용자가 존재해야 한다.
 * - 각 테스트 사용자는 충분한 1:1 대화방과 DM 데이터를 가지고 있어야 한다.
 *
 * Docker Compose 실행 예시:
 * docker compose -f docker-compose-k6.yml run --rm --service-ports `
 *   -e BASE_URL=http://host.docker.internal:8080 `
 *   -e USER_COUNT=10000 `
 *   -e RPS_STAGES=30,60,100,150,200 `
 *   -e STAGE_DURATION=1m `
 *   -e K6_KO_HTML_REPORT=/scripts/reports/conversation/conversation-list-default.html `
 *   k6 run /scripts/conversation-list-default.js
 */
import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { koreanSummary } from './korean-html-report.js';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const PASSWORD = __ENV.TEST_USER_PASSWORD || 'k6-password';
const USER_PREFIX = __ENV.USER_PREFIX || 'k6-conversation';
const EMAIL_DOMAIN = __ENV.EMAIL_DOMAIN || '@moduply.test';
const USER_COUNT = positiveNumber(__ENV.USER_COUNT, 10000);
const SETUP_LOGIN_BATCH_SIZE = positiveNumber(__ENV.SETUP_LOGIN_BATCH_SIZE, 100);
const SETUP_TIMEOUT = __ENV.SETUP_TIMEOUT || '10m';

const LIMIT = positiveNumber(__ENV.LIMIT, 20);
const SORT_BY = __ENV.SORT_BY || 'createdAt';
const SORT_DIRECTION = __ENV.SORT_DIRECTION || 'DESCENDING';
const KEYWORD_LIKE = __ENV.KEYWORD_LIKE || '';

const START_RPS = positiveNumber(__ENV.START_RPS, 30);
const RPS_STAGES = parseRpsStages(__ENV.RPS_STAGES || '30,60,100,150,200');
const STAGE_DURATION = __ENV.STAGE_DURATION || '1m';
const RAMP_DOWN_DURATION = __ENV.RAMP_DOWN_DURATION || '30s';
const PRE_ALLOCATED_VUS = positiveNumber(__ENV.PRE_ALLOCATED_VUS, 300);
const MAX_VUS = positiveNumber(__ENV.MAX_VUS, 1000);   // 목표 RPS 유지를 위해 필요한 VU 상한이다.

const conversationListOk = new Counter('conversation_list_ok');
const conversationListSchemaInvalid = new Rate('conversation_list_schema_invalid');

export const options = {
  setupTimeout: SETUP_TIMEOUT,
  scenarios: {
    conversation_list_default_rate: {
      executor: 'ramping-arrival-rate',
      startRate: START_RPS,
      timeUnit: '1s',
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      stages: buildStages(),
      tags: {
        scenario: 'conversation-list-default',
      },
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_failed{api:conversation-list-default}': ['rate<0.01'],
    'http_req_duration{api:conversation-list-default}': ['p(95)<500'],
    'http_reqs{api:conversation-list-default}': ['count>0'],
    dropped_iterations: ['count==0'],
    conversation_list_schema_invalid: ['rate<0.01'],
  },
};

export function setup() {
  const sessions = batchLogin();

  if (sessions.length === 0) {
    fail('No authenticated sessions were created.');
  }

  return { sessions };
}

export default function (data) {
  const session = data.sessions[exec.scenario.iterationInTest % data.sessions.length];
  const res = http.get(`${BASE_URL}/api/conversations${queryString()}`, {
    headers: {
      Authorization: `Bearer ${session.accessToken}`,
      Accept: 'application/json',
    },
    responseCallback: http.expectedStatuses(200),
    tags: {
      api: 'conversation-list-default',
      name: 'GET /api/conversations',
    },
  });

  const ok = check(res, {
    '대화방 목록 조회가 200을 반환한다.': (response) => response.status === 200,
    '대화방 목록 조회 응답이 CursorResponse 형식이다.': hasCursorResponseShape,
  });

  if (ok) {
    conversationListOk.add(1);
  }

  conversationListSchemaInvalid.add(!hasCursorResponseShape(res));
}

function batchLogin() {
  const sessions = [];

  for (let start = 1; start <= USER_COUNT; start += SETUP_LOGIN_BATCH_SIZE) {
    const end = Math.min(start + SETUP_LOGIN_BATCH_SIZE - 1, USER_COUNT);
    const emails = [];
    const requests = [];

    for (let index = start; index <= end; index += 1) {
      const email = createEmail(index);
      emails.push(email);
      requests.push(createLoginRequest(email));
    }

    const responses = http.batch(requests);
    responses.forEach((res, index) => {
      sessions.push(parseLoginResponse(res, emails[index]));
    });
  }

  return sessions;
}

function createLoginRequest(email) {
  return {
    method: 'POST',
    url: `${BASE_URL}/api/auth/sign-in`,
    body: {
      username: email,
      password: PASSWORD,
    },
    params: {
      responseCallback: http.expectedStatuses(200),
      tags: {
        api: 'conversation-list-login',
        name: 'POST /api/auth/sign-in',
      },
    },
  };
}

function parseLoginResponse(res, email) {
  const loginOk = check(res, {
    '로그인이 200을 반환한다.': (response) => response.status === 200,
  });

  if (!loginOk) {
    fail(`Login failed. email=${email}, status=${res.status}, body=${res.body}`);
  }

  const body = res.json();
  if (!body.accessToken || !body.userDto?.id) {
    fail(`Login response is invalid. email=${email}, body=${res.body}`);
  }

  return {
    userId: body.userDto.id,
    accessToken: body.accessToken,
  };
}

function queryString() {
  const params = [
    ['limit', LIMIT],
    ['sortBy', SORT_BY],
    ['sortDirection', SORT_DIRECTION],
  ];

  if (KEYWORD_LIKE) {
    params.push(['keywordLike', KEYWORD_LIKE]);
  }

  return `?${params
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&')}`;
}

function hasCursorResponseShape(response) {
  if (response.status !== 200) {
    return false;
  }

  try {
    const body = response.json();
    return (
      Array.isArray(body.data) &&
      typeof body.hasNext === 'boolean' &&
      typeof body.totalCount === 'number' &&
      body.sortBy === SORT_BY &&
      body.sortDirection === SORT_DIRECTION
    );
  } catch {
    return false;
  }
}

function createEmail(index) {
  return `${USER_PREFIX}-${String(index).padStart(6, '0')}${EMAIL_DOMAIN}`;
}

function buildStages() {
  return [
    ...RPS_STAGES.map((target) => ({
      target,
      duration: STAGE_DURATION,
    })),
    {
      target: 0,
      duration: RAMP_DOWN_DURATION,
    },
  ];
}

function parseRpsStages(value) {
  const stages = value
    .split(',')
    .map((stage) => Number(stage.trim()))
    .filter((stage) => Number.isFinite(stage) && stage >= 0);

  if (stages.length === 0) {
    return [30, 60, 100, 150, 200];
  }

  return stages;
}

function positiveNumber(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export function handleSummary(data) {
  return koreanSummary(data, 'conversation-list-default');
}
