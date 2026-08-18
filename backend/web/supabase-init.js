// ============================================================
// OficJus Drive — Init Supabase Client
// ============================================================

const SUPABASE_URL = 'https://weaqkaaqalvpbxkxrfee.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ';

const supabaseClient = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);