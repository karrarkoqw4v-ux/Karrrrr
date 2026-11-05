/* التطبيق: شاشة البداية → اللوبي (شخصيات واقعية متحركة) → البحث عن سيرفر → التحميل → المود */
(function () {
  const $ = id => document.getElementById(id);
  const Q = new URLSearchParams(location.search), S = window.GameSettings;
  const renderer = new THREE.WebGLRenderer({ canvas: $('gl'), antialias: true, powerPreference: 'high-performance' });
  renderer.setSize(innerWidth, innerHeight); renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.05;

  /* ---------- الملف الشخصي ---------- */
  let save = { name: 'لاعب_عراقي', xp: 0, coins: 1000, gems: 50, quest: 0, event: 0, matches: 0, wins: 0 };
  try { Object.assign(save, JSON.parse(localStorage.getItem('iraqi-game-save') || '{}')); } catch (e) {}
  const store = () => { try { localStorage.setItem('iraqi-game-save', JSON.stringify(save)); } catch (e) {} };
  const RANKS = ['مجنّد', 'جندي', 'جندي أول', 'عريف', 'رقيب', 'رقيب أول', 'ملازم', 'نقيب', 'رائد', 'مقدّم'];
  function refreshProfile() {
    const lvl = 1 + Math.floor(save.xp / 400); $('pLvl').textContent = lvl; $('pXp').style.width = (save.xp % 400) / 4 + '%'; $('pName').textContent = save.name; $('pRank').textContent = RANKS[Math.min(9, Math.floor((lvl - 1) / 2))];
    $('pCoins').textContent = save.coins.toLocaleString('en'); $('pGems').textContent = save.gems;
    $('mCnt').textContent = Math.min(5, save.quest); $('mProg').style.width = Math.min(100, save.quest * 20) + '%'; $('mCnt2').textContent = Math.min(1, save.event); $('mProg2').style.width = save.event ? '100%' : '0';
  }

  /* ---------- الرسوميات (تطبيق الإعدادات) ---------- */
  let gameScene = null, world = null, game = null, fpsCap = 90;
  window.applyGraphics = () => {
    const dpr = Math.min(devicePixelRatio || 1, 2), st = S.gfxStyle, q = S.gfxQuality, sc = S.renderScale / 100;
    renderer.setPixelRatio((st === 'save' ? Math.min(dpr, 1) * .75 : dpr) * sc);
    renderer.toneMappingExposure = st === 'hdr' ? 1.15 : st === 'hdr10' ? 1.22 : st === 'save' ? .98 : 1.05;
    $('gl').style.filter = st === 'hdr' ? 'contrast(1.06) saturate(1.1)' : st === 'hdr10' ? 'contrast(1.12) saturate(1.25)' : '';
    renderer.shadowMap.enabled = st !== 'save';
    fpsCap = S.fps > 90 ? 90 : S.fps;
    if (world) { const sz = q === 'strong' ? 4096 : q === 'high' ? 2048 : 1024; const sh = world.sun.shadow; if (sh.mapSize.x !== sz) { sh.mapSize.set(sz, sz); if (sh.map) { sh.map.dispose(); sh.map = null; } }
      gameScene.fog.far = q === 'strong' ? 720 : q === 'high' ? 560 : 440; world.sun.castShadow = st !== 'save'; }
    renderer.setSize(innerWidth, innerHeight);
  };

  /* ---------- مشهد اللوبي ---------- */
  const LS = new THREE.Scene(); LS.fog = new THREE.Fog(0x0b0a09, 8, 28);
  const lcam = new THREE.PerspectiveCamera(30, innerWidth / innerHeight, .1, 100);
  (function backdrop() {
    const c = document.createElement('canvas'); c.width = 1024; c.height = 512; const g = c.getContext('2d');
    const gr = g.createLinearGradient(0, 0, 0, 512); gr.addColorStop(0, '#0a0d14'); gr.addColorStop(.55, '#2a2018'); gr.addColorStop(.8, '#8a4d1e'); gr.addColorStop(1, '#d99a4a'); g.fillStyle = gr; g.fillRect(0, 0, 1024, 512);
    const sun = g.createRadialGradient(700, 400, 0, 700, 400, 220); sun.addColorStop(0, 'rgba(255,214,140,.9)'); sun.addColorStop(1, 'rgba(255,160,60,0)'); g.fillStyle = sun; g.fillRect(0, 0, 1024, 512);
    g.fillStyle = '#120d0a'; let x = 0, sd = 7; const rs = () => (sd = (sd * 16807) % 2147483647) / 2147483647;
    while (x < 1024) { const w = 18 + rs() * 46, h = 30 + rs() * 90; g.fillRect(x, 470 - h, w, h + 50); if (rs() < .15) { g.beginPath(); g.arc(x + w / 2, 470 - h, w / 2.2, Math.PI, 0); g.fill(); } x += w + 2; }
    g.fillStyle = '#0c0807'; g.beginPath(); g.arc(300, 400, 46, Math.PI, 0); g.fill(); g.fillRect(254, 400, 92, 70);
    for (const mx of [240, 360]) { g.fillRect(mx - 4, 330, 8, 140); g.beginPath(); g.arc(mx, 330, 6, Math.PI, 0); g.fill(); g.fillRect(mx - 1, 300, 2, 30); }
    g.fillRect(820, 250, 10, 220); g.beginPath(); g.ellipse(825, 255, 26, 10, 0, 0, 7); g.fill();
    g.fillStyle = 'rgba(255,200,110,.85)'; for (let i = 0; i < 260; i++) g.fillRect(Math.random() * 1024, 380 + Math.random() * 90, 2, 3);
    const plane = new THREE.Mesh(new THREE.PlaneGeometry(50, 25), new THREE.MeshBasicMaterial({ map: new THREE.CanvasTexture(c), fog: false })); plane.position.set(2, 6, -16); LS.add(plane);
  })();
  const floor = new THREE.Mesh(new THREE.CircleGeometry(30, 48), new THREE.MeshStandardMaterial({ color: 0x15130f, roughness: .7, metalness: .35 })); floor.rotation.x = -Math.PI / 2; floor.receiveShadow = true; LS.add(floor);
  const pedestal = new THREE.Group(); pedestal.position.set(-1.1, 0, 0); LS.add(pedestal);
  const disc = new THREE.Mesh(new THREE.CylinderGeometry(1.25, 1.35, .1, 56), new THREE.MeshStandardMaterial({ color: 0x1e2024, roughness: .35, metalness: .85 })); disc.position.y = .05; disc.receiveShadow = true; pedestal.add(disc);
  const ring = new THREE.Mesh(new THREE.TorusGeometry(1.22, .018, 8, 80), new THREE.MeshBasicMaterial({ color: 0xf2c14e })); ring.rotation.x = Math.PI / 2; ring.position.y = .105; pedestal.add(ring);
  const ring2 = new THREE.Mesh(new THREE.TorusGeometry(.9, .008, 8, 80), new THREE.MeshBasicMaterial({ color: 0xf2c14e, transparent: true, opacity: .5 })); ring2.rotation.x = Math.PI / 2; ring2.position.y = .108; pedestal.add(ring2);
  const glow = new THREE.Mesh(new THREE.CircleGeometry(2.4, 48), new THREE.MeshBasicMaterial({ color: 0xf2a63a, transparent: true, opacity: .16, blending: THREE.AdditiveBlending, depthWrite: false })); glow.rotation.x = -Math.PI / 2; glow.position.set(-1.1, .02, 0); LS.add(glow);
  LS.add(new THREE.HemisphereLight(0x5a6a88, 0x3a2d20, .7));
  const key = new THREE.SpotLight(0xffd9a0, 2.4, 22, .6, .6, 1); key.position.set(3.2, 4.2, 4); key.target.position.set(-1.1, 1.1, 0); key.castShadow = true; key.shadow.mapSize.set(1024, 1024); key.shadow.bias = -.0004; LS.add(key, key.target);
  const rimL = new THREE.SpotLight(0x5fa8ff, 3, 20, .8, .5, 1); rimL.position.set(-4.5, 3, -3); rimL.target.position.set(-1.1, 1.2, 0); LS.add(rimL, rimL.target);
  const rimR = new THREE.SpotLight(0xffb060, 3.4, 20, .8, .5, 1); rimR.position.set(2.5, 3, -3.5); rimR.target.position.set(-1.1, 1.2, 0); LS.add(rimR, rimR.target);
  const dustN = 160, dgeo = new THREE.BufferGeometry(), dpos = new Float32Array(dustN * 3);
  for (let i = 0; i < dustN; i++) { dpos[i * 3] = (Math.random() - .5) * 10; dpos[i * 3 + 1] = Math.random() * 5; dpos[i * 3 + 2] = (Math.random() - .5) * 8 - 1; }
  dgeo.setAttribute('position', new THREE.BufferAttribute(dpos, 3));
  const dust = new THREE.Points(dgeo, new THREE.PointsMaterial({ color: 0xffd9a0, size: .03, transparent: true, opacity: .55, blending: THREE.AdditiveBlending, depthWrite: false })); LS.add(dust);
  // الشخصيات: البطل + فريقه
  let hero = null, heroObj = null; const crew = [];
  function setupCharacters(ok) {
    if (ok) {
      hero = Characters.create('cts', Models.buildRifle); heroObj = hero.root; heroObj.position.y = .1;
      for (const [look, x, z, ry, anim, gun] of [['cts2', -2.9, -1.4, .55, 'Idle', Weapons.buildAK], ['cts3', .7, -1.6, -.2, 'Idle', Weapons.buildSniper], ['cts2', 2.6, -4, -1.1, 'Walk', Models.buildRifle]]) {
        const c = Characters.create(look, gun); c.root.position.set(x, 0, z); c.root.rotation.y = ry; c.play(anim, 0); c.mixer.update(Math.random() * 2); c.walk = anim === 'Walk'; LS.add(c.root); crew.push(c);
      }
    } else { heroObj = Models.buildSoldier(); heroObj.position.y = .1; }
    heroObj.rotation.y = .5; pedestal.add(heroObj);
  }
  let targetRot = .5, dragging = false, lastX = 0;
  const cv = renderer.domElement;
  cv.addEventListener('pointerdown', e => { if (mode !== 'lobby') return; dragging = true; lastX = e.clientX; });
  addEventListener('pointerup', () => dragging = false);
  addEventListener('pointermove', e => { if (dragging && mode === 'lobby') { targetRot += (e.clientX - lastX) * .012; lastX = e.clientX; } });

  /* ---------- قائمة الأسلحة في اللوبي ---------- */
  let shownW = 'm4';
  function weaponCard() {
    const w = Weapons.W[shownW], bar = (n, v) => `<div class="stat"><span>${n}</span><div><i style="width:${Math.round(v)}%"></i></div></div>`;
    $('wcName').textContent = w.name; $('wcType').textContent = w.type + ' · ' + Weapons.AMMO[w.ammo];
    $('wcStats').innerHTML = bar('الضرر', w.dmg) + bar('المدى', w.scope ? 95 : w.pistol ? 35 : 70) + bar('الثبات', 100 - w.recoil * 2500) + bar('معدل الإطلاق', Math.min(100, 9 / w.rate));
    $('wSel').innerHTML = Object.keys(Weapons.W).map(k => `<button data-w="${k}" class="${k === shownW ? 'on' : ''}">${Weapons.W[k].name}</button>`).join('');
  }
  $('wSel').onclick = e => { const b = e.target.closest('button'); if (!b) return; shownW = b.dataset.w; weaponCard(); if (hero) hero.setGun(Weapons.W[shownW].build); };

  /* ---------- خريطة العراق ---------- */
  const DATA = window.IRAQ_PROVINCES; let lon0 = 1e9, lon1 = -1e9, lat0 = 1e9, lat1 = -1e9;
  for (const p of DATA) for (const poly of p.polys) for (const r of poly) for (const [x, y] of r) { lon0 = Math.min(lon0, x); lon1 = Math.max(lon1, x); lat0 = Math.min(lat0, y); lat1 = Math.max(lat1, y); }
  const kx = Math.cos(33 * Math.PI / 180), proj = (x, y) => [(x - lon0) * kx, (lat1 - y)], VW = (lon1 - lon0) * kx, VH = lat1 - lat0;
  const pathOf = p => p.polys.map(poly => poly.map(r => 'M' + r.map(pt => proj(pt[0], pt[1]).map(v => v.toFixed(3)).join(',')).join('L') + 'Z').join('')).join('');
  function centroid(p) { let best = null, ba = 0; for (const poly of p.polys) { let a = 0, cx = 0, cy = 0; const r = poly[0].map(pt => proj(pt[0], pt[1])); for (let i = 0; i < r.length - 1; i++) { const f = r[i][0] * r[i + 1][1] - r[i + 1][0] * r[i][1]; a += f; cx += (r[i][0] + r[i + 1][0]) * f; cy += (r[i][1] + r[i + 1][1]) * f; } a /= 2; if (Math.abs(a) > ba) { ba = Math.abs(a); best = [cx / (6 * a), cy / (6 * a)]; } } return best; }
  let selected = 'Baghdad', current = 'Baghdad'; const PLAYABLE = { Baghdad: true };
  const DESC = { Baghdad: 'العاصمة. تهبط بالمظلة فوق مطار بغداد الدولي، ثم دجلة والجسور والكرخ والرصافة، نصب الشهيد، مرقد الكاظمين وبرج بغداد. بيوت يمكن دخولها ولوت أسلحة وعلاج.' };
  function buildMaps() {
    const svg = $('iraqSvg'); svg.setAttribute('viewBox', `-.1 -.1 ${VW + .2} ${VH + .2}`); let h = '', lab = '';
    for (const p of DATA) { h += `<path data-id="${p.id}" d="${pathOf(p)}" class="${PLAYABLE[p.id] ? 'playable' : ''}"/>`; const c = centroid(p); lab += `<text x="${c[0]}" y="${c[1]}" font-size="${p.id === 'Baghdad' ? .17 : .13}">${p.ar}</text>`; }
    svg.innerHTML = h + lab; svg.querySelectorAll('path').forEach(el => el.addEventListener('click', () => selectProv(el.dataset.id)));
    $('plist').innerHTML = DATA.slice().sort((a, b) => (PLAYABLE[b.id] ? 1 : 0) - (PLAYABLE[a.id] ? 1 : 0) || a.ar.localeCompare(b.ar, 'ar')).map(p => `<button data-id="${p.id}"><span>${p.ar}</span><em class="${PLAYABLE[p.id] ? 'ok' : ''}">${PLAYABLE[p.id] ? 'متاح' : 'قريباً'}</em></button>`).join('');
    $('plist').querySelectorAll('button').forEach(b => b.addEventListener('click', () => selectProv(b.dataset.id)));
    const th = $('thumbSvg'); th.setAttribute('viewBox', `-.1 -.1 ${VW + .2} ${VH + .2}`); th.innerHTML = DATA.map(p => `<path d="${pathOf(p)}" fill="${p.id === current ? '#e0a92a' : '#2b3646'}" stroke="#6e7f95" stroke-width=".02"/>`).join('');
    placePin();
  }
  function placePin() { const c = centroid(DATA.find(x => x.id === current)), box = $('thumbSvg').getBoundingClientRect(), pin = document.querySelector('.pin'); const sc = Math.min(box.width / (VW + .2), box.height / (VH + .2)), ox = (box.width - (VW + .2) * sc) / 2, oy = (box.height - (VH + .2) * sc) / 2; pin.style.left = (ox + (c[0] + .1) * sc - 5) + 'px'; pin.style.top = (oy + (c[1] + .1) * sc - 5) + 'px'; }
  function selectProv(id) {
    selected = id; const p = DATA.find(x => x.id === id), ok = !!PLAYABLE[id];
    document.querySelectorAll('#iraqSvg path').forEach(el => el.classList.toggle('sel', el.dataset.id === id)); document.querySelectorAll('#plist button').forEach(el => el.classList.toggle('on', el.dataset.id === id));
    $('piName').textContent = p.ar; $('piCap').textContent = p.cap; $('piState').textContent = ok ? 'متاح للعب' : 'قريباً'; $('piState').className = ok ? 'ok' : 'no';
    $('piDesc').textContent = DESC[id] || 'هذه المحافظة قيد التطوير. ستضاف خريطتها في التحديثات القادمة.'; $('piBtn').disabled = !ok; $('piBtn').textContent = ok ? 'اعتماد الخريطة' : 'غير متاح بعد';
  }

  /* ---------- التنقل ---------- */
  let mode = 'start';
  function show(id) { for (const s of ['startScreen', 'lobby', 'mapScreen', 'loading']) $(s).classList.toggle('hidden', s !== id); }
  function modal(t, html) { $('mTitle').textContent = t; $('mBody').innerHTML = html; $('modal').classList.remove('hidden'); }
  $('mClose').onclick = () => $('modal').classList.add('hidden');
  $('modeCard').onclick = () => { show('mapScreen'); mode = 'map'; selectProv(current); };
  $('mapBack').onclick = () => { show('lobby'); mode = 'lobby'; };
  $('piBtn').onclick = () => { if (!PLAYABLE[selected]) return; current = selected; $('mapName').textContent = DATA.find(x => x.id === current).ar; show('lobby'); mode = 'lobby'; buildMaps(); };
  $('modeRow').onclick = e => { const b = e.target.closest('button'); if (!b) return; if (b.dataset.m !== 'solo') return modal('قريباً', '<p>الأوضاع الثنائية والفرق تحتاج سيرفر أونلاين. حالياً: فردي ضد 15 لاعب ذكاء اصطناعي.</p>'); };
  function viewRow() { [...$('viewRow').children].forEach(b => b.classList.toggle('on', b.dataset.v === S.view)); $('startSub').textContent = 'بغداد · فردي · ' + (S.view === 'tpp' ? 'منظور ثالث' : 'منظور أول'); }
  $('viewRow').onclick = e => { const b = e.target.closest('button'); if (!b) return; S.view = b.dataset.v; saveSettings(); viewRow(); };
  window.onSettingsChanged = viewRow;
  $('tabs').onclick = e => { const b = e.target.closest('button'); if (!b) return; const t = b.dataset.t; if (t === 'home') return;
    if (t === 'weapons') return modal('ترسانة الأسلحة', Object.values(Weapons.W).map(w => `<div class="wrow"><b>${w.name}</b><span>${w.type} · ${Weapons.AMMO[w.ammo]} · الضرر ${w.dmg}</span></div>`).join('') + '<p class="note">الأسلحة تجدها لوت داخل البيوت وحظائر المطار.</p>');
    modal({ team: 'الفريق', shop: 'المتجر', missions: 'المهام' }[t], `<p>${t === 'missions' ? `المباريات: ${save.matches} · الانتصارات: ${save.wins}` : 'هذه الصفحة قيد التطوير.'}</p>`); };
  $('squad').onclick = () => modal('دعوة الأصدقاء', '<p>اللعب الجماعي يحتاج سيرفر أونلاين وسيضاف لاحقاً. حالياً السيرفر بذكاء اصطناعي.</p>');
  $('btnSet').onclick = () => openSettings('gfx'); $('btnMail').onclick = () => modal('البريد', '<p>مرحباً بك في لعبة عراقية! هدية الترحيب: 🪙 1000</p>');
  $('countryChip').onclick = () => modal('الدولة والمنطقة', '<p>🇮🇶 العراق — سيرفر بغداد. يمكنك التغيير من الإعدادات ← اللغة والمنطقة.</p>');

  /* ---------- شاشة البداية ---------- */
  let charsReady = false;
  (function boot() {
    let p = 0; const tick = setInterval(() => { p = Math.min(charsReady ? 100 : 90, p + 4 + Math.random() * 6); $('ssFill').style.width = p + '%'; $('ssMsg').textContent = p < 40 ? 'جارِ تحميل الشخصيات…' : p < 80 ? 'جارِ تحميل الموارد…' : p < 100 ? 'جارِ الاتصال بسيرفر العراق…' : 'جاهز'; if (p >= 100) { clearInterval(tick); $('startScreen').classList.add('ready'); } }, 120);
    Characters.ready.then(ok => { setupCharacters(ok); charsReady = true; });
  })();
  $('ssTap').onclick = () => { if (!$('startScreen').classList.contains('ready')) return; show('lobby'); mode = 'lobby'; buildMaps(); };

  /* ---------- البحث عن سيرفر → التحميل → المود ---------- */
  let mmTimer = null;
  const TIPS = ['اهبط قرب حظائر المطار — فيها أسلحة كثيرة.', 'البيوت العراقية فيها لوت: ادخل من الباب وابحث.', 'استخدم الجسور لعبور دجلة.', 'الإسعاف الأولي يرجع صحتك إلى 75، والحقيبة الطبية إلى 100.', 'السترة الواقية تقلل الضرر — المستوى 2 أقوى.', 'الضربة بالرأس تقتل أسرع.', 'اضغط زر العين لتبديل المنظور الأول/الثالث.'];
  $('btnStart').onclick = () => {
    if (mmTimer) { clearInterval(mmTimer); mmTimer = null; $('mm').classList.remove('show'); $('stt').textContent = 'ابدأ'; $('btnStart').classList.remove('cancel'); return; }
    let t = 0; const findAt = 3 + Math.random() * 4; $('mm').classList.add('show'); $('mmT').textContent = 'جارِ البحث عن مباراة'; $('stt').textContent = 'إلغاء'; $('btnStart').classList.add('cancel');
    mmTimer = setInterval(() => { t += .1; $('mmClock').textContent = '00:' + String(Math.floor(t)).padStart(2, '0');
      if (t >= findAt) { clearInterval(mmTimer); mmTimer = null; $('mmT').textContent = '✔ تم العثور على سيرفر · ' + (window.netStatus ? netStatus.ping : 20) + 'ms'; setTimeout(load, 900); } }, 100);
  };
  function load() {
    mode = 'loading'; show('loading'); $('mm').classList.remove('show'); $('stt').textContent = 'ابدأ'; $('btnStart').classList.remove('cancel');
    $('lTip').textContent = TIPS[(Math.random() * TIPS.length) | 0]; $('lSrv').textContent = 'سيرفر ' + ({ baghdad: 'بغداد', basra: 'البصرة', erbil: 'أربيل' }[S.server] || 'بغداد') + ' · 16 لاعب · ذكاء اصطناعي';
    const steps = [[10, 'الاتصال بالسيرفر…'], [20, 'التحقق من الحساب…'], [30, 'تحميل خريطة بغداد…'], [40, 'بناء المدينة والمطار…'], [50, 'تجهيز البيوت واللوت…'], [70, 'تحميل لاعبي الذكاء الاصطناعي…'], [80, 'مزامنة المنطقة الآمنة…'], [100, 'الطائرة جاهزة للإقلاع!']];
    let i = 0;
    const next = () => {
      const [p, m] = steps[i]; $('lFill').style.width = p + '%'; $('lPct').textContent = p + '%'; $('lMsg').textContent = m;
      if (p === 40 && !world) { gameScene = new THREE.Scene(); world = BaghdadWorld.build(gameScene); applyGraphics(); }
      if (p === 70 && !game) game = new Game(renderer, world, gameScene);
      i++; if (i < steps.length) setTimeout(next, 550 + Math.random() * 450); else setTimeout(begin, 600);
    };
    setTimeout(next, 300);
  }
  function begin() { $('loading').classList.add('hidden'); $('lobby').classList.add('hidden'); mode = 'game'; game.start(); applySettingsAll(); updateLock(); save.matches++; store(); }
  function collect() { const r = game && game.reward; if (r) { save.xp += r.xp; save.coins += r.coins; save.quest += r.kills; if (r.win) { save.event = 1; save.wins++; } store(); game.reward = null; } }
  function backToLobby() { collect(); if (game) game.stop(); mode = 'lobby'; $('endScreen').classList.remove('show'); show('lobby'); refreshProfile(); $('lockPrompt').classList.remove('show'); }
  $('btnLobby').onclick = backToLobby;
  $('btnAgain').onclick = () => { collect(); $('endScreen').classList.remove('show'); game.start(); applySettingsAll(); updateLock(); };
  $('bExit').onclick = () => { if (mode === 'game' && !window.__editing) { if (game.over || confirm('الخروج من المباراة؟')) { game.reward = null; backToLobby(); } } };
  const touch = 'ontouchstart' in window;
  function updateLock() { $('lockPrompt').classList.toggle('show', mode === 'game' && !touch && !document.pointerLockElement && !(game && game.over)); }
  document.addEventListener('pointerlockchange', updateLock);
  $('lockPrompt').addEventListener('click', () => cv.requestPointerLock && cv.requestPointerLock());
  addEventListener('resize', () => { renderer.setSize(innerWidth, innerHeight); lcam.aspect = innerWidth / innerHeight; lcam.updateProjectionMatrix(); if (mode === 'lobby') placePin(); });

  /* ---------- الحلقة + محدد الإطارات ---------- */
  let last = performance.now(), T = 0, acc = 0, frames = 0, fpsT = 0;
  function loop(now) {
    requestAnimationFrame(loop);
    const minDt = 1000 / fpsCap - .5; if (now - last < minDt) return;
    const dt = Math.min(.05, (now - last) / 1000); last = now; T += dt;
    frames++; fpsT += dt; if (fpsT > .5) { $('fps').textContent = Math.round(frames / fpsT) + ' FPS'; frames = 0; fpsT = 0; }
    if (mode === 'game' && game && game.active && !window.__editing) { game.update(dt); if ((acc += dt) > .5) { acc = 0; updateLock(); } renderer.render(gameScene, game.cam); return; }
    // اللوبي / البداية / التحميل
    if (heroObj) { heroObj.rotation.y += (targetRot - heroObj.rotation.y) * Math.min(1, dt * 8); if (!dragging) targetRot += Math.sin(T * .4) * dt * .08; if (hero) hero.mixer.update(dt); else heroObj.userData.head && (heroObj.userData.head.rotation.y = Math.sin(T * .5) * .12); }
    for (const c of crew) { c.mixer.update(dt); if (c.walk) { c.root.position.x -= dt * 1.4; c.root.rotation.y = -Math.PI / 2; if (c.root.position.x < -6) c.root.position.x = 5; } }
    ring2.rotation.z = T * .5; glow.material.opacity = .14 + Math.sin(T * 2) * .03;
    const arr = dust.geometry.attributes.position.array; for (let i = 0; i < dustN; i++) { arr[i * 3 + 1] += dt * .06; if (arr[i * 3 + 1] > 5) arr[i * 3 + 1] = 0; } dust.geometry.attributes.position.needsUpdate = true;
    if (mode === 'start') { lcam.position.set(Math.sin(T * .15) * 1.2 - .4, 1.5, 8.2); lcam.lookAt(-.6, 1.1, -1); }
    else { lcam.position.set(Math.sin(T * .2) * .15, 1.25, 6.3); lcam.lookAt(-.1, .95, 0); }
    renderer.render(LS, lcam);
  }

  weaponCard(); refreshProfile(); viewRow(); applySettingsAll();
  if (Q.get('autostart')) { const go = () => { if (!$('startScreen').classList.contains('ready')) return setTimeout(go, 200); $('ssTap').click(); load(); }; go(); }
  requestAnimationFrame(loop);
  window.__app = { get game() { return game; }, get world() { return world; }, load, get mode() { return mode; } };
})();
