/* منطق المود: باتل رويال بغداد — مظلة فوق المطار، لوت أسلحة وعلاج ودروع، منظور أول/ثالث، أعداء بذكاء اصطناعي */
(function () {
  const $ = id => document.getElementById(id);
  const TAU = Math.PI * 2, rnd = (a, b) => a + Math.random() * (b - a), pick = a => a[(Math.random() * a.length) | 0];
  const BOTS = 15;
  const NAMES = ['سجاد_البغدادي', 'أبو_حيدر', 'مصطفى_الكرخي', 'علي_الرصافي', 'حسين_الموصلي', 'كرار_البصراوي', 'زيد_النجفي', 'مرتضى_الكربلائي', 'عباس_الديواني', 'يوسف_الأنباري', 'محمد_العماري', 'أحمد_الحلاوي', 'عمر_التكريتي', 'حيدر_الكوتي', 'منتظر_الناصري', 'ليث_الأربيلي', 'باقر_السماوي', 'رضا_البعقوبي'];
  const ZONE = [[260, 60, 35, 1], [170, 40, 30, 2], [105, 30, 25, 4], [55, 25, 22, 6], [20, 18, 20, 9]];
  const S = () => window.GameSettings || {};

  /* ---------- صوت مُولَّد ---------- */
  let ac = null, master = null;
  function audio() { try { if (!ac) { ac = new (window.AudioContext || window.webkitAudioContext)(); master = ac.createGain(); master.connect(ac.destination); } if (ac.state === 'suspended') ac.resume(); master.gain.value = (S().vol ?? 80) / 100; } catch (e) {} return ac; }
  function shot(vol, suppressed, deep) {
    const a = audio(); if (!a || vol < .02) return; const t = a.currentTime, out = master;
    const len = suppressed ? .1 : .22, buf = a.createBuffer(1, a.sampleRate * len, a.sampleRate), d = buf.getChannelData(0);
    for (let i = 0; i < d.length; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / d.length, suppressed ? 4 : 2.3);
    const n = a.createBufferSource(), f = a.createBiquadFilter(), g = a.createGain();
    f.type = 'lowpass'; f.frequency.value = suppressed ? 1700 : deep ? 2400 : 3600; g.gain.value = vol * (suppressed ? .5 : .85); n.buffer = buf; n.connect(f).connect(g).connect(out); n.start(t);
    const o = a.createOscillator(), og = a.createGain(); o.type = 'sine'; o.frequency.setValueAtTime(deep ? 110 : 150, t); o.frequency.exponentialRampToValueAtTime(35, t + .14);
    og.gain.setValueAtTime(vol * .8, t); og.gain.exponentialRampToValueAtTime(.001, t + .16); o.connect(og).connect(out); o.start(t); o.stop(t + .17);
  }
  function blip(freq, dur, vol, type) { const a = audio(); if (!a) return; const t = a.currentTime, o = a.createOscillator(), g = a.createGain(); o.type = type || 'square'; o.frequency.value = freq; g.gain.setValueAtTime(vol || .08, t); g.gain.exponentialRampToValueAtTime(.001, t + dur); o.connect(g).connect(master); o.start(t); o.stop(t + dur); }
  function wind(on) { const a = audio(); if (!a) return; if (on && !wind.n) { const b = a.createBuffer(1, a.sampleRate * 2, a.sampleRate), d = b.getChannelData(0); for (let i = 0; i < d.length; i++) d[i] = Math.random() * 2 - 1; const n = a.createBufferSource(); n.buffer = b; n.loop = true; const f = a.createBiquadFilter(); f.type = 'bandpass'; f.frequency.value = 500; const g = a.createGain(); g.gain.value = .12; n.connect(f).connect(g).connect(master); n.start(); wind.n = n; } else if (!on && wind.n) { wind.n.stop(); wind.n = null; } }
  const vibrate = ms => { if (S().vibrate !== false && navigator.vibrate) try { navigator.vibrate(ms); } catch (e) {} };

  class Game {
    constructor(renderer, world, scene) {
      this.r = renderer; this.W = world; this.scene = scene; const WD = Weapons.W;
      this.cam = new THREE.PerspectiveCamera(75, innerWidth / innerHeight, .08, 1400); this.cam.rotation.order = 'YXZ'; scene.add(this.cam);
      this.vms = {}; for (const id in WD) { const v = Models.buildViewmodel(WD[id].build, WD[id].pistol); v.visible = false; this.cam.add(v); this.vms[id] = v; }
      this.flash = new THREE.PointLight(0xffc27a, 0, 12); scene.add(this.flash);
      this.flashMesh = new THREE.Mesh(new THREE.PlaneGeometry(.24, .24), new THREE.MeshBasicMaterial({ color: 0xffd98a, transparent: true, opacity: 0, blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide }));
      this.tracers = []; this.puffs = []; this.bots = []; this.loot = []; this.corpses = []; this.decals = [];
      this.keys = {}; this.look = { dx: 0, dy: 0 }; this.joy = { x: 0, y: 0 }; this.fireHeld = false; this.ads = false; this.active = false;
      this.zoneMesh = new THREE.Mesh(new THREE.CylinderGeometry(1, 1, 260, 72, 1, true), new THREE.MeshBasicMaterial({ color: 0xff3b2a, transparent: true, opacity: .16, side: THREE.DoubleSide, depthWrite: false, fog: false }));
      this.zoneMesh.position.y = 120; scene.add(this.zoneMesh);
      // شخصية اللاعب (للمنظور الثالث) + المظلة
      this.pchar = Characters.loaded ? Characters.create('cts', Models.buildRifle) : null;
      this.pmodel = this.pchar ? this.pchar.root : Models.buildSoldier(); scene.add(this.pmodel);
      this.chute = this.buildChute(); scene.add(this.chute);
      this.mini = $('minimap'); this.mctx = this.mini.getContext('2d'); this.compass = $('compass'); this.cctx = this.compass.getContext('2d');
      this.bindInput();
      addEventListener('resize', () => { this.cam.aspect = innerWidth / innerHeight; this.cam.updateProjectionMatrix(); });
    }
    buildChute() {
      const g = new THREE.Group(), cols = [0xce1126, 0xffffff, 0x111111];
      for (let i = 0; i < 9; i++) { const m = new THREE.Mesh(new THREE.SphereGeometry(4, 6, 6, i / 9 * TAU, TAU / 9, 0, Math.PI / 2.6), new THREE.MeshStandardMaterial({ color: cols[i % 3], side: THREE.DoubleSide, roughness: .8 })); g.add(m); }
      g.children.forEach(m => m.scale.set(1, .55, 1));
      const lm = new THREE.LineBasicMaterial({ color: 0x333333 }); for (let i = 0; i < 8; i++) { const a = i / 8 * TAU; g.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(Math.cos(a) * 3.2, 1, Math.sin(a) * 3.2), new THREE.Vector3(0, -5, 0)]), lm)); }
      g.visible = false; return g;
    }

    /* ---------- بدء المباراة ---------- */
    start() {
      const W = this.W, sp = W.airportPoint();
      this.p = { x: sp.x, z: sp.z, y: 110, vy: 0, yaw: rnd(0, TAU), pitch: -.5, hp: 100, boost: 0, slots: [null, 'p92'], cur: 1, mags: { p92: 15 }, ammo: { '556': 0, '762': 0, '9mm': 30 }, meds: { band: 2, fak: 0, kit: 0, drink: 0 }, vest: 0, helmet: 0,
        reload: 0, cd: 0, kills: 0, rec: 0, bob: 0, ads: 0, dead: false, lastHit: 0, speed: 0, chute: true, heal: null, crouch: false, dmgDealt: 0 };
      for (const b of this.bots) this.scene.remove(b.model); for (const c of this.corpses) this.scene.remove(c.model); for (const l of this.loot) this.scene.remove(l.mesh); for (const d of this.decals) this.scene.remove(d);
      this.bots = []; this.corpses = []; this.loot = []; this.decals = [];
      const names = NAMES.slice().sort(() => Math.random() - .5);
      for (let i = 0; i < BOTS; i++) {
        let q; for (let k = 0; k < 40; k++) { q = W.randomOpenPoint(0, 3); if (Math.hypot(q.x - sp.x, q.z - sp.z) > 90) break; }
        const wpn = Math.random() < .5 ? 'm4' : 'akm';
        const ch = Characters.loaded ? Characters.create(pick(['enemy', 'enemy2', 'enemy3']), Weapons.W[wpn].build) : null;
        const model = ch ? ch.root : Models.buildSoldier({ enemy: true }); model.position.set(q.x, 0, q.z); this.scene.add(model);
        this.bots.push({ x: q.x, z: q.z, yaw: rnd(0, TAU), hp: 100, model, ch, wpn, name: names[i], state: 'roam', wp: null, seen: 0, heard: 0, burst: 0, cd: rnd(0, 1), react: 0, phase: rnd(0, 6), stuck: 0, side: Math.sign(q.x - W.riverX(q.z)), anim: '' });
      }
      // اللوت: داخل البيوت والحظائر + الشوارع
      for (const s of W.lootSpots) this.spawnLoot(s.x, s.z);
      for (let i = 0; i < 40; i++) { const q = W.randomOpenPoint(0, 2); this.spawnLoot(q.x, q.z); }
      for (let i = 0; i < 6; i++) { const q = W.airportPoint(); this.addLoot(q.x, q.z, { kind: 'weapon', id: pick(['m4', 'akm', 'akm', 'm4', 'kar']) }); this.addLoot(q.x + 1.2, q.z, { kind: 'ammo', type: pick(['556', '762']), n: 60 }); }
      const cx = rnd(-120, 60), cz = rnd(-120, 60);
      this.zone = { cx, cz, r: 340, fromR: 340, toR: 340, phase: -1, t: 0, state: 'wait', dmg: 0, tcx: cx, tcz: cz, fcx: cx, fcz: cz };
      this.nextPhase();
      this.alive = BOTS + 1; this.time = 0; this.fx = {}; this.over = false; this.virtualT = rnd(35, 50); this.reward = null;
      this.active = true; this.cam.fov = 75; this.cam.updateProjectionMatrix();
      this.setWeapon(1); this.applySettings();
      $('hud').classList.add('on'); $('endScreen').classList.remove('show'); this.updateHud(true); this.updateInv();
      this.msg('أنت فوق مطار بغداد الدولي — وجّه المظلة واهبط', 4); wind(true);
    }
    stop() { this.active = false; wind(false); $('hud').classList.remove('on'); if (document.pointerLockElement) document.exitPointerLock(); }
    applySettings() {
      const s = S(); this.tpp = s.view === 'tpp'; this.layoutFree = s.layout === 'free';
      for (const id in this.vms) this.vms[id].visible = false; this.updateVM();
      if (window.applyHudLayout) window.applyHudLayout();
    }
    get wdef() { return Weapons.W[this.p.slots[this.p.cur]]; }
    get wid() { return this.p.slots[this.p.cur]; }
    setWeapon(slot) {
      const p = this.p; if (!p.slots[slot]) return; if (p.cur !== slot) { p.reload = 0; p.heal = null; } p.cur = slot; p.cd = .35; this.switchT = .35;
      if (this.pchar) this.pchar.setGun(this.wdef.build); this.updateVM(); this.updateAmmo(); this.updateInv(); blip(380, .05, .05, 'triangle');
    }
    updateVM() { for (const id in this.vms) this.vms[id].visible = !this.tpp && this.active && this.p && !this.p.chute && id === this.wid && !(this.wdef.scope && this.p.ads > .6); }

    nextPhase() {
      const z = this.zone; z.phase++; const Z = ZONE[Math.min(z.phase, ZONE.length - 1)];
      z.fromR = z.r; z.toR = Z[0]; z.fcx = z.cx; z.fcz = z.cz;
      const a = rnd(0, TAU), d = rnd(0, Math.max(0, z.fromR - z.toR) * .8);
      z.tcx = z.fcx + Math.cos(a) * d; z.tcz = z.fcz + Math.sin(a) * d; z.dmg = Z[3]; z.shrinkT = Z[2]; z.state = 'wait'; z.t = Z[1];
    }

    /* ---------- اللوت ---------- */
    spawnLoot(x, z) {
      const r = Math.random();
      if (r < .24) { const id = pick(['m4', 'm4', 'akm', 'akm', 'akm', 'kar', 'p92']); this.addLoot(x, z, { kind: 'weapon', id }); const a = Weapons.W[id].ammo; this.addLoot(x + .7, z + .3, { kind: 'ammo', type: a, n: a === '9mm' ? 30 : id === 'kar' ? 20 : 60 }); }
      else if (r < .5) this.addLoot(x, z, { kind: 'ammo', type: pick(['556', '762', '9mm']), n: 30 });
      else if (r < .82) this.addLoot(x, z, { kind: 'med', med: pick(['band', 'band', 'band', 'fak', 'fak', 'kit', 'drink', 'drink']) });
      else this.addLoot(x, z, Math.random() < .55 ? { kind: 'vest', lvl: Math.random() < .7 ? 1 : 2 } : { kind: 'helmet', lvl: Math.random() < .7 ? 1 : 2 });
    }
    addLoot(x, z, item) {
      const g = new THREE.Group(), M = (c, e) => new THREE.MeshStandardMaterial({ color: c, roughness: .6, emissive: e || 0 });
      let glow = 0xffd24a;
      if (item.kind === 'weapon') { const w = Weapons.W[item.id].build(); w.rotation.set(0, 0, Math.PI / 2); w.position.y = .06; g.add(w); glow = 0xffa23a; }
      else if (item.kind === 'ammo') { const b = new THREE.Mesh(new THREE.BoxGeometry(.36, .22, .24), M(item.type === '556' ? 0x5a6b3a : item.type === '762' ? 0x6b4a2a : 0x3a4a5a)); b.position.y = .11; g.add(b); const s = new THREE.Mesh(new THREE.BoxGeometry(.37, .05, .25), M(0xe0b020, 0x403000)); s.position.y = .2; g.add(s); }
      else if (item.kind === 'med') {
        if (item.med === 'drink') { const c = new THREE.Mesh(new THREE.CylinderGeometry(.05, .05, .16, 12), M(0x2a7fff, 0x082040)); c.position.y = .08; g.add(c); }
        else if (item.med === 'band') { const c = new THREE.Mesh(new THREE.CylinderGeometry(.06, .06, .08, 12), M(0xf4f0e6)); c.position.y = .04; g.add(c); }
        else { const sz = item.med === 'kit' ? .45 : .3, b = new THREE.Mesh(new THREE.BoxGeometry(sz, sz * .6, sz * .8), M(item.med === 'kit' ? 0xc8201c : 0xf2f2f2)); b.position.y = sz * .3; g.add(b); const cr = new THREE.Mesh(new THREE.BoxGeometry(sz * .5, .02, sz * .14), M(item.med === 'kit' ? 0xffffff : 0xd01c1c)); cr.position.y = sz * .61; g.add(cr); const cr2 = cr.clone(); cr2.rotation.y = Math.PI / 2; g.add(cr2); }
        glow = 0x6bff8f;
      } else if (item.kind === 'vest') { const b = new THREE.Mesh(new THREE.BoxGeometry(.45, .12, .5), M(item.lvl > 1 ? 0x3a4030 : 0x5a5a48)); b.position.y = .06; g.add(b); glow = 0x5ab4ff; }
      else { const h = new THREE.Mesh(new THREE.SphereGeometry(.15, 14, 10, 0, TAU, 0, Math.PI / 2), M(item.lvl > 1 ? 0x2e3428 : 0x6b5e45)); g.add(h); glow = 0x5ab4ff; }
      const ring = new THREE.Mesh(new THREE.RingGeometry(.35, .5, 24), new THREE.MeshBasicMaterial({ color: glow, transparent: true, opacity: .5, side: THREE.DoubleSide, depthWrite: false })); ring.rotation.x = -Math.PI / 2; ring.position.y = .02; g.add(ring);
      g.position.set(x, 0, z); this.scene.add(g); this.loot.push({ x, z, item, mesh: g });
    }
    lootName(it) {
      if (it.kind === 'weapon') return Weapons.W[it.id].name; if (it.kind === 'ammo') return Weapons.AMMO[it.type] + ' ×' + it.n;
      if (it.kind === 'med') return Weapons.MEDS[it.med].name; return (it.kind === 'vest' ? 'سترة واقية' : 'خوذة') + ' مستوى ' + it.lvl;
    }
    take(l) {
      const p = this.p, it = l.item;
      if (it.kind === 'ammo') { p.ammo[it.type] = Math.min(300, p.ammo[it.type] + it.n); }
      else if (it.kind === 'med') p.meds[it.med]++;
      else if (it.kind === 'vest') { if (it.lvl <= p.vest) return false; p.vest = it.lvl; }
      else if (it.kind === 'helmet') { if (it.lvl <= p.helmet) return false; p.helmet = it.lvl; }
      else if (it.kind === 'weapon') {
        const w = Weapons.W[it.id], slot = w.pistol ? 1 : 0, old = p.slots[slot];
        if (old === it.id) { p.ammo[w.ammo] += w.mag; }
        else { if (old) this.addLoot(p.x + rnd(-.8, .8), p.z + rnd(-.8, .8), { kind: 'weapon', id: old }); p.slots[slot] = it.id; p.mags[it.id] = w.mag; this.setWeapon(slot); }
      }
      l.got = true; this.scene.remove(l.mesh); this.msg('التقطت: ' + this.lootName(it), 1.3); blip(720, .07, .06); this.updateAmmo(); this.updateInv(); return true;
    }

    /* ---------- الحركة والتصادم ---------- */
    move(o, dx, dz, r) {
      const W = this.W, y = o.y || 0;
      const ok = (nx, nz) => !(W.isWater(nx, nz) && y < 1) && Math.abs(nx) < W.HALF - 2 && Math.abs(nz) < W.HALF - 2 && !W.blocked(nx, nz, r, y);
      let hit = false; if (ok(o.x + dx, o.z)) o.x += dx; else hit = true; if (ok(o.x, o.z + dz)) o.z += dz; else hit = true; return hit;
    }

    /* ---------- الرماية ---------- */
    rayWorld(ox, oy, oz, dx, dy, dz, maxT, ignoreBots) {
      let best = { t: maxT, kind: null };
      if (dy < 0) { const t = -oy / dy; if (t > 0 && t < best.t) best = { t, kind: 'ground' }; }
      const seen = new Set();
      for (let s = 0; s < best.t + 10; s += 10) {
        for (const c of this.W.near(ox + dx * s, oz + dz * s, 10)) {
          if (seen.has(c)) continue; seen.add(c); let t0 = 0, t1 = best.t;
          const sl = (o, d, lo, hi) => { if (Math.abs(d) < 1e-8) return o >= lo && o <= hi; let a = (lo - o) / d, b = (hi - o) / d; if (a > b) { const q = a; a = b; b = q; } t0 = Math.max(t0, a); t1 = Math.min(t1, b); return t0 <= t1; };
          if (sl(ox, dx, c.x0, c.x1) && sl(oy, dy, 0, c.h) && sl(oz, dz, c.z0, c.z1) && t0 > 0 && t0 < best.t) best = { t: t0, kind: 'wall' };
        }
      }
      if (!ignoreBots) for (const b of this.bots) {
        const rx = b.x - ox, rz = b.z - oz, dd = dx * dx + dz * dz, tc = (rx * dx + rz * dz) / dd;
        if (tc <= 0 || tc > best.t) continue; const cx = ox + dx * tc - b.x, cz = oz + dz * tc - b.z;
        if (cx * cx + cz * cz > .14) continue; const y = oy + dy * tc; if (y < 0 || y > 1.85) continue;
        best = { t: tc, kind: 'bot', bot: b, head: y > 1.52 };
      }
      return best;
    }
    losClear(ax, ay, az, bx, by, bz) { const dx = bx - ax, dy = by - ay, dz = bz - az, L = Math.hypot(dx, dy, dz); return this.rayWorld(ax, ay, az, dx / L, dy / L, dz / L, L, true).kind !== 'wall'; }
    tracer(from, to, color, life) {
      const l = new THREE.Line(new THREE.BufferGeometry().setFromPoints([from, to]), new THREE.LineBasicMaterial({ color: color || 0xffe2a0, transparent: true, opacity: .9, blending: THREE.AdditiveBlending, fog: false }));
      this.scene.add(l); this.tracers.push({ l, life: life || .07 });
    }
    puff(pos, color, n, spd) {
      for (let i = 0; i < (n || 5); i++) {
        const m = new THREE.Mesh(new THREE.SphereGeometry(.05 + Math.random() * .07, 5, 4), new THREE.MeshBasicMaterial({ color: color || 0xcdbf99, transparent: true, opacity: .85, fog: false }));
        m.position.copy(pos); this.scene.add(m); const k = spd || 1;
        this.puffs.push({ m, v: new THREE.Vector3(rnd(-1, 1) * k, rnd(.2, 2) * k, rnd(-1, 1) * k), life: rnd(.25, .55) });
      }
    }
    blood(pos, big) {
      const on = S().blood !== false; this.puff(pos, on ? 0x7a0606 : 0x9aff8a, big ? 14 : 7, big ? 1.6 : 1);
      if (on && big) { const d = new THREE.Mesh(new THREE.CircleGeometry(rnd(.6, 1.1), 18), new THREE.MeshBasicMaterial({ color: 0x4a0303, transparent: true, opacity: .85, depthWrite: false })); d.rotation.x = -Math.PI / 2; d.position.set(pos.x + rnd(-.3, .3), .03, pos.z + rnd(-.3, .3)); this.scene.add(d); this.decals.push(d); if (this.decals.length > 30) this.scene.remove(this.decals.shift()); }
    }
    muzzleWorld() { const v = new THREE.Vector3(); const m = this.tpp ? (this.pchar && this.pchar.root.userData.muzzle) : this.vms[this.wid].userData.muzzle; if (m) m.getWorldPosition(v); else v.copy(this.cam.position); return v; }

    playerFire() {
      const p = this.p, w = this.wdef; if (p.cd > 0 || p.reload > 0 || p.dead || p.chute) return;
      if (!w.auto && this.fireLatch) return; this.fireLatch = true;
      if (p.mags[this.wid] <= 0) { this.reload(); return; }
      if (p.heal) { p.heal = null; this.msg('أُلغي العلاج', 1); }
      p.mags[this.wid]--; p.cd = w.rate; p.rec = Math.min(p.rec + 1, 12);
      const spread = (this.ads ? w.ads : w.spread) * (1 + p.speed * .12) * (p.crouch ? .75 : 1) + p.rec * .0006 * (w.auto ? 1 : 0);
      const dir = new THREE.Vector3(0, 0, -1).applyQuaternion(this.cam.quaternion);
      dir.x += rnd(-spread, spread); dir.y += rnd(-spread, spread); dir.z += rnd(-spread, spread); dir.normalize();
      const o = this.cam.position, h = this.rayWorld(o.x, o.y, o.z, dir.x, dir.y, dir.z, 400), end = o.clone().addScaledVector(dir, h.t), mw = this.muzzleWorld();
      this.tracer(mw, end);
      if (h.kind === 'bot') { const dmg = w.dmg * (h.head ? w.head : 1) * (h.t > 120 ? .8 : 1); this.damageBot(h.bot, dmg, h.head); this.fx.hit = .18; this.fx.headshot = h.head; blip(h.head ? 1500 : 950, .05, .05); this.blood(end, false); p.dmgDealt += dmg; }
      else this.puff(end, h.kind === 'ground' ? 0xcdbf99 : 0xd8d2c0, 5);
      shot(w.loud, w.sup, w.ammo === '762'); vibrate(12);
      this.flash.position.copy(mw); this.flash.intensity = 5; if (this.flashMesh.parent !== this.scene) this.scene.add(this.flashMesh); this.flashMesh.position.copy(mw); this.flashMesh.lookAt(this.cam.position); this.flashMesh.material.opacity = .9;
      p.pitch += w.recoil * (this.ads ? .7 : 1) * (p.crouch ? .8 : 1) + rnd(0, .003); p.yaw += rnd(-.004, .004) * (w.recoil / .007); this.vmKick = 1; this.fx.shake = Math.min(1, (this.fx.shake || 0) + w.recoil * 18);
      for (const b of this.bots) if (Math.hypot(b.x - p.x, b.z - p.z) < (w.sup ? 60 : 130)) b.heard = 4;
      this.updateAmmo();
    }
    reload() {
      const p = this.p, w = this.wdef; if (!w || p.reload > 0 || p.mags[this.wid] >= w.mag || p.ammo[w.ammo] <= 0 || p.dead) return;
      p.reload = w.reload; p.heal = null; this.msg('إعادة التعبئة…', w.reload * .8); blip(300, .08, .06, 'triangle'); setTimeout(() => blip(520, .06, .06, 'triangle'), w.reload * 450);
    }
    heal() {
      const p = this.p; if (p.heal || p.dead) return; const m = p.meds; let k = null;
      if (p.hp < 75 && m.fak) k = 'fak'; else if (p.hp < 100 && m.kit && p.hp < 60) k = 'kit'; else if (p.hp < 75 && m.band) k = 'band'; else if (p.hp < 100 && m.kit) k = 'kit'; else if (m.drink && p.boost < 100) k = 'drink';
      if (!k) { this.msg(p.hp >= 75 && !m.kit ? 'صحتك جيدة — لا تحتاج علاج' : 'لا توجد مواد علاج', 1.5); return; }
      p.heal = { k, t: Weapons.MEDS[k].time, total: Weapons.MEDS[k].time }; p.reload = 0; blip(420, .1, .05, 'sine');
    }
    damageBot(b, dmg, head) {
      if (b.hp <= 0) return; b.hp -= dmg; b.seen = 6; b.heard = 6;
      if (b.hp <= 0) {
        this.p.kills++; this.alive--; this.feed(`أنت <i>${this.wdef.name}${head ? ' 🎯' : ''}</i> ${b.name}`, true); this.killBot(b);
        if (Math.random() < .7) this.addLoot(b.x + .5, b.z, { kind: 'ammo', type: Weapons.W[b.wpn].ammo, n: 30 }); if (Math.random() < .4) this.addLoot(b.x - .5, b.z, { kind: 'med', med: pick(['band', 'fak', 'drink']) }); if (Math.random() < .35) this.addLoot(b.x, b.z + .6, { kind: 'weapon', id: b.wpn });
        this.fx.kill = 1.5; $('killBanner').textContent = (head ? 'رأسية! ' : '') + 'إطاحة × ' + this.p.kills; $('killBanner').classList.remove('show'); void $('killBanner').offsetWidth; $('killBanner').classList.add('show');
        blip(600, .1, .07); setTimeout(() => blip(900, .12, .07), 90); vibrate([30, 30, 40]);
      }
    }
    killBot(b) { b.hp = 0; this.blood(new THREE.Vector3(b.x, 1, b.z), true); if (b.ch) { b.ch.mixer.stopAllAction(); } this.corpses.push({ model: b.model, t: 0, ttl: 30, yaw: b.yaw }); this.bots = this.bots.filter(x => x !== b); }
    damagePlayer(d, fromX, fromZ, killer) {
      const p = this.p; if (p.dead || this.over) return;
      d *= p.vest === 2 ? .55 : p.vest === 1 ? .7 : 1; if (p.helmet) d *= .93;
      p.hp -= d; this.fx.hurt = .4; this.fx.bloodT = 1; p.lastHit = this.time; vibrate(25);
      if (p.heal) { p.heal = null; }
      if (fromX !== undefined) { this.fx.dirAngle = Math.atan2(fromX - p.x, fromZ - p.z); this.fx.dirT = 1.2; }
      if (p.hp <= 0) { p.hp = 0; p.dead = true; this.feed(`${killer || 'المنطقة الحمراء'} <i>⟵</i> أنت`); this.finish(false, killer); }
      this.updateHud();
    }

    /* ---------- الذكاء الاصطناعي ---------- */
    updateBot(b, dt) {
      const p = this.p, W = this.W, dx = p.x - b.x, dz = p.z - b.z, dist = Math.hypot(dx, dz);
      b.cd -= dt; b.heard = Math.max(0, b.heard - dt); b.seen = Math.max(0, b.seen - dt); b.phase += dt;
      let canSee = false;
      if (!p.dead && !p.chute && dist < 95 && (dist < 50 || b.heard > 0 || b.seen > 0)) {
        const yawTo = Math.atan2(dx, dz), dy = Math.atan2(Math.sin(yawTo - b.yaw), Math.cos(yawTo - b.yaw));
        if (Math.abs(dy) < 1.3 || b.seen > 0 || b.heard > 0) canSee = this.losClear(b.x, 1.55, b.z, p.x, p.crouch ? 1 : 1.4, p.z);
      }
      if (canSee) { if (b.state !== 'fight') { b.state = 'fight'; b.react = rnd(.6, 1.2); } b.seen = 3; } else if (b.state === 'fight' && b.seen <= 0) b.state = 'roam';
      let speed = 0, tyaw = b.yaw;
      if (b.state === 'fight') {
        tyaw = Math.atan2(dx, dz); b.react -= dt;
        const sx = Math.cos(b.phase * .8) * 2.2, fwd = dist > 35 ? 3.2 : dist < 14 ? -2.5 : 0;
        const vx = Math.sin(tyaw) * fwd + Math.cos(tyaw) * sx, vz = Math.cos(tyaw) * fwd - Math.sin(tyaw) * sx;
        this.move(b, vx * dt, vz * dt, .4); speed = Math.hypot(vx, vz);
        if (b.react <= 0 && b.cd <= 0 && canSee) {
          b.cd = b.burst > 0 ? .11 : rnd(.7, 1.4); if (b.burst <= 0) b.burst = (Math.random() * 4 | 0) + 3; b.burst--;
          const acc = Math.max(.04, .4 - dist / 240) * (p.crouch ? .75 : 1) * (p.speed > 5 ? .75 : 1) * ((S().aiLevel ?? 2) / 2);
          const mw = new THREE.Vector3(); if (b.model.userData.muzzle) b.model.userData.muzzle.getWorldPosition(mw); else mw.set(b.x, 1.4, b.z);
          const hp = Math.random() < acc, tg = new THREE.Vector3(p.x + (hp ? 0 : rnd(-1.5, 1.5)), 1.3 + (hp ? 0 : rnd(-.5, .5)), p.z + (hp ? 0 : rnd(-1.5, 1.5)));
          this.tracer(mw, tg, 0xff9a5a, .08); shot(Math.max(0, .55 - dist / 170), false, b.wpn === 'akm');
          if (hp) this.damagePlayer(rnd(4, 8) * (b.wpn === 'akm' ? 1.2 : 1), b.x, b.z, b.name);
        }
      } else {
        if (!b.wp || Math.hypot(b.wp.x - b.x, b.wp.z - b.z) < 3 || b.stuck > 1.2) {
          const Z = this.zone; let q; for (let k = 0; k < 20; k++) { q = W.randomOpenPoint(b.side, 2); if (Math.hypot(q.x - Z.cx, q.z - Z.cz) < Z.r * .85) break; } b.wp = q; b.stuck = 0;
        }
        tyaw = Math.atan2(b.wp.x - b.x, b.wp.z - b.z);
        const outside = Math.hypot(b.x - this.zone.cx, b.z - this.zone.cz) > this.zone.r; speed = outside ? 5.4 : 2.2;
        const hit = this.move(b, Math.sin(tyaw) * speed * dt, Math.cos(tyaw) * speed * dt, .4); b.stuck = hit ? b.stuck + dt : Math.max(0, b.stuck - dt);
      }
      const dyaw = Math.atan2(Math.sin(tyaw - b.yaw), Math.cos(tyaw - b.yaw)); b.yaw += dyaw * Math.min(1, dt * (b.state === 'fight' ? 9 : 5));
      b.model.position.set(b.x, 0, b.z); b.model.rotation.y = b.yaw;
      if (b.ch) { const want = speed < .4 ? 'Idle' : speed < 4 ? 'Walk' : 'Run'; if (want !== b.anim) { b.ch.play(want, .3); b.anim = want; } b.ch.setSpeed(want === 'Walk' ? speed / 1.6 : want === 'Run' ? speed / 5.5 : 1); if (Math.hypot(b.x - p.x, b.z - p.z) < 160) b.ch.mixer.update(dt); }
      else { const u = b.model.userData, sw = Math.sin(b.phase * (speed > 3.5 ? 9 : 6)) * Math.min(1, speed / 3) * .7; u.legL.rotation.x = sw; u.legR.rotation.x = -sw; }
      if (Math.hypot(b.x - this.zone.cx, b.z - this.zone.cz) > this.zone.r) { b.hp -= this.zone.dmg * dt * 1.1; if (b.hp <= 0) { this.alive--; this.feed(`${b.name} سقط خارج المنطقة`); this.killBot(b); } }
    }

    /* ---------- الحلقة ---------- */
    update(dt) {
      if (!this.active) return; dt = Math.min(dt, .05);
      const p = this.p, cam = this.cam, s = S(); this.time += dt;
      if (!p.dead && !this.over) {
        const sens = (this.ads ? (s.adsSens ?? 50) : (s.sens ?? 50)) / 50 * (this.ads ? .0011 * (this.wdef.scope ? .35 : 1) : .0022);
        p.yaw -= this.look.dx * sens; p.pitch -= this.look.dy * sens; this.look.dx = this.look.dy = 0;
        p.rec = Math.max(0, p.rec - dt * 14); p.pitch = Math.max(-1.45, Math.min(1.45, p.pitch));
        let fx = (this.keys.KeyW || this.keys.ArrowUp ? 1 : 0) - (this.keys.KeyS || this.keys.ArrowDown ? 1 : 0) - this.joy.y;
        let sx = (this.keys.KeyD || this.keys.ArrowRight ? 1 : 0) - (this.keys.KeyA || this.keys.ArrowLeft ? 1 : 0) + this.joy.x;
        const l = Math.hypot(fx, sx); if (l > 1) { fx /= l; sx /= l; }
        if (p.chute) {
          // هبوط بالمظلة
          const sp = 9 * Math.max(0, fx) + 3; p.vy = -(fx > 0 && p.pitch < -.7 ? 11 : 6.5);
          this.move(p, -Math.sin(p.yaw) * sp * dt + Math.cos(p.yaw) * sx * 4 * dt, -Math.cos(p.yaw) * sp * dt - Math.sin(p.yaw) * sx * 4 * dt, .4);
          p.y += p.vy * dt; p.speed = sp; $('altm').textContent = Math.max(0, Math.round(p.y)) + ' م';
          if (p.y <= 0) { p.y = 0; p.vy = 0; p.chute = false; wind(false); if (this.W.blocked(p.x, p.z, .5)) { const q = this.W.airportPoint(); p.x = q.x; p.z = q.z; } this.msg('هبطت! ابحث عن سلاح داخل الحظائر والبيوت', 3); this.updateVM(); $('hud').classList.remove('chute'); vibrate(60); }
        } else {
          const sprint = (this.keys.ShiftLeft || this.keys.ShiftRight || this.joy.sprint || this.autoRun) && fx > .3 && !this.ads && !p.crouch;
          const sp = (sprint ? 7.4 + (p.boost > 60 ? .4 : 0) : this.ads ? 2.6 : p.crouch ? 2.4 : 4.6) * (p.reload > 0 ? .85 : 1) * (p.heal ? .45 : 1);
          if (this.autoRun) fx = 1;
          const vx = (-Math.sin(p.yaw) * fx + Math.cos(p.yaw) * sx) * sp, vz = (-Math.cos(p.yaw) * fx - Math.sin(p.yaw) * sx) * sp;
          this.move(p, vx * dt, vz * dt, .38); p.speed = Math.hypot(vx, vz);
          if ((this.keys.Space || this.jumpReq) && p.y <= .001 && !p.crouch) { p.vy = 5.2; } this.jumpReq = false;
          p.vy -= 14 * dt; p.y = Math.max(0, p.y + p.vy * dt); if (p.y === 0) p.vy = 0;
          p.bob += dt * p.speed * 1.6;
        }
        $('hud').classList.toggle('chute', p.chute);
        // سلاح
        this.switchT = Math.max(0, (this.switchT || 0) - dt); p.cd -= dt;
        if (p.reload > 0) { p.reload -= dt; if (p.reload <= 0) { const w = this.wdef, need = w.mag - p.mags[this.wid], take = Math.min(need, p.ammo[w.ammo]); p.mags[this.wid] += take; p.ammo[w.ammo] -= take; this.updateAmmo(); } }
        if (this.fireHeld || this.keys.fire) this.playerFire(); else this.fireLatch = false;
        // علاج
        if (p.heal) { p.heal.t -= dt; if (p.heal.t <= 0) { const M = Weapons.MEDS[p.heal.k]; p.meds[p.heal.k]--; if (M.boost) p.boost = Math.min(100, p.boost + M.boost); else p.hp = Math.min(M.cap, Math.max(p.hp, p.hp + M.heal)); this.msg('تم استخدام ' + M.name, 1.2); blip(660, .15, .06, 'sine'); p.heal = null; this.updateInv(); } }
        if (p.boost > 0) { p.boost = Math.max(0, p.boost - dt * 1.3); if (p.hp < 100) p.hp = Math.min(100, p.hp + dt * (p.boost > 60 ? 1.6 : .8)); }
        // التقاط
        let nearW = null, nd = 9;
        for (const lt of this.loot) { if (lt.got) continue; const d = Math.hypot(lt.x - p.x, lt.z - p.z); if (d > 1.6 || p.chute) continue;
          if (lt.item.kind === 'weapon') { if (!p.slots[Weapons.W[lt.item.id].pistol ? 1 : 0] || (!p.slots[0] && !Weapons.W[lt.item.id].pistol)) this.take(lt); else if (d < nd) { nd = d; nearW = lt; } }
          else if (s.autoPick !== false) this.take(lt); else if (d < nd) { nd = d; nearW = lt; } }
        this.nearLoot = nearW; $('bPick').classList.toggle('show', !!nearW); if (nearW) $('bPick').querySelector('span').textContent = 'التقاط ' + this.lootName(nearW.item);
        if (this.keys.KeyF && nearW) { this.take(nearW); this.keys.KeyF = false; }
        this.loot = this.loot.filter(l => !l.got);
      }
      // ADS + كاميرا
      const adsT = this.ads && !p.dead && !p.chute ? 1 : 0; p.ads += (adsT - p.ads) * Math.min(1, dt * 12);
      cam.fov = 75 + (this.wdef.fov - 75) * p.ads + (p.chute ? 5 : 0); cam.updateProjectionMatrix();
      $('scope').classList.toggle('on', !!(this.wdef.scope && p.ads > .6)); this.updateVM();
      const eye = (p.crouch ? 1.1 : 1.65) + p.y, bobY = Math.sin(p.bob) * .035 * (p.y > 0 ? 0 : 1) * (1 - p.ads * .85), bobX = Math.cos(p.bob * .5) * .02 * (1 - p.ads * .85);
      this.fx.shake = Math.max(0, (this.fx.shake || 0) - dt * 6); const sh = this.fx.shake * .004;
      if (!this.tpp || p.dead) {
        cam.position.set(p.x, eye + bobY, p.z); cam.rotation.set(p.pitch + rnd(-sh, sh), p.yaw + rnd(-sh, sh), 0);
      } else {
        cam.rotation.set(p.pitch + rnd(-sh, sh), p.yaw, 0);
        const piv = new THREE.Vector3(p.x, eye - .05, p.z), back = new THREE.Vector3(0, 0, 1).applyQuaternion(cam.quaternion), right = new THREE.Vector3(1, 0, 0).applyQuaternion(cam.quaternion);
        const desired = (p.ads > .5 ? 1.3 : 2.9) + (p.chute ? 5 : 0); const dir = back.clone().multiplyScalar(desired).addScaledVector(right, p.ads > .5 ? .45 : .62).add(new THREE.Vector3(0, .28, 0)); const L = dir.length(); dir.normalize();
        const h = this.rayWorld(piv.x, piv.y, piv.z, dir.x, dir.y, dir.z, L, true), d = h.kind === 'wall' ? Math.max(.4, h.t - .25) : L;
        cam.position.copy(piv).addScaledVector(dir, d);
      }
      if (p.dead) { this.deadT = (this.deadT || 0) + dt; cam.position.y = Math.max(.4, eye - this.deadT * 1.6); cam.rotation.z = Math.min(.6, this.deadT); } else { this.deadT = 0; cam.rotation.z = 0; }
      // شخصية اللاعب (منظور ثالث) والمظلة
      const showBody = this.tpp && !p.dead; this.pmodel.visible = showBody;
      if (showBody) {
        this.pmodel.position.set(p.x, p.y, p.z); this.pmodel.rotation.y = p.yaw + Math.PI; this.pmodel.scale.y = p.crouch ? .75 : 1;
        if (this.pchar) { const want = p.chute ? 'Idle' : p.speed < .4 ? 'Idle' : p.speed < 5 ? 'Walk' : 'Run'; if (want !== this.panim) { this.pchar.play(want, .25); this.panim = want; } this.pchar.setSpeed(want === 'Walk' ? p.speed / 1.6 : want === 'Run' ? p.speed / 5.5 : 1); this.pchar.mixer.update(dt); }
      }
      this.chute.visible = p.chute; if (p.chute) { this.chute.position.set(p.x, p.y + 6.2, p.z); this.chute.rotation.y = p.yaw; }
      // السلاح بيد الشخص الأول
      this.vmKick = Math.max(0, (this.vmKick || 0) - dt * 14); const vm = this.vms[this.wid], w = this.wdef;
      if (vm) { const rl = p.reload > 0 ? Math.sin(Math.min(1, (w.reload - p.reload) / w.reload) * Math.PI) : 0, sw = this.switchT / .35, hl = p.heal ? .25 : 0;
        vm.position.set((w.pistol ? .16 : .2) - p.ads * (w.pistol ? .16 : .2) + bobX, (w.pistol ? -.2 : -.22) + p.ads * (w.pistol ? .1 : .075) + bobY * .6 - rl * .18 - sw * .25 - hl, (w.pistol ? -.42 : -.38) + this.vmKick * .05 + p.ads * .05);
        vm.rotation.set(this.vmKick * (w.pistol ? .15 : .05) + rl * .5 + sw * .6, 0, 0); }
      this.flash.intensity = Math.max(0, this.flash.intensity - dt * 120); this.flashMesh.material.opacity = Math.max(0, this.flashMesh.material.opacity - dt * 22);
      this.W.sun.position.set(p.x + 120, 160, p.z + 70); this.W.sun.target.position.set(p.x, 0, p.z);
      for (const b of this.bots.slice()) this.updateBot(b, dt);
      this.virtualT -= dt; if (this.virtualT <= 0 && this.alive > 3 && this.bots.length > 2) {
        this.virtualT = rnd(25, 40); const far = this.bots.filter(b => Math.hypot(b.x - p.x, b.z - p.z) > 120);
        if (far.length >= 2) { const a = far[0], b = far[1]; this.alive--; this.feed(`${a.name} <i>${Weapons.W[a.wpn].name}</i> ${b.name}`); this.killBot(b); }
      }
      for (const c of this.corpses) { c.t += dt; c.model.rotation.x = Math.max(c.model.rotation.x - dt * 3.2, -Math.PI / 2); if (c.t > c.ttl) { this.scene.remove(c.model); c.dead = true; } }
      this.corpses = this.corpses.filter(c => !c.dead);
      this.updateZone(dt);
      for (const t of this.tracers) { t.life -= dt; t.l.material.opacity = Math.max(0, t.life * 12); if (t.life <= 0) { this.scene.remove(t.l); t.l.geometry.dispose(); t.dead = true; } } this.tracers = this.tracers.filter(t => !t.dead);
      for (const q of this.puffs) { q.life -= dt; q.v.y -= 5 * dt; q.m.position.addScaledVector(q.v, dt); q.m.material.opacity = Math.max(0, q.life * 2); if (q.life <= 0) { this.scene.remove(q.m); q.m.geometry.dispose(); q.dead = true; } } this.puffs = this.puffs.filter(q => !q.dead);
      for (const l of this.loot) l.mesh.children[l.mesh.children.length - 1].material.opacity = .3 + Math.sin(this.time * 3) * .2;
      for (const k of ['hit', 'hurt', 'kill', 'dirT', 'bloodT']) if (this.fx[k] > 0) this.fx[k] = Math.max(0, this.fx[k] - dt);
      if (this.alive <= 1 && !p.dead && !this.over) this.finish(true);
      this.hudT = (this.hudT || 0) + dt; if (this.hudT > .05) { this.hudT = 0; this.updateHud(); this.drawMini(); this.drawCompass(); }
    }
    updateZone(dt) {
      const z = this.zone, p = this.p;
      if (z.state === 'wait') { z.t -= dt; if (z.t <= 0) { z.state = 'shrink'; z.t = z.shrinkT; this.msg('المنطقة الآمنة تتقلص!', 3); if (S().alerts !== false) blip(200, .4, .1, 'sawtooth'); } }
      else if (z.state === 'shrink') { z.t -= dt; const k = 1 - Math.max(0, z.t) / z.shrinkT, e = k * k * (3 - 2 * k); z.r = z.fromR + (z.toR - z.fromR) * e; z.cx = z.fcx + (z.tcx - z.fcx) * e; z.cz = z.fcz + (z.tcz - z.fcz) * e; if (z.t <= 0) { if (z.phase < ZONE.length - 1) this.nextPhase(); else z.state = 'final'; } }
      this.zoneMesh.scale.set(z.r, 1, z.r); this.zoneMesh.position.x = z.cx; this.zoneMesh.position.z = z.cz;
      this.outside = Math.hypot(p.x - z.cx, p.z - z.cz) > z.r;
      if (this.outside && !p.dead && !p.chute) { this.zoneAcc = (this.zoneAcc || 0) + dt; if (this.zoneAcc >= .5) { this.zoneAcc = 0; this.damagePlayer(z.dmg * .5); } }
    }

    /* ---------- الواجهة ---------- */
    msg(t, sec) { const e = $('toast'); e.textContent = t; e.classList.add('show'); clearTimeout(this._mt); this._mt = setTimeout(() => e.classList.remove('show'), (sec || 2) * 1000); }
    feed(t, mine) { const l = document.createElement('div'); l.className = 'kf' + (mine ? ' mine' : ''); l.innerHTML = t; $('killfeed').prepend(l); setTimeout(() => l.remove(), 6000); while ($('killfeed').children.length > 5) $('killfeed').lastChild.remove(); }
    updateAmmo() { const w = this.wdef, p = this.p; $('mag').textContent = p.mags[this.wid]; $('reserve').textContent = p.ammo[w.ammo]; $('wname').textContent = w.name + ' · ' + Weapons.AMMO[w.ammo]; $('ammoBox').classList.toggle('low', p.mags[this.wid] <= Math.ceil(w.mag * .2)); }
    updateInv() {
      const p = this.p; for (let i = 0; i < 2; i++) { const el = $('slot' + i), id = p.slots[i]; el.classList.toggle('on', p.cur === i); el.classList.toggle('empty', !id); el.querySelector('b').textContent = id ? Weapons.W[id].name : (i ? 'مسدس' : 'فارغ'); el.querySelector('small').textContent = id ? (p.mags[id] + '/' + p.ammo[Weapons.W[id].ammo]) : '—'; }
      const m = p.meds; $('medCnt').textContent = m.band + m.fak + m.kit + m.drink; $('gear').innerHTML = `<span class="${p.helmet ? 'on' : ''}">⛑ ${p.helmet || '-'}</span><span class="${p.vest ? 'on' : ''}">🦺 ${p.vest || '-'}</span><span>🩹${m.band} ✚${m.fak} ⛑${m.kit} ⚡${m.drink}</span>`;
    }
    updateHud(force) {
      const p = this.p, z = this.zone;
      $('hpFill').style.width = p.hp + '%'; $('hpNum').textContent = Math.ceil(p.hp); $('hpFill').classList.toggle('low', p.hp < 35); $('boostFill').style.width = p.boost + '%';
      $('alive').textContent = this.alive; $('kills').textContent = p.kills;
      const secs = Math.max(0, Math.ceil(z.t)); $('zoneT').textContent = (z.state === 'wait' ? 'التقلص بعد ' : z.state === 'shrink' ? 'تتقلص ' : 'نهائية ') + (z.state === 'final' ? '' : Math.floor(secs / 60) + ':' + String(secs % 60).padStart(2, '0'));
      $('zoneBox').classList.toggle('danger', !!this.outside); $('zoneWarn').classList.toggle('show', !!this.outside && !p.dead && !p.chute);
      $('vignette').style.opacity = Math.min(1, (this.fx.hurt || 0) * 2.2 + (p.hp < 30 ? .5 + Math.sin(this.time * 6) * .15 : 0));
      $('bloodScr').style.opacity = S().blood !== false ? Math.min(.9, (this.fx.bloodT || 0) * .9 + (p.hp < 25 ? .35 : 0)) : 0;
      $('hitmark').style.opacity = this.fx.hit > 0 ? 1 : 0; $('hitmark').classList.toggle('head', !!this.fx.headshot);
      $('crosshair').classList.toggle('ads', p.ads > .5); $('crosshair').style.setProperty('--sp', (Math.min(18, p.speed * 1.6 + p.rec * 2)) + 'px');
      const di = $('dmgDir'); di.style.opacity = this.fx.dirT > 0 ? Math.min(1, this.fx.dirT) : 0; if (this.fx.dirAngle !== undefined) di.style.transform = `translate(-50%,-50%) rotate(${-(this.fx.dirAngle + p.yaw) * 180 / Math.PI + 180}deg) translateY(-120px)`;
      const hb = $('healBar'); hb.classList.toggle('show', !!p.heal); if (p.heal) { hb.querySelector('i').style.width = (1 - p.heal.t / p.heal.total) * 100 + '%'; hb.querySelector('span').textContent = Weapons.MEDS[p.heal.k].name + ' ' + p.heal.t.toFixed(1); }
      if (this.fx.kill <= 0) $('killBanner').classList.remove('show');
      if (force) this.updateAmmo(); if ((this.invT = (this.invT || 0) + 1) % 10 === 0) this.updateInv();
    }
    drawMini() {
      const c = this.mctx, Sz = this.mini.width, p = this.p, R = 75, k = Sz / (R * 2), z = this.zone, W = this.W;
      c.save(); c.clearRect(0, 0, Sz, Sz); c.beginPath(); c.arc(Sz / 2, Sz / 2, Sz / 2, 0, TAU); c.clip();
      c.fillStyle = '#b8a67e'; c.fillRect(0, 0, Sz, Sz); c.translate(Sz / 2, Sz / 2); c.rotate(p.yaw); c.scale(k, k); c.translate(-p.x, -p.z);
      c.fillStyle = '#a7a397'; c.fillRect(W.AP.x0, W.AP.z0, W.AP.x1 - W.AP.x0, W.AP.z1 - W.AP.z0); c.fillStyle = '#3a3b3d'; c.fillRect(-338, -346, 32, 248);
      c.fillStyle = '#4b8a98'; c.beginPath(); for (let zz = p.z - R * 1.5; zz <= p.z + R * 1.5; zz += 6) c.lineTo(W.riverX(zz) - W.RIVER_W / 2, zz); for (let zz = p.z + R * 1.5; zz >= p.z - R * 1.5; zz -= 6) c.lineTo(W.riverX(zz) + W.RIVER_W / 2, zz); c.fill();
      c.fillStyle = '#c9b88f'; for (const b of W.BRIDGES) c.fillRect(W.riverX(b) - W.RIVER_W / 2 - 8, b - 9, W.RIVER_W + 16, 18);
      for (const o of W.near(p.x, p.z, R * 1.3)) { c.fillStyle = o.kind === 'h' ? '#8a7350' : o.kind === 'c' ? '#8f9a6a' : o.kind === 't' ? '#5a7a3a' : '#6f6552'; c.fillRect(o.x0, o.z0, o.x1 - o.x0, o.z1 - o.z0); }
      c.strokeStyle = '#fff'; c.lineWidth = 2.4 / k; c.beginPath(); c.arc(z.cx, z.cz, z.r, 0, TAU); c.stroke();
      if (z.state === 'wait' && z.phase > 0) { c.strokeStyle = '#3aa8ff'; c.setLineDash([6 / k, 4 / k]); c.beginPath(); c.arc(z.tcx, z.tcz, z.toR, 0, TAU); c.stroke(); c.setLineDash([]); }
      c.fillStyle = 'rgba(40,110,255,.22)'; c.beginPath(); c.rect(p.x - R * 3, p.z - R * 3, R * 6, R * 6); c.arc(z.cx, z.cz, z.r, 0, TAU, true); c.fill('evenodd');
      for (const b of this.bots) { const d = Math.hypot(b.x - p.x, b.z - p.z); if (b.state === 'fight' || (b.heard > 0 && d < 70)) { c.fillStyle = '#ff3b30'; c.beginPath(); c.arc(b.x, b.z, 3 / k, 0, TAU); c.fill(); } }
      c.restore(); c.save(); c.translate(Sz / 2, Sz / 2); c.fillStyle = '#ffd24a'; c.strokeStyle = '#000'; c.lineWidth = 1.5; c.beginPath(); c.moveTo(0, -9); c.lineTo(6, 7); c.lineTo(0, 3); c.lineTo(-6, 7); c.closePath(); c.fill(); c.stroke(); c.restore();
    }
    drawCompass() {
      const c = this.cctx, w = this.compass.width; c.clearRect(0, 0, w, 40);
      const brg = ((-this.p.yaw * 180 / Math.PI) % 360 + 360) % 360, pp = 3.2, names = { 0: 'ش', 90: 'شر', 180: 'ج', 270: 'غ' }; c.textAlign = 'center'; c.strokeStyle = 'rgba(255,255,255,.6)';
      for (let d = -60; d <= 60; d += 5) { const a = Math.round(brg / 5) * 5 + d, x = w / 2 + (a - brg) * pp, A = ((a % 360) + 360) % 360;
        if (A % 90 === 0) { c.font = 'bold 15px Cairo'; c.fillStyle = A === 0 ? '#ff5a4a' : '#fff'; c.fillText(names[A], x, 22); } else if (A % 15 === 0) { c.font = '11px Cairo'; c.fillStyle = 'rgba(255,255,255,.8)'; c.fillText(A, x, 20); }
        c.beginPath(); c.moveTo(x, 28); c.lineTo(x, A % 15 === 0 ? 24 : 26); c.stroke(); }
      c.fillStyle = '#ffd24a'; c.beginPath(); c.moveTo(w / 2 - 6, 0); c.lineTo(w / 2 + 6, 0); c.lineTo(w / 2, 8); c.fill();
      const z = this.zone, ang = Math.atan2(z.cx - this.p.x, -(z.cz - this.p.z)) * 180 / Math.PI, rel = ((ang - brg + 540) % 360) - 180;
      if (Math.abs(rel) < 60) { c.fillStyle = '#3aa8ff'; c.fillRect(w / 2 + rel * pp - 3, 30, 6, 6); }
    }
    finish(win, killer) {
      this.over = true; const p = this.p; if (document.pointerLockElement) document.exitPointerLock(); wind(false);
      const rank = win ? 1 : this.alive + 0;
      $('endTitle').textContent = win ? 'نصرٌ مؤزَّر! عشاء الأبطال' : 'سقطت في الميدان'; $('endTitle').className = win ? 'win' : 'lose';
      $('endSub').textContent = win ? 'أنت آخر من بقي في بغداد — بطل مكافحة الإرهاب' : `المركز #${rank} من ${BOTS + 1}` + (killer ? ` · أطاح بك ${killer}` : '');
      $('sRank').textContent = '#' + rank; $('sKills').textContent = p.kills; $('sDmg').textContent = Math.round(p.dmgDealt); $('sTime').textContent = Math.floor(this.time / 60) + ':' + String(Math.floor(this.time % 60)).padStart(2, '0');
      const xp = (win ? 300 : 60) + p.kills * 60 + Math.floor(this.time / 6) + Math.max(0, (BOTS + 1 - rank) * 8); $('sXp').textContent = '+' + xp; this.reward = { xp, coins: Math.floor(xp / 2), win, kills: p.kills };
      setTimeout(() => $('endScreen').classList.add('show'), win ? 600 : 1500);
    }

    /* ---------- التحكم ---------- */
    bindInput() {
      const self = this, touch = 'ontouchstart' in window;
      addEventListener('keydown', e => { if (!self.active) return; self.keys[e.code] = true; const k = e.code;
        if (k === 'KeyR') self.reload(); if (k === 'Digit1') self.setWeapon(0); if (k === 'Digit2') self.setWeapon(1); if (k === 'KeyH') self.heal(); if (k === 'KeyC') self.p.crouch = !self.p.crouch; if (k === 'KeyV') { const s = S(); s.view = s.view === 'tpp' ? 'fpp' : 'tpp'; self.applySettings(); }
        if (['Space', 'ArrowUp', 'ArrowDown'].includes(k)) e.preventDefault(); });
      addEventListener('keyup', e => { self.keys[e.code] = false; });
      const cv = this.r.domElement;
      cv.addEventListener('mousedown', e => { if (!self.active || self.over || touch) return; if (!document.pointerLockElement) { cv.requestPointerLock && cv.requestPointerLock(); audio(); return; } if (e.button === 0) self.fireHeld = true; if (e.button === 2) self.ads = true; });
      addEventListener('mouseup', e => { if (e.button === 0) self.fireHeld = false; if (e.button === 2) self.ads = false; });
      addEventListener('contextmenu', e => { if (self.active) e.preventDefault(); });
      addEventListener('mousemove', e => { if (self.active && document.pointerLockElement) { self.look.dx += e.movementX; self.look.dy += e.movementY; } });
      // لمس: عصا (ثابتة أو حرة) + سحب للنظر
      const jb = $('joyBase'), jk = $('joyKnob'); let joyId = null, lookId = null, lx = 0, ly = 0, jc = null; const R = 55;
      addEventListener('touchstart', e => {
        if (!self.active || self.over) return; audio();
        for (const t of e.changedTouches) {
          const el = document.elementFromPoint(t.clientX, t.clientY); if (el && el.closest && (el.closest('.tbtn') || el.closest('.slot') || el.closest('#bPick'))) continue;
          const r0 = jb.getBoundingClientRect(), inJoy = Math.hypot(t.clientX - (r0.left + r0.width / 2), t.clientY - (r0.top + r0.height / 2)) < r0.width * .9;
          if (joyId === null && ((self.layoutFree && t.clientX < innerWidth * .45) || (!self.layoutFree && inJoy))) {
            joyId = t.identifier; if (self.layoutFree) { jb.dataset.ox = jb.style.left; jb.dataset.oy = jb.style.top; jb.style.left = (t.clientX - r0.width / 2) + 'px'; jb.style.top = (t.clientY - r0.height / 2) + 'px'; jc = { x: t.clientX, y: t.clientY }; } else jc = { x: r0.left + r0.width / 2, y: r0.top + r0.height / 2 };
            jb.classList.add('act');
          } else if (lookId === null) { lookId = t.identifier; lx = t.clientX; ly = t.clientY; }
        }
      }, { passive: true });
      addEventListener('touchmove', e => {
        for (const t of e.changedTouches) {
          if (t.identifier === joyId) { let dx = t.clientX - jc.x, dy = t.clientY - jc.y; const l = Math.hypot(dx, dy); self.autoRun = dy < -R * 1.6; if (l > R) { dx *= R / l; dy *= R / l; } jk.style.transform = `translate(${dx}px,${dy}px)`; self.joy.x = dx / R; self.joy.y = dy / R; self.joy.sprint = l > R * .95; }
          else if (t.identifier === lookId) { self.look.dx += (t.clientX - lx) * 1.4; self.look.dy += (t.clientY - ly) * 1.4; lx = t.clientX; ly = t.clientY; }
        }
      }, { passive: true });
      const end = e => { for (const t of e.changedTouches) { if (t.identifier === joyId) { joyId = null; self.joy.x = self.joy.y = 0; self.joy.sprint = false; self.autoRun = false; jk.style.transform = ''; jb.classList.remove('act'); if (self.layoutFree && jb.dataset.ox !== undefined) { jb.style.left = jb.dataset.ox; jb.style.top = jb.dataset.oy; } } if (t.identifier === lookId) lookId = null; } };
      addEventListener('touchend', end); addEventListener('touchcancel', end);
      const hold = (id, on, off) => { const el = $(id); if (!el) return; el.addEventListener('touchstart', e => { e.preventDefault(); e.stopPropagation(); audio(); on(); }, { passive: false }); el.addEventListener('touchend', e => { e.preventDefault(); off && off(); }); el.addEventListener('mousedown', e => { e.stopPropagation(); audio(); on(); }); el.addEventListener('mouseup', () => off && off()); };
      hold('bFire', () => self.fireHeld = true, () => self.fireHeld = false); hold('bFire2', () => self.fireHeld = true, () => self.fireHeld = false);
      hold('bAds', () => self.ads = !self.ads); hold('bJump', () => self.jumpReq = true); hold('bReload', () => self.reload()); hold('bCrouch', () => self.p.crouch = !self.p.crouch);
      hold('bHeal', () => self.heal()); hold('bView', () => { const s = S(); s.view = s.view === 'tpp' ? 'fpp' : 'tpp'; self.applySettings(); window.saveSettings && window.saveSettings(); });
      hold('slot0', () => self.setWeapon(0)); hold('slot1', () => self.setWeapon(1)); hold('bPick', () => self.nearLoot && self.take(self.nearLoot));
    }
  }
  window.Game = Game;
})();
