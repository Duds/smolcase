// Run with Playwright available through NODE_PATH or a local node_modules.
// BASE_URL and OUTPUT_DIR can override the local preview and screenshot folder.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');

(async () => {
  const output = process.env.OUTPUT_DIR || '/tmp/smolcase-expression-table';
  await fs.mkdir(output, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  try {
    const context = await browser.newContext({
      viewport: { width: 1440, height: 1000 },
      permissions: ['clipboard-read', 'clipboard-write']
    });
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('console', message => {
      if (message.type() === 'error') errors.push(message.text());
    });
    page.on('response', response => {
      if (response.status() >= 400 && !response.url().endsWith('favicon.ico')) {
        errors.push(`${response.status()} ${response.url()}`);
      }
    });
    await page.goto(process.env.BASE_URL ||
      'http://localhost:8000/spikes/20260909-face-expressions/', { waitUntil: 'networkidle' });
    const set = async (id, value) => {
      await page.locator(`#${id}`).fill(String(value));
      await page.locator(`#${id}`).dispatchEvent('input');
    };
    await page.locator('#blinking').uncheck();
    await page.locator('#saccade').uncheck();
    await set('blend-speed', 12);
    const moods = await page.locator('#moods button').evaluateAll(buttons => buttons.map(b => b.dataset.mood));
    assert.equal(moods.length, 16);
    const canvas = page.locator('canvas');
    const captures = { raw: [], dots: [] };
    for (const mood of moods) {
      await page.locator(`[data-mood="${mood}"]`).click();
      assert.equal(await page.locator(`[data-mood="${mood}"]`).getAttribute('aria-pressed'), 'true');
      await page.waitForTimeout(1200);
      for (const [mode, bypass] of [['raw', true], ['dots', false]]) {
        await page.locator('#show-source').setChecked(bypass);
        await page.waitForTimeout(50);
        const image = await canvas.screenshot({ path: path.join(output, `${mood.toLowerCase()}-${mode}.png`) });
        captures[mode].push(image.toString('base64'));
      }
    }
    for (const mode of ['raw', 'dots']) {
      assert.equal(new Set(captures[mode]).size, moods.length, `${mode}: presets must produce distinct images`);
    }

    // Intensity 0 must remove width/height scaling AND all shape-family changes.
    await set('intensity', 0);
    await page.locator('#show-source').check();
    await page.locator('[data-mood="NEUTRAL"]').click();
    await page.waitForTimeout(1800);
    const neutral = await canvas.screenshot();
    for (const mood of moods) {
      await page.locator(`[data-mood="${mood}"]`).click();
      await page.waitForTimeout(1800);
      assert.ok(neutral.equals(await canvas.screenshot()), `${mood}: intensity 0 must equal neutral`);
    }

    // Capture and restore a tilted heart, including all animation toggles.
    await set('intensity', 1);
    await set('roll', 18);
    await page.locator('[data-mood="HEART"]').click();
    await page.waitForTimeout(1800);
    const tilted = await canvas.screenshot({ path: path.join(output, 'heart-tilted-raw.png') });
    await page.locator('#show-source').uncheck();
    await canvas.screenshot({ path: path.join(output, 'heart-tilted-dots.png') });
    await page.locator('#show-source').check();
    await page.locator('#capture-params').click();
    const saved = await page.locator('#param-dump').inputValue();
    assert.equal(JSON.parse(saved).roll, 18);
    await set('roll', 0);
    await page.locator('[data-mood="ANGRY"]').click();
    await page.locator('#apply-params').click();
    await page.waitForTimeout(1800);
    assert.equal(await page.locator('#roll').inputValue(), '18');
    assert.ok(tilted.equals(await canvas.screenshot()), 'Captured pose must restore exactly');

    // Sphere mode: the same presets, mapped onto the sphere surface.
    await page.locator('#show-source').check();
    await page.locator('#sphere-mode').check();
    await set('intensity', 1);
    const sphereShots = [];
    for (const mood of ['NEUTRAL', 'HEART', 'ANGRY']) {
      await page.locator(`[data-mood="${mood}"]`).click();
      await page.waitForTimeout(1500);
      sphereShots.push((await canvas.screenshot({ path: path.join(output, `sphere-${mood.toLowerCase()}-raw.png`) })).toString('base64'));
    }
    assert.equal(new Set(sphereShots).size, 3, 'sphere: presets must render differently');
    await page.locator('[data-mood="NEUTRAL"]').click();
    await page.waitForTimeout(1500);
    const box = await canvas.boundingBox();
    await page.mouse.move(box.x + box.width * 0.15, box.y + box.height * 0.5);
    await page.waitForTimeout(1500);
    const gazeLeft = await canvas.screenshot({ path: path.join(output, 'sphere-gaze-left.png') });
    await page.mouse.move(box.x + box.width * 0.85, box.y + box.height * 0.5);
    await page.waitForTimeout(1500);
    const gazeRight = await canvas.screenshot({ path: path.join(output, 'sphere-gaze-right.png') });
    assert.ok(!gazeLeft.equals(gazeRight), 'sphere: gaze must move the eyes on the surface');
    await page.locator('#show-source').uncheck();
    await canvas.screenshot({ path: path.join(output, 'sphere-neutral-dots.png') });

    const downloadPromise = page.waitForEvent('download');
    await page.locator('#capture').click();
    const download = await downloadPromise;
    await download.saveAs(path.join(output, 'capture-download.png'));
    assert.match(download.suggestedFilename(), /^smolcase-expression-neutral-\d+\.png$/);
    assert.deepEqual(errors, [], 'Browser and shader errors');
    const result = { moods, distinctRaw: 16, distinctDots: 16, neutralAtZero: 16,
      tiltedRoundTrip: 'pass', pngDownload: 'pass',
      spherePresets: 'pass', sphereGaze: 'pass', errors };
    await fs.writeFile(path.join(output, 'results.json'), JSON.stringify(result, null, 2));
    // Contact sheets show the same crop of each canvas, without the controls.
    const aspect = await canvas.evaluate(c => c.width / c.height);
    const sheet = await context.newPage();
    await sheet.setViewportSize({ width: 1280, height: 1000 });
    for (const mode of ['raw', 'dots']) {
      const tiles = moods.map((mood, i) => `<article><header>${mood}</header><div>
        <img src="data:image/png;base64,${captures[mode][i]}" /></div></article>`).join('');
      await sheet.setContent(`<style>
        body { margin:0; background:#101820; color:#e0e8ee; font:18px system-ui;
          display:grid; grid-template-columns:repeat(4,320px); }
        article { height:250px; padding:10px; box-sizing:border-box; }
        header { height:28px; }
        article div { height:202px; overflow:hidden; background:black; }
        img { width:300px; transform:translateY(-${300 / aspect * 0.19}px); }
      </style>${tiles}`);
      await sheet.locator('img').evaluateAll(images => Promise.all(images.map(img => img.decode())));
      await sheet.screenshot({ path: path.join(output, `contact-${mode}.png`) });
    }
    await sheet.close();
    console.log(JSON.stringify(result, null, 2));
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
