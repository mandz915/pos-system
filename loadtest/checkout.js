// Load test for "10,000 transactions per minute" (= about 167 completed sales per second).
// One transaction = create sale + checkout with cash, so 334 HTTP requests per second through the gateway.
//
//   k6 run -e BASE=http://localhost:8080 loadtest/checkout.js
//   k6 run -e RATE=167 -e DURATION=10m loadtest/checkout.js        (sustained run)
//
// Before running, start the gateway with a higher per-user limit, because this script uses ONE admin token
// (a real store has many tills, each its own user):   RATE_LIMIT_PER_MINUTE=200000
// Run it against a production-like environment (several replicas, managed Postgres/Kafka/Redis); a laptop will
// tell you about the laptop.
import http from 'k6/http';
import { check, fail } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE = __ENV.BASE || 'http://localhost:8080';
const RATE = parseInt(__ENV.RATE || '167');          // transactions per second
const DURATION = __ENV.DURATION || '5m';

export const options = {
  scenarios: {
    sales: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 200,
      maxVUs: 2000,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],                               // under 1% errors
    'http_req_duration{name:create_sale}': ['p(95)<500'],
    'http_req_duration{name:checkout}': ['p(95)<800'],
    dropped_iterations: ['count<1'],                              // the system kept up with the arrival rate
  },
};

const json = { headers: { 'Content-Type': 'application/json' } };

function authed(token, name) {
  return { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }, tags: { name } };
}

export function setup() {
  const login = http.post(`${BASE}/api/auth/login`,
    JSON.stringify({ username: __ENV.USER || 'admin', password: __ENV.PASSWORD || 'Admin#12345' }), json);
  if (login.status !== 200) fail(`login failed: ${login.status} ${login.body}`);
  const token = login.json('accessToken');

  const product = http.post(`${BASE}/api/catalog/products`, JSON.stringify({
    sku: `LOAD-${Date.now()}`, name: 'Load test item', categoryId: 1, taxRateId: 1, price: 120.0, cost: 35.0,
  }), authed(token, 'setup'));
  if (product.status !== 201) fail(`product create failed: ${product.status} ${product.body}`);

  // 409 means a shift is already open on the terminal, which is exactly what we need.
  http.post(`${BASE}/api/sales/shifts/open`,
    JSON.stringify({ storeId: 1, terminalId: 1, openingCash: 1000 }), authed(token, 'setup'));

  return { token, productId: product.json('id') };
}

export default function (data) {
  const created = http.post(`${BASE}/api/sales`, JSON.stringify({
    clientRef: uuidv4(), storeId: 1, terminalId: 1,
    lines: [{ productId: data.productId, quantity: 1 }],
  }), authed(data.token, 'create_sale'));
  if (!check(created, { 'sale created': (r) => r.status === 201 })) return;

  const done = http.post(`${BASE}/api/sales/${created.json('sale.id')}/checkout`,
    JSON.stringify({ payments: [{ method: 'CASH', amount: 200.0 }] }), authed(data.token, 'checkout'));
  check(done, { 'checkout ok': (r) => r.status === 200 });
}
