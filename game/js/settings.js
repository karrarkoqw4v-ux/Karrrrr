/* الإعدادات: الرسومات، عناصر التحكم + محرر الأزرار، الحساسية، الأصوات، اللغة والمنطقة، الخصوصية، متقدمة + شريط الحالة */
(function () {
  const $ = id => document.getElementById(id);
  const KEY = 'iraqi-game-settings';
  const DEF = { gfxStyle: 'hd', gfxQuality: 'balanced', fps: 90, view: 'fpp', layout: 'fixed', sens: 50, adsSens: 50, vol: 80, music: 60, alerts: true, vibrate: true, voice: true,
    lang: 'ar', region: 'iq', server: 'baghdad', showName: true, friendReq: true, analytics: false, hideHistory: false, online: true, autoPick: true, blood: true, tapFx: true, showFps: false, showPing: true, aiLevel: 2, renderScale: 100, hud: {} };
  let S = Object.assign({}, DEF); try { Object.assign(S, JSON.parse(localStorage.getItem(KEY) || '{}')); } catch (e) {}
  window.GameSettings = S;
  const save = () => { try { localStorage.setItem(KEY, JSON.stringify(S)); } catch (e) {} };
  window.saveSettings = save;
  const toast = (t, sec) => { const e = $('gtoast'); e.textContent = t; e.classList.add('show'); clearTimeout(toast.t); toast.t = setTimeout(() => e.classList.remove('show'), (sec || 1.6) * 1000); };
  window.gtoast = toast;

  /* ---------- تعريف الأقسام ---------- */
  const TABS = [
    { id: 'gfx', name: 'الرسومات', icon: '🖥' }, { id: 'ctl', name: 'عناصر التحكم', icon: '🎮' }, { id: 'sens', name: 'الحساسية', icon: '🎯' },
    { id: 'snd', name: 'الأصوات والتنبيهات', icon: '🔊' }, { id: 'lang', name: 'اللغة والمنطقة', icon: '🌐' }, { id: 'priv', name: 'الخصوصية', icon: '🔒' }, { id: 'adv', name: 'متقدمة', icon: '⚙' },
  ];
  const card = (key, val, title, sub, opt) => `<button class="opt ${S[key] === val ? 'on' : ''} ${opt && opt.locked ? 'locked' : ''}" data-k="${key}" data-v="${val}" ${opt && opt.locked ? 'data-locked="1"' : ''}><b>${title}</b>${sub ? `<small>${sub}</small>` : ''}${opt && opt.locked ? '<i>🔒</i>' : ''}${opt && opt.badge ? `<em>${opt.badge}</em>` : ''}</button>`;
  const toggle = (key, title, sub) => `<div class="srow"><div><b>${title}</b>${sub ? `<small>${sub}</small>` : ''}</div><label class="sw"><input type="checkbox" data-t="${key}" ${S[key] ? 'checked' : ''}><i></i></label></div>`;
  const slider = (key, title, min, max, unit) => `<div class="srow"><div><b>${title}</b></div><div class="sl"><input type="range" data-r="${key}" min="${min}" max="${max}" value="${S[key]}"><span>${S[key]}${unit || ''}</span></div></div>`;
  const selRow = (key, title, opts) => `<div class="srow"><div><b>${title}</b></div><div class="seg">${opts.map(([v, n]) => `<button data-k="${key}" data-v="${v}" class="${S[key] == v ? 'on' : ''}">${n}</button>`).join('')}</div></div>`;
  function ctlCard(v, title, sub, svg) { return `<button class="ctlc ${(v === 'tpp' ? S.view === 'tpp' : (S.layout === v && S.view !== 'tpp')) ? 'on' : ''}" data-ctl="${v}"><div class="pv">${svg}</div><b>${title}</b><small>${sub}</small></button>`; }
  const PV1 = '<svg viewBox="0 0 160 90"><rect width="160" height="90" rx="6" fill="#1b2230"/><circle cx="30" cy="62" r="16" fill="none" stroke="#fff" stroke-opacity=".6" stroke-width="2"/><circle cx="30" cy="62" r="6" fill="#fff" fill-opacity=".7"/><circle cx="135" cy="60" r="12" fill="#ff5a4a" fill-opacity=".7"/><circle cx="112" cy="72" r="7" fill="#fff" fill-opacity=".5"/><circle cx="115" cy="45" r="7" fill="#fff" fill-opacity=".5"/><path d="M76 45h8M80 41v8" stroke="#fff" stroke-width="1.5"/><rect x="56" y="70" width="48" height="8" fill="#3dd664" fill-opacity=".6"/></svg>';
  const PV2 = '<svg viewBox="0 0 160 90"><rect width="160" height="90" rx="6" fill="#1b2230"/><rect x="4" y="22" width="68" height="64" rx="4" fill="#f2c14e" fill-opacity=".12" stroke="#f2c14e" stroke-dasharray="4 3"/><circle cx="40" cy="55" r="14" fill="none" stroke="#fff" stroke-opacity=".7" stroke-width="2"/><circle cx="46" cy="50" r="6" fill="#fff" fill-opacity=".8"/><path d="M22 76c6-3 10-3 14 0" stroke="#f2c14e" stroke-width="2" fill="none"/><circle cx="135" cy="60" r="12" fill="#ff5a4a" fill-opacity=".7"/><text x="38" y="18" fill="#f2c14e" font-size="9" text-anchor="middle">المس أي مكان</text></svg>';
  const PV3 = '<svg viewBox="0 0 160 90"><rect width="160" height="90" rx="6" fill="#1b2230"/><circle cx="80" cy="40" r="7" fill="#9aa"/><rect x="73" y="47" width="14" height="22" rx="4" fill="#9aa"/><path d="M80 82 L60 88 M80 82 L100 88" stroke="#f2c14e"/><path d="M118 20 l-22 14" stroke="#f2c14e" stroke-width="2" stroke-dasharray="3 2"/><rect x="114" y="10" width="18" height="12" rx="2" fill="#f2c14e"/><text x="123" y="32" fill="#f2c14e" font-size="8" text-anchor="middle">الكاميرا</text><circle cx="30" cy="62" r="13" fill="none" stroke="#fff" stroke-opacity=".6" stroke-width="2"/><circle cx="135" cy="62" r="11" fill="#ff5a4a" fill-opacity=".7"/></svg>';

  function panel(id) {
    if (id === 'gfx') return `<h3>نمط الرسومات</h3><div class="grid4">${card('gfxStyle', 'save', 'توفير الطاقة', 'بطارية أطول')}${card('gfxStyle', 'hd', 'عالي الدقة', 'HD · ظلال كاملة')}${card('gfxStyle', 'hdr', 'HDR', 'تباين وإضاءة أعلى')}${card('gfxStyle', 'hdr10', 'HDR 10', 'ألوان غنية جداً', { badge: 'الأفضل' })}</div>
      <h3>جودة الأداء</h3><div class="grid3">${card('gfxQuality', 'high', 'مرتفع', 'ظلال 2K')}${card('gfxQuality', 'balanced', 'طاقة متوازنة', 'أداء + جودة')}${card('gfxQuality', 'strong', 'قوي', 'ظلال 4K · مدى رؤية بعيد')}</div>
      <h3>معدل الإطارات</h3><div class="grid3">${card('fps', 90, '90 إطار', 'مدعوم على جهازك')}${card('fps', 120, '120 إطار', 'غير مدعوم', { locked: true })}${card('fps', 165, '165 إطار', 'غير مدعوم', { locked: true })}</div>
      ${slider('renderScale', 'دقة العرض', 50, 100, '%')}${toggle('showFps', 'إظهار عداد الإطارات', 'يظهر FPS في زاوية الشاشة')}`;
    if (id === 'ctl') return `<h3>تخطيط عناصر التحكم</h3><div class="grid3">${ctlCard('fixed', 'منظور الشخص الأول', 'عصا ثابتة + أزرار كلاسيكية', PV1)}${ctlCard('free', 'عصا الحركة الحرة', 'المس بأي مكان على اليسار وتحرك', PV2)}${ctlCard('tpp', 'منظور الشخص الثالث', 'الكاميرا خلف اللاعب — تُرجع وتقدّم', PV3)}</div>
      <div class="srow"><div><b>تخصيص الأزرار</b><small>حرّك وكبّر وصغّر كل زر واحفظ التخطيط</small></div><button class="confirm sm" id="openEditor">تعديل العناصر</button></div>
      ${toggle('autoPick', 'التقاط تلقائي', 'الذخيرة والعلاج والدروع تُلتقط تلقائياً')}${selRow('aiLevel', 'صعوبة الذكاء الاصطناعي', [[1, 'سهل'], [2, 'متوسط'], [3, 'صعب']])}`;
    if (id === 'sens') return `<h3>حساسية الكاميرا</h3>${slider('sens', 'بدون تصويب', 10, 100)}${slider('adsSens', 'أثناء التصويب', 10, 100)}`;
    if (id === 'snd') return `<h3>الأصوات</h3>${slider('vol', 'المؤثرات الصوتية', 0, 100, '%')}${slider('music', 'الموسيقى', 0, 100, '%')}<h3>التنبيهات</h3>${toggle('alerts', 'تنبيه تقلص المنطقة', '')}${toggle('vibrate', 'الاهتزاز', 'عند الإطلاق والإصابة')}${toggle('voice', 'الدردشة الصوتية', 'قريباً مع اللعب الجماعي')}`;
    if (id === 'lang') return `<h3>اللغة</h3>${selRow('lang', 'لغة اللعبة', [['ar', 'العربية'], ['ku', 'کوردی'], ['en', 'English']])}<h3>المنطقة</h3>${selRow('region', 'الدولة', [['iq', '🇮🇶 العراق'], ['me', 'الشرق الأوسط']])}${selRow('server', 'السيرفر', [['baghdad', 'بغداد'], ['basra', 'البصرة'], ['erbil', 'أربيل']])}<p class="note">اللغات الإضافية قيد الترجمة؛ الواجهة حالياً بالعربية.</p>`;
    if (id === 'priv') return `<h3>الخصوصية</h3>${toggle('showName', 'إظهار اسمي للاعبين', '')}${toggle('friendReq', 'السماح بطلبات الصداقة', '')}${toggle('online', 'إظهار حالة الاتصال', 'متصل / غير متصل')}${toggle('hideHistory', 'إخفاء سجل المباريات', '')}${toggle('analytics', 'مشاركة بيانات الأداء', 'لا تُرسل أي بيانات حالياً — كل شيء محفوظ على جهازك')}
      <div class="srow"><div><b>مسح بياناتي</b><small>يحذف التقدم والإعدادات من هذا الجهاز</small></div><button class="ghost sm" id="wipe">مسح</button></div>`;
    if (id === 'adv') return `<h3>متقدمة</h3>${toggle('blood', 'تأثير الدم', 'إيقافه يجعل الإصابات خضراء')}${toggle('tapFx', 'تأثير اللمس على الشاشة', '')}${toggle('showPing', 'إظهار البنغ والإشارة والبطارية', '')}
      <div class="srow"><div><b>إعادة كل الإعدادات</b></div><button class="ghost sm" id="resetAll">إعادة</button></div>`;
  }
  let curTab = 'gfx';
  function render() {
    $('stabs').innerHTML = TABS.map(t => `<button data-tab="${t.id}" class="${t.id === curTab ? 'on' : ''}"><i>${t.icon}</i>${t.name}</button>`).join('');
    $('spanel').innerHTML = panel(curTab);
  }
  function open(tab) { curTab = tab || curTab; render(); $('settings').classList.remove('hidden'); }
  $('stabs').addEventListener('click', e => { const b = e.target.closest('button'); if (b) { curTab = b.dataset.tab; render(); } });
  $('spanel').addEventListener('click', e => {
    const b = e.target.closest('button'); if (!b) return;
    if (b.dataset.locked) return toast('جهازك لا يدعم هذا المعدل — مقفول');
    if (b.dataset.k) { const v = isNaN(+b.dataset.v) ? b.dataset.v : +b.dataset.v; S[b.dataset.k] = v; save(); render(); apply(); toast('تم الحفظ'); }
    if (b.dataset.ctl) { if (b.dataset.ctl === 'tpp') S.view = 'tpp'; else { S.layout = b.dataset.ctl; S.view = 'fpp'; } save(); render(); apply(); toast('تم تطبيق التخطيط'); }
    if (b.id === 'openEditor') openEditor();
    if (b.id === 'resetAll') { const hud = {}; Object.assign(S, DEF, { hud }); save(); render(); apply(); toast('تمت إعادة الإعدادات'); }
    if (b.id === 'wipe') { try { localStorage.clear(); } catch (e) {} toast('تم المسح — أعد تشغيل اللعبة'); }
  });
  $('spanel').addEventListener('change', e => { const t = e.target; if (t.dataset.t) { S[t.dataset.t] = t.checked; save(); apply(); } if (t.dataset.r) { S[t.dataset.r] = +t.value; save(); apply(); } });
  $('spanel').addEventListener('input', e => { const t = e.target; if (t.dataset.r) t.nextElementSibling.textContent = t.value + (t.max == 100 && t.dataset.r !== 'sens' && t.dataset.r !== 'adsSens' ? '%' : ''); });
  $('setBack').onclick = () => { $('settings').classList.add('hidden'); };
  function apply() { if (window.applyGraphics) window.applyGraphics(); applyHudLayout(); $('fps').style.display = S.showFps ? 'block' : 'none'; document.body.classList.toggle('noStatus', !S.showPing); if (window.__app && __app.game && __app.game.active) __app.game.applySettings(); if (window.onSettingsChanged) onSettingsChanged(); }
  window.openSettings = open; window.applySettingsAll = apply;

  /* ---------- تخطيط الأزرار (حفظ / تطبيق / محرر) ---------- */
  const CTL = ['joyBase', 'bFire', 'bFire2', 'bAds', 'bJump', 'bCrouch', 'bReload', 'bHeal', 'bView'];
  function applyHudLayout() {
    for (const id of CTL) { const el = $(id); if (!el) continue; const L = S.hud[id];
      el.style.left = L ? L.x * 100 + 'vw' : ''; el.style.top = L ? L.y * 100 + 'vh' : ''; el.style.right = L ? 'auto' : ''; el.style.bottom = L ? 'auto' : '';
      el.style.transform = L && L.s ? `scale(${L.s})` : ''; el.style.opacity = L && L.o ? L.o : ''; }
  }
  window.applyHudLayout = applyHudLayout;
  let edSel = null, drag = null;
  function openEditor() {
    $('settings').classList.add('hidden'); document.body.classList.add('editing'); $('hud').classList.add('on', 'edit'); $('editorBar').classList.remove('hidden'); applyHudLayout(); window.__editing = true;
  }
  function closeEditor() { document.body.classList.remove('editing'); $('hud').classList.remove('edit'); if (!(window.__app && __app.game && __app.game.active)) $('hud').classList.remove('on'); $('editorBar').classList.add('hidden'); window.__editing = false; if (edSel) $(edSel).classList.remove('sel'); edSel = null; open('ctl'); }
  document.addEventListener('pointerdown', e => {
    if (!window.__editing) return; const el = e.target.closest && e.target.closest('.ctl'); if (!el) return; e.preventDefault();
    if (edSel) $(edSel).classList.remove('sel'); edSel = el.id; el.classList.add('sel');
    const L = S.hud[el.id] || {}; $('edSize').value = (L.s || 1) * 100; $('edOp').value = (L.o || 1) * 100; $('edSel').textContent = 'العنصر المحدد: ' + (el.title || el.id);
    const r = el.getBoundingClientRect(); drag = { id: el.id, dx: e.clientX - r.left, dy: e.clientY - r.top, w: r.width, h: r.height };
    const t = el.style.transform; el.style.transform = ''; const r2 = el.getBoundingClientRect(); el.style.transform = t; drag.dx = e.clientX - r2.left; drag.dy = e.clientY - r2.top;
  });
  document.addEventListener('pointermove', e => { if (!drag) return; const L = S.hud[drag.id] = S.hud[drag.id] || {}; L.x = Math.max(0, Math.min(.95, (e.clientX - drag.dx) / innerWidth)); L.y = Math.max(0, Math.min(.95, (e.clientY - drag.dy) / innerHeight)); applyHudLayout(); });
  document.addEventListener('pointerup', () => { drag = null; });
  const pos = id => { const el = $(id), t = el.style.transform; el.style.transform = ''; const r = el.getBoundingClientRect(); el.style.transform = t; return { x: r.left / innerWidth, y: r.top / innerHeight }; };
  $('edSize').oninput = () => { if (!edSel) return; const L = S.hud[edSel] = S.hud[edSel] || pos(edSel); if (L.x === undefined) Object.assign(L, pos(edSel)); L.s = $('edSize').value / 100; applyHudLayout(); };
  $('edOp').oninput = () => { if (!edSel) return; const L = S.hud[edSel] = S.hud[edSel] || pos(edSel); if (L.x === undefined) Object.assign(L, pos(edSel)); L.o = $('edOp').value / 100; applyHudLayout(); };
  $('edSave').onclick = () => { save(); toast('✔ تم حفظ العناصر'); };
  $('edReset').onclick = () => { S.hud = {}; applyHudLayout(); toast('تمت إعادة التخطيط الافتراضي'); };
  $('edExit').onclick = closeEditor;

  /* ---------- شريط الحالة: البنغ + الإشارة + البطارية ---------- */
  const st = { ping: 20, bat: null, charging: false };
  function measure() {
    const c = navigator.connection; let rtt = c && c.rtt ? c.rtt : null;
    if (navigator.onLine === false) rtt = 677;
    if (rtt === null) rtt = 20 + Math.round(Math.random() * 6);
    st.ping = Math.max(20, Math.min(677, rtt <= 60 ? 20 + Math.round(Math.random() * 4) : rtt));
    draw();
  }
  function draw() {
    const p = st.ping, bars = p < 60 ? 4 : p < 150 ? 3 : p < 300 ? 2 : 1, col = p < 60 ? '#6bff8f' : p < 150 ? '#ffd24a' : p < 300 ? '#ff9a3a' : '#ff4a3a';
    const sig = `<span class="sig">${[1, 2, 3, 4].map(i => `<i style="height:${i * 3 + 2}px;background:${i <= bars ? col : 'rgba(255,255,255,.25)'}"></i>`).join('')}</span>`;
    const bat = st.bat === null ? '' : `<span class="bat ${st.bat < 20 ? 'low' : ''}"><i style="width:${st.bat}%"></i></span><b>${st.bat}%${st.charging ? '⚡' : ''}</b>`;
    const html = `${sig}<b style="color:${col}">${p}ms</b>${bat}`;
    for (const id of ['ssStatus', 'lobStatus', 'ldStatus', 'hudStatus']) { const e = $(id); if (e) e.innerHTML = html; }
  }
  if (navigator.getBattery) navigator.getBattery().then(b => { const u = () => { st.bat = Math.round(b.level * 100); st.charging = b.charging; draw(); }; u(); b.addEventListener('levelchange', u); b.addEventListener('chargingchange', u); }).catch(() => {});
  measure(); setInterval(measure, 4000); addEventListener('online', measure); addEventListener('offline', measure);
  window.netStatus = st;

  /* ---------- تأثير اللمس ---------- */
  document.addEventListener('pointerdown', e => { if (!S.tapFx || window.__editing) return; const r = document.createElement('div'); r.className = 'ripple'; r.style.left = e.clientX + 'px'; r.style.top = e.clientY + 'px'; document.body.appendChild(r); setTimeout(() => r.remove(), 500); }, true);
})();
