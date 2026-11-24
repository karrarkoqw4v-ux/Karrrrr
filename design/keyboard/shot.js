const { chromium } = require('playwright');
(async () => {
  const b = await chromium.launch();
  const pg = await b.newPage({ viewport: { width: 2300, height: 1500 }, deviceScaleFactor: 1 });
  await pg.goto('file://' + process.cwd() + '/styles.html');
  await pg.waitForTimeout(300);
  await pg.screenshot({ path: 'keyboard_styles.png', clip: { x: 0, y: 0, width: 2300, height: 1500 } });
  await b.close();
})();
