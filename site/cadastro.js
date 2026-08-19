// ============================================================
// OficJus Drive — Cadastro
// ============================================================

const SUPABASE_URL = 'https://weaqkaaqalvpbxkxrfee.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ';

const supabaseClient = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

const form = document.getElementById('authForm');
const emailInput = document.getElementById('email');
const senhaInput = document.getElementById('senha');
const senhaConfirm = document.getElementById('senhaConfirm');
const submitBtn = document.querySelector('.btn-submit');
const errorMsg = document.getElementById('errorMsg');
const successMsg = document.getElementById('successMsg');
const loadingOverlay = document.getElementById('loadingOverlay');
const strengthBar = document.getElementById('strengthBar');
const strengthText = document.getElementById('strengthText');
const confirmFeedback = document.getElementById('confirmFeedback');

// ===== Etapa 1: criar conta (email + senha) =====

senhaInput.addEventListener('input', () => {
  const val = senhaInput.value;
  let score = 0;
  if (val.length >= 8) score += 1;
  if (val.length >= 12) score += 1;
  if (/[a-z]/.test(val) && /[A-Z]/.test(val)) score += 1;
  if (/\d/.test(val)) score += 1;
  if (/[^a-zA-Z0-9]/.test(val)) score += 1;
  strengthBar.className = 'strength-bar';
  strengthText.className = 'strength-text';
  if (score < 2) { strengthBar.classList.add('weak'); strengthText.classList.add('weak'); strengthText.textContent = 'Fraca'; }
  else if (score < 4) { strengthBar.classList.add('medium'); strengthText.classList.add('medium'); strengthText.textContent = 'Média'; }
  else { strengthBar.classList.add('strong'); strengthText.classList.add('strong'); strengthText.textContent = 'Forte'; }
  validarFormulario();
});

senhaConfirm.addEventListener('input', validarFormulario);
[emailInput].forEach(el => el.addEventListener('input', validarFormulario));
senhaInput.addEventListener('input', validarFormulario);

function validarFormulario() {
  const email = emailInput.value.trim();
  const senha = senhaInput.value;
  const senhaConf = senhaConfirm.value;
  if (senhaConf) {
    confirmFeedback.textContent = senha === senhaConf ? '✓ Senhas conferem' : '✗ Senhas não conferem';
    confirmFeedback.className = 'confirm-feedback ' + (senha === senhaConf ? 'ok' : 'error');
  } else { confirmFeedback.textContent = ''; confirmFeedback.className = 'confirm-feedback'; }
  const score = calcularForca(senha);
  submitBtn.disabled = !(email.length > 0 && senha.length >= 8 && senhaConf.length >= 8 && senha === senhaConf && score >= 2);
}

function calcularForca(val) { let s = 0; if (val.length >= 8) s++; if (val.length >= 12) s++; if (/[a-z]/.test(val) && /[A-Z]/.test(val)) s++; if (/\d/.test(val)) s++; if (/[^a-zA-Z0-9]/.test(val)) s++; return s; }

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorMsg.style.display = 'none';
  successMsg.style.display = 'none';
  loadingOverlay.classList.add('active');
  try {
    if (senhaInput.value !== senhaConfirm.value) throw new Error('As senhas não conferem.');
    const { error } = await supabaseClient.auth.signUp({
      email: emailInput.value.trim(),
      password: senhaInput.value,
      options: {
        data: { plano: 'drive', trial_start: new Date().toISOString(), trial_end: new Date(Date.now() + 7*24*60*60*1000).toISOString(), status_assinatura: 'trial' },
        emailRedirectTo: 'https://oficjus-drive.onrender.com/cadastro.html?confirmado=true',
      },
    });
    if (error) throw error;
    form.style.display = 'none';
    successMsg.innerHTML = 'Conta criada! 🎉<br><br>Enviamos um e-mail de confirmação para <strong>' + emailInput.value.trim() + '</strong>.<br><br>Após confirmar, complete seu cadastro.';
    successMsg.style.display = 'block';
  } catch (err) { errorMsg.innerHTML = err.message; errorMsg.style.display = 'block'; }
  finally { loadingOverlay.classList.remove('active'); }
});

// Se chegou com ?confirmado=true, redireciona para o perfil
const params = new URLSearchParams(window.location.search);
if (params.get('confirmado') === 'true' || params.get('type') === 'signup') {
  window.location.href = 'perfil.html';
    });
    if (error) throw error;
    document.getElementById('etapaCompletar').style.display = 'none';
    successMsg.innerHTML = 'Cadastro completo! 🎉<br><br>Agora faça login no aplicativo OficJus Drive.';
    successMsg.style.display = 'block';
  } catch (err) { errorMsg.innerHTML = err.message; errorMsg.style.display = 'block'; }
  finally { loadingOverlay.classList.remove('active'); }
});