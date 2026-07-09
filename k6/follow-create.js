import http from 'k6/http';
import exec from 'k6/execution';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { koreanSummary } from './korean-html-report.js';

// 목적: 여러 follower 사용자가 로그인한 뒤 여러 followee를 대상으로 동시에 팔로우 요청을 보냈을때
// 로그인부터 /api/follows 생성까지의 성능과 실패율을 측정하는 테스트

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.TEST_USER_PASSWORD || 'k6-password';
const FOLLOWER_COUNT = Number(__ENV.FOLLOWER_COUNT || 1000);
const FOLLOWEE_COUNT = Number(__ENV.FOLLOWEE_COUNT || 3000);
const VUS = Number(__ENV.VUS || 30);
const ITERATIONS = Number(__ENV.ITERATIONS || 3000);

const EMAIL_DOMAIN = '@moduply.test';
const followCreated = new Counter('follow_created');

export const options = {
  vus: VUS,
  iterations: ITERATIONS,
  setupTimeout: '10m',
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  const followees = [];

  for (let i = 1; i <= FOLLOWEE_COUNT; i += 1) {
    followees.push(login(createEmail('k6-followee', i), false).userId);
  }

  return { followees };
}

export default function (data) {
  const iteration = exec.scenario.iterationInTest;
  const followerIndex = (iteration % FOLLOWER_COUNT) + 1;
  const follower = login(createEmail('k6-follower', followerIndex), true);
  const followeeId = data.followees[(iteration * 997) % data.followees.length];

  const res = http.post(
    `${BASE_URL}/api/follows`,
    JSON.stringify({ followeeId }),
    {
      headers: {
        Authorization: `Bearer ${follower.accessToken}`,
        Cookie: `XSRF-TOKEN=${follower.csrfCookie}`,
        'X-XSRF-TOKEN': follower.csrfHeader,
        'Content-Type': 'application/json',
      },
      tags: {
        request_type: 'follow-create',
      },
    }
  );

  check(res, {
    'follow created': (r) => r.status === 201,
  });

  if (res.status === 201) {
    followCreated.add(1);
  }

  sleep(1);
}

function login(email, requireCsrf) {
  const res = http.post(
    `${BASE_URL}/api/auth/sign-in`,
    {
      username: email,
      password: PASSWORD,
    },
    {
      tags: {
        request_type: requireCsrf ? 'follower-login' : 'followee-login',
      },
    }
  );

  check(res, {
    'login succeeded': (r) => r.status === 200,
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
      tags: {
        request_type: 'csrf-token',
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
