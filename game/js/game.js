/* منطق المود: باتل رويال في بغداد — لاعب + أعداء + منطقة آمنة تتقلص + ذخيرة وإسعافات */
(function () {
  const $ = id => document.getElementById(id);
  const TAU = Math.PI * 2, rnd = (a, b) => a + Math.random() * (b - a);
  const BOTS = 15;
  const ZONE = [ // [نصف القطر النهائي، مدة الانتظار، مدة التقلص، ضرر/ثانية]
    [260, 40, 30, 1], [170, 30, 28, 2], [105, 25, 25, 4], [55, 20, 22, 6], [20, 15, 20, 9]
  ];

  /* ---------- صوت مُولَّد ---------- */
  let ac = null;
  function audio() { try { ac = ac || new (window.AudioContext || window.webkitAudioContext)(); if (ac.state === 'suspended') ac.resume(); } catch (e) {} return ac; }
  function shot(vol, suppressed) {
    const a = audio(); if (!a || vol < .02) return; const t = a.currentTime;
    const len = suppressed ? .09 : .18, buf = a.createBuffer(1, a.sampleRate * len, a.sampleRate), d = buf.getChannelData(0);
    for (let i = 0; i < d.length; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / d.length, suppressed ? 4 : 2.5);
    const n = a.createBufferSource(), f = a.createBiquadFilter(), g = a.createGain();
    f.type = 'lowpass'; f.frequency.value = suppressed ? 1800 : 3600; g.gain.value = vol * (suppressed ? .5 : .8); n.buffer = buf; n.connect(f).connect(g).connect(a.destination); n.start(t);
    const o = a.createOscillator(), og = a.createGain(); o.type = 'sine'; o.frequency.setValueAtTime(150, t); o.frequency.exponentialRampToValueAtTime(40, t + .12);
    og.gain.setValueAtTime(vol * .7, t); og.gain.exponentialRampToValueAtTime(.001, t + .14); o.connect(og).connect(a.destination); o.start(t); o.stop(t + .15);
  }
  function blip(freq, dur, vol, type) {
    const a = audio(); if (!a) return; const t = a.currentTime, o = a.createOscillator(), g = a.createGain();
    o.type = type || 'square'; o.frequency.value = freq; g.gain.setValueAtTime(vol || .08, t); g.gain.exponentialRampToValueAtTime(.001, t + dur);
    o.connect(g).connect(a.destination); o.start(t); o.stop(t + dur);
  }

  class Game {
    constructor(renderer, world, scene, onExit) {
      this.r = renderer; this.W = world; this.scene = scene; this.onExit = onExit;
      this.cam = new THREE.PerspectiveCamera(75, innerWidth / innerHeight, .08, 1400);
      this.cam.rotation.order = 'YXZ'; scene.add(this.cam);
      this.vm = Models.buildViewmodel(); this.vm.position.set(.2, -.22, -.38); this.cam.add(this.vm);
      this.flash = new THREE.PointLight(0xffc27a, 0, 12); this.vm.userData.muzzle.add(this.flash);
      this.flashMesh = new THREE.Mesh(new THREE.PlaneGeometry(.22, .22), new THREE.MeshBasicMaterial({ color: 0xffd98a, transparent: true, opacity: 0, blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide }));
      this.vm.userData.muzzle.add(this.flashMesh);
      this.tracers = []; this.puffs = []; this.bots = []; this.loot = []; this.corpses = [];
      this.keys = {}; this.look = { dx: 0, dy: 0 }; this.joy = { x: 0, y: 0 }; this.fireHeld = false; this.ads = false;
      this.running = false; this.active = false;
      // الأعداء (نماذج مشتركة)
      this.zoneMesh = new THREE.Mesh(new THREE.CylinderGeometry(1, 1, 260, 72, 1, true), new THREE.MeshBasicMaterial({ color: 0xff3b2a, transparent: true, opacity: .16, side: THREE.DoubleSide, depthWrite: false, fog: false }));
      this.zoneMesh.position.y = 120; scene.add(this.zoneMesh);
      this.mini = $('minimap'); this.mctx = this.mini.getContext('2d');
      this.compass = $('compass'); this.cctx = this.compass.getContext('2d');
      this.bindInput();
      addEventListener('resize', () => { this.cam.aspect = innerWidth / innerHeight; this.cam.updateProjectionMatrix(); });
    }

    /* ---------- بدء المباراة ---------- */
    start() {
      const W = this.W, side = Math.random() < .5 ? 1 : -1, sp = W.randomOpenPoint(side, 3);
      this.p = { x: sp.x, z: sp.z, y: 0, vy: 0, yaw: rnd(0, TAU), pitch: 0, hp: 100, mag: 30, reserve: 120, reload: 0, cd: 0, kills: 0, rec: 0, bob: 0, ads: 0, dead: false, lastHit: 0, speed: 0 };
      for (const b of this.bots) this.scene.remove(b.model); for (const c of this.corpses) this.scene.remove(c.model); for (const l of this.loot) this.scene.remove(l.mesh);
      this.bots = []; this.corpses = []; this.loot = [];
      for (let i = 0; i < BOTS; i++) {
        let q; for (let k = 0; k < 40; k++) { q = W.randomOpenPoint(0, 3); if (Math.hypot(q.x - sp.x, q.z - sp.z) > 70) break; }
        const model = Models.buildSoldier({ enemy: true }); model.position.set(q.x, 0, q.z); this.scene.add(model);
        this.bots.push({ x: q.x, z: q.z, yaw: rnd(0, TAU), hp: 100, model, state: 'roam', wp: null, seen: 0, burst: 0, cd: rnd(0, 1), react: 0, phase: rnd(0, 6), stuck: 0, id: i, side: Math.sign(q.x - W.riverX(q.z)) });
      }
      for (let i = 0; i < 55; i++) { const q = W.randomOpenPoint(0, 2); this.addLoot(q.x, q.z, i % 3 === 0 ? 'med' : 'ammo'); }
      // المنطقة الآمنة
      const cx = rnd(-90, 90), cz = rnd(-90, 90);
      this.zone = { cx, cz, r: 340, fromR: 340, toR: 340, phase: -1, t: 0, state: 'wait', wait: 50, dmg: 0, tcx: cx, tcz: cz, fcx: cx, fcz: cz };
      this.nextPhase(); this.zone.state = 'wait'; this.zone.t = 50;
      this.alive = BOTS + 1; this.time = 0; this.fx = {}; this.killfeed = []; this.over = false; this.virtualT = rnd(25, 40);
      this.active = true; this.running = true; this.cam.fov = 75; this.cam.updateProjectionMatrix();
      $('hud').classList.add('on'); $('endScreen').classList.remove('show'); this.updateHud(true);
      this.msg('هبطت في بغداد — اذهب إلى المنطقة الآمنة', 4);
      if (!('ontouchstart' in window)) $('hint').classList.add('show'), setTimeout(() => $('hint').classList.remove('show'), 7000);
      this.last = performance.now();
    }
    stop() { this.active = false; this.running = false; $('hud').classList.remove('on'); if (document.pointerLockElement) document.exitPointerLock(); this.vm.visible = false; }
    show() { this.vm.visible = true; }

    nextPhase() {
      const z = this.zone; z.phase++; const Z = ZONE[Math.min(z.phase, ZONE.length - 1)];
      z.fromR = z.r; z.toR = Z[0]; z.fcx = z.cx; z.fcz = z.cz;
      const maxShift = Math.max(0, z.fromR - z.toR) * .8, a = rnd(0, TAU), d = rnd(0, maxShift);
      z.tcx = z.fcx + Math.cos(a) * d; z.tcz = z.fcz + Math.sin(a) * d; z.dmg = Z[3]; z.shrinkT = Z[2]; z.waitT = Z[1];
      z.state = 'wait'; z.t = Z[1];
    }

    addLoot(x, z, kind) {
      const g = new THREE.Group();
      if (kind === 'ammo') { const b = new THREE.Mesh(new THREE.BoxGeometry(.4, .28, .25), new THREE.MeshStandardMaterial({ color: 0x5a6b3a, roughness: .7 })); b.position.y = .25; g.add(b);
        const s = new THREE.Mesh(new THREE.BoxGeometry(.42, .06, .27), new THREE.MeshStandardMaterial({ color: 0xe0b020, emissive: 0x6a5000 })); s.position.y = .35; g.add(s); }
      else { const b = new THREE.Mesh(new THREE.BoxGeometry(.4, .28, .3), new THREE.MeshStandardMaterial({ color: 0xf2f2f2, roughness: .5 })); b.position.y = .25; g.add(b);
        const c1 = new THREE.Mesh(new THREE.BoxGeometry(.22, .3, .06), new THREE.MeshStandardMaterial({ color: 0xd01c1c, emissive: 0x500000 })); c1.position.set(0, .25, .15); g.add(c1);
        const c2 = new THREE.Mesh(new THREE.BoxGeometry(.06, .3, .22), c1.material); c2.position.set(0, .25, .15); g.add(c2); }
      const ring = new THREE.Mesh(new THREE.CylinderGeometry(.5, .5, 14, 16, 1, true), new THREE.MeshBasicMaterial({ color: kind === 'ammo' ? 0xffd24a : 0x6bff8f, transparent: true, opacity: .12, depthWrite: false, side: THREE.DoubleSide, fog: false }));
      ring.position.y = 7; g.add(ring);
      g.position.set(x, 0, z); this.scene.add(g); this.loot.push({ x, z, kind, mesh: g });
    }

    /* ---------- الحركة والتصادم ---------- */
    move(o, dx, dz, r) {
      const W = this.W;
      const tryMove = (nx, nz) => { if (W.isWater(nx, nz) || Math.abs(nx) > W.HALF - 2 || Math.abs(nz) > W.HALF - 2) return false; return !W.blocked(nx, nz, r, o.y !== undefined ? o.y : 0); };
      let hit = false;
      if (tryMove(o.x + dx, o.z)) o.x += dx; else hit = true;
      if (tryMove(o.x, o.z + dz)) o.z += dz; else hit = true;
      return hit;
    }

    /* ---------- إطلاق نار ---------- */
    rayWorld(ox, oy, oz, dx, dy, dz, maxT, ignoreBot) {
      let best = { t: maxT, kind: null };
      // الأرض
      if (dy < 0) { const t = -oy / dy; if (t > 0 && t < best.t) best = { t, kind: 'ground' }; }
      // المباني والعوائق (تقاطع صندوق)
      const seen = new Set(), step = 10;
      for (let s = 0; s < maxT; s += step) {
        const px = ox + dx * s, pz = oz + dz * s;
        for (const c of this.W.near(px, pz, 10)) {
          if (seen.has(c)) continue; seen.add(c);
          let t0 = 0, t1 = best.t;
          const sl = (o, d, lo, hi) => { if (Math.abs(d) < 1e-8) return o >= lo && o <= hi; let a = (lo - o) / d, b = (hi - o) / d; if (a > b) { const tmp = a; a = b; b = tmp; } t0 = Math.max(t0, a); t1 = Math.min(t1, b); return t0 <= t1; };
          if (sl(ox, dx, c.x0, c.x1) && sl(oy, dy, 0, c.h) && sl(oz, dz, c.z0, c.z1) && t0 > 0 && t0 < best.t) best = { t: t0, kind: 'wall' };
        }
      }
      if (ignoreBot !== true) for (const b of this.bots) {
        if (b.hp <= 0 || b === ignoreBot) continue;
        const rx = b.x - ox, rz = b.z - oz, dd = dx * dx + dz * dz, tc = (rx * dx + rz * dz) / dd;
        if (tc <= 0 || tc > best.t) continue; const cx = ox + dx * tc - b.x, cz = oz + dz * tc - b.z;
        if (cx * cx + cz * cz > .13) continue; const y = oy + dy * tc; if (y < 0 || y > 1.85) continue;
        best = { t: tc, kind: 'bot', bot: b, head: y > 1.5 };
      }
      return best;
    }
    losClear(ax, ay, az, bx, by, bz) { const dx = bx - ax, dy = by - ay, dz = bz - az, L = Math.hypot(dx, dy, dz); const h = this.rayWorld(ax, ay, az, dx / L, dy / L, dz / L, L, true); return !(h.kind === 'wall'); }

    tracer(from, to, color, life) {
      const g = new THREE.BufferGeometry().setFromPoints([from, to]);
      const l = new THREE.Line(g, new THREE.LineBasicMaterial({ color: color || 0xffe2a0, transparent: true, opacity: .9, blending: THREE.AdditiveBlending, fog: false }));
      this.scene.add(l); this.tracers.push({ l, life: life || .07 });
    }
    puff(pos, color, n) {
      for (let i = 0; i < (n || 5); i++) {
        const m = new THREE.Mesh(new THREE.SphereGeometry(.07 + Math.random() * .08, 5, 4), new THREE.MeshBasicMaterial({ color: color || 0xcdbf99, transparent: true, opacity: .8, fog: false }));
        m.position.copy(pos); this.scene.add(m);
        this.puffs.push({ m, v: new THREE.Vector3(rnd(-1, 1), rnd(.4, 2), rnd(-1, 1)), life: rnd(.25, .5) });
      }
    }

    playerFire() {
      const p = this.p; if (p.cd > 0 || p.reload > 0 || p.dead) return;
      if (p.mag <= 0) { this.reload(); return; }
      p.mag--; p.cd = .085; p.rec = Math.min(p.rec + 1, 12);
      const spread = (this.ads ? .0025 : .009) * (1 + p.speed * .12) + p.rec * .0006;
      const cam = this.cam, dir = new THREE.Vector3(0, 0, -1).applyQuaternion(cam.quaternion);
      dir.x += rnd(-spread, spread); dir.y += rnd(-spread, spread); dir.z += rnd(-spread, spread); dir.normalize();
      const o = cam.position, h = this.rayWorld(o.x, o.y, o.z, dir.x, dir.y, dir.z, 300);
      const end = o.clone().addScaledVector(dir, h.t);
      const mw = new THREE.Vector3(); this.vm.userData.muzzle.getWorldPosition(mw); this.tracer(mw, end);
      if (h.kind === 'bot') {
        const dmg = h.head ? 70 : 28; this.damageBot(h.bot, dmg, h.head); this.fx.hit = .18; this.fx.headshot = h.head; blip(h.head ? 1400 : 900, .05, .05);
        this.puff(end, 0x992222, 4);
      } else this.puff(end, h.kind === 'ground' ? 0xcdbf99 : 0xd8d2c0, 5);
      shot(.5, true); this.fx.flash = .05; this.flash.intensity = 6; this.flashMesh.material.opacity = .9; this.flashMesh.rotation.z = Math.random() * 6;
      this.p.pitch += .0065 + rnd(0, .004); this.p.yaw += rnd(-.0035, .0035); this.vmKick = 1;
      // الأعداء يسمعون
      for (const b of this.bots) if (b.hp > 0 && Math.hypot(b.x - p.x, b.z - p.z) < 90) b.heard = 4;
      this.updateAmmo();
    }
    reload() {
      const p = this.p; if (p.reload > 0 || p.mag >= 30 || p.reserve <= 0 || p.dead) return;
      p.reload = 2.0; this.msg('إعادة التعبئة…', 1.6); blip(300, .08, .06, 'triangle'); setTimeout(() => blip(500, .06, .06, 'triangle'), 900);
    }
    damageBot(b, dmg, head) {
      if (b.hp <= 0) return; b.hp -= dmg; b.seen = 6; b.heard = 6;
      if (b.hp <= 0) {
        this.p.kills++; this.alive--; this.feed(`أنت ${head ? '🎯 ' : ''}أطحت بلاعب`); this.killBot(b); this.addLoot(b.x, b.z, 'ammo');
        this.fx.kill = 1.2; blip(600, .1, .07); setTimeout(() => blip(900, .12, .07), 90);
      }
    }
    killBot(b) { b.hp = 0; this.corpses.push({ model: b.model, t: 0, ttl: 25 }); this.bots = this.bots.filter(x => x !== b); }

    damagePlayer(d, fromX, fromZ) {
      const p = this.p; if (p.dead) return; p.hp -= d; this.fx.hurt = .35; p.lastHit = this.time;
      if (fromX !== undefined) { const a = Math.atan2(fromX - p.x, fromZ - p.z); this.fx.dirAngle = a; this.fx.dirT = 1; }
      if (p.hp <= 0) { p.hp = 0; p.dead = true; this.finish(false); }
      this.updateHud();
    }

    /* ---------- الذكاء الاصطناعي ---------- */
    updateBot(b, dt) {
      const p = this.p, W = this.W, dx = p.x - b.x, dz = p.z - b.z, dist = Math.hypot(dx, dz);
      b.cd -= dt; b.heard = Math.max(0, (b.heard || 0) - dt); b.seen = Math.max(0, b.seen - dt); b.phase += dt;
      let canSee = false;
      if (!p.dead && dist < 85 && (dist < 45 || b.heard > 0 || b.seen > 0)) {
        const yawTo = Math.atan2(dx, dz); let dy = Math.atan2(Math.sin(yawTo - b.yaw), Math.cos(yawTo - b.yaw));
        if (Math.abs(dy) < 1.3 || b.seen > 0 || b.heard > 0) canSee = this.losClear(b.x, 1.55, b.z, p.x, 1.4, p.z);
      }
      if (canSee) { if (b.state !== 'fight') { b.state = 'fight'; b.react = rnd(.5, 1.1); } b.seen = 3; } else if (b.state === 'fight' && b.seen <= 0) b.state = 'roam';
      let speed = 0, tyaw = b.yaw;
      if (b.state === 'fight') {
        tyaw = Math.atan2(dx, dz);
        b.react -= dt;
        // مناورة: اقتراب إذا بعيد، تراجع إذا قريب، تحرك جانبي
        const sx = Math.cos(b.phase * .8) * 2.2;
        let fwd = dist > 35 ? 3 : dist < 14 ? -2.5 : 0;
        const vx = Math.sin(tyaw) * fwd + Math.cos(tyaw) * sx, vz = Math.cos(tyaw) * fwd - Math.sin(tyaw) * sx;
        const hit = this.move(b, vx * dt, vz * dt, .4); speed = Math.hypot(vx, vz);
        if (b.react <= 0 && b.cd <= 0 && canSee) {
          b.cd = b.burst > 0 ? .11 : rnd(.7, 1.4); if (b.burst <= 0) b.burst = (Math.random() * 4 | 0) + 3; b.burst--;
          const acc = Math.max(.04, .42 - dist / 230) * (this.ads ? .85 : 1) * (p.speed > 5 ? .8 : 1);
          const mw = new THREE.Vector3(); b.model.userData.muzzle.getWorldPosition(mw);
          const hitP = Math.random() < acc, target = new THREE.Vector3(p.x + (hitP ? 0 : rnd(-1.5, 1.5)), 1.3 + (hitP ? 0 : rnd(-.5, .5)), p.z + (hitP ? 0 : rnd(-1.5, 1.5)));
          this.tracer(mw, target, 0xff9a5a, .08); shot(Math.max(0, .5 - dist / 160), false);
          if (hitP) this.damagePlayer(rnd(4, 7), b.x, b.z);
          this.puff(mw, 0xffc27a, 1);
        }
      } else {
        // تجوال نحو نقطة
        if (!b.wp || Math.hypot(b.wp.x - b.x, b.wp.z - b.z) < 3 || b.stuck > 1.2) {
          const Z = this.zone; let q; for (let k = 0; k < 20; k++) { q = W.randomOpenPoint(b.side, 2); if (Math.hypot(q.x - Z.cx, q.z - Z.cz) < Z.r * .85) break; }
          b.wp = q; b.stuck = 0;
        }
        const ex = b.wp.x - b.x, ez = b.wp.z - b.z; tyaw = Math.atan2(ex, ez);
        const outside = Math.hypot(b.x - this.zone.cx, b.z - this.zone.cz) > this.zone.r; speed = outside ? 5.2 : 2.8;
        const hit = this.move(b, Math.sin(tyaw) * speed * dt, Math.cos(tyaw) * speed * dt, .4);
        b.stuck = hit ? b.stuck + dt : Math.max(0, b.stuck - dt);
      }
      let dyaw = Math.atan2(Math.sin(tyaw - b.yaw), Math.cos(tyaw - b.yaw)); b.yaw += dyaw * Math.min(1, dt * (b.state === 'fight' ? 9 : 5));
      b.model.position.set(b.x, 0, b.z); b.model.rotation.y = b.yaw;
      const u = b.model.userData, sw = Math.sin(b.phase * (speed > 3.5 ? 9 : 6)) * Math.min(1, speed / 3) * .7;
      u.legL.rotation.x = sw; u.legR.rotation.x = -sw; b.model.position.y = Math.abs(Math.sin(b.phase * 6)) * .02 * Math.min(1, speed);
      // ضرر المنطقة
      if (Math.hypot(b.x - this.zone.cx, b.z - this.zone.cz) > this.zone.r) { b.hp -= this.zone.dmg * dt * 1.1; if (b.hp <= 0) { this.alive--; this.feed('لاعب سقط خارج المنطقة'); this.killBot(b); } }
    }

    /* ---------- الحلقة ---------- */
    update(dt) {
      if (!this.active) return; dt = Math.min(dt, .05);
      const p = this.p, cam = this.cam, W = this.W;
      this.time += dt;
      if (!p.dead && !this.over) {
        // نظر
        const sens = this.ads ? .0013 : .0022;
        p.yaw -= this.look.dx * sens; p.pitch -= this.look.dy * sens; this.look.dx = this.look.dy = 0;
        p.rec = Math.max(0, p.rec - dt * 14); p.pitch = Math.max(-1.45, Math.min(1.45, p.pitch));
        // حركة
        let fx = (this.keys.KeyW || this.keys.ArrowUp ? 1 : 0) - (this.keys.KeyS || this.keys.ArrowDown ? 1 : 0) + -this.joy.y;
        let sx = (this.keys.KeyD || this.keys.ArrowRight ? 1 : 0) - (this.keys.KeyA || this.keys.ArrowLeft ? 1 : 0) + this.joy.x;
        const l = Math.hypot(fx, sx); if (l > 1) { fx /= l; sx /= l; }
        const sprint = (this.keys.ShiftLeft || this.keys.ShiftRight || this.joy.sprint) && fx > .3 && !this.ads;
        const sp = (sprint ? 7.6 : this.ads ? 2.6 : 4.6) * (p.reload > 0 ? .85 : 1);
        const vx = (-Math.sin(p.yaw) * fx + Math.cos(p.yaw) * sx) * sp, vz = (-Math.cos(p.yaw) * fx - Math.sin(p.yaw) * sx) * sp;
        this.move(p, vx * dt, vz * dt, .38); p.speed = Math.hypot(vx, vz);
        if ((this.keys.Space || this.jumpReq) && p.y <= .001) { p.vy = 5.2; this.jumpReq = false; }
        p.vy -= 14 * dt; p.y = Math.max(0, p.y + p.vy * dt); if (p.y === 0) p.vy = 0;
        p.bob += dt * p.speed * 1.6;
        // سلاح
        p.cd -= dt; if (p.reload > 0) { p.reload -= dt; if (p.reload <= 0) { const need = 30 - p.mag, take = Math.min(need, p.reserve); p.mag += take; p.reserve -= take; this.updateAmmo(); } }
        if ((this.fireHeld || this.keys.fire) ) this.playerFire();
        if (this.keys.KeyR) this.reload();
        // علاج بطيء
        if (this.time - p.lastHit > 8 && p.hp < 100) { p.hp = Math.min(100, p.hp + dt * 1.2); }
        // التقاط
        for (const lt of this.loot) if (!lt.got && Math.hypot(lt.x - p.x, lt.z - p.z) < 1.6) {
          if (lt.kind === 'ammo' && p.reserve < 240) { p.reserve = Math.min(240, p.reserve + 30); lt.got = true; this.msg('+30 ذخيرة', 1.2); blip(700, .08, .06); }
          else if (lt.kind === 'med' && p.hp < 100) { p.hp = Math.min(100, p.hp + 40); lt.got = true; this.msg('+40 صحة', 1.2); blip(500, .12, .06, 'sine'); }
          if (lt.got) { this.scene.remove(lt.mesh); this.updateAmmo(); }
        }
        this.loot = this.loot.filter(l => !l.got);
      }
      // ADS
      const adsTarget = (this.ads && !p.dead) ? 1 : 0; p.ads += (adsTarget - p.ads) * Math.min(1, dt * 12);
      cam.fov = 75 - p.ads * 22; cam.updateProjectionMatrix();
      // الكاميرا
      const bobY = Math.sin(p.bob) * .035 * (p.y > 0 ? 0 : 1) * (1 - p.ads * .8), bobX = Math.cos(p.bob * .5) * .02 * (1 - p.ads * .8);
      cam.position.set(p.x, 1.65 + p.y + bobY, p.z); cam.rotation.set(p.pitch, p.yaw, 0);
      if (p.dead) { cam.position.y = Math.max(.4, 1.65 - this.time * 0 - (this.deadT = (this.deadT || 0) + dt) * 1.6); cam.rotation.z = Math.min(.6, this.deadT); }
      else { this.deadT = 0; cam.rotation.z = 0; }
      // موقع السلاح (ADS يضعه في المنتصف)
      this.vmKick = Math.max(0, (this.vmKick || 0) - dt * 14);
      const vx0 = .2 - p.ads * .2 + bobX, vy0 = -.22 + p.ads * .075 + bobY * .6, vz0 = -.38 + this.vmKick * .05 + p.ads * .05;
      this.vm.position.set(vx0, vy0 - (p.reload > 0 ? Math.sin(Math.min(1, (2 - p.reload) / 2) * Math.PI) * .18 : 0), vz0);
      this.vm.rotation.set(this.vmKick * .05 + (p.reload > 0 ? Math.sin(Math.min(1, (2 - p.reload) / 2) * Math.PI) * .5 : 0), 0, 0);
      this.flash.intensity = Math.max(0, this.flash.intensity - dt * 120); this.flashMesh.material.opacity = Math.max(0, this.flashMesh.material.opacity - dt * 22);
      // الشمس تتبع اللاعب (ظلال قريبة)
      W.sun.position.set(p.x + 120, 160, p.z + 70); W.sun.target.position.set(p.x, 0, p.z);
      // الأعداء
      for (const b of this.bots.slice()) this.updateBot(b, dt);
      // اللاعبون "البعيدون" يتساقطون بمرور الوقت
      this.virtualT -= dt; if (this.virtualT <= 0 && this.alive > 3 && this.bots.length > 2) {
        this.virtualT = rnd(20, 35); const far = this.bots.filter(b => Math.hypot(b.x - p.x, b.z - p.z) > 110);
        if (far.length) { const b = far[(Math.random() * far.length) | 0]; this.alive--; this.feed('اشتباك بعيد: سقط لاعب'); this.killBot(b); }
      }
      // جثث
      for (const c of this.corpses) { c.t += dt; c.model.rotation.x = Math.max(c.model.rotation.x - dt * 3.2, -Math.PI / 2); c.model.position.y = Math.max(0, .1 - c.t); if (c.t > c.ttl) { this.scene.remove(c.model); c.dead = true; } }
      this.corpses = this.corpses.filter(c => !c.dead);
      // المنطقة
      this.updateZone(dt);
      // جزيئات
      for (const t of this.tracers) { t.life -= dt; t.l.material.opacity = Math.max(0, t.life * 12); if (t.life <= 0) { this.scene.remove(t.l); t.l.geometry.dispose(); t.dead = true; } } this.tracers = this.tracers.filter(t => !t.dead);
      for (const q of this.puffs) { q.life -= dt; q.v.y -= 4 * dt; q.m.position.addScaledVector(q.v, dt); q.m.material.opacity = Math.max(0, q.life * 2); if (q.life <= 0) { this.scene.remove(q.m); q.m.geometry.dispose(); q.dead = true; } } this.puffs = this.puffs.filter(q => !q.dead);
      for (const l of this.loot) { l.mesh.rotation.y += dt; }
      // تأثيرات الواجهة
      for (const k of ['hit', 'hurt', 'kill', 'dirT', 'flash']) if (this.fx[k] > 0) this.fx[k] = Math.max(0, this.fx[k] - dt);
      if (this.alive <= 1 && !p.dead && !this.over) this.finish(true);
      this.hudT = (this.hudT || 0) + dt; if (this.hudT > .05) { this.hudT = 0; this.updateHud(); this.drawMini(); this.drawCompass(); }
    }

    updateZone(dt) {
      const z = this.zone, p = this.p;
      if (z.state === 'wait') { z.t -= dt; if (z.t <= 0) { z.state = 'shrink'; z.t = z.shrinkT; this.msg('المنطقة الآمنة تتقلص!', 3); blip(200, .4, .1, 'sawtooth'); } }
      else if (z.state === 'shrink') { z.t -= dt; const k = 1 - Math.max(0, z.t) / z.shrinkT, e = k * k * (3 - 2 * k); z.r = z.fromR + (z.toR - z.fromR) * e; z.cx = z.fcx + (z.tcx - z.fcx) * e; z.cz = z.fcz + (z.tcz - z.fcz) * e; if (z.t <= 0) { if (z.phase < ZONE.length - 1) this.nextPhase(); else { z.state = 'final'; } } }
      this.zoneMesh.scale.set(z.r, 1, z.r); this.zoneMesh.position.x = z.cx; this.zoneMesh.position.z = z.cz;
      this.outside = Math.hypot(p.x - z.cx, p.z - z.cz) > z.r;
      if (this.outside && !p.dead) { this.zoneAcc = (this.zoneAcc || 0) + dt; if (this.zoneAcc >= .5) { this.zoneAcc = 0; this.damagePlayer(z.dmg * .5); } }
    }

    /* ---------- الواجهة ---------- */
    msg(t, sec) { const e = $('toast'); e.textContent = t; e.classList.add('show'); clearTimeout(this._mt); this._mt = setTimeout(() => e.classList.remove('show'), (sec || 2) * 1000); }
    feed(t) { const l = document.createElement('div'); l.className = 'kf'; l.textContent = t; $('killfeed').prepend(l); setTimeout(() => l.remove(), 5000); while ($('killfeed').children.length > 5) $('killfeed').lastChild.remove(); }
    updateAmmo() { $('mag').textContent = this.p.mag; $('reserve').textContent = this.p.reserve; $('ammoBox').classList.toggle('low', this.p.mag <= 6); }
    updateHud(force) {
      const p = this.p, z = this.zone;
      $('hpFill').style.width = p.hp + '%'; $('hpNum').textContent = Math.ceil(p.hp); $('hpFill').classList.toggle('low', p.hp < 35);
      $('alive').textContent = this.alive; $('kills').textContent = p.kills;
      const secs = Math.max(0, Math.ceil(z.t)); $('zoneT').textContent = (z.state === 'wait' ? 'التقلص بعد ' : z.state === 'shrink' ? 'المنطقة تتقلص ' : 'المنطقة النهائية ') + (z.state === 'final' ? '' : Math.floor(secs / 60) + ':' + String(secs % 60).padStart(2, '0'));
      $('zoneBox').classList.toggle('danger', !!this.outside);
      $('zoneWarn').classList.toggle('show', !!this.outside && !p.dead);
      $('vignette').style.opacity = Math.min(1, (this.fx.hurt || 0) * 2.2 + (p.hp < 30 ? .5 + Math.sin(this.time * 6) * .15 : 0));
      $('hitmark').style.opacity = this.fx.hit > 0 ? 1 : 0; $('hitmark').classList.toggle('head', !!this.fx.headshot);
      $('crosshair').classList.toggle('ads', this.p.ads > .5); $('crosshair').style.setProperty('--sp', (Math.min(18, p.speed * 1.6 + p.rec * 2)) + 'px');
      const di = $('dmgDir'); di.style.opacity = this.fx.dirT > 0 ? this.fx.dirT : 0; if (this.fx.dirAngle !== undefined) di.style.transform = `translate(-50%,-50%) rotate(${(this.fx.dirAngle + p.yaw) * 180 / Math.PI * -1 + 180}deg)`;
      if (force) this.updateAmmo();
    }
    drawMini() {
      const c = this.mctx, S = this.mini.width, p = this.p, R = 70, k = S / (R * 2), z = this.zone;
      c.save(); c.clearRect(0, 0, S, S); c.beginPath(); c.arc(S / 2, S / 2, S / 2, 0, TAU); c.clip();
      c.fillStyle = '#b8a67e'; c.fillRect(0, 0, S, S);
      c.translate(S / 2, S / 2); c.rotate(p.yaw); c.scale(k, k); c.translate(-p.x, -p.z);
      // نهر
      c.fillStyle = '#4b8a98'; c.beginPath(); const W = this.W;
      for (let zz = p.z - R * 1.5; zz <= p.z + R * 1.5; zz += 6) c.lineTo(W.riverX(zz) - W.RIVER_W / 2, zz);
      for (let zz = p.z + R * 1.5; zz >= p.z - R * 1.5; zz -= 6) c.lineTo(W.riverX(zz) + W.RIVER_W / 2, zz); c.fill();
      c.fillStyle = '#c9b88f'; for (const b of W.BRIDGES) c.fillRect(W.riverX(b) - W.RIVER_W / 2 - 8, b - 9, W.RIVER_W + 16, 18);
      // مبانٍ
      c.fillStyle = '#6f6552'; for (const o of W.near(p.x, p.z, R * 1.2)) if (o.kind === 'b' || o.kind === 'm') c.fillRect(o.x0, o.z0, o.x1 - o.x0, o.z1 - o.z0);
      c.fillStyle = '#8f9a6a'; for (const o of W.near(p.x, p.z, R * 1.2)) if (o.kind === 'c') c.fillRect(o.x0, o.z0, o.x1 - o.x0, o.z1 - o.z0);
      // المنطقة الآمنة
      c.strokeStyle = '#3aa8ff'; c.lineWidth = 2 / k * 1.6; c.beginPath(); c.arc(z.cx, z.cz, z.r, 0, TAU); c.stroke();
      c.fillStyle = 'rgba(255,40,30,.16)'; c.beginPath(); c.rect(p.x - R * 3, p.z - R * 3, R * 6, R * 6); c.arc(z.cx, z.cz, z.r, 0, TAU, true); c.fill('evenodd');
      // الأعداء القريبون
      for (const b of this.bots) { const d = Math.hypot(b.x - p.x, b.z - p.z); if (d < 55 || b.state === 'fight') { c.fillStyle = '#ff3b30'; c.beginPath(); c.arc(b.x, b.z, 2.2 / k * 1.5, 0, TAU); c.fill(); } }
      for (const l of this.loot) { const d = Math.hypot(l.x - p.x, l.z - p.z); if (d < R) { c.fillStyle = l.kind === 'ammo' ? '#ffd24a' : '#6bff8f'; c.fillRect(l.x - 1, l.z - 1, 2, 2); } }
      c.restore();
      // لاعب
      c.save(); c.translate(S / 2, S / 2); c.fillStyle = '#fff'; c.strokeStyle = '#000'; c.lineWidth = 1.5; c.beginPath(); c.moveTo(0, -9); c.lineTo(6, 7); c.lineTo(0, 3); c.lineTo(-6, 7); c.closePath(); c.fill(); c.stroke(); c.restore();
    }
    drawCompass() {
      const c = this.cctx, w = this.compass.width, h = this.compass.height; c.clearRect(0, 0, w, h);
      const brg = ((-this.p.yaw * 180 / Math.PI) % 360 + 360) % 360, pxPer = 3.2; c.font = 'bold 15px Cairo,sans-serif'; c.textAlign = 'center'; c.fillStyle = '#fff'; c.strokeStyle = 'rgba(255,255,255,.6)';
      for (let d = -60; d <= 60; d += 5) { const a = Math.round(brg / 5) * 5 + d, x = w / 2 + (a - brg) * pxPer, A = ((a % 360) + 360) % 360;
        const names = { 0: 'ش', 90: 'شر', 180: 'ج', 270: 'غ' };
        if (A % 90 === 0) { c.fillStyle = A === 0 ? '#ff5a4a' : '#fff'; c.fillText(names[A], x, 22); } else if (A % 15 === 0) { c.fillStyle = 'rgba(255,255,255,.8)'; c.font = '11px Cairo'; c.fillText(A, x, 20); c.font = 'bold 15px Cairo'; }
        c.beginPath(); c.moveTo(x, 28); c.lineTo(x, A % 15 === 0 ? 24 : 26); c.stroke(); }
      c.fillStyle = '#ffd24a'; c.beginPath(); c.moveTo(w / 2 - 6, 0); c.lineTo(w / 2 + 6, 0); c.lineTo(w / 2, 8); c.fill();
      // اتجاه المنطقة
      const z = this.zone, ang = Math.atan2(z.cx - this.p.x, -(z.cz - this.p.z)) * 180 / Math.PI, rel = ((ang - brg + 540) % 360) - 180;
      if (Math.abs(rel) < 60) { c.fillStyle = '#3aa8ff'; c.fillRect(w / 2 + rel * pxPer - 3, 30, 6, 6); }
    }
    finish(win) {
      this.over = true; const p = this.p; if (document.pointerLockElement) document.exitPointerLock();
      const rank = win ? 1 : this.alive;
      $('endTitle').textContent = win ? 'نصرٌ مؤزَّر!' : 'سقطت في الميدان'; $('endTitle').className = win ? 'win' : 'lose';
      $('endSub').textContent = win ? 'أنت آخر من بقي في بغداد — بطل مكافحة الإرهاب' : `المركز #${rank} من ${BOTS + 1}`;
      $('sRank').textContent = '#' + rank; $('sKills').textContent = p.kills; $('sTime').textContent = Math.floor(this.time / 60) + ':' + String(Math.floor(this.time % 60)).padStart(2, '0');
      const xp = (win ? 300 : 80) + p.kills * 60 + Math.floor(this.time / 6); $('sXp').textContent = '+' + xp; this.reward = { xp, coins: Math.floor(xp / 2), win, kills: p.kills };
      setTimeout(() => $('endScreen').classList.add('show'), win ? 600 : 1500);
    }

    /* ---------- التحكم ---------- */
    bindInput() {
      const self = this;
      addEventListener('keydown', e => { if (!self.active) return; self.keys[e.code] = true; if (e.code === 'KeyR') self.reload(); if (['Space', 'ArrowUp', 'ArrowDown'].includes(e.code)) e.preventDefault(); });
      addEventListener('keyup', e => { self.keys[e.code] = false; });
      const cv = this.r.domElement;
      cv.addEventListener('mousedown', e => { if (!self.active || self.over) return; if (!document.pointerLockElement && !('ontouchstart' in window)) { cv.requestPointerLock && cv.requestPointerLock(); audio(); return; } if (e.button === 0) self.fireHeld = true; if (e.button === 2) self.ads = true; });
      addEventListener('mouseup', e => { if (e.button === 0) self.fireHeld = false; if (e.button === 2) self.ads = false; });
      addEventListener('contextmenu', e => { if (self.active) e.preventDefault(); });
      addEventListener('mousemove', e => { if (self.active && document.pointerLockElement) { self.look.dx += e.movementX; self.look.dy += e.movementY; } });
      // لمس
      const joyBase = $('joyBase'), joyKnob = $('joyKnob'); let joyId = null, lookId = null, lx = 0, ly = 0, jc = null;
      const R = 55;
      addEventListener('touchstart', e => {
        if (!self.active) return;
        for (const t of e.changedTouches) {
          const el = document.elementFromPoint(t.clientX, t.clientY); if (el && el.closest && el.closest('.tbtn')) continue;
          if (t.clientX < innerWidth * .4 && joyId === null) { joyId = t.identifier; jc = { x: t.clientX, y: t.clientY }; joyBase.style.left = jc.x - 60 + 'px'; joyBase.style.top = jc.y - 60 + 'px'; joyBase.style.opacity = 1; }
          else if (t.clientX > innerWidth * .4 && lookId === null) { lookId = t.identifier; lx = t.clientX; ly = t.clientY; }
        }
      }, { passive: true });
      addEventListener('touchmove', e => {
        for (const t of e.changedTouches) {
          if (t.identifier === joyId) { let dx = t.clientX - jc.x, dy = t.clientY - jc.y; const l = Math.hypot(dx, dy); if (l > R) { dx *= R / l; dy *= R / l; } joyKnob.style.transform = `translate(${dx}px,${dy}px)`; self.joy.x = dx / R; self.joy.y = dy / R; self.joy.sprint = l > R * .95; }
          else if (t.identifier === lookId) { self.look.dx += (t.clientX - lx) * 1.3; self.look.dy += (t.clientY - ly) * 1.3; lx = t.clientX; ly = t.clientY; }
        }
      }, { passive: true });
      const end = e => { for (const t of e.changedTouches) { if (t.identifier === joyId) { joyId = null; self.joy.x = self.joy.y = 0; self.joy.sprint = false; joyKnob.style.transform = ''; joyBase.style.opacity = .35; } if (t.identifier === lookId) lookId = null; } };
      addEventListener('touchend', end); addEventListener('touchcancel', end);
      const hold = (id, on, off) => { const el = $(id); el.addEventListener('touchstart', e => { e.preventDefault(); audio(); on(); }, { passive: false }); el.addEventListener('touchend', e => { e.preventDefault(); off && off(); }); el.addEventListener('mousedown', () => { audio(); on(); }); el.addEventListener('mouseup', () => off && off()); };
      hold('bFire', () => self.fireHeld = true, () => self.fireHeld = false);
      hold('bAds', () => self.ads = !self.ads); hold('bJump', () => self.jumpReq = true); hold('bReload', () => self.reload());
    }
  }
  window.Game = Game;
})();
