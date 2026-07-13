import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { koreanSummary } from './korean-html-report.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.TEST_USER_PASSWORD || 'k6-password';
const FOLLOWER_COUNT = Number(__ENV.FOLLOWER_COUNT || 1000);
const FOLLOWEE_COUNT = Number(__ENV.FOLLOWEE_COUNT || 3000);
const EMAIL_DOMAIN = __ENV.EMAIL_DOMAIN || '@moduply.test';
const FOLLOWER_PREFIX = __ENV.FOLLOWER_PREFIX || 'k6-follower';
const FOLLOWEE_PREFIX = __ENV.FOLLOWEE_PREFIX || 'k6-followee';

const START_RPS = Number(__ENV.START_RPS || 10);
const RPS_STAGES = (__ENV.RPS_STAGES || '10,30,50,100,150,200')
  .split(',')
  .map((value) => Number(value.trim()))
  .filter((value) => Number.isFinite(value) && value >= 0);
const STAGE_DURATION = __ENV.STAGE_DURATION || '1m';
const RAMP_DOWN_DURATION = __ENV.RAMP_DOWN_DURATION || '30s';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 200);
const MAX_VUS = Number(__ENV.MAX_VUS || 2000);

const followCreated = new Counter('follow_created');
const followAlreadyExists = new Counter('follow_already_exists');

export const options = {
  scenarios: {
    follow_create_rate: {
      executor: 'ramping-arrival-rate',
      startRate: START_RPS,
      timeUnit: '1s',
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      stages: buildStages(),
    },
  },
  setupTimeout: '30m',
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_failed{api:follow-create}': ['rate<0.01'],
    'http_req_duration{api:follow-create}': ['p(95)<1000'],
    'http_reqs{api:follow-create}': ['count>0'],
  },
};

export function setup() {
  const followers = [];
  const followees = [];

  for (let i = 1; i <= FOLLOWER_COUNT; i += 1) {
    followers.push(login(createEmail(FOLLOWER_PREFIX, i), true));
  }

  for (let i = 1; i <= FOLLOWEE_COUNT; i += 1) {
    followees.push(login(createEmail(FOLLOWEE_PREFIX, i), false).userId);
  }

  return { followers, followees };
}

export default function (data) {
  const iteration = exec.scenario.iterationInTest;
  const pair = pickUniquePair(iteration, data.followers, data.followees);

  const res = http.post(
    `${BASE_URL}/api/follows`,
    JSON.stringify({ followeeId: pair.followeeId }),
    {
      headers: {
        Authorization: `Bearer ${pair.follower.accessToken}`,
        Cookie: `XSRF-TOKEN=${pair.follower.csrfCookie}`,
        'X-XSRF-TOKEN': pair.follower.csrfHeader,
        'Content-Type': 'application/json',
      },
      responseCallback: http.expectedStatuses(201),
      tags: {
        api: 'follow-create',
        request_type: 'follow-create',
        name: 'POST /api/follows',
      },
    }
  );

  check(res, {
    'follow created': (response) => response.status === 201,
  });

  if (res.status === 201) {
    followCreated.add(1);
  }

  if (res.status === 409) {
    followAlreadyExists.add(1);
  }
}

function pickUniquePair(iteration, followers, followees) {
  const capacity = followers.length * followees.length;
  if (iteration >= capacity) {
    throw new Error(
      `Unique follow pair exhausted. iteration=${iteration}, capacity=${capacity}. ` +
      'Increase FOLLOWER_COUNT/FOLLOWEE_COUNT or shorten the RPS scenario.'
    );
  }

  const followerIndex = iteration % followers.length;
  const followeeIndex = Math.floor(iteration / followers.length) % followees.length;

  return {
    follower: followers[followerIndex],
    followeeId: followees[followeeIndex],
  };
}

function buildStages() {
  const stages = RPS_STAGES.map((target) => ({
    target,
    duration: STAGE_DURATION,
  }));

  stages.push({ target: 0, duration: RAMP_DOWN_DURATION });
  return stages;
}

function login(email, requireCsrf) {
  const res = http.post(
    `${BASE_URL}/api/auth/sign-in`,
    {
      username: email,
      password: PASSWORD,
    },
    {
      responseCallback: http.expectedStatuses(200),
      tags: {
        api: requireCsrf ? 'follower-login' : 'followee-login',
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
  let csrfCookie = getCookieValue(res, 'XSRF-TOKEN');
  if (requireCsrf && !csrfCookie) {
    const csrfRes = http.get(`${BASE_URL}/api/auth/csrf-token`, {
      headers: {
        Authorization: `Bearer ${body.accessToken}`,
      },
      responseCallback: http.expectedStatuses(200, 204),
      tags: {
        api: 'csrf-token',
        name: 'GET /api/auth/csrf-token',
      },
    });
    csrfCookie = getCookieValue(csrfRes, 'XSRF-TOKEN');
  }

  if (requireCsrf && !csrfCookie) {
    throw new Error(`XSRF-TOKEN cookie was not issued. email=${email}`);
  }

  return {
    userId: body.userDto.id,
    accessToken: body.accessToken,
    csrfCookie,
    csrfHeader: csrfCookie ? decodeURIComponent(csrfCookie) : undefined,
  };
}

function createEmail(prefix, index) {
  return `${prefix}-${String(index).padStart(6, '0')}${EMAIL_DOMAIN}`;
}

function getCookieValue(res, name) {
  const responseCookie = res.cookies[name]?.[0]?.value;
  if (responseCookie) {
    return responseCookie;
  }

  const jarCookie = http.cookieJar().cookiesForURL(BASE_URL)[name]?.[0];
  if (jarCookie) {
    return jarCookie;
  }

  return undefined;
}

export function handleSummary(data) {
  return koreanSummary(data, 'follow-create');
}
