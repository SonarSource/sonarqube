# README images

## hero.png

The hero image at the top of the repository `README.md`.

### Source

`hero.html` in this directory is the editable source — a single self-contained HTML file (inline CSS, Google Fonts via `<link>`, SVG inline). Open it in any browser to preview; Puppeteer is used to render the PNG at 2× for retina crispness.

### Embedded content

The image embeds stats that may need periodic refresh:

| Element | Current value | Where to verify |
|---|---|---|
| Languages | 30+ | [sonarsource.com product page](https://www.sonarsource.com/products/sonarqube) |
| Analysis rules | 5,000+ | [rules.sonarsource.com](https://rules.sonarsource.com) |
| Projects analyzed | 400K+ | SonarSource marketing / public decks |
| Years defining quality | 18 | SonarQube was released in 2008 (`current_year − 2008`) |

Update those in `hero.html` under the `.stats` block (`.stat .n`) and re-render.

### Re-rendering the PNG

Requires Node and Puppeteer. From the repo root:

```bash
npm install puppeteer      # one-time, ~170 MB Chromium download
cat > /tmp/render.js <<'EOF'
const puppeteer = require('puppeteer');
const path = require('path');
(async () => {
  const browser = await puppeteer.launch({ headless: 'new', args: ['--no-sandbox'] });
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 500, deviceScaleFactor: 2 });
  await page.goto('file://' + path.resolve('docs/images/hero.html'), { waitUntil: 'networkidle0' });
  await page.evaluateHandle('document.fonts.ready');
  await new Promise(r => setTimeout(r, 500));
  await page.screenshot({ path: 'docs/images/hero.png', type: 'png' });
  await browser.close();
})();
EOF
node /tmp/render.js
```

Optional: compress with `pngquant --quality=80-95 docs/images/hero.png --output docs/images/hero.png --force` to keep the PNG under ~150 KB.
