/* التطبيق الرئيسي: اللوبي ثلاثي الأبعاد + خريطة المحافظات + تشغيل المود */
(function () {
  const $ = id => document.getElementById(id);
  const Q = new URLSearchParams(location.search);
  const renderer = new THREE.WebGLRenderer({ canvas: $('gl'), antialias: true, powerPreference: 'high-performance' });
  renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 2)); renderer.setSize(innerWidth, innerHeight);
  renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.05;

  /* ---------- الملف الشخصي (محفوظ محلياً) ---------- */
  let save = { name: 'لاعب_عراقي', xp: 0, coins: 1000, gems: 50, quest: 0, event: 0 };
  try { Object.assign(save, JSON.parse(localStorage.getItem('iraqi-game-save') || '{}')); } catch (e) {}
  const store = () => { try { localStorage.setItem('iraqi-game-save', JSON.stringify(save)); } catch (e) {} };
  const RANKS = ['مجنّد', 'جندي', 'جندي أول', 'عريف', 'رقيب', 'رقيب أول', 'ملازم', 'نقيب', 'رائد', 'مقدّم'];
  function refreshProfile() {
    const lvl = 1 + Math.floor(save.xp / 400), into = (save.xp % 400) / 400;
    $('pLvl').textContent = lvl; $('pXp').style.width = (into * 100) + '%'; $('pName').textContent = save.name; $('pRank').textContent = 'الرتبة: ' + RANKS[Math.min(RANKS.length - 1, Math.floor((lvl - 1) / 2))];
    $('pCoins').textContent = save.coins.toLocaleString('en'); $('pGems').textContent = save.gems;
    $('mCnt').textContent = Math.min(5, save.quest); $('mProg').style.width = Math.min(100, save.quest / 5 * 100) + '%';
    $('mCnt2').textContent = Math.min(1, save.event); $('mProg2').style.width = Math.min(100, save.event * 100) + '%';
  }

  /* ---------- مشهد اللوبي ---------- */
  const LS = new THREE.Scene(); LS.fog = new THREE.Fog(0x0b0a09, 7, 26);
  const lcam = new THREE.PerspectiveCamera(30, innerWidth / innerHeight, .1, 100);
  // خلفية: سماء الغروب + أفق بغداد
  (function backdrop() {
    const c = document.createElement('canvas'); c.width = 1024; c.height = 512; const g = c.getContext('2d');
    const gr = g.createLinearGradient(0, 0, 0, 512); gr.addColorStop(0, '#0a0d14'); gr.addColorStop(.55, '#2a2018'); gr.addColorStop(.8, '#8a4d1e'); gr.addColorStop(1, '#d99a4a');
    g.fillStyle = gr; g.fillRect(0, 0, 1024, 512);
    const sun = g.createRadialGradient(700, 400, 0, 700, 400, 220); sun.addColorStop(0, 'rgba(255,214,140,.9)'); sun.addColorStop(1, 'rgba(255,160,60,0)'); g.fillStyle = sun; g.fillRect(0, 0, 1024, 512);
    // أفق المدينة
    g.fillStyle = '#120d0a';
    let x = 0; const rs = (() => { let s = 7; return () => (s = (s * 16807) % 2147483647) / 2147483647; })();
    while (x < 1024) { const w = 18 + rs() * 46, h = 30 + rs() * 90; g.fillRect(x, 470 - h, w, h + 50); if (rs() < .15) { g.beginPath(); g.arc(x + w / 2, 470 - h, w / 2.2, Math.PI, 0); g.fill(); g.fillRect(x + w / 2 - 1, 470 - h - 30, 2, 30); } x += w + 2; }
    // قبة ومآذن
    g.fillStyle = '#0c0807'; g.beginPath(); g.arc(300, 400, 46, Math.PI, 0); g.fill(); g.fillRect(254, 400, 92, 70);
    for (const mx of [240, 360]) { g.fillRect(mx - 4, 330, 8, 140); g.beginPath(); g.arc(mx, 330, 6, Math.PI, 0); g.fill(); g.fillRect(mx - 1, 300, 2, 30); }
    g.fillStyle = 'rgba(255,200,110,.85)'; for (let i = 0; i < 260; i++) g.fillRect(Math.random() * 1024, 380 + Math.random() * 90, 2, 3);
    const t = new THREE.CanvasTexture(c);
    const plane = new THREE.Mesh(new THREE.PlaneGeometry(46, 23), new THREE.MeshBasicMaterial({ map: t, fog: false })); plane.position.set(2, 5.5, -15); LS.add(plane);
  })();
  // أرضية + منصة
  const floor = new THREE.Mesh(new THREE.CircleGeometry(30, 48), new THREE.MeshStandardMaterial({ color: 0x15130f, roughness: .75, metalness: .3 })); floor.rotation.x = -Math.PI / 2; floor.receiveShadow = true; LS.add(floor);
  const pedestal = new THREE.Group(); LS.add(pedestal);
  const disc = new THREE.Mesh(new THREE.CylinderGeometry(1.25, 1.35, .1, 56), new THREE.MeshStandardMaterial({ color: 0x1e2024, roughness: .35, metalness: .85 })); disc.position.y = .05; disc.receiveShadow = true; pedestal.add(disc);
  const ring = new THREE.Mesh(new THREE.TorusGeometry(1.22, .018, 8, 80), new THREE.MeshBasicMaterial({ color: 0xf2c14e })); ring.rotation.x = Math.PI / 2; ring.position.y = .105; pedestal.add(ring);
  const ring2 = new THREE.Mesh(new THREE.TorusGeometry(.9, .008, 8, 80), new THREE.MeshBasicMaterial({ color: 0xf2c14e, transparent: true, opacity: .5 })); ring2.rotation.x = Math.PI / 2; ring2.position.y = .108; pedestal.add(ring2);
  const glow = new THREE.Mesh(new THREE.CircleGeometry(2.4, 48), new THREE.MeshBasicMaterial({ color: 0xf2a63a, transparent: true, opacity: .16, blending: THREE.AdditiveBlending, depthWrite: false })); glow.rotation.x = -Math.PI / 2; glow.position.y = .02; LS.add(glow);
  // الجندي
  const soldier = Models.buildSoldier(); soldier.position.set(0, .1, 0); soldier.rotation.y = .55; pedestal.add(soldier);
  pedestal.position.set(-1.1, 0, 0);
  // إضاءة سينمائية
  LS.add(new THREE.HemisphereLight(0x5a6a88, 0x3a2d20, .65));
  const key = new THREE.SpotLight(0xffd9a0, 2.2, 20, .55, .6, 1); key.position.set(3.2, 4.2, 4); key.target.position.set(-1.1, 1.1, 0); key.castShadow = true; key.shadow.mapSize.set(1024, 1024); key.shadow.bias = -.0004; LS.add(key, key.target);
  const rimL = new THREE.SpotLight(0x5fa8ff, 2.4, 20, .7, .5, 1); rimL.position.set(-4.5, 3, -3); rimL.target.position.set(-1.1, 1.2, 0); LS.add(rimL, rimL.target);
  const rimR = new THREE.SpotLight(0xffb060, 3, 20, .7, .5, 1); rimR.position.set(2.5, 3, -3.5); rimR.target.position.set(-1.1, 1.2, 0); LS.add(rimR, rimR.target);
  const fill = new THREE.PointLight(0xffe0b0, .25, 8); fill.position.set(-2, 1.2, 3); LS.add(fill);
  // غبار عالق
  const dustN = 160, dgeo = new THREE.BufferGeometry(), dpos = new Float32Array(dustN * 3);
  for (let i = 0; i < dustN; i++) { dpos[i * 3] = (Math.random() - .5) * 10; dpos[i * 3 + 1] = Math.random() * 5; dpos[i * 3 + 2] = (Math.random() - .5) * 8 - 1; }
  dgeo.setAttribute('position', new THREE.BufferAttribute(dpos, 3));
  const dust = new THREE.Points(dgeo, new THREE.PointsMaterial({ color: 0xffd9a0, size: .03, transparent: true, opacity: .55, blending: THREE.AdditiveBlending, depthWrite: false })); LS.add(dust);
  lcam.position.set(0, 1.25, 6.3); lcam.lookAt(-.1, .95, 0);

  let rotVel = 0, dragging = false, lastX = 0, targetRot = .55;
  const cv = renderer.domElement;
  cv.addEventListener('pointerdown', e => { if (mode !== 'lobby') return; dragging = true; lastX = e.clientX; });
  addEventListener('pointerup', () => dragging = false);
  addEventListener('pointermove', e => { if (dragging && mode === 'lobby') { targetRot += (e.clientX - lastX) * .012; lastX = e.clientX; } });

  /* ---------- خريطة العراق ---------- */
  const DATA = window.IRAQ_PROVINCES;
  let lon0 = 1e9, lon1 = -1e9, lat0 = 1e9, lat1 = -1e9;
  for (const p of DATA) for (const poly of p.polys) for (const ring of poly) for (const [x, y] of ring) { lon0 = Math.min(lon0, x); lon1 = Math.max(lon1, x); lat0 = Math.min(lat0, y); lat1 = Math.max(lat1, y); }
  const kx = Math.cos(33 * Math.PI / 180), proj = (x, y) => [(x - lon0) * kx, (lat1 - y)];
  const VW = (lon1 - lon0) * kx, VH = lat1 - lat0;
  const pathOf = p => p.polys.map(poly => poly.map(r => 'M' + r.map(pt => proj(pt[0], pt[1]).map(v => v.toFixed(3)).join(',')).join('L') + 'Z').join('')).join('');
  function centroid(p) { let best = null, ba = 0; for (const poly of p.polys) { let a = 0, cx = 0, cy = 0; const r = poly[0].map(pt => proj(pt[0], pt[1])); for (let i = 0; i < r.length - 1; i++) { const f = r[i][0] * r[i + 1][1] - r[i + 1][0] * r[i][1]; a += f; cx += (r[i][0] + r[i + 1][0]) * f; cy += (r[i][1] + r[i + 1][1]) * f; } a /= 2; if (Math.abs(a) > ba) { ba = Math.abs(a); best = [cx / (6 * a), cy / (6 * a)]; } } return best; }
  const DESC = {
    Baghdad: 'العاصمة. تقاطع دجلة والجسور والأحياء القديمة. ساحة المعركة الأولى: الكرخ والرصافة، نصب الشهيد، مرقد الكاظمين وبرج بغداد.',
  };
  let selected = 'Baghdad', current = 'Baghdad';
  const PLAYABLE = { Baghdad: true };
  function buildMaps() {
    const svg = $('iraqSvg'); svg.setAttribute('viewBox', `-.1 -.1 ${VW + .2} ${VH + .2}`);
    let h = '', lab = '';
    for (const p of DATA) { h += `<path data-id="${p.id}" d="${pathOf(p)}" class="${PLAYABLE[p.id] ? 'playable' : ''}"/>`; const c = centroid(p); const big = p.id === 'Baghdad'; lab += `<text x="${c[0]}" y="${c[1]}" font-size="${big ? .17 : .13}">${p.ar}</text>`; if (PLAYABLE[p.id]) lab += `<circle class="cap" cx="${c[0]}" cy="${c[1] + .12}" r=".035"/>`; }
    svg.innerHTML = h + lab;
    svg.querySelectorAll('path').forEach(el => el.addEventListener('click', () => selectProv(el.dataset.id)));
    // قائمة
    $('plist').innerHTML = DATA.slice().sort((a, b) => (PLAYABLE[b.id] ? 1 : 0) - (PLAYABLE[a.id] ? 1 : 0) || a.ar.localeCompare(b.ar, 'ar')).map(p => `<button data-id="${p.id}"><span>${p.ar}</span><em class="${PLAYABLE[p.id] ? 'ok' : ''}">${PLAYABLE[p.id] ? 'متاح' : 'قريباً'}</em></button>`).join('');
    $('plist').querySelectorAll('button').forEach(b => b.addEventListener('click', () => selectProv(b.dataset.id)));
    // مصغّرة اللوبي
    const th = $('thumbSvg'); th.setAttribute('viewBox', `-.1 -.1 ${VW + .2} ${VH + .2}`);
    th.innerHTML = DATA.map(p => `<path d="${pathOf(p)}" fill="${p.id === current ? '#e0a92a' : '#2b3646'}" stroke="#6e7f95" stroke-width=".02"/>`).join('');
    placePin();
  }
  function placePin() { const p = DATA.find(x => x.id === current), c = centroid(p), th = $('thumbSvg'), box = th.getBoundingClientRect(); const pin = document.querySelector('.pin'); const sc = Math.min(box.width / (VW + .2), box.height / (VH + .2)), ox = (box.width - (VW + .2) * sc) / 2, oy = (box.height - (VH + .2) * sc) / 2; pin.style.left = (ox + (c[0] + .1) * sc - 5) + 'px'; pin.style.top = (oy + (c[1] + .1) * sc - 5) + 'px'; }
  function selectProv(id) {
    selected = id; const p = DATA.find(x => x.id === id);
    document.querySelectorAll('#iraqSvg path').forEach(el => el.classList.toggle('sel', el.dataset.id === id));
    document.querySelectorAll('#plist button').forEach(el => el.classList.toggle('on', el.dataset.id === id));
    $('piName').textContent = p.ar; $('piCap').textContent = p.cap;
    const ok = !!PLAYABLE[id]; $('piState').textContent = ok ? 'متاح للعب' : 'قريباً'; $('piState').className = ok ? 'ok' : 'no';
    $('piDesc').textContent = DESC[id] || 'هذه المحافظة قيد التطوير. سيتم إضافة خريطتها في التحديثات القادمة.'; $('piBtn').disabled = !ok; $('piBtn').textContent = ok ? 'اعتماد الخريطة' : 'غير متاح بعد';
  }

  /* ---------- التنقل بين الشاشات ---------- */
  let mode = 'lobby', game = null, gameScene = null, world = null;
  function showScreen(id) { for (const s of ['lobby', 'mapScreen', 'loading']) $(s).classList.toggle('hidden', s !== id); }
  function openModal(t, b) { $('mTitle').textContent = t; $('mBody').textContent = b; $('modal').classList.remove('hidden'); }
  $('mClose').onclick = () => $('modal').classList.add('hidden');
  $('modeCard').onclick = () => { $('mapScreen').classList.remove('hidden'); $('lobby').classList.add('hidden'); mode = 'map'; selectProv(current); };
  $('mapBack').onclick = () => { showScreen('lobby'); mode = 'lobby'; };
  $('piBtn').onclick = () => { if (!PLAYABLE[selected]) return; current = selected; $('mapName').textContent = DATA.find(x => x.id === current).ar; showScreen('lobby'); mode = 'lobby'; buildMaps(); selectProv(current); };
  $('modeRow').onclick = e => { const b = e.target.closest('button'); if (!b) return; if (b.dataset.m !== 'solo') return openModal('قريباً', 'الأوضاع الثنائية والفرق ستتوفر عند إضافة اللعب الجماعي عبر الإنترنت. حالياً الوضع الفردي ضد 15 عدو.'); [...$('modeRow').children].forEach(x => x.classList.toggle('on', x === b)); };
  $('tabs').onclick = e => { const b = e.target.closest('button'); if (!b) return; const t = b.dataset.t; if (t === 'home') return; const names = { weapons: 'الأسلحة', team: 'الفريق', shop: 'المتجر', missions: 'المهام' }; openModal(names[t], t === 'weapons' ? 'سلاحك الحالي: بندقية M4A1 مع كاتم صوت ومنظار هولوغرافي وقبضة أمامية وليزر. المزيد من الأسلحة قريباً.' : 'هذه الصفحة قيد التطوير وستتوفر في التحديثات القادمة.'); };
  $('squad').onclick = () => openModal('دعوة الأصدقاء', 'اللعب الجماعي يحتاج خادماً على الإنترنت، وسيضاف لاحقاً.');
  for (const id of ['btnMail', 'btnSet']) $(id).onclick = () => openModal(id === 'btnSet' ? 'الإعدادات' : 'البريد', id === 'btnSet' ? 'التحكم: WASD + الفأرة، زر أيسر إطلاق، زر أيمن تصويب، R تعبئة، Shift ركض، Space قفز. على الموبايل: عصا الحركة والأزرار على الشاشة.' : 'لا توجد رسائل جديدة.');

  /* ---------- بدء المباراة ---------- */
  const TIPS = ['استخدم الجسور لعبور دجلة — الماء يعيقك.', 'اضغط زر التصويب لتقليل الارتداد وزيادة الدقة.', 'الضربة بالرأس تقتل أسرع بكثير.', 'ابقَ داخل الدائرة الآمنة، الخارج يؤذيك.', 'النخيل والسيارات والحواجز الكونكريتية غطاء جيد.', 'خذ الصناديق الصفراء للذخيرة والبيضاء للإسعاف.'];
  function startMatch() {
    mode = 'loading'; showScreen('loading'); $('lTip').textContent = TIPS[(Math.random() * TIPS.length) | 0]; setLoad(5, 'جارِ إنزال القوات في بغداد…');
    setTimeout(() => {
      if (!world) {
        gameScene = new THREE.Scene(); setLoad(25, 'جارِ بناء مدينة بغداد…');
        setTimeout(() => {
          world = BaghdadWorld.build(gameScene); setLoad(70, 'جارِ تجهيز الأعداء…');
          setTimeout(() => { game = new Game(renderer, world, gameScene, backToLobby); begin(); }, 60);
        }, 60);
      } else begin();
    }, 80);
  }
  function setLoad(p, m) { $('lFill').style.width = p + '%'; if (m) $('lMsg').textContent = m; }
  function begin() {
    setLoad(100, 'اهبط!'); setTimeout(() => { $('loading').classList.add('hidden'); $('lobby').classList.add('hidden'); mode = 'game'; game.show(); game.start(); updateLock(); }, 350);
  }
  function backToLobby() {
    if (game) { const r = game.reward; game.stop(); if (r) { save.xp += r.xp; save.coins += r.coins; save.quest += r.kills; if (r.win) save.event = 1; store(); } }
    mode = 'lobby'; $('endScreen').classList.remove('show'); showScreen('lobby'); refreshProfile(); $('lockPrompt').classList.remove('show');
  }
  $('btnStart').onclick = () => startMatch();
  $('btnLobby').onclick = backToLobby;
  $('btnAgain').onclick = () => { const r = game.reward; if (r) { save.xp += r.xp; save.coins += r.coins; save.quest += r.kills; if (r.win) save.event = 1; store(); game.reward = null; } $('endScreen').classList.remove('show'); game.start(); updateLock(); };
  $('bExit').onclick = () => { if (mode === 'game') { game.over = true; game.reward = null; backToLobby(); } };
  addEventListener('keydown', e => { if (e.code === 'Escape' && mode === 'game' && !document.pointerLockElement && !game.over) { /* المؤشر يحرَّر بـ Esc تلقائياً */ } });

  const touch = 'ontouchstart' in window;
  function updateLock() { $('lockPrompt').classList.toggle('show', mode === 'game' && !touch && !document.pointerLockElement && !(game && game.over)); }
  document.addEventListener('pointerlockchange', updateLock);
  $('lockPrompt').addEventListener('mousedown', () => { cv.requestPointerLock && cv.requestPointerLock(); });
  $('lockPrompt').style.pointerEvents = 'auto'; $('lockPrompt').addEventListener('click', () => cv.requestPointerLock && cv.requestPointerLock());

  addEventListener('resize', () => { renderer.setSize(innerWidth, innerHeight); lcam.aspect = innerWidth / innerHeight; lcam.updateProjectionMatrix(); if (mode === 'lobby') placePin(); });

  /* ---------- الحلقة ---------- */
  let last = performance.now(), T = 0;
  function loop(now) {
    requestAnimationFrame(loop); const dt = Math.min(.05, (now - last) / 1000); last = now; T += dt;
    if (mode === 'game' && game && game.active) { game.update(dt); updateLockMaybe(); renderer.render(gameScene, game.cam); return; }
    if (mode === 'lobby' || mode === 'map' || mode === 'loading' || !game) {
      pedestal.rotation.y += (targetRot - pedestal.rotation.y - .55) * .1 * 0; soldier.rotation.y += (targetRot - soldier.rotation.y) * Math.min(1, dt * 8);
      if (!dragging) targetRot += Math.sin(T * .4) * dt * .08;
      const br = Math.sin(T * 1.6); soldier.userData.head.rotation.y = Math.sin(T * .5) * .12; soldier.position.y = .1 + br * .006; soldier.userData.rifle.rotation.x = br * .008;
      ring2.rotation.z = T * .5; glow.material.opacity = .14 + Math.sin(T * 2) * .03;
      const arr = dust.geometry.attributes.position.array; for (let i = 0; i < dustN; i++) { arr[i * 3 + 1] += dt * .06; arr[i * 3] += Math.sin(T + i) * dt * .02; if (arr[i * 3 + 1] > 5) arr[i * 3 + 1] = 0; } dust.geometry.attributes.position.needsUpdate = true;
      lcam.position.x = Math.sin(T * .2) * .15; lcam.lookAt(-.1, .95, 0);
      renderer.render(LS, lcam);
    } else if (mode === 'game' && game) renderer.render(gameScene, game.cam);
  }
  let lockT = 0; function updateLockMaybe() { lockT += 1; if (lockT % 20 === 0) updateLock(); }

  buildMaps(); selectProv(current); refreshProfile();
  if (Q.get('autostart')) setTimeout(startMatch, 300);
  requestAnimationFrame(loop);
  window.__app = { get game() { return game; }, startMatch, get world() { return world; } };
})();
