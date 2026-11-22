const { chromium } = require('playwright');
(async()=>{
  const b=await chromium.launch();
  const pg=await b.newPage({viewport:{width:1080,height:2400},deviceScaleFactor:1});
  await pg.goto('file://'+process.cwd()+'/home.html');
  await pg.waitForTimeout(300);
  await pg.screenshot({path:'home_icons.png'});
  await b.close();
})();
