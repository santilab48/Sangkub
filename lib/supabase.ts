import { createClient } from '@supabase/supabase-js'

// Provider config stays behind this adapter so deployment can move later.
const url = process.env.NEXT_PUBLIC_DATABASE_API_URL || process.env.NEXT_PUBLIC_SUPABASE_URL || 'https://uhhfthgbtpuljelmqmpc.supabase.co'
const key = process.env.NEXT_PUBLIC_DATABASE_PUBLIC_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || 'sb_publishable_cTG_MtgCQkUgJJ30RCdmTw_mHXYaSJy'

export const supabase = createClient(url, key)
