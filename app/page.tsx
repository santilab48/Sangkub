const features = [
  ['📱','สแกนสั่ง','ลูกค้าสแกน QR ที่โต๊ะแล้วสั่งได้ทันที'],
  ['🔊','ครัวได้ยิน','ออเดอร์เข้าเป็นคิวเสียงบนมือถือครัว'],
  ['🧾','บิลอัตโนมัติ','สั่งเพิ่มกี่ครั้งก็รวมเข้าบิลโต๊ะเดียว'],
  ['💳','จ่ายที่โต๊ะ','พร้อมต่อ QR ชำระตามยอดและเสียงเงินเข้า'],
]

export default function Home(){
 return <main>
  <section className="hero"><div className="brand">สั่งครับ</div><h1>ร้านเล็ก<br/>ทำงานง่ายขึ้น</h1><p>สแกนสั่ง • ครัวได้ยิน • บิลพร้อม</p><a className="primary" href="/admin">เปิดหลังบ้านร้าน</a></section>
  <section className="grid">{features.map(([icon,title,text])=><article key={title}><span>{icon}</span><h2>{title}</h2><p>{text}</p></article>)}</section>
  <section className="price"><h2>เริ่มต้น 100 บาท/เดือน</h2><p>2 จุดรับงาน • เพิ่มจุดละ 50 บาท</p></section>
 </main>
}
