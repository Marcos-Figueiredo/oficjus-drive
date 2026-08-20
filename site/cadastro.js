// ============================================================
// OficJus Drive — Cadastro (via backend /api/register-account)
// ============================================================

const API_URL = 'https://oficjus-backend.onrender.com';

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
  errorMsg.style.display = 'none';
  successMsg.style.display = 'none';
  loadingOverlay.classList.add('active');

  // Salva dados no localStorage para usar na proxima tela
  try {
    localStorage.setItem('drv_nome', nomeInput.value.trim());
    localStorage.setItem('drv_email', emailInput.value.trim());
  } catch { /* */ }

  try {
    if (senhaInput.value !== senhaConfirm.value) throw new Error('As senhas não conferem.');

    const res = await fetch(API_URL + '/api/register-account', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: emailInput.value.trim(),
        password: senhaInput.value,
        full_name: nomeInput.value.trim(),
      }),
    });

    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.error || 'Erro ao criar conta.');
    }

    // Salva user_id no localStorage para usar no completar-cadastro
    if (data.user_id) {
      try { localStorage.setItem('drv_user_id', data.user_id); } catch { /* */ }
    }

    form.style.display = 'none';
    successMsg.innerHTML = 'Conta criada! 🎉<br><br>Enviamos um e-mail de confirmação para <strong>' + emailInput.value.trim() + '</strong>.<br><br>Após confirmar, você será redirecionado.';
    successMsg.style.display = 'block';
  } catch (err) {
    errorMsg.innerHTML = err.message;
    errorMsg.style.display = 'block';
  } finally {
    loadingOverlay.classList.remove('active');
  }
});