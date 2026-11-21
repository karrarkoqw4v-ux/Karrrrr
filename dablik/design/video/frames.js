const { chromium } = require('playwright');
const fs=require('fs');
const [,, from, to, outdir] = process.argv;
(async()=>{
  const b=await chromium.launch();
  const pg=await b.newPage({viewport:{width:540,height:960},deviceScaleFactor:2});
  await pg.goto('file://'+process.cwd()+'/scene.html');
  await pg.waitForTimeout(500);
  fs.mkdirSync(outdir,{recursive:true});
  for(let i=+from;i<+to;i++){
    await pg.evaluate(t=>{render(t)}, i/30);
    await pg.screenshot({path:`${outdir}/f${String(i).padStart(4,'0')}.png`});
  }
  await b.close();
})();
