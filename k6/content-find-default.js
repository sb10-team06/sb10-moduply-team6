/**
 * 콘텐츠 단건 조회 성능 테스트
 *
 * 대상 API: GET /api/contents/{contentId}
 *
 * 테스트 모드
 * - CONTENT_FIND_MODE=popular: 인기 콘텐츠 소수에 요청 집중
 * - CONTENT_FIND_MODE=random: 콘텐츠 풀 전체에 요청 분산
 * - CONTENT_FIND_MODE=mixed: 80% 인기 콘텐츠, 20% 랜덤 콘텐츠
 *
 * Docker Compose 실행 예시
 * docker compose -f docker-compose-k6.yml run --rm --service-ports `
 *   -e BASE_URL=http://host.docker.internal:8080 `
 *   -e USER_COUNT=1000 `
 *   -e CONTENT_FIND_MODE=mixed `
 *   -e RPS_STAGES=100,300,500 `
 *   -e K6_KO_HTML_REPORT=/scripts/reports/content/content-find-default.html `
 *   k6 run /scripts/content-find-default.js
 */
import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';
import { koreanSummary } from './korean-html-report.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.TEST_USER_PASSWORD || 'k6-password';
const USER_COUNT = Number(__ENV.USER_COUNT || 1000);
const USER_PREFIX = __ENV.USER_PREFIX || 'k6-follower';
const EMAIL_DOMAIN = __ENV.EMAIL_DOMAIN || '@moduply.test';
const START_RPS = Number(__ENV.START_RPS || 100);
const RPS_STAGES = (__ENV.RPS_STAGES || '100,300,500')
  .split(',')
  .map((value) => Number(value.trim()))
  .filter((value) => Number.isFinite(value) && value >= 0);
const STAGE_DURATION = __ENV.STAGE_DURATION || '1m';
const RAMP_DOWN_DURATION = __ENV.RAMP_DOWN_DURATION || '30s';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 300);
const MAX_VUS = Number(__ENV.MAX_VUS || 3000);
const SETUP_TIMEOUT = __ENV.SETUP_TIMEOUT || '5m';
const LOGIN_BATCH_SIZE = Number(__ENV.LOGIN_BATCH_SIZE || 50);
const CONTENT_POOL_SIZE = Number(__ENV.CONTENT_POOL_SIZE || 100);
const CONTENT_POOL_SORT_BY = __ENV.CONTENT_POOL_SORT_BY || 'rate';
const HOT_CONTENT_COUNT = Number(__ENV.HOT_CONTENT_COUNT || 10);
const CONTENT_FIND_MODE = (__ENV.CONTENT_FIND_MODE || 'mixed').toLowerCase();
const MIXED_POPULAR_RATIO = Number(__ENV.MIXED_POPULAR_RATIO || 0.8);

const contentFindOk = new Counter('content_find_ok');

export const options = {
  setupTimeout: SETUP_TIMEOUT,
  scenarios: {
    content_find_default_rate: {
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
    'http_req_failed{api:content-find-default}': ['rate<0.01'],
    'http_req_duration{api:content-find-default}': ['p(95)<500'],
    'http_reqs{api:content-find-default}': ['count>0'],
  },
};

export function setup() {
  validateMode();

  const sessions = loginUsers();
  // 조회할 콘텐츠 ID 확보
  const contentIds = resolveContentIds(sessions[0].accessToken);
  const hotContentIds = contentIds.slice(0, Math.max(1, Math.min(HOT_CONTENT_COUNT, contentIds.length)));

  return {
    sessions,
    contentIds,
    hotContentIds,
  };
}

export default function (data) {
  const session = data.sessions[exec.scenario.iterationInTest % data.sessions.length];
  const selection = selectContentId(data);
  const res = http.get(
    `${BASE_URL}/api/contents/${selection.contentId}`,
    {
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
      responseCallback: http.expectedStatuses(200),
      tags: {
        api: 'content-find-default',
        find_mode: selection.mode,
        name: 'GET /api/contents/{contentId}',
      },
    }
  );

  if (check(res, {
    'content detail loaded': (response) => response.status === 200,
    'content id matched': (response) => response.status === 200 && response.json('id') === selection.contentId,
  })) {
    contentFindOk.add(1);
  }
}

function resolveContentIds(accessToken) {
  const explicitContentIds = parseContentIds(__ENV.CONTENT_IDS);
  if (explicitContentIds.length > 0) {
    return explicitContentIds;
  }

  const res = http.get(
    `${BASE_URL}/api/contents?limit=${CONTENT_POOL_SIZE}&sortBy=${CONTENT_POOL_SORT_BY}&sortDirection=DESCENDING`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      responseCallback: http.expectedStatuses(200),
      tags: {
        api: 'content-find-setup-list',
        name: 'GET /api/contents setup',
      },
    }
  );

  check(res, {
    'content pool loaded': (response) => response.status === 200,
  });

  if (res.status !== 200) {
    throw new Error(`Content pool request failed. status=${res.status}, body=${res.body}`);
  }

  const contentIds = (res.json('data') || [])
    .map((content) => content.id)
    .filter((id) => typeof id === 'string' && id.length > 0);

  if (contentIds.length === 0) {
    throw new Error('No content IDs found. Set CONTENT_IDS or seed contents before running this test.');
  }

  return contentIds;
}

function selectContentId(data) {
  if (CONTENT_FIND_MODE === 'popular') {
    return {
      contentId: pickByIteration(data.hotContentIds),
      mode: 'popular',
    };
  }

  if (CONTENT_FIND_MODE === 'random') {
    return {
      contentId: pickRandom(data.contentIds),
      mode: 'random',
    };
  }

  if (Math.random() < MIXED_POPULAR_RATIO) {
    return {
      contentId: pickByIteration(data.hotContentIds),
      mode: 'popular',
    };
  }

  return {
    contentId: pickRandom(data.contentIds),
    mode: 'random',
  };
}

function pickByIteration(values) {
  return values[exec.scenario.iterationInTest % values.length];
}

function pickRandom(values) {
  return values[Math.floor(Math.random() * values.length)];
}

// LOGIN_bATCH_SIZE 만큼 묶어서 병렬 로그인하는 함수
function loginUsers() {
  const sessions = [];

  for (let start = 1; start <= USER_COUNT; start += LOGIN_BATCH_SIZE) {
    const end = Math.min(start + LOGIN_BATCH_SIZE - 1, USER_COUNT);
    const requests = [];

    for (let i = start; i <= end; i += 1) {
      const email = createEmail(USER_PREFIX, i);
      requests.push({
        method: 'POST',
        url: `${BASE_URL}/api/auth/sign-in`,
        body: {
          username: email,
          password: PASSWORD,
        },
        params: {
          responseCallback: http.expectedStatuses(200),
          tags: {
            api: 'content-find-login',
            name: 'POST /api/auth/sign-in',
          },
        },
      });
    }

    const responses = http.batch(requests);
    responses.forEach((res, index) => {
      const email = createEmail(USER_PREFIX, start + index);
      check(res, {
        'login succeeded': (response) => response.status === 200,
      });

      if (res.status !== 200) {
        throw new Error(`Login failed. email=${email}, status=${res.status}, body=${res.body}`);
      }

      const body = res.json();
      sessions.push({
        userId: body.userDto.id,
        accessToken: body.accessToken,
      });
    });
  }

  return sessions;
}

function parseContentIds(value) {
  if (!value) {
    return [];
  }

  return value
    .split(',')
    .map((id) => id.trim())
    .filter((id) => id.length > 0);
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

function validateMode() {
  if (!['popular', 'random', 'mixed'].includes(CONTENT_FIND_MODE)) {
    throw new Error(`Invalid CONTENT_FIND_MODE=${CONTENT_FIND_MODE}. Use popular, random, or mixed.`);
  }

  if (!Number.isFinite(LOGIN_BATCH_SIZE) || LOGIN_BATCH_SIZE <= 0) {
    throw new Error(`Invalid LOGIN_BATCH_SIZE=${LOGIN_BATCH_SIZE}. Use a positive number.`);
  }

  if (!['createdAt', 'rate', 'watcherCount'].includes(CONTENT_POOL_SORT_BY)) {
    throw new Error(`Invalid CONTENT_POOL_SORT_BY=${CONTENT_POOL_SORT_BY}. Use createdAt, rate, or watcherCount.`);
  }

  if (MIXED_POPULAR_RATIO < 0 || MIXED_POPULAR_RATIO > 1) {
    throw new Error(`Invalid MIXED_POPULAR_RATIO=${MIXED_POPULAR_RATIO}. Use a value between 0 and 1.`);
  }
}

export function handleSummary(data) {
  return koreanSummary(data, 'content-find-default');
}
