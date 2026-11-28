const { chromium } = require('playwright');
const names=["1_home","2_home_apps","3_form","4_icon_editor","5_console_running","6_console_done","7_about"];
(async()=>{
  const b=await chromium.launch();
  const pg=await b.newPage({viewport:{width:390,height:844},deviceScaleFactor:2});
  await pg.goto('file://'+process.cwd()+'/shots2.html');
  await pg.waitForTimeout(700);
  for(let i=1;i<=names.length;i++){
    await pg.evaluate(i=>{document.querySelectorAll('.s').forEach(e=>e.classList.remove('on'));document.getElementById('s'+i).classList.add('on')},i);
    await pg.waitForTimeout(150);
    await pg.screenshot({path:`screens/${names[i-1]}.png`});
  }
  await b.close();
})();
