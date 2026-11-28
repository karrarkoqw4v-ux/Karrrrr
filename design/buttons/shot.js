const { chromium } = require('playwright');
(async () => {
  const b = await chromium.launch();
  const pg = await b.newPage({ viewport: { width: 2500, height: 3200 }, deviceScaleFactor: 1 });
  await pg.goto('file://' + process.cwd() + '/styles.html');
  await pg.waitForTimeout(300);

  const sections = [
    { id: 'navs', out: 'bottom_nav_styles.png', title: true },
    { id: 'tops', out: 'top_bar_styles.png' },
    { id: 'settingsrows', out: 'settings_row_styles.png' },
    { id: 'logins', out: 'login_redesigns.png' },
  ];
  for (const s of sections) {
    const el = await pg.$('#' + s.id);
    const box = await el.boundingBox();
    // include the h1 title right above the row
    await pg.screenshot({
      path: s.out,
      clip: { x: 0, y: box.y - 90, width: 2500, height: box.height + 110 },
    });
  }
  await b.close();
})();
