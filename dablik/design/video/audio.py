import numpy as np, wave
SR=44100; DUR=30.0; N=int(SR*DUR)
rng=np.random.default_rng(7)
t=np.arange(N)/SR
L=np.zeros(N); R=np.zeros(N)
def add(sig,at,gain=1.0,pan=0.0):
    i=int(at*SR); n=min(len(sig),N-i)
    if n<=0: return
    L[i:i+n]+=sig[:n]*gain*(1-max(0,pan)); R[i:i+n]+=sig[:n]*gain*(1+min(0,pan))
def env(n,a=0.01,d=0.2):
    x=np.arange(n)/SR
    return np.minimum(1,x/max(a,1e-4))*np.exp(-x/d)
def tone(f,dur,a=0.01,d=0.2,harm=(1,.4,.2)):
    n=int(dur*SR); x=np.arange(n)/SR
    s=sum(h*np.sin(2*np.pi*f*(k+1)*x) for k,h in enumerate(harm))
    return s*env(n,a,d)
def noise_sweep(dur,f0,f1,gain=1.0):
    n=int(dur*SR); x=np.arange(n)/SR
    nz=rng.standard_normal(n)
    # crude band-pass sweep via moving sine mod + smoothing
    f=np.linspace(f0,f1,n); ph=2*np.pi*np.cumsum(f)/SR
    s=nz*np.sin(ph)
    k=np.ones(8)/8; s=np.convolve(s,k,'same')
    e=np.sin(np.pi*np.arange(n)/n)**2
    return s*e*gain
# ---- pad chords (A minor feel, warm) ----
chords=[[220,261.63,329.63],[174.61,220,261.63],[196,246.94,293.66],[164.81,207.65,246.94]]
for ci in range(4):
    st=ci*7.5; n=int(8.2*SR); x=np.arange(n)/SR
    e=np.minimum(1,x/1.2)*np.minimum(1,(8.2-x)/1.6)
    for f in chords[ci]:
        for det in (-0.8,0,0.8):
            s=np.sin(2*np.pi*(f+det)*x)+.35*np.sin(2*np.pi*(f+det)*2*x)+.12*np.sin(2*np.pi*(f+det)*3*x)
            add(s*e*0.030,st,1,pan=det*0.3)
    # bass
    b=np.sin(2*np.pi*chords[ci][0]/2*x)*e*0.10
    add(b,st,1)
# ---- soft rhythm from 3.0s: kick + hat at 100 bpm ----
bpm=100; beat=60/bpm
k_t=np.arange(int(.35*SR))/SR
kick=np.sin(2*np.pi*(52+90*np.exp(-k_t*28))*k_t)*np.exp(-k_t*9)
hat=lambda: (rng.standard_normal(int(.05*SR))*np.exp(-np.arange(int(.05*SR))/SR*90))
b=3.0
while b<27.4:
    vol=0.22 if b<26 else 0.12
    add(kick,b,vol)
    h=hat(); h=np.diff(h,prepend=0)
    add(h,b+beat/2,0.10,pan=0.3)
    if int((b-3.0)/beat)%4==2: add(h,b,0.06,pan=-0.3)
    b+=beat
# ---- logo reveal sting ----
add(tone(523.25,1.8,0.005,0.6,(1,.5,.25)),0.25,0.18); add(tone(783.99,1.8,0.005,0.7,(1,.4,.2)),0.45,0.15); add(tone(1046.5,2.0,0.005,0.9,(1,.3,.1)),0.7,0.13)
add(noise_sweep(1.0,300,5000,0.35),0.05,0.5)
# ---- transitions whoosh ----
for tt in (2.6,6.85,11.2,15.2,18.7,21.7,26.9,27.4):
    add(noise_sweep(0.55,400,6000,0.5),tt,0.45)
# ---- taps / clicks ----
def click(f=900): return tone(f,0.12,0.002,0.05,(1,.5))
for tt in (4.7,10.9,12.5,18.4,26.5): add(click(980),tt,0.30); add(click(1480),tt+0.03,0.12)
add(tone(784,.5,0.005,0.2),6.0,0.18); add(tone(1046.5,.7,0.005,0.3),6.12,0.16)   # installed
# typing ticks
def ticks(a,b,cps,g):
    x=a
    while x<b: add(click(2200+rng.integers(0,600))*0.5,x,g); x+=1/cps*(0.85+0.3*rng.random())
ticks(7.7,8.6,6,0.05); ticks(8.8,10.0,11,0.04); ticks(10.0,10.6,7,0.05); ticks(15.9,18.0,22,0.025)
# toggles
for tt in (19.6,20.15,20.7,21.25,21.7): add(tone(660,.25,0.003,0.08),tt,0.18); add(tone(990,.25,0.003,0.1),tt+.04,0.12)
# console blips
for i in range(30): add(tone(1800+60*i,0.05,0.001,0.02),22.6+i*0.1,0.05)
# done chord
for f,dl in ((523.25,0),(659.25,.08),(783.99,.16),(1046.5,.24)): add(tone(f,2.2,0.005,0.9,(1,.4,.15)),25.8+dl,0.16)
# outro bell
for f,dl in ((392,0),(523.25,.1),(659.25,.2),(783.99,.3)): add(tone(f,3.0,0.005,1.2,(1,.45,.2)),28.3+dl,0.16)
# master
m=np.stack([L,R],1)
m=np.tanh(m*1.3)
fade=np.minimum(1,t/0.4)*np.minimum(1,(DUR-t)/1.2)
m*=fade[:,None]
m/= (np.abs(m).max()+1e-9); m*=0.88
pcm=(m*32767).astype('<i2')
with wave.open('music.wav','wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
print('ok')
