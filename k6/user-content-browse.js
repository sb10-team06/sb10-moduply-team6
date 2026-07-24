/**
 * 운영 사용자 시나리오: 콘텐츠 탐색
 *
 * 로그인 -> 콘텐츠 목록 -> 콘텐츠 상세 -> 리뷰 목록
 *
 * 모든 본 테스트 요청은 조회 전용이다. 운영에서는 낮은 RPS로 시작하고,
 * 테스트 전용 계정만 사용한다.
 */
import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail, group, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { koreanSummary } from './korean-html-report.js';

const BASE_URL = (__ENV.BASE_URL || 'https://moduply.co.kr').replace(/\/+$/, '');
const EMAIL_TEMPLATE = __ENV.TEST_USER_EMAIL_TEMPLATE || '';
const PASSWORD_TEMPLATE = __ENV.TEST_USER_PASSWORD_TEMPLATE || '';
const USER_COUNT = positiveNumber(__ENV.USER_COUNT, 20);
const USER_START_INDEX = positiveNumber(__ENV.USER_START_INDEX, 1);
const SETUP_TIMEOUT = __ENV.SETUP_TIMEOUT || '5m';

const CONTENT_LIMIT = positiveNumber(__ENV.CONTENT_LIMIT, 20);
const REVIEW_LIMIT = positiveNumber(__ENV.REVIEW_LIMIT, 20);
const HOT_CONTENT_COUNT = positiveNumber(__ENV.HOT_CONTENT_COUNT, 10);
const HOT_CONTENT_RATIO = ratio(__ENV.HOT_CONTENT_RATIO, 0.8);

const START_RPS = nonNegativeNumber(__ENV.START_RPS, 1);
const RPS_STAGES = parseRpsStages(__ENV.RPS_STAGES || '1,2,5');
const STAGE_DURATION = __ENV.STAGE_DURATION || '1m';
const RAMP_DOWN_DURATION = __ENV.RAMP_DOWN_DURATION || '30s';
const PRE_ALLOCATED_VUS = positiveNumber(__ENV.PRE_ALLOCATED_VUS, 20);
const MAX_VUS = positiveNumber(__ENV.MAX_VUS, 100);
const THINK_TIME_MIN = nonNegativeNumber(__ENV.THINK_TIME_MIN, 0.2);
const THINK_TIME_MAX = nonNegativeNumber(__ENV.THINK_TIME_MAX, 0.8);

const journeyFailed = new Rate('content_browse_journey_failed');
const contentListSchemaInvalid = new Rate('content_list_schema_invalid');
const contentDetailSchemaInvalid = new Rate('content_detail_schema_invalid');
const reviewListSchemaInvalid = new Rate('review_list_schema_invalid');

export const options = {
  setupTimeout: SETUP_TIMEOUT,
  scenarios: {
    user_content_browse: {
      executor: 'ramping-arrival-rate',
      startRate: START_RPS,
      timeUnit: '1s',
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      stages: buildStages(),
      tags: {
        scenario: 'user-content-browse',
      },
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: [
      {
        threshold: 'rate<0.01',
        abortOnFail: true,
        delayAbortEval: '30s',
      },
    ],
    checks: ['rate>0.99'],
    dropped_iterations: ['count==0'],
    content_browse_journey_failed: ['rate<0.01'],
    content_list_schema_invalid: ['rate<0.01'],
    content_detail_schema_invalid: ['rate<0.01'],
    review_list_schema_invalid: ['rate<0.01'],
    'http_req_failed{api:setup-login}': ['rate<0.01'],
    'http_reqs{api:setup-login}': ['count>0'],
    'http_req_duration{api:setup-login}': ['p(95)<1000'],
    'http_req_failed{api:content-list}': ['rate<0.01'],
    'http_reqs{api:content-list}': ['count>0'],
    'http_req_duration{api:content-list}': ['p(95)<1000', 'p(99)<2000'],
    'http_req_failed{api:content-detail}': ['rate<0.01'],
    'http_reqs{api:content-detail}': ['count>0'],
    'http_req_duration{api:content-detail}': ['p(95)<700', 'p(99)<1500'],
    'http_req_failed{api:review-list}': ['rate<0.01'],
    'http_reqs{api:review-list}': ['count>0'],
    'http_req_duration{api:review-list}': ['p(95)<1000', 'p(99)<2000'],
  },
};

export function setup() {
  validateCredentialTemplates();
  const sessions = loginUsers();
  if (sessions.length === 0) {
    fail('인증된 테스트 세션을 생성하지 못했습니다.');
  }

  return { sessions };
}

export default function (data) {
  const session = data.sessions[exec.scenario.iterationInTest % data.sessions.length];
  const headers = authHeaders(session.accessToken);
  let journeyOk = true;

  group('콘텐츠 탐색 사용자 여정', () => {
    const listResponse = http.get(
      `${BASE_URL}/api/contents?limit=${CONTENT_LIMIT}&sortBy=rate&sortDirection=DESCENDING`,
      requestParams(headers, 'content-list', 'GET /api/contents')
    );
    const listBody = parseJson(listResponse);
    const listSchemaOk = hasCursorResponseShape(listBody);

    const listOk = check(listResponse, {
      '콘텐츠 목록 조회가 성공한다.': (response) => response.status === 200,
      '콘텐츠 목록이 CursorResponse 형식이다.': () => listSchemaOk,
      '콘텐츠 목록에 조회할 콘텐츠가 있다.': () =>
        listSchemaOk && listBody.data.length > 0,
    });
    contentListSchemaInvalid.add(!listSchemaOk);
    journeyOk = journeyOk && listOk;

    if (!listSchemaOk || listBody.data.length === 0) {
      return;
    }

    think();

    const content = selectContent(listBody.data);
    const detailResponse = http.get(
      `${BASE_URL}/api/contents/${encodeURIComponent(content.id)}`,
      requestParams(headers, 'content-detail', 'GET /api/contents/{contentId}')
    );
    const detailBody = parseJson(detailResponse);
    const detailSchemaOk =
      detailBody !== null &&
      typeof detailBody === 'object' &&
      detailBody.id === content.id;

    const detailOk = check(detailResponse, {
      '콘텐츠 상세 조회가 성공한다.': (response) => response.status === 200,
      '콘텐츠 상세 ID가 요청 ID와 일치한다.': () => detailSchemaOk,
    });
    contentDetailSchemaInvalid.add(!detailSchemaOk);
    journeyOk = journeyOk && detailOk;

    think();

    const reviewResponse = http.get(
      `${BASE_URL}/api/reviews?contentId=${encodeURIComponent(content.id)}` +
        `&limit=${REVIEW_LIMIT}&sortBy=createdAt&sortDirection=DESCENDING`,
      requestParams(headers, 'review-list', 'GET /api/reviews')
    );
    const reviewBody = parseJson(reviewResponse);
    const reviewSchemaOk = hasCursorResponseShape(reviewBody);

    const reviewOk = check(reviewResponse, {
      '리뷰 목록 조회가 성공한다.': (response) => response.status === 200,
      '리뷰 목록이 CursorResponse 형식이다.': () => reviewSchemaOk,
    });
    reviewListSchemaInvalid.add(!reviewSchemaOk);
    journeyOk = journeyOk && reviewOk;
  });

  journeyFailed.add(!journeyOk);
}

function loginUsers() {
  const sessions = [];

  for (let offset = 0; offset < USER_COUNT; offset += 1) {
    const userIndex = USER_START_INDEX + offset;
    const credential = createCredential(userIndex);
    sessions.push(login(credential.email, credential.password));
  }

  return sessions;
}

function login(email, password) {
  const response = http.post(
    `${BASE_URL}/api/auth/sign-in`,
    {
      username: email,
      password,
    },
    {
      responseCallback: http.expectedStatuses(200),
      tags: {
        api: 'setup-login',
        name: 'POST /api/auth/sign-in',
      },
    }
  );
  const body = parseJson(response);
  const loginOk = check(response, {
    '콘텐츠 시나리오 테스트 계정 로그인이 성공한다.': (res) =>
      res.status === 200 &&
      typeof body?.accessToken === 'string' &&
      typeof body?.userDto?.id === 'string',
  });

  if (!loginOk) {
    fail(
      `로그인에 실패했습니다. email=${email}, status=${response.status}, body=${response.body}`
    );
  }

  return {
    userId: body.userDto.id,
    accessToken: body.accessToken,
  };
}

function selectContent(contents) {
  const hotCount = Math.min(HOT_CONTENT_COUNT, contents.length);
  const candidates =
    Math.random() < HOT_CONTENT_RATIO ? contents.slice(0, hotCount) : contents;
  return candidates[Math.floor(Math.random() * candidates.length)];
}

function authHeaders(accessToken) {
  return {
    Authorization: `Bearer ${accessToken}`,
    Accept: 'application/json',
  };
}

function requestParams(headers, api, name) {
  return {
    headers,
    responseCallback: http.expectedStatuses(200),
    tags: { api, name },
  };
}

function parseJson(response) {
  try {
    return response.json();
  } catch {
    return null;
  }
}

function hasCursorResponseShape(body) {
  return (
    body !== null &&
    typeof body === 'object' &&
    Array.isArray(body.data) &&
    typeof body.hasNext === 'boolean' &&
    typeof body.totalCount === 'number'
  );
}

function think() {
  const upper = Math.max(THINK_TIME_MIN, THINK_TIME_MAX);
  sleep(THINK_TIME_MIN + Math.random() * (upper - THINK_TIME_MIN));
}

function validateCredentialTemplates() {
  if (!EMAIL_TEMPLATE.includes('{index}') || !PASSWORD_TEMPLATE.includes('{index}')) {
    fail(
      'TEST_USER_EMAIL_TEMPLATE과 TEST_USER_PASSWORD_TEMPLATE에 {index}를 포함해 주세요.'
    );
  }
}

function createCredential(index) {
  return {
    email: applyIndexTemplate(EMAIL_TEMPLATE, index),
    password: applyIndexTemplate(PASSWORD_TEMPLATE, index),
  };
}

function applyIndexTemplate(template, index) {
  return template.split('{index}').join(String(index));
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

  return stages.length > 0 ? stages : [1, 2, 5];
}

function positiveNumber(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function nonNegativeNumber(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback;
}

function ratio(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 && parsed <= 1 ? parsed : fallback;
}

export function handleSummary(data) {
  return koreanSummary(data, 'user-content-browse');
}
