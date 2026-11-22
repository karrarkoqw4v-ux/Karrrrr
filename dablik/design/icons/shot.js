const { chromium } = require('playwright');
(async()=>{
  const b=await chromium.launch();
  const pg=await b.newPage({viewport:{width:1200,height:300},deviceScaleFactor:2});
  await pg.goto('file://'+process.cwd()+'/grid.html');
  await pg.waitForTimeout(300);
  await pg.screenshot({path:'icons_grid.png',fullPage:true});
  await b.close();
})();
