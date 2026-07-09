function value(data, metricName, valueName, fallback = 0) {
  return data.metrics?.[metricName]?.values?.[valueName] ?? fallback;
}

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

function statusText(failedRate) {
  return failedRate < 0.01 ? '통과' : '실패';
}

function statusClass(failedRate) {
  return failedRate < 0.01 ? 'pass' : 'fail';
}

export function createKoreanHtmlReport(data, scenarioName) {
  const durationAvg = value(data, 'http_req_duration', 'avg');
  const durationMed = value(data, 'http_req_duration', 'med');
  const durationP90 = value(data, 'http_req_duration', 'p(90)');
  const durationP95 = value(data, 'http_req_duration', 'p(95)');
  const durationP99 = value(data, 'http_req_duration', 'p(99)');
  const durationMax = value(data, 'http_req_duration', 'max');
  const durationMaxForBar = Math.max(durationAvg, durationMed, durationP90, durationP95, durationP99, durationMax);

  const requestCount = value(data, 'http_reqs', 'count');
  const requestRate = value(data, 'http_reqs', 'rate');
  const failedRate = value(data, 'http_req_failed', 'rate');
  const failedCount = value(data, 'http_req_failed', 'passes');
  const successCount = Math.max(0, requestCount - failedCount);
  const iterationCount = value(data, 'iterations', 'count');
  const iterationRate = value(data, 'iterations', 'rate');
  const checksRate = value(data, 'checks', 'rate');
  const checksPassed = value(data, 'checks', 'passes');
  const checksFailed = value(data, 'checks', 'fails');
  const transactionCount = value(data, 'content_created', 'count')
    || value(data, 'follow_created', 'count');
  const transactionTps = value(data, 'content_created', 'rate')
    || value(data, 'follow_created', 'rate');

  const status = statusText(failedRate);
  const statusCss = statusClass(failedRate);
  const reportTime = new Date().toLocaleString('ko-KR');

  return `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${scenarioName} k6 부하 테스트 리포트</title>
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
    .header {
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
    th, td {
      padding: 10px 8px;
      border-bottom: 1px solid #e4e9ef;
      text-align: left;
    }
    th { color: #667085; font-weight: 700; }
    td:last-child, th:last-child { text-align: right; }
    @media (max-width: 820px) {
      .header, .two-column { grid-template-columns: 1fr; display: grid; }
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
        <h1>${scenarioName} k6 부하 테스트 리포트</h1>
        <div class="muted">생성 시각: ${reportTime}</div>
      </div>
      <div class="badge ${statusCss}">${status}</div>
    </section>

    <section class="grid">
      <div class="panel">
        <div class="metric-label">총 요청 수</div>
        <div class="metric-value">${formatNumber(requestCount, 0)}</div>
      </div>
      <div class="panel">
        <div class="metric-label">${transactionTps > 0 ? 'TPS' : '초당 요청 수'}</div>
        <div class="metric-value">${formatNumber(transactionTps > 0 ? transactionTps : requestRate, 2)}</div>
      </div>
      <div class="panel">
        <div class="metric-label">실패율</div>
        <div class="metric-value">${formatRate(failedRate)}</div>
      </div>
      <div class="panel">
        <div class="metric-label">반복 수</div>
        <div class="metric-value">${formatNumber(iterationCount, 0)}</div>
      </div>
    </section>

    <section class="panel">
      <h2>응답 시간 막대그래프</h2>
      ${row('평균', durationAvg, formatMs(durationAvg), durationMaxForBar)}
      ${row('중앙값', durationMed, formatMs(durationMed), durationMaxForBar)}
      ${row('p90', durationP90, formatMs(durationP90), durationMaxForBar, 'warn')}
      ${row('p95', durationP95, formatMs(durationP95), durationMaxForBar, 'warn')}
      ${durationP99 ? row('p99', durationP99, formatMs(durationP99), durationMaxForBar, 'warn') : ''}
      ${row('최대', durationMax, formatMs(durationMax), durationMaxForBar, 'fail')}
    </section>

    <section class="two-column">
      <div class="panel">
        <h2>요청 성공/실패</h2>
        ${row('성공', successCount, `${formatNumber(successCount, 0)}건`, requestCount)}
        ${row('실패', failedCount, `${formatNumber(failedCount, 0)}건`, requestCount, 'fail')}
      </div>
      <div class="panel">
        <h2>체크 결과</h2>
        ${row('성공', checksPassed, `${formatNumber(checksPassed, 0)}건`, checksPassed + checksFailed)}
        ${row('실패', checksFailed, `${formatNumber(checksFailed, 0)}건`, checksPassed + checksFailed, 'fail')}
      </div>
    </section>

    <section class="panel" style="margin-top:20px">
      <h2>상세 지표</h2>
      <table>
        <tr><th>항목</th><th>값</th></tr>
        <tr><td>평균 응답 시간</td><td>${formatMs(durationAvg)}</td></tr>
        <tr><td>p95 응답 시간</td><td>${formatMs(durationP95)}</td></tr>
        <tr><td>최대 응답 시간</td><td>${formatMs(durationMax)}</td></tr>
        <tr><td>초당 반복 수</td><td>${formatNumber(iterationRate, 2)}</td></tr>
        <tr><td>성공 트랜잭션 수</td><td>${formatNumber(transactionCount, 0)}</td></tr>
        <tr><td>TPS</td><td>${formatNumber(transactionTps, 2)}</td></tr>
        <tr><td>체크 성공률</td><td>${formatRate(checksRate)}</td></tr>
        <tr><td>요청 실패율</td><td>${formatRate(failedRate)}</td></tr>
      </table>
    </section>
  </main>
</body>
</html>`;
}

export function koreanSummary(data, scenarioName) {
  const reportPath = __ENV.K6_KO_HTML_REPORT || `/scripts/reports/${scenarioName}.html`;
  return {
    [reportPath]: createKoreanHtmlReport(data, scenarioName),
    stdout: `\n한글 HTML 리포트 생성: ${reportPath}\n`,
  };
}
