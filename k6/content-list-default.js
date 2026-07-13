/**
 * 50 RPS -> 100 RPS ... 500 RPS -> 0 RPS 30초 까지 1분씩 부하 테스트
 * 홈 화면에서 가장 빈번하게 호출되는 기본 최신순 콘텐츠 목록조회 성능 확인
 * GET /api/contents로 해당 API만 분리 측정
 **/
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
const START_RPS = Number(__ENV.START_RPS || 50);
const RPS_STAGES = (__ENV.RPS_STAGES || '50,100,200,300,500')
  .split(',')
  .map((value) => Number(value.trim()))
  .filter((value) => Number.isFinite(value) && value >= 0);
const STAGE_DURATION = __ENV.STAGE_DURATION || '1m';
const RAMP_DOWN_DURATION = __ENV.RAMP_DOWN_DURATION || '30s';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 200);
const MAX_VUS = Number(__ENV.MAX_VUS || 2000);
const SETUP_TIMEOUT = __ENV.SETUP_TIMEOUT || '5m';

const contentListOk = new Counter('content_list_ok');

export const options = {
  setupTimeout: SETUP_TIMEOUT,
  scenarios: {
    content_list_default_rate: {
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
    'http_req_failed{api:content-list-default}': ['rate<0.01'],
    'http_req_duration{api:content-list-default}': ['p(95)<1000'],
    'http_reqs{api:content-list-default}': ['count>0'],
  },
};

export function setup() {
  const sessions = [];

  for (let i = 1; i <= USER_COUNT; i += 1) {
    sessions.push(login(createEmail(USER_PREFIX, i)));
  }

  return { sessions };
}

export default function (data) {
  const session = data.sessions[exec.scenario.iterationInTest % data.sessions.length];
  const res = http.get(
    `${BASE_URL}/api/contents?limit=20&sortBy=createdAt&sortDirection=DESCENDING`,
    {
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
      responseCallback: http.expectedStatuses(200),
      tags: {
        api: 'content-list-default',
        name: 'GET /api/contents default',
      },
    }
  );

  if (check(res, { 'content list loaded': (response) => response.status === 200 })) {
    contentListOk.add(1);
  }
}

function login(email) {
  const res = http.post(
    `${BASE_URL}/api/auth/sign-in`,
    {
      username: email,
      password: PASSWORD,
    },
    {
      responseCallback: http.expectedStatuses(200),
      tags: {
        api: 'content-list-login',
        name: 'POST /api/auth/sign-in',
      },
    }
  );

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
  return koreanSummary(data, 'content-list-default');
}
