// ============================================================
// OficJus Drive — Cadastro
// ============================================================

const SUPABASE_URL = 'https://weaqkaaqalvpbxkxrfee.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ';

const supabaseClient = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

const form = document.getElementById('authForm');
const nomeInput = document.getElementById('nome');
const emailInput = document.getElementById('email');
const senhaInput = document.getElementById('senha');
const senhaConfirm = document.getElementById('senhaConfirm');
const submitBtn = document.querySelector('.btn-submit');
const errorMsg = document.getElementById('errorMsg');
const successMsg = document.getElementById('successMsg');
const loadingOverlay = document.getElementById('loadingOverlay');

senhaInput.addEventListener('input', atualizar);
senhaConfirm.addEventListener('input', atualizar);
emailInput.addEventListener('input', atualizar);
nomeInput.addEventListener('input', atualizar);

function atualizar() {
  const nome = nomeInput.value.trim();
  const email = emailInput.value.trim();
  const senha = senhaInput.value;
  const senhaConf = senhaConfirm.value;
  let score = 0;
  if (senha.length >= 8) score++;
  if (senha.length >= 12) score++;
  if (/[a-z]/.test(senha) && /[A-Z]/.test(senha)) score++;
  if (/\d/.test(senha)) score++;
  if (/[^a-zA-Z0-9]/.test(senha)) score++;
  const el = document.getElementById('strengthBar');
  const tx = document.getElementById('strengthText');
  el.className = 'strength-bar' + (score < 2 ? ' weak' : score < 4 ? ' medium' : ' strong');
  tx.textContent = score < 2 ? 'Fraca' : score < 4 ? 'Média' : 'Forte';
  tx.className = 'strength-text ' + (score < 2 ? 'weak' : score < 4 ? 'medium' : 'strong');
  const fb = document.getElementById('confirmFeedback');
  if (senhaConf) { fb.textContent = senha === senhaConf ? '✓ Senhas conferem' : '✗ Senhas não conferem'; fb.className = 'confirm-feedback ' + (senha === senhaConf ? 'ok' : 'error'); }
  else { fb.textContent = ''; fb.className = 'confirm-feedback'; }
  submitBtn.disabled = !(nome.length > 0 && email.length > 0 && senha.length >= 8 && senhaConf.length >= 8 && senha === senhaConf && score >= 2);
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorMsg.style.display = 'none'; successMsg.style.display = 'none'; loadingOverlay.classList.add('active');
  try {
    if (senhaInput.value !== senhaConfirm.value) throw new Error('As senhas não conferem.');
    const { error } = await supabaseClient.auth.signUp({
      email: emailInput.value.trim(), password: senhaInput.value,
      options: {
        data: { full_name: nomeInput.value.trim(), plano: 'drive', status_assinatura: 'trial' },
        emailRedirectTo: 'https://oficjus-drive.onrender.com/obrigado.html',
      },
    });
    if (error) throw error;
    form.style.display = 'none';
    successMsg.innerHTML = 'Conta criada! 🎉<br><br>Enviamos um e-mail de confirmação para <strong>' + emailInput.value.trim() + '</strong>.<br><br>Após confirmar, você será redirecionado.';
    successMsg.style.display = 'block';
  } catch (err) { errorMsg.innerHTML = err.message; errorMsg.style.display = 'block'; }
  finally { loadingOverlay.classList.remove('active'); }
});

const params = new URLSearchParams(window.location.search);
if (params.get('confirmado') === 'true' || params.get('type') === 'signup') {
  window.location.href = 'obrigado.html';
}

// Se chegou com ?confirmado=true, redireciona para o perfil
const params = new URLSearchParams(window.location.search);
const params = new URLSearchParams(window.location.search);
if (params.get('confirmado') === 'true' || params.get('type') === 'signup') {
  window.location.href = 'obrigado.html';
}