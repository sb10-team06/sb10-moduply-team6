/**
 * 대화방 목록 조회 부하테스트
 * 사용자 10,000명 기준 각 사용자는 100개 이상의 대화방, 각 대화방당 10개이상의 DM을 가진다.
 * 초당 요청수 100 ~ 500까지
 */
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { koreanSummary } from './korean-html-report.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.TEST_USER_PASSWORD || 'k6-password';
const USER_COUNT = Number(__ENV.USER_COUNT || 10000);
const USER_PREFIX = __ENV.USER_PREFIX || 'k6-conversation';
const EMAIL_DOMAIN = __ENV.EMAIL_DOMAIN || '@moduply.test';
const SETUP_LOGIN_BATCH_SIZE = Math.max(1, Number(__ENV.SETUP_LOGIN_BATCH_SIZE || 200));

const LIMIT = Number(__ENV.LIMIT || 20);
const SORT_BY = __ENV.SORT_BY || 'createdAt';
const SORT_DIRECTION = __ENV.SORT_DIRECTION || 'DESCENDING';
const KEYWORD_LIKE = __ENV.KEYWORD_LIKE;

const START_RPS = Number(__ENV.START_RPS || 100);
const RPS_STAGES = (__ENV.RPS_STAGES || '100,200,300,400,500')
  .split(',')
  .map((value) => Number(value.trim()))
  .filter((value) => Number.isFinite(value) && value >= 0);
const STAGE_DURATION = __ENV.STAGE_DURATION || '1m';
const RAMP_DOWN_DURATION = __ENV.RAMP_DOWN_DURATION || '30s';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 1000);
const MAX_VUS = Number(__ENV.MAX_VUS || 5000);
const SETUP_TIMEOUT = __ENV.SETUP_TIMEOUT || '10m';

const conversationListOk = new Counter('conversation_list_ok');

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
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_failed{api:conversation-list-default}': ['rate<0.01'],
    'http_req_duration{api:conversation-list-default}': ['p(95)<1000'],
    'http_reqs{api:conversation-list-default}': ['count>0'],
  },
};

// 10,000명의 테스트 사용자를 모두 로그인시켜 Access Token을 미리 발급
// 200명씩 로그인 요청 생성 -> http.batch()로 동시에 로그인 -> Access Token 추출
export function setup() {
  return {
    sessions: batchLogin(USER_PREFIX, USER_COUNT),
  };
}

export default function (data) {
  const session = data.sessions[exec.scenario.iterationInTest % data.sessions.length];
  const res = http.get(`${BASE_URL}/api/conversations${queryString()}`, {
    headers: {
      Authorization: `Bearer ${session.accessToken}`,
    },
    responseCallback: http.expectedStatuses(200),
    tags: {
      api: 'conversation-list-default',
      name: 'GET /api/conversations default',
    },
  });

  if (check(res, { 'conversation list loaded': (response) => response.status === 200 })) {
    conversationListOk.add(1);
  }
}

function batchLogin(prefix, count) {
  const sessions = [];

  for (let start = 1; start <= count; start += SETUP_LOGIN_BATCH_SIZE) {
    const end = Math.min(start + SETUP_LOGIN_BATCH_SIZE - 1, count);
    const emails = [];
    const requests = [];

    for (let i = start; i <= end; i += 1) {
      const email = createEmail(prefix, i);
      emails.push(email);
      requests.push(createLoginRequest(email));
    }

    const responses = http.batch(requests);
    sessions.push(...responses.map((res, index) => parseLoginResponse(res, emails[index])));
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
  check(res, {
    'login succeeded': (response) => response.status === 200,
  });

  if (res.status !== 200) {
    throw new Error(`Login failed. email=${email}, status=${res.status}, body=${res.body}`);
  }

  const body = res.json();
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

function createEmail(prefix, index) {
  return `${prefix}-${String(index).padStart(6, '0')}${EMAIL_DOMAIN}`;
}

function buildStages() {
  const stages = RPS_STAGES.map((target) => ({
    target,
    duration: STAGE_DURATION,
  }));

  stages.push({ target: 0, duration: RAMP_DOWN_DURATION });
  return stages;
}

export function handleSummary(data) {
  return koreanSummary(data, 'conversation-list-default');
}
