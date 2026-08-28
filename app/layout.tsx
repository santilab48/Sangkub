import './globals.css'

export const metadata = { title: 'สั่งครับ', description: 'สแกนสั่ง ครัวได้ยิน บิลพร้อม' }

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="th"><body>{children}</body></html>
}
