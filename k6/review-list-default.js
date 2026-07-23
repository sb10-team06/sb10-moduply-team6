import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { koreanSummary } from './korean-html-report.js';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const PASSWORD = __ENV.TEST_USER_PASSWORD || 'k6-password';
const USER_PREFIX = __ENV.USER_PREFIX || 'k6-review';
const EMAIL_DOMAIN = __ENV.EMAIL_DOMAIN || '@moduply.test';
const USER_COUNT = positiveNumber(__ENV.USER_COUNT, 1000);
const SETUP_LOGIN_BATCH_SIZE = positiveNumber(__ENV.SETUP_LOGIN_BATCH_SIZE, 100);
const SETUP_TIMEOUT = __ENV.SETUP_TIMEOUT || '10m';

const LIMIT = positiveNumber(__ENV.LIMIT, 20);
const SORT_BY = __ENV.SORT_BY || 'createdAt';
const SORT_DIRECTION = __ENV.SORT_DIRECTION || 'DESCENDING';
const CONTENT_POOL_SIZE = positiveNumber(__ENV.CONTENT_POOL_SIZE, 100);
const CONTENT_POOL_SORT_BY = __ENV.CONTENT_POOL_SORT_BY || 'watcherCount';
const REVIEW_LIST_MODE = (__ENV.REVIEW_LIST_MODE || 'mixed').toLowerCase();
const HOT_CONTENT_COUNT = positiveNumber(__ENV.HOT_CONTENT_COUNT, 10);
const MIXED_HOT_RATIO = ratioNumber(__ENV.MIXED_HOT_RATIO, 0.8);

const START_RPS = positiveNumber(__ENV.START_RPS, 50);
const RPS_STAGES = parseRpsStages(__ENV.RPS_STAGES || '50,100,200,300,500');
const STAGE_DURATIONS = parseDurations(__ENV.STAGE_DURATIONS);
const STAGE_DURATION = __ENV.STAGE_DURATION || '1m';
const RAMP_DOWN_DURATION = __ENV.RAMP_DOWN_DURATION || '30s';
const PRE_ALLOCATED_VUS = positiveNumber(__ENV.PRE_ALLOCATED_VUS, 300);
const MAX_VUS = positiveNumber(__ENV.MAX_VUS, 2000);      //목표 RPS를 위한 동시 실행 VU수

const reviewListOk = new Counter('review_list_ok');
const reviewListSchemaInvalid = new Rate('review_list_schema_invalid');

export const options = {
  setupTimeout: SETUP_TIMEOUT,
  scenarios: {
    review_list_default_rate: {
      executor: 'ramping-arrival-rate',
      startRate: START_RPS,
      timeUnit: '1s',
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      stages: buildStages(),
      tags: {
        scenario: 'review-list-default',
      },
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_failed{api:review-list-default}': ['rate<0.01'],
    'http_req_duration{api:review-list-default}': ['p(95)<1000', 'p(99)<3000'],
    'http_reqs{api:review-list-default}': ['count>0'],
    dropped_iterations: ['count==0'],
    review_list_schema_invalid: ['rate<0.01'],
  },
};

// USER_COUNT 만큼 로그인
export function setup() {
  validateOptions();

  const sessions = batchLogin();
  if (sessions.length === 0) {
    fail('No authenticated sessions were created.');
  }

  const contentIds = resolveContentIds(sessions[0].accessToken);
  // watcherCount로 조회된 10개 콘텐츠 조회
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

  const res = http.get(`${BASE_URL}/api/reviews${queryString(selection.contentId)}`, {
    headers: {
      Authorization: `Bearer ${session.accessToken}`,
      Accept: 'application/json',
    },
    responseCallback: http.expectedStatuses(200),
    tags: {
      api: 'review-list-default',
      review_list_mode: selection.mode,
      sort_by: SORT_BY,
      name: 'GET /api/reviews',
    },
  });

  const schemaValid = hasCursorResponseShape(res);
  const ok = check(res, {
    'review list loaded': (response) => response.status === 200,
    'review list response has cursor shape': () => schemaValid,
  });

  if (ok) {
    reviewListOk.add(1);
  }

  reviewListSchemaInvalid.add(!schemaValid);
}

function resolveContentIds(accessToken) {
  const explicitContentIds = parseCsv(__ENV.CONTENT_IDS);
  if (explicitContentIds.length > 0) {
    return explicitContentIds;
  }

  const res = http.get(
    `${BASE_URL}/api/contents?limit=${CONTENT_POOL_SIZE}&sortBy=${CONTENT_POOL_SORT_BY}&sortDirection=DESCENDING`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        Accept: 'application/json',
      },
      responseCallback: http.expectedStatuses(200),
      tags: {
        api: 'review-list-setup-content-list',
        name: 'GET /api/contents setup',
      },
    }
  );

  if (res.status !== 200) {
    fail(`Content pool request failed. status=${res.status}, body=${res.body}`);
  }

  const contentIds = (res.json('data') || [])
    .map((content) => content.id)
    .filter((id) => typeof id === 'string' && id.length > 0);

  if (contentIds.length === 0) {
    fail('No content IDs found. Set CONTENT_IDS or seed contents before running this test.');
  }

  return contentIds;
}

function selectContentId(data) {
  if (REVIEW_LIST_MODE === 'hot') {
    return {
      contentId: pickByIteration(data.hotContentIds),
      mode: 'hot',
    };
  }

  if (REVIEW_LIST_MODE === 'random') {
    return {
      contentId: pickRandom(data.contentIds),
      mode: 'random',
    };
  }

  if (Math.random() < MIXED_HOT_RATIO) {
    return {
      contentId: pickByIteration(data.hotContentIds),
      mode: 'hot',
    };
  }

  return {
    contentId: pickRandom(data.contentIds),
    mode: 'random',
  };
}

function queryString(contentId) {
  const params = [
    ['contentId', contentId],
    ['limit', LIMIT],
    ['sortBy', SORT_BY],
    ['sortDirection', SORT_DIRECTION],
  ];

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
        api: 'review-list-login',
        name: 'POST /api/auth/sign-in',
      },
    },
  };
}

function parseLoginResponse(res, email) {
  const loginOk = check(res, {
    'login succeeded': (response) => response.status === 200,
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

function buildStages() {
  const stages = RPS_STAGES.map((target, index) => ({
    target,
    duration: STAGE_DURATIONS[index] || STAGE_DURATION,
  }));

  stages.push({
    target: 0,
    duration: RAMP_DOWN_DURATION,
  });

  return stages;
}

function validateOptions() {
  if (!['createdAt', 'rating'].includes(SORT_BY)) {
    fail(`Invalid SORT_BY=${SORT_BY}. Use createdAt or rating.`);
  }

  if (!['ASCENDING', 'DESCENDING'].includes(SORT_DIRECTION)) {
    fail(`Invalid SORT_DIRECTION=${SORT_DIRECTION}. Use ASCENDING or DESCENDING.`);
  }

  if (!['hot', 'random', 'mixed'].includes(REVIEW_LIST_MODE)) {
    fail(`Invalid REVIEW_LIST_MODE=${REVIEW_LIST_MODE}. Use hot, random, or mixed.`);
  }

  if (!['createdAt', 'rate', 'watcherCount'].includes(CONTENT_POOL_SORT_BY)) {
    fail(`Invalid CONTENT_POOL_SORT_BY=${CONTENT_POOL_SORT_BY}. Use createdAt, rate, or watcherCount.`);
  }
}

function createEmail(index) {
  return `${USER_PREFIX}-${String(index).padStart(6, '0')}${EMAIL_DOMAIN}`;
}

function pickByIteration(values) {
  return values[exec.scenario.iterationInTest % values.length];
}

function pickRandom(values) {
  return values[Math.floor(Math.random() * values.length)];
}

function parseCsv(value) {
  if (!value) {
    return [];
  }

  return value
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function parseRpsStages(value) {
  const stages = parseCsv(value)
    .map((stage) => Number(stage))
    .filter((stage) => Number.isFinite(stage) && stage >= 0);

  return stages.length > 0 ? stages : [50, 100, 200, 300, 500];
}

function parseDurations(value) {
  return parseCsv(value);
}

function positiveNumber(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function ratioNumber(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 && parsed <= 1 ? parsed : fallback;
}

export function handleSummary(data) {
  return koreanSummary(data, 'review-list-default');
}
