import encoding from 'k6/encoding';
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN;
const VUS = Number(__ENV.VUS || 5);
const ITERATIONS = Number(__ENV.ITERATIONS || 50);

// 1x1 PNG. Keep the test self-contained so no local image fixture is needed.
const THUMBNAIL = encoding.b64decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAwUBAUlS5k8AAAAASUVORK5CYII=',
  'std'
);

export const options = {
  vus: VUS,
  iterations: ITERATIONS,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],

  // 실패율이 1% 미만이어야한다.
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  if (!ACCESS_TOKEN) {
    throw new Error('ACCESS_TOKEN is required. Pass it with -e ACCESS_TOKEN=<admin access token>.');
  }

  const res = http.get(`${BASE_URL}/api/contents?limit=1`, {
    headers: {
      Authorization: `Bearer ${ACCESS_TOKEN}`,
    },
  });

  const csrfCookie = res.cookies['XSRF-TOKEN']?.[0]?.value;
  if (!csrfCookie) {
    throw new Error(`XSRF-TOKEN cookie was not issued. status=${res.status}`);
  }

  return {
    csrfCookie,
    csrfHeader: decodeURIComponent(csrfCookie),
  };
}

export default function (data) {
  const unique = `${Date.now()}-${__VU}-${__ITER}`;
  const request = {
    type: 'movie',
    title: `k6-content-${unique}`,
    description: 'k6 content creation performance test',
    tags: ['k6', 'performance'],
  };

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

  check(res, {
    'content created': (r) => r.status === 201,
  });

  sleep(1);
}
