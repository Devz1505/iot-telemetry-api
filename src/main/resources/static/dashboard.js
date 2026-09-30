// Dashboard for the telemetry API. Plain JavaScript: fetch() + Chart.js, no build step.
// It only talks to the same REST endpoints the ESP32 and the tests use.

const REFRESH_MS = 5000;
const $ = (id) => document.getElementById(id);

let chart = null;

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!response.ok) {
    // Error bodies are RFC 9457 problem+json: { status, title, detail }
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail || `${response.status} ${response.statusText}`);
  }
  return response.status === 204 ? null : response.json();
}

function css(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

function formatTime(iso) {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

function formatValue(value, unit) {
  return value == null ? '–' : `${Number(value).toFixed(1)} ${unit}`;
}

// Builds a table row with textContent (never innerHTML), so text sent by a
// device such as its name can't inject HTML into the page.
function row(cells) {
  const tr = document.createElement('tr');
  for (const { text, className } of cells) {
    const td = document.createElement('td');
    td.textContent = text;
    if (className) td.className = className;
    tr.appendChild(td);
  }
  return tr;
}

async function loadDevices() {
  const devices = await api('/api/devices');
  const select = $('device');
  const previous = select.value;
  select.replaceChildren(...devices.map((d) => new Option(`${d.name} (${d.deviceId})`, d.deviceId)));
  if (devices.some((d) => d.deviceId === previous)) select.value = previous;
  return devices.length > 0;
}

async function loadReadings() {
  const deviceId = $('device').value;
  const sensor = $('sensor').value;
  const hours = Number($('window').value);
  const from = new Date(Date.now() - hours * 3600 * 1000).toISOString();
  const base = `/api/devices/${encodeURIComponent(deviceId)}`;

  const [page, stats, thresholds] = await Promise.all([
    api(`${base}/readings?sensorType=${sensor}&from=${from}&size=500`),
    api(`${base}/stats?sensorType=${sensor}&from=${from}`),
    api(`${base}/thresholds`),
  ]);

  const points = page.content.slice().reverse(); // API is newest-first; the chart wants oldest-first
  const unit = stats.unit;
  const rule = thresholds.find((t) => t.sensorType === sensor);

  $('chart-title').textContent = `${sensor.charAt(0)}${sensor.slice(1).toLowerCase()} (${unit})`;
  $('stat-latest').textContent = formatValue(points.at(-1)?.value, unit);
  $('stat-min').textContent = formatValue(stats.min, unit);
  $('stat-max').textContent = formatValue(stats.max, unit);
  $('stat-avg').textContent = formatValue(stats.avg, unit);
  $('stat-count').textContent = stats.count;
  $('empty').hidden = points.length > 0;

  drawChart(points, unit, rule);
  $('readings-table').replaceChildren(...page.content.slice(0, 50).map((r) => row([
    { text: new Date(r.recordedAt).toLocaleString(), className: 'time' },
    { text: formatValue(r.value, unit), className: 'num' },
  ])));
}

function drawChart(points, unit, rule) {
  const labels = points.map((p) => formatTime(p.recordedAt));
  const datasets = [{
    label: 'Reading',
    data: points.map((p) => p.value),
    borderColor: css('--series'),
    backgroundColor: css('--series'),
    borderWidth: 2,
    pointRadius: 0,
    pointHoverRadius: 4,
    tension: 0.25,
  }];
  // Threshold limits as dashed reference lines, so you can see why an alert fired.
  for (const [name, limit] of [['max', rule?.max], ['min', rule?.min]]) {
    if (limit != null) {
      datasets.push({
        label: `Threshold ${name} ${limit} ${unit}`,
        data: points.map(() => limit),
        borderColor: css('--critical'),
        borderWidth: 1.5,
        borderDash: [6, 4],
        pointRadius: 0,
        pointHoverRadius: 0,
      });
    }
  }

  if (chart) {
    chart.data.labels = labels;
    chart.data.datasets = datasets;
    chart.update('none');
    return;
  }
  chart = new Chart($('chart'), {
    type: 'line',
    data: { labels, datasets },
    options: {
      maintainAspectRatio: false,
      animation: false,
      interaction: { mode: 'index', intersect: false },  // hover anywhere on the x-axis
      plugins: {
        legend: { display: false },
        tooltip: { callbacks: { label: (c) => `${c.dataset.label}: ${formatValue(c.parsed.y, unit)}` } },
      },
      scales: {
        x: { ticks: { color: css('--muted'), maxTicksLimit: 8 }, grid: { display: false } },
        y: { ticks: { color: css('--muted') }, grid: { color: css('--grid') } },
      },
    },
  });
}

async function loadAlerts() {
  const page = await api('/api/alerts?acknowledged=false&size=20');
  $('no-alerts').hidden = page.content.length > 0;
  $('alerts-table').replaceChildren(...page.content.map((alert) => {
    const tr = row([
      { text: new Date(alert.createdAt).toLocaleString(), className: 'time' },
      { text: alert.deviceId },
      { text: alert.message },
    ]);
    const button = document.createElement('button');
    button.textContent = 'Acknowledge';
    button.addEventListener('click', async () => {
      await api(`/api/alerts/${alert.id}/acknowledge`, { method: 'POST' });
      loadAlerts();
    });
    const td = document.createElement('td');
    td.appendChild(button);
    tr.appendChild(td);
    return tr;
  }));
}

async function refresh() {
  try {
    const health = await api('/actuator/health');
    $('health').textContent = `API ${health.status.toLowerCase()}`;
    $('health').className = 'health up';

    if (await loadDevices()) {
      await loadReadings();
    }
    await loadAlerts();
  } catch (error) {
    $('health').textContent = `API unreachable: ${error.message}`;
    $('health').className = 'health down';
  }
}

for (const id of ['device', 'sensor', 'window']) {
  $(id).addEventListener('change', refresh);
}
refresh();
setInterval(refresh, REFRESH_MS);
