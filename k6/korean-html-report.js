function value(data, metricName, valueName, fallback = 0) {
  return data.metrics?.[metricName]?.values?.[valueName] ?? fallback;
}

function hasMetric(data, metricName) {
  return Boolean(data.metrics?.[metricName]?.values);
}

function taggedMetric(metricName, tagName, tagValue) {
  return `${metricName}{${tagName}:${tagValue}}`;
}

const API_DEFINITIONS = [
  { tagValue: 'setup-login', title: '로그인 API', endpoint: 'POST /api/auth/sign-in', targetP95: 1000 },
  { tagValue: 'content-list', title: '콘텐츠 목록 API', endpoint: 'GET /api/contents', targetP95: 1000 },
  { tagValue: 'content-detail', title: '콘텐츠 상세 API', endpoint: 'GET /api/contents/{contentId}', targetP95: 700 },
  { tagValue: 'review-list', title: '리뷰 목록 API', endpoint: 'GET /api/reviews', targetP95: 1000 },
  { tagValue: 'conversation-list', title: '대화 목록 API', endpoint: 'GET /api/conversations', targetP95: 800 },
  {
    tagValue: 'direct-message-list',
    title: '최근 DM 목록 API',
    endpoint: 'GET /api/conversations/{conversationId}/direct-messages',
    targetP95: 1000,
  },
  { tagValue: 'notification-list', title: '알림 목록 API', endpoint: 'GET /api/notifications', targetP95: 800 },
  { tagValue: 'profile-read', title: '프로필 조회 API', endpoint: '프로필 조회', targetP95: 1000 },
  { tagValue: 'follow-status', title: '팔로우 여부 조회 API', endpoint: '팔로우 여부 조회', targetP95: 1000 },
  { tagValue: 'follow-create', title: '팔로우 생성 API', endpoint: 'POST /api/follows', targetP95: 1000 },
  {
    tagValue: 'content-list-default',
    title: '콘텐츠 기본 목록조회 API',
    endpoint: 'GET /api/contents',
    targetP95: 1000,
  },
  {
    tagValue: 'content-find-default',
    title: '콘텐츠 단건 조회 API',
    endpoint: 'GET /api/contents/{contentId}',
    targetP95: 500,
  },
  {
    tagValue: 'conversation-list-default',
    title: '대화방 기본 목록조회 API',
    endpoint: 'GET /api/conversations',
    targetP95: 500,
  },
  {
    tagValue: 'user-profile-update',
    title: '프로필 수정 API',
    endpoint: 'PATCH /api/users/{userId}',
    targetP95: 2000,
  },
  {
    tagValue: 'review-list-default',
    title: '리뷰 목록조회 API',
    endpoint: 'GET /api/reviews',
    targetP95: 1000,
  },
];

const SCENARIO_TITLES = {
  'user-content-browse': '콘텐츠 탐색 사용자 시나리오',
  'user-conversation-inbox': '대화함 확인 사용자 시나리오',
  'content-list-default': '콘텐츠 기본 목록 조회',
  'content-find-default': '콘텐츠 단건 조회',
  'conversation-list-default': '대화방 기본 목록 조회',
  'follow-create': '팔로우 생성',
  'content-create': '콘텐츠 생성',
};

function formatNumber(number, digits = 2) {
  if (!Number.isFinite(number)) {
    return '-';
  }
  return number.toFixed(digits);
}

function formatMs(number) {
  return `${formatNumber(number, 2)} ms`;
}

function formatRate(rate) {
  return `${formatNumber(rate * 100, 2)}%`;
}

function statusText(failedRate) {
  return failedRate < 0.01 ? '통과' : '실패';
}

function statusClass(failedRate) {
  return failedRate < 0.01 ? 'pass' : 'fail';
}

function bar(widthPercent, className = '') {
  const width = Math.max(0, Math.min(100, widthPercent));
  return `<div class="bar-track"><div class="bar ${className}" style="width:${width}%"></div></div>`;
}

function row(label, rawValue, displayValue, maxValue, className = '') {
  const width = maxValue > 0 ? (rawValue / maxValue) * 100 : 0;
  return `
    <div class="bar-row">
      <div class="bar-label">${label}</div>
      ${bar(width, className)}
      <div class="bar-value">${displayValue}</div>
    </div>
  `;
}

function metricSet(data, scope) {
  const suffix = scope?.tagName ? `{${scope.tagName}:${scope.tagValue}}` : '';
  const metric = (name) => `${name}${suffix}`;

  const durationAvg = value(data, metric('http_req_duration'), 'avg');
  const durationMed = value(data, metric('http_req_duration'), 'med');
  const durationP90 = value(data, metric('http_req_duration'), 'p(90)');
  const durationP95 = value(data, metric('http_req_duration'), 'p(95)');
  const durationP99 = value(data, metric('http_req_duration'), 'p(99)');
  const durationMax = value(data, metric('http_req_duration'), 'max');
  const durationMaxForBar = Math.max(durationAvg, durationMed, durationP90, durationP95, durationP99, durationMax);

  const requestCount = value(data, metric('http_reqs'), 'count');
  const requestRate = value(data, metric('http_reqs'), 'rate');
  const failedRate = value(data, metric('http_req_failed'), 'rate');
  const failedCount = value(data, metric('http_req_failed'), 'passes');
  const successCount = Math.max(0, requestCount - failedCount);

  return {
    durationAvg,
    durationMed,
    durationP90,
    durationP95,
    durationP99,
    durationMax,
    durationMaxForBar,
    requestCount,
    requestRate,
    failedRate,
    failedCount,
    successCount,
  };
}

function responseTimePanel(title, metrics) {
  return `
    <section class="panel">
      <h2>${title}</h2>
      ${row('평균', metrics.durationAvg, formatMs(metrics.durationAvg), metrics.durationMaxForBar)}
      ${row('중앙값', metrics.durationMed, formatMs(metrics.durationMed), metrics.durationMaxForBar)}
      ${row('p90', metrics.durationP90, formatMs(metrics.durationP90), metrics.durationMaxForBar, 'warn')}
      ${row('p95', metrics.durationP95, formatMs(metrics.durationP95), metrics.durationMaxForBar, 'warn')}
      ${metrics.durationP99 ? row('p99', metrics.durationP99, formatMs(metrics.durationP99), metrics.durationMaxForBar, 'warn') : ''}
      ${row('최대', metrics.durationMax, formatMs(metrics.durationMax), metrics.durationMaxForBar, 'fail')}
    </section>
  `;
}

function requestResultPanel(title, metrics) {
  return `
    <div class="panel">
      <h2>${title}</h2>
      ${row('성공', metrics.successCount, `${formatNumber(metrics.successCount, 0)}건`, metrics.requestCount)}
      ${row('실패', metrics.failedCount, `${formatNumber(metrics.failedCount, 0)}건`, metrics.requestCount, 'fail')}
    </div>
  `;
}

function detailRows(metrics, transactionCount, transactionTps, duplicateCount) {
  return `
    <tr><td>총 요청 수</td><td>${formatNumber(metrics.requestCount, 0)}</td></tr>
    <tr><td>초당 요청 수</td><td>${formatNumber(metrics.requestRate, 2)}</td></tr>
    <tr><td>평균 응답 시간</td><td>${formatMs(metrics.durationAvg)}</td></tr>
    <tr><td>p95 응답 시간</td><td>${formatMs(metrics.durationP95)}</td></tr>
    <tr><td>p99 응답 시간</td><td>${formatMs(metrics.durationP99)}</td></tr>
    <tr><td>최대 응답 시간</td><td>${formatMs(metrics.durationMax)}</td></tr>
    <tr><td>성공 트랜잭션 수</td><td>${formatNumber(transactionCount, 0)}</td></tr>
    ${duplicateCount === undefined ? '' : `<tr><td>중복 팔로우 수</td><td>${formatNumber(duplicateCount, 0)}</td></tr>`}
    <tr><td>TPS</td><td>${formatNumber(transactionTps, 2)}</td></tr>
    <tr><td>요청 실패율</td><td>${formatRate(metrics.failedRate)}</td></tr>
  `;
}

function taggedApiSection(data, tagValue, title, transactionCount = 0, transactionTps = 0, duplicateCount) {
  const tagName = 'api';
  const durationMetric = taggedMetric('http_req_duration', tagName, tagValue);

  if (!hasMetric(data, durationMetric)) {
    return '';
  }

  const metrics = metricSet(data, { tagName, tagValue });
  const status = statusText(metrics.failedRate);
  const statusCss = statusClass(metrics.failedRate);
  const tps = transactionTps > 0 ? transactionTps : metrics.requestRate;
  const count = transactionCount > 0 ? transactionCount : metrics.successCount;

  return `
    <section class="section-gap">
      <div class="section-heading">
        <h2>${title}</h2>
        <div class="badge ${statusCss}">${status}</div>
      </div>
      <section class="grid">
        <div class="panel">
          <div class="metric-label">요청 수</div>
          <div class="metric-value">${formatNumber(metrics.requestCount, 0)}</div>
        </div>
        <div class="panel">
          <div class="metric-label">초당 요청 수</div>
          <div class="metric-value">${formatNumber(tps, 2)}</div>
        </div>
        <div class="panel">
          <div class="metric-label">실패율</div>
          <div class="metric-value">${formatRate(metrics.failedRate)}</div>
        </div>
        <div class="panel">
          <div class="metric-label">성공 수</div>
          <div class="metric-value">${formatNumber(count, 0)}</div>
        </div>
      </section>
      ${responseTimePanel(`${title} 응답 시간`, metrics)}
      <section class="two-column">
        ${requestResultPanel(`${title} 성공/실패`, metrics)}
        <div class="panel">
          <h2>${title} 상세 지표</h2>
          <table>
            <tr><th>항목</th><th>값</th></tr>
            ${detailRows(metrics, count, tps, duplicateCount)}
          </table>
        </div>
      </section>
    </section>
  `;
}

function taggedApiSections(data, transactionCount, transactionTps) {
  const duplicateCount = hasMetric(data, 'follow_already_exists')
    ? value(data, 'follow_already_exists', 'count')
    : undefined;
  const sections = API_DEFINITIONS.map((api) =>
    taggedApiSection(
      data,
      api.tagValue,
      api.title,
      api.tagValue === 'follow-create' ? transactionCount : 0,
      api.tagValue === 'follow-create' ? transactionTps : 0,
      api.tagValue === 'follow-create' ? duplicateCount : undefined
    )
  ).join('');

  if (sections) {
    return sections;
  }

  return `
    <section class="panel" style="margin-top:20px">
      <h2>API별 지표</h2>
      <p class="muted">
        태그별 지표가 없습니다. 운영 시나리오 k6 스크립트를 실행하면 api 태그 기준 응답 시간이 분리되어 표시됩니다.
      </p>
    </section>
  `;
}

function apiMetricRows(data) {
  return API_DEFINITIONS
    .filter((api) => hasMetric(data, taggedMetric('http_req_duration', 'api', api.tagValue)))
    .map((api) => ({
      ...api,
      metrics: metricSet(data, { tagName: 'api', tagValue: api.tagValue }),
    }));
}

function apiResult(api) {
  return api.metrics.failedRate < 0.01 && api.metrics.durationP95 < api.targetP95
    ? '통과'
    : '확인 필요';
}

function apiComparisonTable(data) {
  const rows = apiMetricRows(data);
  if (rows.length === 0) {
    return '';
  }

  const body = rows.map((api) => {
    const result = apiResult(api);
    const resultCss = result === '통과' ? 'pass-text' : 'fail-text';
    return `
      <tr>
        <td>
          <strong>${api.title}</strong>
          <div class="endpoint">${api.endpoint}</div>
        </td>
        <td class="number">${formatNumber(api.metrics.requestCount, 0)}</td>
        <td class="number">${formatRate(api.metrics.failedRate)}</td>
        <td class="number">${formatMs(api.metrics.durationAvg)}</td>
        <td class="number">${formatMs(api.metrics.durationP90)}</td>
        <td class="number emphasize">${formatMs(api.metrics.durationP95)}</td>
        <td class="number">${formatMs(api.metrics.durationP99)}</td>
        <td class="number">${formatMs(api.metrics.durationMax)}</td>
        <td class="number">&lt; ${formatNumber(api.targetP95, 0)} ms</td>
        <td class="${resultCss}">${result}</td>
      </tr>
    `;
  }).join('');

  return `
    <section class="panel api-comparison">
      <h2>API별 응답시간 비교</h2>
      <p class="muted">p95 목표는 각 시나리오 스크립트에 설정된 API별 기준입니다.</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>API</th>
              <th>요청 수</th>
              <th>실패율</th>
              <th>평균</th>
              <th>p90</th>
              <th>p95</th>
              <th>p99</th>
              <th>최대</th>
              <th>p95 목표</th>
              <th>결과</th>
            </tr>
          </thead>
          <tbody>${body}</tbody>
        </table>
      </div>
    </section>
  `;
}

export function createKoreanHtmlReport(data, scenarioName) {
  const scenarioTitle = SCENARIO_TITLES[scenarioName] || scenarioName;
  const overall = metricSet(data);
  const requestCount = overall.requestCount;
  const iterationCount = value(data, 'iterations', 'count');
  const iterationRate = value(data, 'iterations', 'rate');
  const checksRate = value(data, 'checks', 'rate');
  const checksPassed = value(data, 'checks', 'passes');
  const checksFailed = value(data, 'checks', 'fails');
  const transactionCount = value(data, 'content_created', 'count')
    || value(data, 'follow_created', 'count')
    || value(data, 'content_list_ok', 'count')
    || value(data, 'content_find_ok', 'count')
    || value(data, 'conversation_list_ok', 'count')
    || value(data, 'profile_updated', 'count')
    || value(data, 'review_list_ok', 'count');
  const transactionTps = value(data, 'content_created', 'rate')
    || value(data, 'follow_created', 'rate')
    || value(data, 'content_list_ok', 'rate')
    || value(data, 'content_find_ok', 'rate')
    || value(data, 'conversation_list_ok', 'rate')
    || value(data, 'profile_updated', 'rate')
    || value(data, 'review_list_ok', 'rate');
  const duplicateCount = hasMetric(data, 'follow_already_exists')
    ? value(data, 'follow_already_exists', 'count')
    : undefined;

  const status = statusText(overall.failedRate);
  const statusCss = statusClass(overall.failedRate);
  const reportTime = new Date().toLocaleString('ko-KR');

  return `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${scenarioTitle} k6 부하 테스트 리포트</title>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      background: #f4f6f8;
      color: #17202a;
      font-family: Arial, "Malgun Gothic", sans-serif;
      line-height: 1.5;
    }
    main {
      max-width: 1120px;
      margin: 0 auto;
      padding: 32px 20px 48px;
    }
    .header, .section-heading {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      align-items: flex-end;
      margin-bottom: 24px;
    }
    h1 {
      margin: 0 0 8px;
      font-size: 28px;
      letter-spacing: 0;
    }
    .section-heading h2 {
      margin: 0;
      font-size: 22px;
    }
    .section-gap { margin-top: 28px; }
    .muted { color: #667085; }
    .badge {
      display: inline-flex;
      min-width: 80px;
      justify-content: center;
      padding: 8px 14px;
      border-radius: 6px;
      color: #fff;
      font-weight: 700;
    }
    .badge.pass { background: #16825d; }
    .badge.fail { background: #c2410c; }
    .grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 12px;
      margin-bottom: 20px;
    }
    .panel {
      background: #fff;
      border: 1px solid #d9e0e8;
      border-radius: 8px;
      padding: 18px;
    }
    .metric-label {
      color: #667085;
      font-size: 13px;
      margin-bottom: 8px;
    }
    .metric-value {
      font-size: 24px;
      font-weight: 700;
    }
    h2 {
      margin: 0 0 14px;
      font-size: 18px;
    }
    .bar-row {
      display: grid;
      grid-template-columns: 96px minmax(0, 1fr) 92px;
      gap: 12px;
      align-items: center;
      margin: 12px 0;
    }
    .bar-label {
      color: #344054;
      font-weight: 700;
    }
    .bar-track {
      height: 22px;
      background: #e7ecf2;
      border-radius: 4px;
      overflow: hidden;
    }
    .bar {
      height: 100%;
      background: #2563eb;
    }
    .bar.warn { background: #d97706; }
    .bar.fail { background: #dc2626; }
    .bar-value {
      text-align: right;
      font-variant-numeric: tabular-nums;
    }
    .two-column {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      margin-top: 20px;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;
    }
    .table-wrap { overflow-x: auto; }
    .api-comparison { margin-top: 20px; }
    .api-comparison table { min-width: 1080px; }
    .api-comparison th { white-space: nowrap; }
    .api-comparison td { vertical-align: middle; }
    .api-comparison .number {
      text-align: right;
      white-space: nowrap;
      font-variant-numeric: tabular-nums;
    }
    .api-comparison .emphasize {
      background: #fff7ed;
      color: #9a3412;
      font-weight: 700;
    }
    .endpoint {
      margin-top: 3px;
      color: #667085;
      font-family: Consolas, monospace;
      font-size: 12px;
      white-space: nowrap;
    }
    .pass-text { color: #16825d; font-weight: 700; white-space: nowrap; }
    .fail-text { color: #c2410c; font-weight: 700; white-space: nowrap; }
    th, td {
      padding: 10px 8px;
      border-bottom: 1px solid #e4e9ef;
      text-align: left;
    }
    th { color: #667085; font-weight: 700; }
    td:last-child, th:last-child { text-align: right; }
    @media (max-width: 820px) {
      .header, .section-heading, .two-column { grid-template-columns: 1fr; display: grid; }
      .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .bar-row { grid-template-columns: 82px minmax(0, 1fr); }
      .bar-value { grid-column: 2; text-align: left; }
    }
  </style>
</head>
<body>
  <main>
    <section class="header">
      <div>
        <h1>${scenarioTitle} k6 부하 테스트 리포트</h1>
        <div class="muted">생성 시각: ${reportTime}</div>
      </div>
      <div class="badge ${statusCss}">${status}</div>
    </section>

    <section class="grid">
      <div class="panel">
        <div class="metric-label">전체 요청 수</div>
        <div class="metric-value">${formatNumber(requestCount, 0)}</div>
      </div>
      <div class="panel">
        <div class="metric-label">${transactionTps > 0 ? 'TPS' : '초당 요청 수'}</div>
        <div class="metric-value">${formatNumber(transactionTps > 0 ? transactionTps : overall.requestRate, 2)}</div>
      </div>
      <div class="panel">
        <div class="metric-label">전체 실패율</div>
        <div class="metric-value">${formatRate(overall.failedRate)}</div>
      </div>
      <div class="panel">
        <div class="metric-label">반복 수</div>
        <div class="metric-value">${formatNumber(iterationCount, 0)}</div>
      </div>
    </section>

    ${responseTimePanel('전체 HTTP 응답 시간', overall)}

    <section class="two-column">
      ${requestResultPanel('전체 요청 성공/실패', overall)}
      <div class="panel">
        <h2>체크 결과</h2>
        ${row('성공', checksPassed, `${formatNumber(checksPassed, 0)}건`, checksPassed + checksFailed)}
        ${row('실패', checksFailed, `${formatNumber(checksFailed, 0)}건`, checksPassed + checksFailed, 'fail')}
      </div>
    </section>

    <section class="panel" style="margin-top:20px">
      <h2>전체 상세 지표</h2>
      <table>
        <tr><th>항목</th><th>값</th></tr>
        ${detailRows(overall, transactionCount, transactionTps, duplicateCount)}
        <tr><td>초당 반복 수</td><td>${formatNumber(iterationRate, 2)}</td></tr>
        <tr><td>체크 성공률</td><td>${formatRate(checksRate)}</td></tr>
      </table>
    </section>

    ${apiComparisonTable(data)}

    ${taggedApiSections(data, transactionCount, transactionTps)}
  </main>
</body>
</html>`;
}

export function createKoreanMarkdownReport(data, scenarioName) {
  const scenarioTitle = SCENARIO_TITLES[scenarioName] || scenarioName;
  const overall = metricSet(data);
  const iterationCount = value(data, 'iterations', 'count');
  const iterationRate = value(data, 'iterations', 'rate');
  const checksRate = value(data, 'checks', 'rate');
  const reportTime = new Date().toLocaleString('ko-KR');
  const apiRows = apiMetricRows(data);

  const comparisonRows = apiRows.length > 0
    ? apiRows.map((api) =>
      `| ${api.title}<br>\`${api.endpoint}\` | ${formatNumber(api.metrics.requestCount, 0)} | ${formatRate(api.metrics.failedRate)} | ${formatMs(api.metrics.durationAvg)} | ${formatMs(api.metrics.durationP90)} | **${formatMs(api.metrics.durationP95)}** | ${formatMs(api.metrics.durationP99)} | ${formatMs(api.metrics.durationMax)} | < ${formatNumber(api.targetP95, 0)} ms | ${apiResult(api)} |`
    ).join('\n')
    : '| 측정된 API 태그가 없습니다. | - | - | - | - | - | - | - | - | 확인 필요 |';

  return `# ${scenarioTitle} k6 부하 테스트 결과

- 생성 시각: ${reportTime}
- 전체 판정: **${statusText(overall.failedRate)}**

## 전체 결과

| 항목 | 값 |
|---|---:|
| 전체 요청 수 | ${formatNumber(overall.requestCount, 0)} |
| 초당 요청 수 | ${formatNumber(overall.requestRate, 2)} |
| 전체 실패율 | ${formatRate(overall.failedRate)} |
| 평균 응답시간 | ${formatMs(overall.durationAvg)} |
| p90 응답시간 | ${formatMs(overall.durationP90)} |
| p95 응답시간 | ${formatMs(overall.durationP95)} |
| p99 응답시간 | ${formatMs(overall.durationP99)} |
| 최대 응답시간 | ${formatMs(overall.durationMax)} |
| 반복 수 | ${formatNumber(iterationCount, 0)} |
| 초당 반복 수 | ${formatNumber(iterationRate, 2)} |
| 체크 성공률 | ${formatRate(checksRate)} |

## API별 응답시간 비교

| API | 요청 수 | 실패율 | 평균 | p90 | p95 | p99 | 최대 | p95 목표 | 결과 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
${comparisonRows}

> p95 목표는 각 시나리오 스크립트에 설정된 API별 기준입니다.
`;
}

export function koreanSummary(data, scenarioName) {
  const reportPath = __ENV.K6_KO_HTML_REPORT || `/scripts/reports/${scenarioName}.html`;
  const markdownPath = __ENV.K6_KO_MARKDOWN_REPORT
    || (reportPath.toLowerCase().endsWith('.html')
      ? reportPath.slice(0, -5) + '.md'
      : `/scripts/reports/${scenarioName}.md`);

  return {
    [reportPath]: createKoreanHtmlReport(data, scenarioName),
    [markdownPath]: createKoreanMarkdownReport(data, scenarioName),
    stdout:
      `\n한국어 HTML 리포트 생성: ${reportPath}` +
      `\n한국어 Markdown 요약 생성: ${markdownPath}\n`,
  };
}
