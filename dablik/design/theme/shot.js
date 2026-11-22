const { chromium } = require('playwright');
(async()=>{
  const b=await chromium.launch();
  const pg=await b.newPage({viewport:{width:1080,height:2400},deviceScaleFactor:1});
  await pg.goto('file://'+process.cwd()+'/scene.html');
  await pg.waitForTimeout(300);
  // lockscreen (cc hidden off-screen below)
  await pg.evaluate(()=>{document.getElementById('cc').style.transform='translateY(100%)'});
  await pg.screenshot({path:'lockscreen.png'});
  // control center full
  await pg.evaluate(()=>{document.getElementById('cc').style.transform='translateY(0)'});
  await pg.waitForTimeout(100);
  await pg.screenshot({path:'controlcenter.png'});
  await b.close();
})();
