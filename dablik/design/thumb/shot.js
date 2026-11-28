const { chromium } = require('playwright');
(async()=>{
  const b=await chromium.launch();
  const pg=await b.newPage({viewport:{width:1280,height:720},deviceScaleFactor:2});
  await pg.goto('file://'+process.cwd()+'/thumb.html');
  await pg.waitForTimeout(400);
  await pg.screenshot({path:'thumbnail.png'});
  await b.close();
})();
