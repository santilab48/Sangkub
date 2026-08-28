import { createClient } from '@supabase/supabase-js'

const url = process.env.NEXT_PUBLIC_DATABASE_API_URL || process.env.NEXT_PUBLIC_SUPABASE_URL
const key = process.env.NEXT_PUBLIC_DATABASE_PUBLIC_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY

if (!url || !key) {
  throw new Error('Missing database API environment configuration')
}

export const supabase = createClient(url, key)
