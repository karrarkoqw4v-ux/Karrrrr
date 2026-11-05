/* الأسلحة: خصائص + موديلات (AKM، قناصة، مسدس) — M4 موجود في soldier.js */
(function () {
  const mk = (c, r, m) => new THREE.MeshStandardMaterial({ color: c, roughness: r, metalness: m });
  const MT = { blk: mk(0x161718, .45, .7), blk2: mk(0x2a2c2f, .5, .6), wood: mk(0x6b3c1c, .6, .05), wood2: mk(0x8a5226, .55, .05), steel: mk(0x55585e, .35, .85), glass: mk(0x223344, .1, .9), rub: mk(0x0f0f10, .9, 0) };
  function B(g, w, h, d, m, x, y, z, rx) { const o = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), m); o.position.set(x, y, z); if (rx) o.rotation.x = rx; o.castShadow = true; g.add(o); return o; }
  function C(g, r1, r2, l, m, x, y, z) { const o = new THREE.Mesh(new THREE.CylinderGeometry(r1, r2, l, 14), m); o.rotation.x = Math.PI / 2; o.position.set(x, y, z); o.castShadow = true; g.add(o); return o; }
  function muzzle(g, z, y) { const m = new THREE.Object3D(); m.position.set(0, y || .03, z); g.add(m); g.userData.muzzle = m; }

  function buildAK() {
    const g = new THREE.Group();
    B(g, .045, .07, .32, MT.blk, 0, 0, .04);                       // جسم
    B(g, .042, .03, .3, MT.blk2, 0, .045, .05);                    // غطاء
    B(g, .05, .055, .22, MT.wood, 0, .005, .33);                   // واقي يد خشبي
    B(g, .04, .035, .2, MT.wood2, 0, .052, .33);
    C(g, .009, .009, .3, MT.steel, 0, .03, .56);                   // ماسورة
    C(g, .012, .012, .07, MT.blk, 0, .03, .72);                    // مانع وميض
    C(g, .006, .006, .25, MT.steel, 0, .065, .45);                 // أنبوب الغاز
    B(g, .01, .035, .01, MT.blk, 0, .06, .66);                     // شعيرة
    B(g, .045, .1, .26, MT.wood, 0, -.02, -.24, .12);              // أخمص خشبي
    B(g, .03, .1, .045, MT.wood2, 0, -.09, -.07, .35);             // مقبض
    const mag = B(g, .03, .16, .07, MT.blk, 0, -.12, .12, -.35);    // مخزن مقوس
    B(g, .031, .06, .07, MT.blk2, 0, -.2, .16, -.6);
    muzzle(g, .76); return g;
  }
  function buildSniper() {
    const g = new THREE.Group();
    B(g, .05, .07, .62, MT.wood, 0, -.01, .02);                    // جسم خشبي طويل
    B(g, .055, .13, .3, MT.wood2, 0, -.04, -.38, .1);              // أخمص
    B(g, .04, .05, .22, MT.blk, 0, .04, .02);                      // الترباس
    C(g, .011, .009, .55, MT.steel, 0, .035, .55);                 // ماسورة طويلة
    C(g, .022, .022, .3, MT.blk, 0, .11, .02);                     // منظار
    C(g, .03, .022, .06, MT.blk, 0, .11, .18); C(g, .028, .022, .06, MT.blk, 0, .11, -.14);
    B(g, .02, .04, .02, MT.blk, 0, .075, .1); B(g, .02, .04, .02, MT.blk, 0, .075, -.06);
    const bolt = C(g, .008, .008, .07, MT.steel, .04, .05, -.04); bolt.rotation.z = Math.PI / 2;
    muzzle(g, .83); return g;
  }
  function buildPistol() {
    const g = new THREE.Group();
    B(g, .032, .038, .19, MT.blk2, 0, .02, .02);                   // المزلاج
    B(g, .03, .03, .15, MT.blk, 0, -.012, .03);
    B(g, .03, .11, .05, MT.rub, 0, -.07, -.045, .25);              // المقبض
    B(g, .008, .03, .05, MT.blk, 0, -.035, .02);
    muzzle(g, .12, .02); return g;
  }

  const W = {
    m4: { name: 'M4A1', type: 'بندقية هجومية', ammo: '556', mag: 30, rate: .085, dmg: 28, head: 2.4, spread: .009, ads: .0025, recoil: .0065, auto: true, fov: 52, reload: 2.0, sup: true, build: () => Models.buildRifle(), loud: .5 },
    akm: { name: 'AKM', type: 'بندقية هجومية', ammo: '762', mag: 30, rate: .1, dmg: 36, head: 2.4, spread: .011, ads: .0035, recoil: .0105, auto: true, fov: 54, reload: 2.3, build: buildAK, loud: .75 },
    kar: { name: 'Kar98k', type: 'قناصة', ammo: '762', mag: 5, rate: 1.25, dmg: 95, head: 2.6, spread: .03, ads: .0002, recoil: .035, auto: false, fov: 18, scope: true, reload: 3.2, build: buildSniper, loud: .9 },
    p92: { name: 'P92', type: 'مسدس', ammo: '9mm', mag: 15, rate: .17, dmg: 21, head: 2, spread: .012, ads: .006, recoil: .009, auto: false, fov: 62, reload: 1.5, build: buildPistol, pistol: true, loud: .35 },
  };
  const AMMO = { '556': '5.56 مم', '762': '7.62 مم', '9mm': '9 مم' };
  const MEDS = {
    band: { name: 'ضمادة', heal: 15, cap: 75, time: 2.5, icon: '🩹' },
    fak: { name: 'إسعاف أولي', heal: 75, cap: 75, time: 4.5, icon: '✚' },
    kit: { name: 'حقيبة طبية', heal: 100, cap: 100, time: 6, icon: '⛑' },
    drink: { name: 'مشروب طاقة', boost: 40, time: 2, icon: '⚡' },
  };
  window.Weapons = { W, AMMO, MEDS, buildAK, buildSniper, buildPistol };
})();
