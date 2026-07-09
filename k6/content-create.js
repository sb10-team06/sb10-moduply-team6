import encoding from 'k6/encoding';     // encoding: base64 데이터를 바이너리로 변환
import http from 'k6/http';             // HTTP 요청을 보내는 모듈
import { Counter } from 'k6/metrics';
import { check, sleep } from 'k6';      // check: 응답이 정상인지 검증
import { koreanSummary } from './korean-html-report.js';

// 환경변수
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN;                          // 직접 주입할 JWT Access Token
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL;
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;
const VUS = Number(__ENV.VUS || 1);                 // 관리자 1명이 연속 등록하는 현실 시나리오
const ITERATIONS = Number(__ENV.ITERATIONS || 1000);  // 기본: 콘텐츠 1000건 등록
const TARGET_VUS = Number(__ENV.TARGET_VUS || 0);
const RAMP_DURATION = __ENV.RAMP_DURATION || '5m';
const HOLD_DURATION = __ENV.HOLD_DURATION || '0s';
const THINK_TIME_SECONDS = Number(__ENV.THINK_TIME_SECONDS ?? 1);

const contentCreated = new Counter('content_created');

// Base64 문자열을 PNG 이미지로 바꾼다.
// 콘텐츠 썸네일 업로드 테스트 이미지
const THUMBNAIL = encoding.b64decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAwUBAUlS5k8AAAAASUVORK5CYII=',
  'std'
);

// K6 설정
export const options = TARGET_VUS > 0 ? {
  scenarios: {
    content_create_ramp: {
      executor: 'ramping-vus',
      stages: [
        { duration: RAMP_DURATION, target: TARGET_VUS },
        { duration: HOLD_DURATION, target: TARGET_VUS },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
} : {
  vus: VUS,
  iterations: ITERATIONS,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],

  // 실패율이 1% 미만이어야한다.
  thresholds: {
    http_req_failed: ['rate<0.01'], // 실패율 1% 보다 높으면 테스트 실패.
  },
};

// setup(): VU마다 실행 X, 전체 테스트 시작전에 한번만 실행
export function setup() {
  const auth = ACCESS_TOKEN ? authenticateWithToken(ACCESS_TOKEN) : loginAdmin();

  return {      // 모든 Virtual User가 공유
    accessToken: auth.accessToken,
    csrfCookie: auth.csrfCookie,
    csrfHeader: auth.csrfHeader,
  };
}

function authenticateWithToken(accessToken) {
  // GET /api/contents?limit=1 요청 -> Spring Security가 응답에 Set-Cookie, XSRF-TOKEN 내려주기때문
  const res = http.get(`${BASE_URL}/api/contents?limit=1`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  const csrfCookie = getCookieValue(res, 'XSRF-TOKEN');
  if (!csrfCookie) {
    throw new Error(`XSRF-TOKEN cookie was not issued. status=${res.status}`);
  }

  return {
    accessToken,
    csrfCookie,
    csrfHeader: decodeURIComponent(csrfCookie),
  };
}

function loginAdmin() {
  if (!ADMIN_EMAIL || !ADMIN_PASSWORD) {
    throw new Error('ACCESS_TOKEN or ADMIN_EMAIL/ADMIN_PASSWORD is required for content create test.');
  }

  const res = http.post(
    `${BASE_URL}/api/auth/sign-in`,
    {
      username: ADMIN_EMAIL,
      password: ADMIN_PASSWORD,
    },
    {
      tags: {
        request_type: 'admin-login',
      },
    }
  );

  check(res, {
    'login succeeded': (r) => r.status === 200,
  });

  if (res.status !== 200) {
    throw new Error(`Admin login failed. status=${res.status}, body=${res.body}`);
  }

  const body = res.json();
  let csrfCookie = getCookieValue(res, 'XSRF-TOKEN');
  if (!csrfCookie) {
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

  if (!csrfCookie) {
    throw new Error('XSRF-TOKEN cookie was not issued for admin user.');
  }

  return {
    accessToken: body.accessToken,
    csrfCookie,
    csrfHeader: decodeURIComponent(csrfCookie),
  };
}

// 모든 Virtual User가 반복 실행
export default function (data) {                       // data: setup()의 return값 csrfCookie, csrfHeader
  const unique = `${Date.now()}-${__VU}-${__ITER}`;
  const request = {
    type: 'movie',
    title: `k6-content-${unique}`,
    description: 'k6 content creation performance test',
    tags: ['k6', 'performance'],
  };

  // POST /api/contents: 콘텐츠 생성 요청
  const res = http.post(
    `${BASE_URL}/api/contents`,
    {
      request: http.file(JSON.stringify(request), 'request.json', 'application/json'),
      thumbnail: http.file(THUMBNAIL, `thumbnail-${unique}.png`, 'image/png'),
    },
    {
      headers: {
        Authorization: `Bearer ${data.accessToken}`,
        Cookie: `XSRF-TOKEN=${data.csrfCookie}`,
        'X-XSRF-TOKEN': data.csrfHeader,
      },
    }
  );

  // 201 Created가 왔는지 검사.
  check(res, {
    'content created': (r) => r.status === 201,
  });

  if (res.status === 201) {
    contentCreated.add(1);
  }

  if (THINK_TIME_SECONDS > 0) {
    sleep(THINK_TIME_SECONDS);
  }
}

export function handleSummary(data) {
  return koreanSummary(data, 'content-create');
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
