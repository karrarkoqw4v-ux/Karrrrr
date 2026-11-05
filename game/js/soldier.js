/* نماذج ثلاثية الأبعاد: جندي مكافحة الإرهاب + بندقية M4 (مبنية بالكود، بدون ملفات خارجية) */
(function () {
  const V3 = (x, y, z) => new THREE.Vector3(x, y, z);

  function noiseTex(size, base, amp, repeat) {
    const c = document.createElement('canvas'); c.width = c.height = size;
    const g = c.getContext('2d'); const im = g.createImageData(size, size);
    for (let i = 0; i < size * size; i++) {
      const v = base + (Math.random() - .5) * amp;
      im.data[i * 4] = im.data[i * 4 + 1] = im.data[i * 4 + 2] = v; im.data[i * 4 + 3] = 255;
    }
    g.putImageData(im, 0, 0);
    // نسيج قماش (خطوط متقاطعة)
    g.globalAlpha = .18; g.fillStyle = '#000';
    for (let i = 0; i < size; i += 2) { g.fillRect(i, 0, 1, size); g.fillRect(0, i, size, 1); }
    const t = new THREE.CanvasTexture(c); t.wrapS = t.wrapT = THREE.RepeatWrapping; t.repeat.set(repeat, repeat);
    return t;
  }

  function flagTexture() {
    const c = document.createElement('canvas'); c.width = 192; c.height = 128;
    const g = c.getContext('2d');
    g.fillStyle = '#ce1126'; g.fillRect(0, 0, 192, 43);
    g.fillStyle = '#fff'; g.fillRect(0, 43, 192, 42);
    g.fillStyle = '#000'; g.fillRect(0, 85, 192, 43);
    g.fillStyle = '#007a3d'; g.font = 'bold 34px "Reem Kufi",Arial'; g.textAlign = 'center'; g.textBaseline = 'middle';
    g.fillText('الله أكبر', 96, 66);
    g.strokeStyle = 'rgba(0,0,0,.55)'; g.lineWidth = 6; g.strokeRect(0, 0, 192, 128);
    return new THREE.CanvasTexture(c);
  }
  function tapeTexture(text) {
    const c = document.createElement('canvas'); c.width = 256; c.height = 64;
    const g = c.getContext('2d');
    g.fillStyle = '#111'; g.fillRect(0, 0, 256, 64);
    g.strokeStyle = '#8a7a5c'; g.lineWidth = 3; g.strokeRect(3, 3, 250, 58);
    g.fillStyle = '#d8c89c'; g.font = 'bold 30px "Cairo",Arial'; g.textAlign = 'center'; g.textBaseline = 'middle';
    g.fillText(text, 128, 34);
    return new THREE.CanvasTexture(c);
  }

  let MATS = null;
  function mats() {
    if (MATS) return MATS;
        const mk = (c, r, m, ex) => new THREE.MeshStandardMaterial(Object.assign({ color: c, roughness: r, metalness: m }, ex || {}));
    MATS = {
      uniform: mk(0x2b2f32, .95, 0),
      uniform2: mk(0x33373a, .95, 0),
      vest: mk(0x222527, .85, .05),
      pouch: mk(0x2d3033, .9, 0),
      tan: mk(0x8a7656, .7, .05),
      helmet: mk(0x7d6c4e, .45, .15),
      boot: mk(0x3b2f21, .8, 0),
      glove: mk(0x101112, .7, .05),
      skin: mk(0xb98a68, .7, 0),
      mask: mk(0x0c0d0e, .95, 0),
      lens: mk(0x0a0f14, .08, .9, { envMapIntensity: 1 }),
      metal: mk(0x15161a, .42, .75),
      metalLight: mk(0x3a3d42, .4, .8),
      fde: mk(0x9c8760, .5, .3),
      rubber: mk(0x0e0e0f, .95, 0),
      glass: mk(0x7fe3ff, .05, .2, { transparent: true, opacity: .45, emissive: 0x103a4a }),
      reddot: new THREE.MeshBasicMaterial({ color: 0xff2020 }),
      flag: new THREE.MeshStandardMaterial({ map: flagTexture(), roughness: .8 }),
      tape: new THREE.MeshStandardMaterial({ map: tapeTexture('مكافحة الإرهاب'), roughness: .8 }),
    };
    return MATS;
  }

  function mesh(geo, mat, x, y, z) {
    const m = new THREE.Mesh(geo, mat); m.position.set(x || 0, y || 0, z || 0);
    m.castShadow = true; m.receiveShadow = true; return m;
  }
  const box = (w, h, d, mat, x, y, z) => mesh(new THREE.BoxGeometry(w, h, d), mat, x, y, z);
  const cyl = (rt, rb, h, mat, x, y, z, seg) => mesh(new THREE.CylinderGeometry(rt, rb, h, seg || 14), mat, x, y, z);
  function limb(a, b, r1, r2, mat, seg) {
    const dir = new THREE.Vector3().subVectors(b, a), len = dir.length();
    const m = mesh(new THREE.CylinderGeometry(r2, r1, len, seg || 10), mat);
    m.position.copy(a).addScaledVector(dir, .5);
    m.quaternion.setFromUnitVectors(V3(0, 1, 0), dir.normalize());
    return m;
  }

  /* ---------------- بندقية M4A1 (الفوهة باتجاه +Z) ---------------- */
  function buildRifle() {
    const M = mats(), r = new THREE.Group();
    // الجسم السفلي والعلوي
    r.add(box(.042, .062, .21, M.metal, 0, -.03, .0));
    r.add(box(.046, .052, .31, M.metal, 0, .03, .06));
    r.add(box(.01, .03, .06, M.metalLight, .026, .02, .0));            // نافذة طرد
    // غطاء اليد (FDE) مع سكك
    const hg = cyl(.032, .032, .3, M.fde, 0, .028, .37, 18); hg.rotation.x = Math.PI / 2; r.add(hg);
    for (let i = 0; i < 4; i++) {
      const a = i * Math.PI / 2, rail = box(.016, .016, .3, M.metal, Math.sin(a) * .036, .028 + Math.cos(a) * .036, .37);
      r.add(rail);
      for (let k = 0; k < 9; k++) r.add(box(.018, .004, .006, M.metalLight, Math.sin(a) * .036, .028 + Math.cos(a) * .0425, .24 + k * .033));
    }
    // ماسورة + كاتم صوت
    const bar = cyl(.0085, .0085, .16, M.metalLight, 0, .028, .58, 10); bar.rotation.x = Math.PI / 2; r.add(bar);
    const sup = cyl(.024, .024, .21, M.metal, 0, .028, .7, 18); sup.rotation.x = Math.PI / 2; r.add(sup);
    const supCap = cyl(.026, .026, .012, M.metalLight, 0, .028, .81, 18); supCap.rotation.x = Math.PI / 2; r.add(supCap);
    // الأخمص
    const tube = cyl(.017, .017, .2, M.metal, 0, .018, -.17, 12); tube.rotation.x = Math.PI / 2; r.add(tube);
    r.add(box(.04, .085, .17, M.rubber, 0, -.005, -.27));
    r.add(box(.036, .02, .1, M.metalLight, 0, -.058, -.2));
    // مقبض مسدس
    const grip = box(.032, .105, .05, M.rubber, 0, -.1, -.065); grip.rotation.x = .38; r.add(grip);
    r.add(box(.012, .05, .09, M.metal, 0, -.058, .035));              // حامي الزناد
    // مخزن
    const mag = box(.03, .14, .062, M.metal, 0, -.12, .105); mag.rotation.x = -.12; r.add(mag);
    r.add(box(.032, .012, .066, M.metalLight, 0, -.19, .098));
    // قبضة أمامية + ليزر
    r.add(cyl(.014, .017, .09, M.rubber, 0, -.045, .34, 12));
    r.add(box(.026, .026, .075, M.metalLight, .045, .03, .43));
    // منظار هولوغرافي
    r.add(box(.05, .014, .1, M.metal, 0, .062, .09));
    const sightBody = box(.046, .05, .075, M.metal, 0, .095, .09); r.add(sightBody);
    const lens = box(.036, .036, .004, M.glass, 0, .098, .129); r.add(lens);
    const dot = box(.004, .004, .002, M.reddot, 0, .098, .132); r.add(dot);
    // مقبض الإمساك
    r.add(box(.02, .02, .035, M.metalLight, 0, .066, -.07));
    const muzzle = new THREE.Object3D(); muzzle.position.set(0, .028, .83); r.add(muzzle);
    r.userData.muzzle = muzzle;
    return r;
  }

  /* ---------------- جندي مكافحة الإرهاب ---------------- */
  function buildSoldier(opt) {
    opt = opt || {};
    const M = mats(), S = new THREE.Group();
    const U = opt.enemy ? new THREE.MeshStandardMaterial({ color: 0x4a2c26, roughness: .95,  }) : M.uniform;
    const U2 = opt.enemy ? new THREE.MeshStandardMaterial({ color: 0x3d3a2e, roughness: .95 }) : M.uniform2;
    const VEST = opt.enemy ? new THREE.MeshStandardMaterial({ color: 0x2a2620, roughness: .9 }) : M.vest;

    // الساقان (محور الدوران عند الورك)
    function leg(side) {
      const g = new THREE.Group(); g.position.set(side * .1, .96, 0);
      g.add(limb(V3(0, 0, 0), V3(0, -.46, .02), .088, .068, U, 12));
      g.add(limb(V3(0, -.46, .02), V3(0, -.9, 0), .066, .056, U2, 12));
      g.add(mesh(new THREE.SphereGeometry(.07, 12, 10), U, 0, -.46, .02)); g.add(box(.11, .13, .06, M.pouch, 0, -.48, .07));                   // ركبة
      g.add(box(.1, .09, .27, M.boot, 0, -.94, .05));                     // حذاء
      g.add(box(.1, .17, .11, M.boot, 0, -.84, -.01));
      g.add(box(.102, .02, .275, M.rubber, 0, -.985, .05));
      return g;
    }
    const legL = leg(1), legR = leg(-1); S.add(legL, legR);
    legR.add(box(.05, .16, .11, M.pouch, -.1, -.14, .02));               // جراب مسدس
    legL.add(box(.06, .13, .1, M.pouch, .1, -.18, .02));

    // الجذع
    S.add(box(.34, .2, .2, U, 0, 1.06, 0));
    S.add(box(.36, .44, .21, U, 0, 1.3, 0));
    // سترة حاملة الدروع
    const plate = box(.39, .4, .25, VEST, 0, 1.33, 0); S.add(plate);
    const chestRound = cyl(.2, .2, .4, VEST, 0, 1.33, 0, 20); chestRound.scale.set(1, 1, .62); S.add(chestRound);
    S.add(box(.4, .12, .27, VEST, 0, 1.08, 0));                         // حزام
    for (let i = -1; i <= 1; i++) S.add(box(.085, .12, .06, M.pouch, i * .105, 1.26, .16)); // مخازن
    S.add(box(.2, .1, .05, M.pouch, 0, 1.43, .15));                      // لوحة إدارية
    const tape = new THREE.Mesh(new THREE.PlaneGeometry(.17, .045), M.tape); tape.position.set(.0, 1.37, .2); S.add(tape);
    S.add(box(.14, .15, .08, M.pouch, .2, 1.05, .1)); S.add(box(.14, .15, .08, M.pouch, -.2, 1.05, .1));
    S.add(box(.26, .34, .12, VEST, 0, 1.3, -.2));                        // حقيبة ظهر
    const ant = cyl(.004, .004, .35, M.rubber, .12, 1.65, -.22, 5); S.add(ant);   // هوائي
    S.add(box(.06, .06, .05, M.metal, .12, 1.5, -.2));
    // الكتفان
    S.add(box(.1, .06, .2, VEST, .24, 1.5, 0)); S.add(box(.1, .06, .2, VEST, -.24, 1.5, 0));

    // الرأس
    S.add(cyl(.045, .05, .08, M.mask, 0, 1.54, 0, 10));
    const head = new THREE.Group(); head.position.set(0, 1.66, .01); S.add(head);
    const skull = mesh(new THREE.SphereGeometry(.105, 20, 16), M.mask); skull.scale.set(.95, 1.12, 1.05); head.add(skull);
    head.add(box(.17, .03, .05, M.lens, 0, .02, .09));                    // نظارة واقية
    head.add(box(.16, .012, .04, M.skin, 0, .047, .085));
    // الخوذة (Ops-Core)
    const helm = mesh(new THREE.SphereGeometry(.128, 28, 18, 0, Math.PI * 2, 0, Math.PI * .56), M.helmet);
    helm.scale.set(1, .98, 1.12); helm.position.set(0, .03, -.005); head.add(helm);
    head.add(box(.2, .016, .05, M.helmet, 0, .075, .13));                 // حافة أمامية
    head.add(box(.06, .06, .035, M.metal, 0, .11, .13));                  // قاعدة NVG
    const nvg = cyl(.024, .024, .07, M.metal, .03, .17, .12, 12); nvg.rotation.x = Math.PI / 2 - .5; head.add(nvg);
    const nvg2 = nvg.clone(); nvg2.position.x = -.03; head.add(nvg2);
    head.add(box(.012, .02, .15, M.metal, .125, .06, 0)); head.add(box(.012, .02, .15, M.metal, -.125, .06, 0)); // سكك جانبية
    // سماعات الاتصال
    head.add(cyl(.05, .05, .035, M.rubber, .12, -.01, -.005, 16).rotateZ(Math.PI / 2));
    head.add(cyl(.05, .05, .035, M.rubber, -.12, -.01, -.005, 16).rotateZ(Math.PI / 2));
    head.add(limb(V3(.12, -.04, .0), V3(.05, -.075, .1), .004, .004, M.metal, 5));  // ميكروفون

    // البندقية
    const rifle = buildRifle(); rifle.position.set(-.1, 1.3, .27); rifle.rotation.y = .03; S.add(rifle);
    // الذراعان
    const rp = rifle.position, rh = V3(rp.x, rp.y - .095, rp.z - .06), lh = V3(rp.x, rp.y - .035, rp.z + .36);
    const rs = V3(-.26, 1.46, 0), ls = V3(.26, 1.46, 0), re = V3(-.3, 1.2, .1), le = V3(.22, 1.22, .28);
    S.add(limb(rs, re, .062, .052, U, 10), limb(re, rh, .05, .042, U, 10));
    for (const j of [rs, re, ls, le]) S.add(mesh(new THREE.SphereGeometry(.06, 12, 10), U, j.x, j.y, j.z));
    S.add(limb(ls, le, .062, .052, U, 10), limb(le, lh, .05, .042, U, 10));
    S.add(box(.075, .07, .09, M.glove, rh.x, rh.y, rh.z), box(.08, .07, .1, M.glove, lh.x, lh.y, lh.z));
    // رقعة العلم العراقي على الكتف
    const patch = new THREE.Mesh(new THREE.PlaneGeometry(.085, .057), M.flag); patch.position.set(.292, 1.38, 0); patch.rotation.y = Math.PI / 2; S.add(patch);

    S.userData = { legL, legR, head, rifle, muzzle: rifle.userData.muzzle };
    S.traverse(o => { if (o.isMesh) { o.castShadow = true; o.receiveShadow = true; } });
    return S;
  }

  /* ---------------- منظور الشخص الأول ---------------- */
  function buildViewmodel(builder, pistol) {
    const M = mats(), g = new THREE.Group();
    const rifle = (builder || buildRifle)(); rifle.rotation.y = Math.PI; g.add(rifle);   // الفوهة باتجاه -Z
    const sleeve = M.uniform, glove = M.glove;
    // اليد اليمنى على المقبض + الساعد
    g.add(box(.075, .07, .1, glove, 0, -.095, .06));
    g.add(limb(V3(.05, -.2, .35), V3(0, -.09, .07), .055, .045, sleeve, 10));
    // اليد اليسرى على غطاء اليد
    const lz = pistol ? .02 : -.36;
    g.add(box(.08, .07, .11, glove, pistol ? -.03 : 0, -.06, lz));
    g.add(limb(V3(-.14, -.24, .2), V3(pistol ? -.03 : 0, -.05, lz + .01), .058, .046, sleeve, 10));
    g.traverse(o => { if (o.isMesh) { o.castShadow = false; o.receiveShadow = false; o.frustumCulled = false; } });
    g.userData.muzzle = rifle.userData.muzzle;
    return g;
  }

  window.Models = { buildSoldier, buildRifle, buildViewmodel, mats };
})();
