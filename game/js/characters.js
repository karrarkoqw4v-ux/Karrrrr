/* شخصيات واقعية: موديل جندي مُهيكل (Skinned) مع حركات Idle / Walk / Run + سلاح مثبت باليد */
(function () {
  const BASE = (window.GAME_BASE || '') + 'models/Soldier.glb';
  let src = null, clips = null;
  const ready = new Promise(res => {
    if (!THREE.GLTFLoader) return res(false);
    new THREE.GLTFLoader().load(BASE, g => {
      src = g.scene; clips = {}; for (const a of g.animations) clips[a.name] = a;
      src.traverse(o => {
        if (o.isMesh) {
          o.castShadow = true; o.frustumCulled = false;
          const mp = o.material.map; // نحول النسيج لرمادي حتى نصبغه بألوان الأزياء
          if (mp && mp.image) try {
            const im = mp.image, c = document.createElement('canvas'); c.width = im.width; c.height = im.height; const g = c.getContext('2d'); g.drawImage(im, 0, 0);
            const d = g.getImageData(0, 0, c.width, c.height), a = d.data;
            for (let i = 0; i < a.length; i += 4) { const v = Math.min(255, (a[i] * .3 + a[i + 1] * .59 + a[i + 2] * .11) * 1.5 + 30); a[i] = a[i + 1] = a[i + 2] = v; }
            g.putImageData(d, 0, 0); const t = new THREE.CanvasTexture(c); t.flipY = mp.flipY; t.encoding = mp.encoding; o.material.map = t;
          } catch (e) {}
        }
      });
      res(true);
    }, undefined, e => { console.warn('model load failed', e); res(false); });
  });

  // ألوان الأزياء: الزي الأصلي يُصبغ حسب الفريق
  const LOOKS = {
    cts: { tint: 0x3a3e44, visor: 0x111418, name: 'مكافحة الإرهاب' },
    cts2: { tint: 0x5b5446, visor: 0x1a1a1a, name: 'الفرقة الذهبية' },
    cts3: { tint: 0x45503c, visor: 0x101410, name: 'قوات خاصة' },
    enemy: { tint: 0x6b4a3a, visor: 0x2a0d08, name: 'عدو' },
    enemy2: { tint: 0x55503a, visor: 0x201a0a, name: 'عدو' },
    enemy3: { tint: 0x403a46, visor: 0x150a18, name: 'عدو' },
  };

  function create(look, weaponBuilder) {
    if (!src) return null;
    const L = LOOKS[look] || LOOKS.cts;
    const model = THREE.SkeletonUtils.clone(src); model.rotation.y = Math.PI; const root = new THREE.Group(); root.add(model);
    root.traverse(o => {
      if (o.isMesh) {
        const m = o.material.clone();
        if (/visor/i.test(o.name)) { m.color = new THREE.Color(L.visor); m.metalness = .9; m.roughness = .15; }
        else { m.color = new THREE.Color(L.tint); m.roughness = .78; m.metalness = .15; }
        o.material = m; o.castShadow = true; o.frustumCulled = false;
      }
    });
    const mixer = new THREE.AnimationMixer(model), actions = {};
    for (const k in clips) { actions[k] = mixer.clipAction(clips[k]); actions[k].enabled = true; }
    let cur = null;
    function play(name, fade) {
      const a = actions[name]; if (!a || a === cur) return;
      a.reset(); a.setEffectiveTimeScale(1); a.setEffectiveWeight(1); a.play();
      if (cur) a.crossFadeFrom(cur, fade === undefined ? .25 : fade, true);
      cur = a;
    }
    // السلاح في اليد اليمنى (قابل للتبديل)
    let hand = null; root.traverse(o => { if (o.isBone && /RightHand$/.test(o.name)) hand = o; });
    let gun = null, holder = null;
    if (hand) {
      holder = new THREE.Group(); const ws = new THREE.Vector3(); root.updateMatrixWorld(true); hand.getWorldScale(ws); const s = 1 / (ws.x || 1);
      holder.scale.setScalar(s); holder.position.set(0, .06 * s, .03 * s); hand.add(holder);
    }
    function setGun(builder) {
      if (!holder) return; if (gun) holder.remove(gun); gun = builder ? builder() : null;
      if (gun) { holder.add(gun); gun.traverse(o => { if (o.isMesh) o.castShadow = true; }); }
      root.userData.muzzle = gun && gun.userData.muzzle;
    }
    setGun(weaponBuilder);
    play('Idle', 0);
    return { root, mixer, actions, play, setGun, get gun() { return gun; }, look: L, setSpeed(v) { if (cur) cur.setEffectiveTimeScale(v); } };
  }
  window.Characters = { ready, create, LOOKS, get loaded() { return !!src; } };
})();
