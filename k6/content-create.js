import encoding from 'k6/encoding';     // encoding: base64 데이터를 바이너리로 변환
import http from 'k6/http';             // HTTP 요청을 보내는 모듈
import { check, sleep } from 'k6';      // check: 응답이 정상인지 검증

// 환경변수
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN;                          // JWT Access Token
const VUS = Number(__ENV.VUS || 5);                 // Virtual User 수(동시 실행 사용자 수): 기본 5
const ITERATIONS = Number(__ENV.ITERATIONS || 50);  // 전체 반복 횟수

// Base64 문자열을 PNG 이미지로 바꾼다.
// 콘텐츠 썸네일 업로드 테스트 이미지
const THUMBNAIL = encoding.b64decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAwUBAUlS5k8AAAAASUVORK5CYII=',
  'std'
);

// K6 설정
export const options = {
  vus: VUS,
  iterations: ITERATIONS,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],

  // 실패율이 1% 미만이어야한다.
  thresholds: {
    http_req_failed: ['rate<0.01'], // 실패율 1% 보다 높으면 테스트 실패.
  },
};

// setup(): VU마다 실행 X, 전체 테스트 시작전에 한번만 실행
export function setup() {
  if (!ACCESS_TOKEN) {      // 토큰 없으면 테스트 종료
    throw new Error('ACCESS_TOKEN is required. Pass it with -e ACCESS_TOKEN=<admin access token>.');
  }

  // GET /api/contents?limit=1 요청 -> Spring Security가 응답에 Set-Cookie, XSRF-TOKEN 내려주기때문
  const res = http.get(`${BASE_URL}/api/contents?limit=1`, {
    headers: {
      Authorization: `Bearer ${ACCESS_TOKEN}`,
    },
  });

  const csrfCookie = res.cookies['XSRF-TOKEN']?.[0]?.value;
  if (!csrfCookie) {
    throw new Error(`XSRF-TOKEN cookie was not issued. status=${res.status}`);
  }

  return {      // 모든 Virtual User가 공유
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
        Authorization: `Bearer ${ACCESS_TOKEN}`,
        Cookie: `XSRF-TOKEN=${data.csrfCookie}`,
        'X-XSRF-TOKEN': data.csrfHeader,
      },
    }
  );

  // 201 Created가 왔는지 검사.
  check(res, {
    'content created': (r) => r.status === 201,
  });

  // 1초뒤 다음 콘텐츠 등록
  sleep(1);
}
