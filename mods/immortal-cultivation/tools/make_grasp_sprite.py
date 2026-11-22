"""D205: the Soul Grasp icon (technique sprite and focus item): a front-facing sheep with two tan ram horns and two
hypnotic swirl eyes, light sand skin, faint purple glow. Run: python3 tools/make_grasp_sprite.py"""
import zlib, struct, math, os
C = {
 'K': (38,28,48,255), 'W': (246,246,250,255), 'w': (206,210,222,255),
 'F': (252,240,232,255), 'f': (226,206,196,255),
 'G': (190,170,150,255), 'g': (146,124,108,255),
 'N': (236,140,160,255), 'n': (170,80,100,255),
 'H': (218,174,106,255), 'h': (160,112,54,255),
 'P': (150,60,210,255), 'M': (236,70,196,255), 'B': (16,10,24,255), 'E': (255,255,255,255),
 'L': (255,170,230,255), 'v': (178,184,204,255), 'Q': (250,226,170,255), 'j': (92,54,22,255), 'D': (120,40,170,255), 'Y':(90,200,255,255), 'Z':(70,36,110,255),
}
def png(path, img, scale=1):
    h=len(img); w=len(img[0])
    raw=b''
    for row in img:
        r=b''.join(bytes(p) for p in row)
        raw+=b'\0'+r
    def ch(t,d):
        c=struct.pack('>I',len(d))+t+d
        return c+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    open(path,'wb').write(b'\x89PNG\r\n\x1a\n'+ch(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+ch(b'IDAT',zlib.compress(raw,9))+ch(b'IEND',b''))
CLEAR=(0,0,0,0)
class Cv:
    def __init__(s): s.p=[[CLEAR]*16 for _ in range(16)]
    def set(s,x,y,c):
        if 0<=x<16 and 0<=y<16: s.p[y][x]=C[c] if isinstance(c,str) else c
    def circ(s,cx,cy,r,c):
        for y in range(16):
            for x in range(16):
                if (x+.5-cx)**2+(y+.5-cy)**2<=r*r: s.set(x,y,c)
    def ell(s,cx,cy,rx,ry,c):
        for y in range(16):
            for x in range(16):
                if ((x+.5-cx)/rx)**2+((y+.5-cy)/ry)**2<=1: s.set(x,y,c)
    def spiral(s,cx,cy,r0,r1,a0,turns,t0,t1,c,cd=None,n=80):
        # horn stroke along a spiral, thick t0 -> thin t1
        pts=[]
        for i in range(n+1):
            u=i/n; a=a0+u*turns*2*math.pi; r=r0+(r1-r0)*u
            pts.append((cx+r*math.cos(a),cy+r*math.sin(a),t0+(t1-t0)*u))
        for x,y,t in pts: s.circ(x,y,t,c)
    def outline(s,col='K'):
        o=[r[:] for r in s.p]
        for y in range(16):
            for x in range(16):
                if s.p[y][x][3]: continue
                for dx,dy in((1,0),(-1,0),(0,1),(0,-1)):
                    nx,ny=x+dx,y+dy
                    if 0<=nx<16 and 0<=ny<16 and s.p[ny][nx][3]: o[y][x]=C[col];break
        s.p=o
    def glow(s,cx,cy,r_in,r_out,a=120,col=(160,70,220)):
        for y in range(16):
            for x in range(16):
                d=math.hypot(x+.5-cx,y+.5-cy)
                if not s.p[y][x][3] and r_in<=d<=r_out: s.p[y][x]=col+(a,)
    def art(s,rows,pal=None):  # overlay ascii, '.' transparent-skip
        for y,row in enumerate(rows):
            for x,chh in enumerate(row):
                if chh!='.': s.set(x,y,chh)



def stamp(c,x0,y0,rows,m={}):
    for j,r in enumerate(rows):
        for i,ch in enumerate(r):
            if ch!='.': c.set(x0+i,y0+j,m.get(ch,ch))
def mirror(rows): return [r+r[::-1] for r in rows]
HALF = [
"....KKK.",
"..KKKWWK",
".KWWWWWW",
".KWWwWWW",
".KWWWWvW",
".KWvWWWW",
".KKwwWwK",
"..KFFFFF",
"..KFFFFF",
"..KFFFFF",
"..KFFFFF",
"..KFFFFF",
"...KFFFF",
"...KFFFN",
"....KFFn",
".....KKK",
"........",
]
HALF=HALF[:16]
HALF[14]="....KFFn"
def stamp_disc(c,x0,y0,rows):
    for j,r in enumerate(rows):
        for i,ch in enumerate(r):
            if ch!='.': c.set(x0+i,y0+j,{'Q':'Q','H':'H','h':'h','j':'j'}[ch])
HD5=[".QQQ.","QjjjH","QjHjh","QHHhh",".hhh."]
HD6=[".QQQQ.","QHHHHh","QHjjjh","QHjHjh","QHjHHh",".hhhh."]
HD4=[".QQ.","QjjH","QHjh",".hh."]
def flip(rows): return [r[::-1] for r in rows]
def mkc(face='F',fsh='f'):
    c=Cv()
    for y,row in enumerate(mirror(HALF)):
        for x,ch in enumerate(row):
            if ch!='.': c.set(x,y,{'F':face,'f':fsh,'N':'N','n':'n'}.get(ch,ch))
    for y in range(15):
        for x in range(16):
            p=c.p[y][x]; b=c.p[y+1][x]
            if p==C['W'] and b==C['K']: c.p[y][x]=C['w']
            if p==C[face] and b==C['K']: c.p[y][x]=C[fsh]
    for y in range(15):
        for x in range(16):
            if c.p[y][x]==C['W'] and c.p[y+1][x]==C[face]: c.p[y][x]=C['w']
    return c
def hornd(c,x,y,rows):
    # K ring around disc via outline-like pass limited to the disc box
    box=Cv(); stamp_disc(box,x,y,rows)
    for yy in range(16):
        for xx in range(16):
            if box.p[yy][xx][3]: c.p[yy][xx]=box.p[yy][xx]
    for yy in range(16):
        for xx in range(16):
            if not box.p[yy][xx][3]:
                for dx,dy in((1,0),(-1,0),(0,1),(0,-1)):
                    nx,ny=xx+dx,yy+dy
                    if 0<=nx<16 and 0<=ny<16 and box.p[ny][nx][3]: c.set(xx,yy,'K');break
def horns2(c,x,y,rows):
    hornd(c,x,y,rows); hornd(c,16-x-len(rows[0]),y,flip(rows))
def ears(c,face,y=8):
    for sx in (0,1):
        rows=[".KK","KNF","KNF",".KK"]
        if sx: rows=flip(rows)
        stamp(c,0 if not sx else 13,y,rows,{'F':face})
def eyes(c,pat,x=4,y=8,halo=False):
    if halo:
        for ex in (x,16-x-3):
            for (xx,yy) in ((ex-1,y+1),(ex+3,y+1),(ex+1,y-1),(ex+1,y+3)):
                if c.p[yy][xx]!=C['K']: c.set(xx,yy,'L')
    stamp(c,x,y,pat); stamp(c,16-x-3,y,flip(pat))
def glow_ring(c,col,a=130,rin=6.4,rout=8.6): c.glow(8,8,rin,rout,a,col)
def disc(c,col=(130,60,180,80),r=8.2):
    for y in range(16):
        for x in range(16):
            if not c.p[y][x][3] and math.hypot(x+.5-8,y+.5-8)<=r: c.p[y][x]=col
RING=["PMP","MBM","PMP"]
RINGE=["MEM","EBE","MEM"]
SWIRL=["PMM","MBM","MMP"]
def spiral_bg(c,cx=8,cy=8,rmax=8.4):
    for y in range(16):
        for x in range(16):
            if c.p[y][x][3]: continue
            dx,dy=x+.5-cx,y+.5-cy; r=math.hypot(dx,dy)
            if r>rmax: continue
            t=(math.atan2(dy,dx)/(2*math.pi)+r/2.6)%1.0
            c.p[y][x]=(120,40,175,235) if t<0.5 else (205,70,200,235)
def main():
    C['T']=(234,216,192,255); C['t']=(188,164,138,255)  # D205: Huy picked option 2 with skin tone 5 (light sand)
    c=mkc('T','t'); horns2(c,0,0,HD6); ears(c,'T',8); eyes(c,SWIRL); c.outline(); glow_ring(c,(150,70,220))
    tex=os.path.join(os.path.dirname(os.path.abspath(__file__)),"..","src","main","resources","assets","permahearts","textures")
    png(os.path.join(tex,"gui","sprites","technique","grasp.png"),c.p)
    png(os.path.join(tex,"item","focus_grasp.png"),c.p)

if __name__ == "__main__":
    main()
