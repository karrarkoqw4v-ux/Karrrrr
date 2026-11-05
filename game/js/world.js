/* عالم بغداد ثلاثي الأبعاد: دجلة، الكرخ والرصافة، الجسور، المباني، النخيل، المعالم */
(function () {
  const HALF = 360, PITCH = 60, RIVER_W = 46;
  const BRIDGES = [-180, 0, 180], BRIDGE_HW = 9;
  const AP = { x0: -348, x1: -128, z0: -348, z1: -96 }; // مطار بغداد الدولي

  function rng(seed) { return function () { seed |= 0; seed = seed + 0x6D2B79F5 | 0; let t = Math.imul(seed ^ seed >>> 15, 1 | seed); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }
  const riverX = z => 34 * Math.sin(z / 125) + 13 * Math.sin(z / 52 + 1.3);
  const onBridge = z => BRIDGES.some(b => Math.abs(z - b) < BRIDGE_HW);
  const isWater = (x, z) => Math.abs(x - riverX(z)) < RIVER_W / 2 && !onBridge(z);

  /* دمج الأشكال مع ألوان رؤوس (لأجل InstancedMesh) */
  function merge(parts) {
    const pos = [], nor = [], col = [];
    for (const { geo, color, matrix } of parts) {
      let g = geo.index ? geo.toNonIndexed() : geo.clone();
      if (matrix) g.applyMatrix4(matrix);
      const p = g.attributes.position.array, n = g.attributes.normal.array, c = new THREE.Color(color);
      for (let i = 0; i < p.length; i += 3) { pos.push(p[i], p[i + 1], p[i + 2]); nor.push(n[i], n[i + 1], n[i + 2]); col.push(c.r, c.g, c.b); }
    }
    const G = new THREE.BufferGeometry();
    G.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    G.setAttribute('normal', new THREE.Float32BufferAttribute(nor, 3));
    G.setAttribute('color', new THREE.Float32BufferAttribute(col, 3));
    return G;
  }
  const T = (x, y, z, rx, ry, rz) => { const m = new THREE.Matrix4(); const q = new THREE.Quaternion().setFromEuler(new THREE.Euler(rx || 0, ry || 0, rz || 0)); return m.compose(new THREE.Vector3(x, y, z), q, new THREE.Vector3(1, 1, 1)); };

  function groundTexture() {
    const N = 2048, c = document.createElement('canvas'); c.width = c.height = N;
    const g = c.getContext('2d'), k = N / (HALF * 2), W = x => (x + HALF) * k;
    g.fillStyle = '#b79f78'; g.fillRect(0, 0, N, N);
    // حبيبات تراب
    for (let i = 0; i < 90000; i++) { const v = 150 + Math.random() * 60; g.fillStyle = `rgba(${v},${v - 20},${v - 55},.25)`; g.fillRect(Math.random() * N, Math.random() * N, 2, 2); }
    // الشوارع
    g.fillStyle = '#3b3b3e';
    for (let i = -7; i <= 6; i++) {
      const p = i * PITCH + 30;
      g.fillRect(W(p - 6), 0, 12 * k, N); g.fillRect(0, W(p - 6), N, 12 * k);
    }
    // أرصفة + خطوط
    g.fillStyle = '#8f8a80';
    for (let i = -7; i <= 6; i++) {
      const p = i * PITCH + 30;
      for (const o of [-7.5, 6.5]) { g.fillRect(W(p + o), 0, 1 * k, N); g.fillRect(0, W(p + o), N, 1 * k); }
    }
    g.fillStyle = '#d9d2b4';
    for (let i = -7; i <= 6; i++) {
      const p = i * PITCH + 30;
      for (let t = 0; t < N; t += 14 * k) { g.fillRect(W(p) - .5, t, 1.2, 6 * k); g.fillRect(t, W(p) - .5, 6 * k, 1.2); }
    }
    // المطار: ساحة خرسانية + مدرج
    g.fillStyle = '#a7a397'; g.fillRect(W(AP.x0), W(AP.z0), (AP.x1 - AP.x0) * k, (AP.z1 - AP.z0) * k);
    g.strokeStyle = 'rgba(0,0,0,.12)'; g.lineWidth = 1; for (let x = AP.x0; x < AP.x1; x += 8) { g.beginPath(); g.moveTo(W(x), W(AP.z0)); g.lineTo(W(x), W(AP.z1)); g.stroke(); } for (let z = AP.z0; z < AP.z1; z += 8) { g.beginPath(); g.moveTo(W(AP.x0), W(z)); g.lineTo(W(AP.x1), W(z)); g.stroke(); }
    g.fillStyle = '#2c2d2f'; g.fillRect(W(-338), W(-346), 32 * k, 248 * k);
    g.fillStyle = '#eee'; for (let z = -330; z < -110; z += 16) g.fillRect(W(-322.5), W(z), 1 * k, 8 * k);
    for (let i = 0; i < 8; i++) { g.fillRect(W(-336 + i * 3.6), W(-344), 2 * k, 12 * k); g.fillRect(W(-336 + i * 3.6), W(-112), 2 * k, 12 * k); }
    g.fillStyle = '#d8b630'; g.fillRect(W(-306), W(-300), 60 * k, 1 * k); g.fillRect(W(-306), W(-140), 60 * k, 1 * k); g.fillRect(W(-262), W(-300), 1 * k, 160 * k);
    for (let i = 0; i < 40000; i++) { g.fillStyle = `rgba(0,0,0,${Math.random() * .05})`; g.fillRect(Math.random() * N, Math.random() * N, 3, 3); }
    const t = new THREE.CanvasTexture(c); t.anisotropy = 8; return t;
  }

  function build(scene) {
    const R = rng(1958), colliders = [], grid = new Map(), CELL = 20;
    const key = (i, j) => i * 1000 + j;
    function addCollider(x0, x1, z0, z1, h, kind) {
      const c = { x0, x1, z0, z1, h, kind: kind || 'b' }; colliders.push(c);
      for (let i = Math.floor(x0 / CELL); i <= Math.floor(x1 / CELL); i++)
        for (let j = Math.floor(z0 / CELL); j <= Math.floor(z1 / CELL); j++) {
          const k = key(i, j); if (!grid.has(k)) grid.set(k, []); grid.get(k).push(c);
        }
      return c;
    }
    const near = (x, z, r) => { // مصادمات قريبة
      const out = new Set();
      for (let i = Math.floor((x - r) / CELL); i <= Math.floor((x + r) / CELL); i++)
        for (let j = Math.floor((z - r) / CELL); j <= Math.floor((z + r) / CELL); j++) { const a = grid.get(key(i, j)); if (a) for (const c of a) out.add(c); }
      return out;
    };

    /* السماء والضباب والإضاءة */
    const fogC = new THREE.Color(0xd9c9a3);
    scene.fog = new THREE.Fog(fogC, 90, 560);
    const sky = new THREE.Mesh(new THREE.SphereGeometry(900, 24, 16), new THREE.ShaderMaterial({
      side: THREE.BackSide, depthWrite: false, fog: false,
      uniforms: { top: { value: new THREE.Color(0x5d8fc4) }, mid: { value: new THREE.Color(0xc9d6e0) }, hor: { value: fogC } },
      vertexShader: 'varying vec3 p;void main(){p=normalize(position);gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.);}',
      fragmentShader: 'varying vec3 p;uniform vec3 top,mid,hor;void main(){float h=clamp(p.y,0.,1.);vec3 c=mix(hor,mid,smoothstep(0.,.18,h));c=mix(c,top,smoothstep(.15,.7,h));gl_FragColor=vec4(c,1.);}'
    }));
    sky.name = 'sky'; scene.add(sky);
    const hemi = new THREE.HemisphereLight(0xdbe8ff, 0xa88f68, .85); scene.add(hemi);
    const sun = new THREE.DirectionalLight(0xfff0d0, 1.25); sun.position.set(120, 160, 70);
    sun.castShadow = true; sun.shadow.mapSize.set(2048, 2048);
    Object.assign(sun.shadow.camera, { left: -90, right: 90, top: 90, bottom: -90, near: 1, far: 500 });
    sun.shadow.bias = -.0006; scene.add(sun, sun.target);

    /* الأرض */
    const ground = new THREE.Mesh(new THREE.PlaneGeometry(HALF * 2, HALF * 2), new THREE.MeshStandardMaterial({ map: groundTexture(), roughness: 1 }));
    ground.rotation.x = -Math.PI / 2; ground.receiveShadow = true; scene.add(ground);
    const outer = new THREE.Mesh(new THREE.PlaneGeometry(4000, 4000), new THREE.MeshStandardMaterial({ color: 0xa88f68, roughness: 1 }));
    outer.rotation.x = -Math.PI / 2; outer.position.y = -.05; scene.add(outer);

    /* نهر دجلة */
    const wpos = [], widx = [], wuv = [];
    for (let z = -HALF - 20, n = 0; z <= HALF + 20; z += 6, n++) {
      const x = riverX(z);
      wpos.push(x - RIVER_W / 2, .04, z, x + RIVER_W / 2, .04, z); wuv.push(0, z / 20, 1, z / 20);
      if (n > 0) { const a = (n - 1) * 2; widx.push(a, a + 2, a + 1, a + 1, a + 2, a + 3); }
    }
    const wg = new THREE.BufferGeometry();
    wg.setAttribute('position', new THREE.Float32BufferAttribute(wpos, 3)); wg.setAttribute('uv', new THREE.Float32BufferAttribute(wuv, 2));
    wg.setIndex(widx); wg.computeVertexNormals();
    const rc = document.createElement('canvas'); rc.width = rc.height = 128; const rg = rc.getContext('2d');
    rg.fillStyle = '#888'; rg.fillRect(0, 0, 128, 128);
    for (let i = 0; i < 400; i++) { rg.fillStyle = `rgba(${Math.random() > .5 ? 255 : 0},${Math.random() > .5 ? 255 : 0},${Math.random() > .5 ? 255 : 0},.12)`; rg.beginPath(); rg.ellipse(Math.random() * 128, Math.random() * 128, 2 + Math.random() * 8, 1 + Math.random() * 2, 0, 0, 7); rg.fill(); }
    const ripple = new THREE.CanvasTexture(rc); ripple.wrapS = ripple.wrapT = THREE.RepeatWrapping; ripple.repeat.set(2, 1);
    const water = new THREE.Mesh(wg, new THREE.MeshStandardMaterial({ color: 0x3f7f8a, roughness: .12, metalness: .35, bumpMap: ripple, bumpScale: 2.2 }));
    water.receiveShadow = true; scene.add(water);
    // حواف الضفاف
    const banks = new THREE.MeshStandardMaterial({ color: 0x8b7c5c, roughness: 1 });
    for (const sgn of [-1, 1]) {
      const bp = [], bi = [];
      for (let z = -HALF - 20, n = 0; z <= HALF + 20; z += 6, n++) {
        const x = riverX(z) + sgn * (RIVER_W / 2);
        bp.push(x, .06, z, x + sgn * 5, .06, z);
        if (n > 0) { const a = (n - 1) * 2; bi.push(a, a + 1, a + 2, a + 1, a + 3, a + 2); }
      }
      const bg = new THREE.BufferGeometry(); bg.setAttribute('position', new THREE.Float32BufferAttribute(bp, 3)); bg.setIndex(bi); bg.computeVertexNormals();
      const m = new THREE.Mesh(bg, banks); m.material.side = THREE.DoubleSide; scene.add(m);
    }

    /* الجسور */
    const bridgeMat = new THREE.MeshStandardMaterial({ color: 0x6b6e70, roughness: .7, metalness: .2 });
    const archMat = new THREE.MeshStandardMaterial({ color: 0x9aa2a8, roughness: .45, metalness: .6 });
    for (const bz of BRIDGES) {
      const cx = riverX(bz), span = RIVER_W + 24;
      const deck = new THREE.Mesh(new THREE.BoxGeometry(span, .7, BRIDGE_HW * 2), bridgeMat); deck.position.set(cx, .05, bz); deck.receiveShadow = true; deck.castShadow = true; scene.add(deck);
      for (const s of [-1, 1]) {
        const rail = new THREE.Mesh(new THREE.BoxGeometry(span, 1.1, .4), bridgeMat); rail.position.set(cx, 1, bz + s * (BRIDGE_HW - .2)); rail.castShadow = true; scene.add(rail);
        const arch = new THREE.Mesh(new THREE.TorusGeometry(RIVER_W / 2 + 4, .55, 8, 28, Math.PI), archMat); arch.position.set(cx, .5, bz + s * (BRIDGE_HW - .2)); arch.castShadow = true; scene.add(arch);
        for (let k = -3; k <= 3; k++) {
          const a = k / 3 * (Math.PI / 2 - .25), hx = Math.sin(a) * (RIVER_W / 2 + 4), hy = Math.cos(a) * (RIVER_W / 2 + 4);
          const h = new THREE.Mesh(new THREE.CylinderGeometry(.09, .09, hy, 5), archMat); h.position.set(cx + hx, .5 + hy / 2, bz + s * (BRIDGE_HW - .2)); scene.add(h);
        }
      }
    }

    /* المباني (InstancedMesh + نوافذ بالـ shader) */
    const bl = [];
    const palette = [0xd9c7a1, 0xcbb892, 0xe3d8c0, 0xbfae8c, 0xd2c6b0, 0xc9a98a, 0xb9b2a4, 0xe8e0d0, 0xc4936c];
    const specials = { monument: [180, -120], shrine: [-180, 180], tower: [-240, 120] };
    const hw = [], lootSpots = [], winD = [], L = [];
    const inAP = (cx, cz) => cx - 30 < AP.x1 && cx + 30 > AP.x0 && cz - 30 < AP.z1 && cz + 30 > AP.z0;
    const parks = [];
    const bandHit = (cx, cz) => { for (let z = cz - 28; z <= cz + 28; z += 7) { const xr = riverX(z); if (cx + 28 > xr - RIVER_W / 2 - 16 && cx - 28 < xr + RIVER_W / 2 + 16) return true; } return false; };
    /* بيت عراقي يمكن دخوله: جدران + باب + غرفتين + سقف + شبابيك */
    const plaster = [0xe2d3b0, 0xd6c49c, 0xeadfca, 0xcdb894, 0xd9cdb6];
    function wall(x0, x1, z0, z1, h, c, col) { hw.push({ x: (x0 + x1) / 2, y: 0, z: (z0 + z1) / 2, w: x1 - x0, h, d: z1 - z0, c }); if (col !== false) addCollider(x0, x1, z0, z1, h, 'h'); }
    const houseRects = [];
    function house(x0, x1, z0, z1) {
      houseRects.push([x0 - 1, x1 + 1, z0 - 1, z1 + 1]);
      const H = 3.6, t = .3, c = plaster[(R() * plaster.length) | 0], door = (R() * 4) | 0, gap = 1.7;
      const seg = (a0, a1, fixed, horiz, hasDoor) => {
        if (!hasDoor) return horiz ? wall(a0, a1, fixed - t / 2, fixed + t / 2, H, c) : wall(fixed - t / 2, fixed + t / 2, a0, a1, H, c);
        const m = a0 + (a1 - a0) * (.3 + R() * .4);
        if (horiz) { wall(a0, m - gap / 2, fixed - t / 2, fixed + t / 2, H, c); wall(m + gap / 2, a1, fixed - t / 2, fixed + t / 2, H, c); wall(m - gap / 2, m + gap / 2, fixed - t / 2, fixed + t / 2, H - 2.4, c, false); hw[hw.length - 1].y = 2.4; }
        else { wall(fixed - t / 2, fixed + t / 2, a0, m - gap / 2, H, c); wall(fixed - t / 2, fixed + t / 2, m + gap / 2, a1, H, c); wall(fixed - t / 2, fixed + t / 2, m - gap / 2, m + gap / 2, H - 2.4, c, false); hw[hw.length - 1].y = 2.4; }
      };
      seg(x0, x1, z0 + t / 2, true, door === 0); seg(x0, x1, z1 - t / 2, true, door === 1);
      seg(z0, z1, x0 + t / 2, false, door === 2); seg(z0, z1, x1 - t / 2, false, door === 3);
      // جدار داخلي بفتحة
      if (x1 - x0 > z1 - z0) seg(z0 + t, z1 - t, (x0 + x1) / 2, false, true); else seg(x0 + t, x1 - t, (z0 + z1) / 2, true, true);
      // سقف وأرضية
      hw.push({ x: (x0 + x1) / 2, y: H, z: (z0 + z1) / 2, w: x1 - x0 + .4, h: .3, d: z1 - z0 + .4, c: 0xc8bba0 });
      hw.push({ x: (x0 + x1) / 2, y: .01, z: (z0 + z1) / 2, w: x1 - x0 - .2, h: .03, d: z1 - z0 - .2, c: 0x9c8b70 });
      for (const [px, pz, pw, pd] of [[(x0 + x1) / 2, z0, x1 - x0, .25], [(x0 + x1) / 2, z1, x1 - x0, .25], [x0, (z0 + z1) / 2, .25, z1 - z0], [x1, (z0 + z1) / 2, .25, z1 - z0]]) hw.push({ x: px, y: H + .3, z: pz, w: pw, h: .7, d: pd, c });
      // شبابيك (إطار داكن) على الجدران الخارجية
      for (let k = 0; k < 2; k++) { const fx = x0 + (x1 - x0) * (.25 + k * .5), fz = z0 + (z1 - z0) * (.25 + k * .5);
        if (door !== 0) winD.push([fx, 1.5, z0 - .02, 1.3, 1.1, .1]); if (door !== 1) winD.push([fx, 1.5, z1 + .02, 1.3, 1.1, .1]);
        if (door !== 2) winD.push([x0 - .02, 1.5, fz, .1, 1.1, 1.3]); if (door !== 3) winD.push([x1 + .02, 1.5, fz, .1, 1.1, 1.3]); }
      if (R() < .6) hw.push({ x: x0 + 1.5, y: H + .3, z: z0 + 1.5, w: 1.4, h: 1.3, d: 1.4, c: 0x2b3a4a });
      // لوت داخل البيت
      const nL = 1 + ((R() * 3) | 0); for (let k = 0; k < nL; k++) lootSpots.push({ x: x0 + 1.2 + R() * (x1 - x0 - 2.4), z: z0 + 1.2 + R() * (z1 - z0 - 2.4), inside: true });
    }
    for (let i = -6; i <= 5; i++) for (let j = -6; j <= 5; j++) {
      const cx = i * PITCH, cz = j * PITCH;
      if (Object.values(specials).some(s => s[0] === cx && s[1] === cz)) continue;
      if (bandHit(cx, cz) || inAP(cx, cz)) continue;
      const r = R();
      if (r < .09) { parks.push([cx, cz]); continue; }
      if (r < .16) continue; // ساحة فارغة
      const n = R() < .25 ? 1 : R() < .66 ? 2 : 3, lot = 48 / n, gap = n === 1 ? 0 : 3.5;
      for (let a = 0; a < n; a++) for (let b = 0; b < n; b++) {
        const w = lot - gap - R() * 2, d = lot - gap - R() * 2, x = cx - 24 + lot * (a + .5), z = cz - 24 + lot * (b + .5);
        if (n >= 2 && R() < .45) { house(x - w / 2, x + w / 2, z - d / 2, z + d / 2); continue; }
        const q = R(), h = q < .5 ? 6 + R() * 6 : q < .85 ? 12 + R() * 8 : 20 + R() * 14;
        bl.push({ x, z, w, d, h, c: palette[(R() * palette.length) | 0] });
        addCollider(x - w / 2, x + w / 2, z - d / 2, z + d / 2, h, 'b');
      }
    }
    const boxG = new THREE.BoxGeometry(1, 1, 1); boxG.translate(0, .5, 0);
    const bMat = new THREE.MeshStandardMaterial({ roughness: .92, metalness: 0 });
    bMat.onBeforeCompile = sh => {
      sh.vertexShader = 'varying vec3 vWP;varying vec3 vWN;\n' + sh.vertexShader.replace('#include <begin_vertex>', `#include <begin_vertex>
        mat4 im = modelMatrix;
        #ifdef USE_INSTANCING
          im = modelMatrix * instanceMatrix;
        #endif
        vWP = (im * vec4(transformed,1.)).xyz; vWN = normalize(mat3(im) * normal);`);
      sh.fragmentShader = 'varying vec3 vWP;varying vec3 vWN;\n' + sh.fragmentShader.replace('#include <color_fragment>', `#include <color_fragment>
        if (abs(vWN.y) < .5 && vWP.y > 2.4) {
          float u = (abs(vWN.x) > .5) ? vWP.z : vWP.x;
          float fy = mod(vWP.y, 3.4), fu = mod(u, 3.2);
          if (fy > 1.1 && fy < 2.5 && fu > .9 && fu < 2.3) diffuseColor.rgb = mix(diffuseColor.rgb * .25, vec3(.10,.14,.18), .75);
          else if (fy > .95 && fy < 1.1 && fu > .8 && fu < 2.4) diffuseColor.rgb *= .6;
        }
        if (vWN.y > .5) diffuseColor.rgb *= .78;`);
    };
    const bi = new THREE.InstancedMesh(boxG, bMat, bl.length); bi.castShadow = bi.receiveShadow = true;
    const dm = new THREE.Object3D();
    bl.forEach((b, k) => { dm.position.set(b.x, 0, b.z); dm.scale.set(b.w, b.h, b.d); dm.updateMatrix(); bi.setMatrixAt(k, dm.matrix); bi.setColorAt(k, new THREE.Color(b.c)); });
    scene.add(bi);
    // تفاصيل الأسطح (خزانات ماء + مكيفات + حواجز)
    const roof = [];
    bl.forEach(b => {
      if (R() < .65) roof.push({ x: b.x + (R() - .5) * b.w * .4, y: b.h, z: b.z + (R() - .5) * b.d * .4, s: [1.5 + R(), 1.4 + R() * .8, 1.5 + R()], c: R() < .5 ? 0x2b3a4a : 0xa9a9a9 });
      roof.push({ x: b.x, y: b.h, z: b.z - b.d / 2 + .15, s: [b.w, 1, .3], c: 0xbbb0a0 }); roof.push({ x: b.x, y: b.h, z: b.z + b.d / 2 - .15, s: [b.w, 1, .3], c: 0xbbb0a0 });
      roof.push({ x: b.x - b.w / 2 + .15, y: b.h, z: b.z, s: [.3, 1, b.d], c: 0xbbb0a0 }); roof.push({ x: b.x + b.w / 2 - .15, y: b.h, z: b.z, s: [.3, 1, b.d], c: 0xbbb0a0 });
    });
    const ri = new THREE.InstancedMesh(boxG, new THREE.MeshStandardMaterial({ roughness: .8 }), roof.length); ri.castShadow = true;
    roof.forEach((r, k) => { dm.position.set(r.x, r.y, r.z); dm.scale.set(...r.s); dm.updateMatrix(); ri.setMatrixAt(k, dm.matrix); ri.setColorAt(k, new THREE.Color(r.c)); });
    scene.add(ri);
    const hi = new THREE.InstancedMesh(boxG, new THREE.MeshStandardMaterial({ roughness: .95 }), hw.length); hi.castShadow = hi.receiveShadow = true;
    hw.forEach((b, k) => { dm.position.set(b.x, b.y, b.z); dm.scale.set(b.w, b.h, b.d); dm.updateMatrix(); hi.setMatrixAt(k, dm.matrix); hi.setColorAt(k, new THREE.Color(b.c)); });
    scene.add(hi);
    const wi = new THREE.InstancedMesh(boxG, new THREE.MeshStandardMaterial({ color: 0x1b2329, roughness: .3, metalness: .5 }), Math.max(1, winD.length));
    winD.forEach((b, k) => { dm.position.set(b[0], b[1], b[2]); dm.scale.set(b[3], b[4], b[5]); dm.updateMatrix(); wi.setMatrixAt(k, dm.matrix); }); scene.add(wi);

    /* ---- مطار بغداد الدولي ---- */
    { const M = (c, r, m, ex) => new THREE.MeshStandardMaterial(Object.assign({ color: c, roughness: r, metalness: m || 0 }, ex || {}));
      const add = (geo, mat, x, y, z, ry) => { const o = new THREE.Mesh(geo, mat); o.position.set(x, y, z); if (ry) o.rotation.y = ry; o.castShadow = o.receiveShadow = true; scene.add(o); return o; };
      const conc = M(0xe4dccb, .8), glassM = M(0x5d8aa8, .08, .7, { transparent: true, opacity: .85 }), metal = M(0xb8bcc0, .4, .7);
      // صالة الركاب
      const tx = -190, tz = -215;
      add(new THREE.BoxGeometry(24, 13, 50), conc, tx, 6.5, tz); addCollider(tx - 12, tx + 12, tz - 25, tz + 25, 13, 'm');
      add(new THREE.BoxGeometry(.4, 10, 46), glassM, tx - 12.2, 6, tz);
      for (let k = -5; k <= 5; k++) add(new THREE.BoxGeometry(.6, 11, .6), conc, tx - 12.5, 5.5, tz + k * 4.4);
      add(new THREE.BoxGeometry(32, .8, 58), M(0xf0ebe0, .6), tx - 3, 13.4, tz);                           // سقف ممتد
      for (let k = -3; k <= 3; k++) { const a = add(new THREE.TorusGeometry(3.2, .35, 8, 16, Math.PI), conc, tx - 18.5, 9.5, tz + k * 7.5, Math.PI / 2); }
      for (let k = -3; k <= 3; k++) add(new THREE.CylinderGeometry(.35, .35, 9.5, 10), conc, tx - 18.5, 4.75, tz + k * 7.5 + 3.2);
      const sc = document.createElement('canvas'); sc.width = 1024; sc.height = 160; const sg = sc.getContext('2d');
      sg.fillStyle = '#0e3a26'; sg.fillRect(0, 0, 1024, 160); sg.fillStyle = '#fff'; sg.textAlign = 'center';
      sg.font = 'bold 70px "Reem Kufi","Cairo",Arial'; sg.fillText('مطار بغداد الدولي', 512, 72); sg.font = 'bold 40px Arial'; sg.fillText('BAGHDAD INTERNATIONAL AIRPORT', 512, 135);
      const sign = new THREE.Mesh(new THREE.PlaneGeometry(26, 4), new THREE.MeshBasicMaterial({ map: new THREE.CanvasTexture(sc) })); sign.position.set(tx - 19.3, 15.2, tz); sign.rotation.y = -Math.PI / 2; scene.add(sign);
      // برج المراقبة
      add(new THREE.CylinderGeometry(2.2, 3, 34, 16), conc, -150, 17, -285); addCollider(-153, -147, -288, -282, 34, 'm');
      add(new THREE.CylinderGeometry(5.5, 4, 5, 10), glassM, -150, 36.5, -285); add(new THREE.CylinderGeometry(6.2, 6.2, .7, 10), conc, -150, 39.3, -285);
      // حظائر الطائرات (يمكن دخولها)
      for (const [hx, hz] of [[-200, -128], [-200, -318]]) {
        const hm = M(0x8d969c, .55, .6);
        add(new THREE.BoxGeometry(.6, 11, 26), hm, hx + 15, 5.5, hz); addCollider(hx + 14.7, hx + 15.3, hz - 13, hz + 13, 11, 'm');
        add(new THREE.BoxGeometry(30, 11, .6), hm, hx, 5.5, hz - 13); addCollider(hx - 15, hx + 15, hz - 13.3, hz - 12.7, 11, 'm');
        add(new THREE.BoxGeometry(30, 11, .6), hm, hx, 5.5, hz + 13); addCollider(hx - 15, hx + 15, hz + 12.7, hz + 13.3, 11, 'm');
        const roofG = new THREE.CylinderGeometry(13.3, 13.3, 30, 24, 1, true, 0, Math.PI); const rf = add(roofG, M(0x9aa3a8, .5, .6, { side: THREE.DoubleSide }), hx, 11, hz); rf.rotation.set(0, 0, Math.PI / 2); rf.scale.set(1, 1, .35);
        for (let k = 0; k < 6; k++) lootSpots.push({ x: hx - 10 + R() * 22, z: hz - 10 + R() * 20, inside: true });
        for (let k = 0; k < 3; k++) { const cr = add(new THREE.BoxGeometry(1.6, 1.4, 1.6), M(0x6b5a3a, .9), hx + 8 - k * 2.2, .7, hz + 9); addCollider(hx + 7.2 - k * 2.2, hx + 8.8 - k * 2.2, hz + 8.2, hz + 9.8, 1.4, 'c'); }
      }
      // طائرات (الخطوط الجوية العراقية)
      const white = M(0xf4f4f2, .35, .3), green = M(0x0b6b3a, .4, .2), gray = M(0x9aa0a6, .4, .7);
      for (const [ax, az] of [[-262, -190], [-262, -262]]) {
        const f = add(new THREE.CylinderGeometry(2.1, 2.1, 34, 20), white, ax, 3.6, az); f.rotation.x = Math.PI / 2;
        const nose = add(new THREE.SphereGeometry(2.1, 20, 12), white, ax, 3.6, az + 17); nose.scale.set(1, 1, 1.6);
        const tail = add(new THREE.ConeGeometry(2.1, 7, 20), white, ax, 4.2, az - 20.5); tail.rotation.x = -Math.PI / 2;
        add(new THREE.BoxGeometry(4.3, .5, 32), green, ax, 2.6, az);                                     // شريط أخضر
        add(new THREE.BoxGeometry(36, .4, 5.5), white, ax, 2.8, az + 1); add(new THREE.BoxGeometry(12, .3, 3), white, ax, 4.6, az - 20);
        add(new THREE.BoxGeometry(.4, 7, 5), green, ax, 8, az - 20.5);
        for (const s of [-1, 1]) { const e = add(new THREE.CylinderGeometry(1.1, 1, 4, 16), gray, ax + s * 8, 1.6, az + 3); e.rotation.x = Math.PI / 2; }
        for (const [gx, gz] of [[0, 13], [-3, -1], [3, -1]]) add(new THREE.CylinderGeometry(.5, .5, .5, 10), M(0x111111, .9), ax + gx, .5, az + gz).rotation.z = Math.PI / 2;
        addCollider(ax - 2.1, ax + 2.1, az - 22, az + 19, 5.7, 'm');
      }
      for (let k = 0; k < 10; k++) lootSpots.push({ x: -240 + R() * 40, z: -300 + R() * 180, inside: false });
      L.push({ n: 'مطار بغداد الدولي', x: tx, z: tz });
    }

    /* النخيل */
    const palmParts = [];
    palmParts.push({ geo: new THREE.CylinderGeometry(.22, .34, 8, 8).translate(0, 4, 0), color: 0x6b5336 });
    for (let k = 0; k < 9; k++) {
      const a = k / 9 * Math.PI * 2, leaf = new THREE.ConeGeometry(.5, 5, 4); leaf.translate(0, 2.5, 0); leaf.scale(1, 1, .18);
      palmParts.push({ geo: leaf, color: k % 2 ? 0x3f6b2c : 0x4f7d33, matrix: new THREE.Matrix4().compose(new THREE.Vector3(Math.sin(a) * .25, 8, Math.cos(a) * .25), new THREE.Quaternion().setFromEuler(new THREE.Euler(Math.cos(a) * 1.15, 0, -Math.sin(a) * 1.15)).premultiply(new THREE.Quaternion().setFromEuler(new THREE.Euler(0, a, 0))), new THREE.Vector3(1, 1, 1)) });
    }
    // إصلاح اتجاه الأوراق: نبنيها بدوران حول المحور الأفقي ثم حول Y
    palmParts.length = 1;
    for (let k = 0; k < 10; k++) {
      const a = k / 10 * Math.PI * 2, leaf = new THREE.ConeGeometry(.55, 5.4, 4); leaf.translate(0, 2.7, 0); leaf.scale(1, 1, .2);
      const q = new THREE.Quaternion().setFromEuler(new THREE.Euler(0, a, 0)).multiply(new THREE.Quaternion().setFromEuler(new THREE.Euler(1.15, 0, 0)));
      palmParts.push({ geo: leaf, color: k % 2 ? 0x3f6b2c : 0x55873a, matrix: new THREE.Matrix4().compose(new THREE.Vector3(0, 8, 0), q, new THREE.Vector3(1, 1, 1)) });
    }
    const palmGeo = merge(palmParts);
    const palmPos = [];
    const tryPalm = (x, z) => { if (houseRects.some(h => x > h[0] && x < h[1] && z > h[2] && z < h[3])) return; if (Math.abs(x) > HALF - 5 || Math.abs(z) > HALF - 5 || isWater(x, z) || Math.abs(x - riverX(z)) < RIVER_W / 2 + 1) return; for (const c of near(x, z, 3)) if (x > c.x0 - 1.2 && x < c.x1 + 1.2 && z > c.z0 - 1.2 && z < c.z1 + 1.2) return; palmPos.push([x, z, R() * 6.28, .85 + R() * .5]); };
    for (let z = -HALF; z < HALF; z += 8) for (const s of [-1, 1]) tryPalm(riverX(z) + s * (RIVER_W / 2 + 6 + R() * 2), z + R() * 3);
    for (let k = 0; k < 120; k++) tryPalm((R() - .5) * HALF * 2, (R() - .5) * HALF * 2);
    for (const [px, pz] of parks) for (let k = 0; k < 26; k++) tryPalm(px + (R() - .5) * 46, pz + (R() - .5) * 46);
    const palmI = new THREE.InstancedMesh(palmGeo, new THREE.MeshStandardMaterial({ vertexColors: true, roughness: .9 }), palmPos.length); palmI.castShadow = true;
    palmPos.forEach((p, k) => { dm.position.set(p[0], 0, p[1]); dm.rotation.set(0, p[2], 0); dm.scale.setScalar(p[3]); dm.updateMatrix(); palmI.setMatrixAt(k, dm.matrix); addCollider(p[0] - .35, p[0] + .35, p[1] - .35, p[1] + .35, 8, 't'); });
    dm.rotation.set(0, 0, 0); scene.add(palmI);
    // حدائق (عشب)
    const grassM = new THREE.MeshStandardMaterial({ color: 0x5b7a3a, roughness: 1 });
    for (const [px, pz] of parks) { const g = new THREE.Mesh(new THREE.PlaneGeometry(48, 48), grassM); g.rotation.x = -Math.PI / 2; g.position.set(px, .03, pz); g.receiveShadow = true; scene.add(g); }

    /* سيارات + حواجز كونكريتية */
    const carG = merge([
      { geo: new THREE.BoxGeometry(4.3, .8, 1.85).translate(0, .75, 0), color: 0xffffff },
      { geo: new THREE.BoxGeometry(2.3, .72, 1.65).translate(-.2, 1.5, 0), color: 0x9fb4c4 },
      ...[[-1.4, -.85], [-1.4, .85], [1.4, -.85], [1.4, .85]].map(w => ({ geo: new THREE.CylinderGeometry(.36, .36, .3, 10).rotateX(Math.PI / 2).translate(w[0], .36, w[1]), color: 0x111111 }))
    ]);
    const cars = [], barriers = [];
    const carCols = [0xf2f2f2, 0xc9c9c9, 0x1c1c1e, 0xb02a2a, 0xe8c547, 0x3b5f8a, 0xe9e3d3, 0x6e6e72];
    for (let k = 0; k < 140; k++) {
      const horiz = R() < .5, idx = ((R() * 12) | 0) - 6, road = idx * PITCH + 30, t = (R() - .5) * (HALF * 2 - 20), side = R() < .5 ? -1 : 1;
      const x = horiz ? t : road + side * 3.6, z = horiz ? road + side * 3.6 : t;
      if (isWater(x, z) || Math.abs(x - riverX(z)) < RIVER_W / 2 + 3) continue;
      if (BRIDGES.some(b => Math.abs(z - b) < BRIDGE_HW + 4) && Math.abs(x - riverX(z)) < RIVER_W) continue;
      const w = horiz ? 4.3 : 1.9, d = horiz ? 1.9 : 4.3;
      let bad = false; for (const c of near(x, z, 5)) if (x + w / 2 > c.x0 && x - w / 2 < c.x1 && z + d / 2 > c.z0 && z - d / 2 < c.z1) bad = true; if (bad) continue;
      cars.push({ x, z, rot: horiz ? 0 : Math.PI / 2, c: carCols[(R() * carCols.length) | 0] }); addCollider(x - w / 2, x + w / 2, z - d / 2, z + d / 2, 1.9, 'c');
    }
    const carI = new THREE.InstancedMesh(carG, new THREE.MeshStandardMaterial({ vertexColors: true, roughness: .35, metalness: .5 }), cars.length); carI.castShadow = carI.receiveShadow = true;
    cars.forEach((c, k) => { dm.position.set(c.x, 0, c.z); dm.rotation.set(0, c.rot, 0); dm.scale.set(1, 1, 1); dm.updateMatrix(); carI.setMatrixAt(k, dm.matrix); carI.setColorAt(k, new THREE.Color(c.c)); });
    dm.rotation.set(0, 0, 0); scene.add(carI);
    for (let k = 0; k < 160; k++) {
      const horiz = R() < .5, idx = ((R() * 12) | 0) - 6, road = idx * PITCH + 30, t = (R() - .5) * (HALF * 2 - 20);
      const x = horiz ? t : road + (R() - .5) * 8, z = horiz ? road + (R() - .5) * 8 : t, w = horiz ? 1.2 : 3.2, d = horiz ? 3.2 : 1.2;
      if (isWater(x, z) || Math.abs(x - riverX(z)) < RIVER_W / 2 + 3) continue;
      let bad = false; for (const c of near(x, z, 5)) if (x + w / 2 > c.x0 && x - w / 2 < c.x1 && z + d / 2 > c.z0 && z - d / 2 < c.z1) bad = true; if (bad) continue;
      barriers.push({ x, z, w, d }); addCollider(x - w / 2, x + w / 2, z - d / 2, z + d / 2, 3, 'w');
    }
    const barI = new THREE.InstancedMesh(boxG, new THREE.MeshStandardMaterial({ color: 0x9a968c, roughness: 1 }), barriers.length); barI.castShadow = barI.receiveShadow = true;
    barriers.forEach((b, k) => { dm.position.set(b.x, 0, b.z); dm.scale.set(b.w, 3, b.d); dm.updateMatrix(); barI.setMatrixAt(k, dm.matrix); });
    scene.add(barI);

    /* ---- المعالم ---- */
    // نصب الشهيد (قبة فيروزية منشطرة)
    { const [mx, mz] = specials.monument; const tile = new THREE.MeshStandardMaterial({ color: 0x1fa6a4, roughness: .25, metalness: .35, side: THREE.DoubleSide });
      const plat = new THREE.Mesh(new THREE.CylinderGeometry(24, 26, 2, 40), new THREE.MeshStandardMaterial({ color: 0x8a8578, roughness: .9 })); plat.position.set(mx, 1, mz); plat.receiveShadow = plat.castShadow = true; scene.add(plat);
      for (const s of [-1, 1]) { const d = new THREE.Mesh(new THREE.SphereGeometry(13, 36, 18, 0, Math.PI, 0, Math.PI / 2), tile); d.position.set(mx + s * 1.8, 2, mz); d.rotation.set(s * .1, s > 0 ? Math.PI / 2 : -Math.PI / 2, s * -.08); d.castShadow = true; scene.add(d); }
      const flame = new THREE.Mesh(new THREE.CylinderGeometry(.6, 1.2, 20, 10), new THREE.MeshStandardMaterial({ color: 0xeeeeee, roughness: .4 })); flame.position.set(mx, 12, mz); scene.add(flame);
      addCollider(mx - 14, mx + 14, mz - 14, mz + 14, 14, 'm'); L.push({ n: 'نصب الشهيد', x: mx, z: mz }); }
    // مرقد الكاظمين (قباب ذهبية + مآذن)
    { const [sx, sz] = specials.shrine; const gold = new THREE.MeshStandardMaterial({ color: 0xd9a928, roughness: .25, metalness: .85 }), teal = new THREE.MeshStandardMaterial({ color: 0x1e7f93, roughness: .5 }), wall = new THREE.MeshStandardMaterial({ color: 0xe6d9b8, roughness: .85 });
      const base = new THREE.Mesh(new THREE.BoxGeometry(34, 11, 22), wall); base.position.set(sx, 5.5, sz); base.castShadow = base.receiveShadow = true; scene.add(base);
      for (const s of [-1, 1]) { const dome = new THREE.Mesh(new THREE.SphereGeometry(7.5, 28, 16, 0, Math.PI * 2, 0, Math.PI / 2), gold); dome.position.set(sx + s * 8, 11, sz); dome.castShadow = true; scene.add(dome);
        const drum = new THREE.Mesh(new THREE.CylinderGeometry(6.2, 6.8, 2.5, 24), teal); drum.position.set(sx + s * 8, 11.2, sz); scene.add(drum);
        const spire = new THREE.Mesh(new THREE.ConeGeometry(.6, 4, 8), gold); spire.position.set(sx + s * 8, 21.8, sz); scene.add(spire); }
      for (const mxo of [-19, 19]) for (const mzo of [-13, 13]) { const sh = new THREE.Mesh(new THREE.CylinderGeometry(1.1, 1.6, 30, 14), teal); sh.position.set(sx + mxo, 15, sz + mzo); sh.castShadow = true; scene.add(sh);
        const bal = new THREE.Mesh(new THREE.CylinderGeometry(2.2, 1.6, 1.2, 14), gold); bal.position.set(sx + mxo, 25, sz + mzo); scene.add(bal);
        const top = new THREE.Mesh(new THREE.SphereGeometry(1.6, 14, 8, 0, Math.PI * 2, 0, Math.PI / 2), gold); top.position.set(sx + mxo, 30, sz + mzo); scene.add(top); }
      addCollider(sx - 17, sx + 17, sz - 11, sz + 11, 11, 'm'); for (const mxo of [-19, 19]) for (const mzo of [-13, 13]) addCollider(sx + mxo - 1.5, sx + mxo + 1.5, sz + mzo - 1.5, sz + mzo + 1.5, 30, 'm');
      L.push({ n: 'مرقد الكاظمين', x: sx, z: sz }); }
    // برج بغداد
    { const [tx, tz] = specials.tower; const conc = new THREE.MeshStandardMaterial({ color: 0xcfc8b8, roughness: .6 });
      const shaft = new THREE.Mesh(new THREE.CylinderGeometry(2.6, 5, 120, 18), conc); shaft.position.set(tx, 60, tz); shaft.castShadow = true; scene.add(shaft);
      const pod = new THREE.Mesh(new THREE.CylinderGeometry(9, 6, 7, 24), new THREE.MeshStandardMaterial({ color: 0x8aa4b4, roughness: .2, metalness: .6 })); pod.position.set(tx, 118, tz); scene.add(pod);
      const pod2 = new THREE.Mesh(new THREE.CylinderGeometry(6, 9, 4, 24), conc); pod2.position.set(tx, 123, tz); scene.add(pod2);
      const mast = new THREE.Mesh(new THREE.CylinderGeometry(.2, .6, 40, 8), conc); mast.position.set(tx, 145, tz); scene.add(mast);
      const baseP = new THREE.Mesh(new THREE.CylinderGeometry(22, 24, 1.2, 32), new THREE.MeshStandardMaterial({ color: 0x8a8578, roughness: .9 })); baseP.position.set(tx, .6, tz); baseP.receiveShadow = true; scene.add(baseP);
      addCollider(tx - 5, tx + 5, tz - 5, tz + 5, 120, 'm'); L.push({ n: 'برج بغداد', x: tx, z: tz }); }
    // حدود الخريطة: أسوار
    const edge = new THREE.MeshStandardMaterial({ color: 0x9c9686, roughness: 1 });
    for (const [x, z, w, d] of [[0, -HALF, HALF * 2, 2], [0, HALF, HALF * 2, 2], [-HALF, 0, 2, HALF * 2], [HALF, 0, 2, HALF * 2]]) {
      const m = new THREE.Mesh(new THREE.BoxGeometry(w, 8, d), edge); m.position.set(x, 4, z); scene.add(m); addCollider(x - w / 2, x + w / 2, z - d / 2, z + d / 2, 8, 'e');
    }

    /* مساعدات */
    function blocked(x, z, r, y) {
      for (const c of near(x, z, r + 1)) { if (y !== undefined && y > c.h) continue; if (x + r > c.x0 && x - r < c.x1 && z + r > c.z0 && z - r < c.z1) return c; }
      return null;
    }
    function airportPoint() { for (let k = 0; k < 200; k++) { const x = -300 + R() * 80, z = -300 + R() * 170; if (!blocked(x, z, 2)) return { x, z }; } return { x: -280, z: -150 }; }
    function randomOpenPoint(side, margin) {
      for (let k = 0; k < 300; k++) {
        const x = (R() * 2 - 1) * (HALF - 20), z = (R() * 2 - 1) * (HALF - 20);
        if (Math.abs(x - riverX(z)) < RIVER_W / 2 + 4) continue; if (side && Math.sign(x - riverX(z)) !== side) continue;
        if (blocked(x, z, margin || 2)) continue; return { x, z };
      }
      return { x: 40, z: 40 };
    }
    return { AP, lootSpots, airportPoint, colliders, near, blocked, riverX, isWater, onBridge, randomOpenPoint, HALF, RIVER_W, BRIDGES, landmarks: L, sun, hemi, palms: palmPos.length, buildings: bl.length, rand: R };
  }

  window.BaghdadWorld = { build, riverX, isWater, HALF, RIVER_W, BRIDGES };
})();
