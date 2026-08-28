'use client'
import {useEffect,useMemo,useState} from 'react'
import {useParams} from 'next/navigation'
import {supabase} from '../../../lib/supabase'

type Item={id:string;name:string;price:number;unit:string;station:string}
type Cat={id:string;name:string;items:Item[]}
export default function TableOrder(){
 const {token}=useParams<{token:string}>(); const [data,setData]=useState<any>(); const [cart,setCart]=useState<Record<string,number>>({}); const [busy,setBusy]=useState(false); const [done,setDone]=useState<any>();
 useEffect(()=>{supabase.rpc('get_table_menu',{p_qr_token:token}).then(({data,error})=>{if(error)setData({error:error.message});else setData(data)})},[token])
 const items:Item[]=useMemo(()=>data?.categories?.flatMap((c:Cat)=>c.items||[])||[],[data]); const total=items.reduce((s,i)=>s+(cart[i.id]||0)*Number(i.price),0)
 const add=(id:string,n:number)=>setCart(c=>({...c,[id]:Math.max(0,(c[id]||0)+n)}))
 async function submit(){const lines=Object.entries(cart).filter(([,q])=>q>0).map(([menu_item_id,quantity])=>({menu_item_id,quantity}));if(!lines.length)return;setBusy(true);const {data:r,error}=await supabase.rpc('submit_table_order',{p_qr_token:token,p_idempotency_key:crypto.randomUUID(),p_items:lines});setBusy(false);if(error)alert(error.message);else{setDone(r);setCart({})}}
 if(!data)return <main className="order"><h2>กำลังเปิดเมนู…</h2></main>; if(data.error)return <main className="order"><h2>QR นี้ใช้งานไม่ได้</h2></main>
 if(done)return <main className="order success"><div className="check">✓</div><h1>ส่งออเดอร์แล้ว</h1><p>{data.table.name}</p><h2>฿{Number(done.total).toFixed(0)}</h2><button onClick={()=>setDone(null)}>สั่งเพิ่ม</button></main>
 return <main className="order"><header><b>{data.restaurant.name}</b><span>{data.table.name}</span></header><section className="orderHero"><h1>สั่งอะไรดี?</h1><p>เลือกอาหารแล้วกดส่งเข้าครัวได้เลย</p></section>{data.categories.map((c:Cat)=><section className="menuCat" key={c.id}><h2>{c.name}</h2>{c.items.map(i=><article className="menuItem" key={i.id}><div><b>{i.name}</b><small>฿{Number(i.price).toFixed(0)} / {i.unit}</small></div><div className="qty"><button onClick={()=>add(i.id,-1)}>−</button><strong>{cart[i.id]||0}</strong><button onClick={()=>add(i.id,1)}>+</button></div></article>)}</section>)}{total>0&&<div className="cartbar"><div><small>รวม</small><b>฿{total.toFixed(0)}</b></div><button disabled={busy} onClick={submit}>{busy?'กำลังส่ง…':'ส่งเข้าครัว'}</button></div>}</main>
}
