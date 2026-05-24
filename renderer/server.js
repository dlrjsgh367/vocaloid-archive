const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const TEMPLATE = fs.readFileSync(path.join(__dirname, 'template.html'), 'utf8');
let browser;

async function render(data) {
  const json = JSON.stringify(data).replace(/</g, '\\u003c');
  const html = TEMPLATE.replace('__DATA__', json);
  const page = await browser.newPage({
    viewport: { width: 1080, height: 1080 },
    deviceScaleFactor: 1,
  });
  try {
    await page.setContent(html, { waitUntil: 'networkidle' });
    await page.evaluate(() => document.fonts.ready);
    const el = await page.$('.card');
    if (!el) throw new Error('.card element not found');
    return await el.screenshot({ type: 'png' });
  } finally {
    await page.close();
  }
}

const server = http.createServer((req, res) => {
  if (req.method === 'GET' && req.url === '/health') {
    res.writeHead(200).end('ok');
    return;
  }
  if (req.method === 'POST' && req.url === '/render') {
    let body = '';
    req.on('data', (c) => (body += c));
    req.on('end', async () => {
      try {
        const png = await render(JSON.parse(body));
        res.writeHead(200, { 'Content-Type': 'image/png' }).end(png);
      } catch (e) {
        console.error(e);
        res.writeHead(500).end(String(e));
      }
    });
    return;
  }
  res.writeHead(404).end();
});

(async () => {
  browser = await chromium.launch({ args: ['--no-sandbox'] });
  server.listen(3000, () => console.log('renderer on :3000'));
})();
