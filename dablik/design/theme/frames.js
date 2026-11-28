const { chromium } = require('playwright');
const fs=require('fs');
fs.mkdirSync('frames',{recursive:true});
(async()=>{
  const b=await chromium.launch();
  const pg=await b.newPage({viewport:{width:1080,height:2400},deviceScaleFactor:1});
  await pg.goto('file://'+process.cwd()+'/scene.html');
  await pg.waitForTimeout(200);
  const total=120; // 4s @30fps: 0-2s open, 2-3.4s hold, 3.4-4s close
  for(let i=0;i<total;i++){
    const t=i/29; // seconds
    let p;
    if(t<1.6){ const x=t/1.6; p=1-Math.pow(1-x,3); }
    else if(t<3.2){ p=1; }
    else { const x=(t-3.2)/0.8; p=1-(1-Math.pow(1-x,2)); }
    await pg.evaluate(p=>{
      const cc=document.getElementById('cc');
      cc.style.transform=`translateY(${(1-p)*100}%)`;
    }, p);
    await pg.screenshot({path:`frames/f${String(i).padStart(4,'0')}.png`});
  }
  await b.close();
})();
