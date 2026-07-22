import encoding from 'k6/encoding';
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { koreanSummary } from './korean-html-report.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.TEST_USER_PASSWORD || 'k6-password';
const USER_COUNT = Number(__ENV.USER_COUNT || 1000);
const SETUP_LOGIN_BATCH_SIZE = Math.max(1, Number(__ENV.SETUP_LOGIN_BATCH_SIZE || 100));
const EMAIL_DOMAIN = __ENV.EMAIL_DOMAIN || '@moduply.test';
const USER_PREFIX = __ENV.USER_PREFIX || 'k6-user';

const START_RPS = Number(__ENV.START_RPS || 10);
const RPS_STAGES = (__ENV.RPS_STAGES || '10,20,30,50')
  .split(',')
  .map((value) => Number(value.trim()))
  .filter((value) => Number.isFinite(value) && value >= 0);
const STAGE_DURATIONS = (__ENV.STAGE_DURATIONS || '1m,1m,2m,2m')
  .split(',')
  .map((value) => value.trim())
  .filter((value) => value.length > 0);
const RAMP_DOWN_DURATION = __ENV.RAMP_DOWN_DURATION || '30s';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 100);
const MAX_VUS = Number(__ENV.MAX_VUS || 1000);

const IMAGE_PATH = __ENV.IMAGE_PATH;
const IMAGE_MIME_TYPE = __ENV.IMAGE_MIME_TYPE || 'image/jpeg';
const IMAGE_FILE_NAME = __ENV.IMAGE_FILE_NAME || 'profile-test.jpg';

const DEFAULT_PROFILE_IMAGE = encoding.b64decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAwUBAUlS5k8AAAAASUVORK5CYII=',
  'std'
);
const profileImage = IMAGE_PATH ? open(IMAGE_PATH, 'b') : DEFAULT_PROFILE_IMAGE;

const profileUpdated = new Counter('profile_updated');

export const options = {
  scenarios: {
    user_profile_update_rate: {
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
    'http_req_failed{api:user-profile-update}': ['rate<0.01'],
    'http_req_duration{api:user-profile-update}': ['p(95)<2000', 'p(99)<5000'],
    'http_reqs{api:user-profile-update}': ['count>0'],
  },
};

// 테스트 유저 로그인시켜 JWT, CSRF 토큰 확보
export function setup() {
  const users = batchLogin(USER_PREFIX, USER_COUNT);
  return { users };
}

export default function (data) {
  const user = data.users[exec.scenario.iterationInTest % data.users.length];
  const unique = `${Date.now()}-${exec.vu.idInTest}-${exec.scenario.iterationInTest}`;
  const request = {
    name: `k6-profile-${unique}`,
  };

  const res = http.patch(
    `${BASE_URL}/api/users/${user.userId}`,
    {
      request: http.file(JSON.stringify(request), 'request.json', 'application/json'),
      image: http.file(profileImage, IMAGE_FILE_NAME, IMAGE_MIME_TYPE),
    },
    {
      headers: {
        Authorization: `Bearer ${user.accessToken}`,
        Cookie: `XSRF-TOKEN=${user.csrfCookie}`,
        'X-XSRF-TOKEN': user.csrfHeader,
      },
      responseCallback: http.expectedStatuses(200),
      tags: {
        api: 'user-profile-update',
        request_type: 'user-profile-update',
        name: 'PATCH /api/users/{userId}',
      },
    }
  );

  check(res, {
    'profile updated': (response) => response.status === 200,
  });

  if (res.status === 200) {
    profileUpdated.add(1);
  }
}

function buildStages() {
  const stages = RPS_STAGES.map((target, index) => ({
    target,
    duration: STAGE_DURATIONS[index] || STAGE_DURATIONS[STAGE_DURATIONS.length - 1] || '1m',
  }));

  stages.push({ target: 0, duration: RAMP_DOWN_DURATION });
  return stages;
}

function batchLogin(prefix, count) {
  const users = [];

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
    const chunkUsers = responses.map((res, index) =>
      parseLoginResponse(res, emails[index])
    );

    fillMissingCsrfCookies(chunkUsers);
    users.push(...chunkUsers);
  }

  return users;
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
        api: 'profile-update-login',
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
  const csrfCookie = getCookieValue(res, 'XSRF-TOKEN');

  return {
    email,
    userId: body.userDto.id,
    accessToken: body.accessToken,
    csrfCookie,
    csrfHeader: csrfCookie ? decodeURIComponent(csrfCookie) : undefined,
  };
}

function fillMissingCsrfCookies(users) {
  const missingUsers = users.filter((user) => !user.csrfCookie);
  if (missingUsers.length === 0) {
    return;
  }

  const responses = http.batch(
    missingUsers.map((user) => ({
      method: 'GET',
      url: `${BASE_URL}/api/auth/csrf-token`,
      params: {
        headers: {
          Authorization: `Bearer ${user.accessToken}`,
        },
        responseCallback: http.expectedStatuses(200, 204),
        tags: {
          api: 'csrf-token',
          name: 'GET /api/auth/csrf-token',
        },
      },
    }))
  );

  responses.forEach((res, index) => {
    const user = missingUsers[index];
    const csrfCookie = getCookieValue(res, 'XSRF-TOKEN');

    if (!csrfCookie) {
      throw new Error(`XSRF-TOKEN cookie was not issued. email=${user.email}`);
    }

    user.csrfCookie = csrfCookie;
    user.csrfHeader = decodeURIComponent(csrfCookie);
  });
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
  return koreanSummary(data, 'user-profile-update');
}
