// ============================================================
// OficJus Drive — Cadastro e Login (site)
// ============================================================

const SUPABASE_URL = 'https://weaqkaaqalvpbxkxrfee.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ';

const supabaseClient = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// ===== DOM Elements =====
const form = document.getElementById('authForm');
const emailInput = document.getElementById('email');
const senhaInput = document.getElementById('senha');
const senhaConfirm = document.getElementById('senhaConfirm');
const nomeInput = document.getElementById('nome');
const nomeGroup = document.getElementById('nomeGroup');
const submitBtn = document.getElementById('submitBtn');
const toggleText = document.getElementById('toggleText');
const toggleLink = document.getElementById('toggleLink');
const formTitle = document.getElementById('formTitle');
const errorMsg = document.getElementById('errorMsg');
const successMsg = document.getElementById('successMsg');
const loadingOverlay = document.getElementById('loadingOverlay');
const strengthBar = document.getElementById('strengthBar');
const strengthText = document.getElementById('strengthText');
const confirmFeedback = document.getElementById('confirmFeedback');

let isLogin = false; // começa em modo cadastro

// ===== Medidor de força da senha =====
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
  if (score < 2) {
    strengthBar.classList.add('weak');
    strengthText.classList.add('weak');
    strengthText.textContent = 'Fraca';
  } else if (score < 4) {
    strengthBar.classList.add('medium');
    strengthText.classList.add('medium');
    strengthText.textContent = 'Média';
  } else {
    strengthBar.classList.add('strong');
    strengthText.classList.add('strong');
    strengthText.textContent = 'Forte';
  }
  validarFormulario();
});

// ===== Validação de confirmação de senha =====
senhaConfirm.addEventListener('input', () => {
  validarFormulario();
});

// ===== Valida geral e habilita/desabilita botão =====
function validarFormulario() {
  const nome = nomeInput.value.trim();
  const email = emailInput.value.trim();
  const senha = senhaInput.value;
  const senhaConf = senhaConfirm.value;
  const emModoCadastro = !isLogin;

  if (emModoCadastro && senhaConf) {
    if (senha === senhaConf) {
      confirmFeedback.textContent = '✓ Senhas conferem';
      confirmFeedback.className = 'confirm-feedback ok';
    } else {
      confirmFeedback.textContent = '✗ Senhas não conferem';
      confirmFeedback.className = 'confirm-feedback error';
    }
  } else {
    confirmFeedback.textContent = '';
    confirmFeedback.className = 'confirm-feedback';
  }

  // Desabilita se campos obrigatórios vazios ou senha fraca ou senhas diferentes
  if (emModoCadastro) {
    const senhaVal = senhaInput.value;
    const score = calcularForca(senhaVal);
    const valida =
      nome.length > 0 &&
      email.length > 0 &&
      senhaVal.length >= 8 &&
      senhaConfirm.value.length >= 8 &&
      senhaVal === senhaConfirm.value &&
      score >= 2;
    submitBtn.disabled = !valida;
  } else {
    const valida = email.length > 0 && senha.length >= 8;
    submitBtn.disabled = !valida;
  }
}

function calcularForca(val) {
  let score = 0;
  if (val.length >= 8) score += 1;
  if (val.length >= 12) score += 1;
  if (/[a-z]/.test(val) && /[A-Z]/.test(val)) score += 1;
  if (/\d/.test(val)) score += 1;
  if (/[^a-zA-Z0-9]/.test(val)) score += 1;
  return score;
}

// ===== Validação em tempo real nos campos =====
[nomeInput, emailInput].forEach(el => {
  el.addEventListener('input', validarFormulario);
});
senhaInput.addEventListener('input', validarFormulario);

// ===== Toggle mode =====
toggleLink.addEventListener('click', (e) => {
  e.preventDefault();
  isLogin = !isLogin;
  if (isLogin) {
    formTitle.textContent = 'Acessar OficJus Drive';
    submitBtn.textContent = 'Entrar';
    nomeGroup.style.display = 'none';
    nomeInput.removeAttribute('required');
    toggleText.innerHTML = 'Ainda não tem conta? <a id="toggleLink">Cadastre-se grátis por 7 dias</a>';
  } else {
    formTitle.textContent = 'Criar conta — Teste grátis 7 dias';
    submitBtn.textContent = 'Criar conta e testar';
    nomeGroup.style.display = 'block';
    nomeInput.setAttribute('required', '');
    toggleText.innerHTML = 'Já tem conta? <a id="toggleLink">Fazer login</a>';
  }
  // Limpa campos e feedback
  senhaConfirm.value = '';
  confirmFeedback.textContent = '';
  confirmFeedback.className = 'confirm-feedback';
  document.getElementById('toggleLink').addEventListener('click', toggleLink.click.bind(toggleLink));
  errorMsg.style.display = 'none';
  successMsg.style.display = 'none';
  validarFormulario();
});

// ===== Submit =====
form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorMsg.style.display = 'none';
  successMsg.style.display = 'none';
  loadingOverlay.classList.add('active');

  const email = emailInput.value.trim();
  const senha = senhaInput.value;
  const nome = nomeInput.value.trim();

  try {
    if (isLogin) {
      // LOGIN
      const { data, error } = await supabaseClient.auth.signInWithPassword({ email, password: senha });
      if (error) throw error;
      showSuccess('Login realizado! Redirecionando...');
      setTimeout(() => { window.location.href = 'oficjus-drive://open'; }, 1500);
    } else {
      // CADASTRO
      if (senha !== senhaConfirm.value) {
        throw new Error('As senhas não conferem.');
      }

      const { data, error } = await supabaseClient.auth.signUp({
        email,
        password: senha,
        options: {
          data: {
            full_name: nome,
            plano: 'drive',
            trial_start: new Date().toISOString(),
            trial_end: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
            status_assinatura: 'trial',
          },
          emailRedirectTo: 'https://oficjus-drive.onrender.com/cadastro.html?confirmado=true',
        },
      });
      if (error) throw error;
      showSuccess(
        'Conta criada! 🎉<br><br>' +
        'Enviamos um e-mail de confirmação para <strong>' + email + '</strong>.<br><br>' +
        'Passo a passo:<br>' +
        '1. Verifique sua caixa de entrada (e o spam)<br>' +
        '2. Clique no link de confirmação<br>' +
        '3. Após confirmar, faça <a href="cadastro.html" style="color:#6ee7b7;font-weight:600;">login aqui</a><br><br>' +
        'Seu teste grátis de <strong>7 dias</strong> começa agora!'
      );
      form.reset();
    }
  } catch (err) {
    showError(err.message || 'Erro ao processar. Tente novamente.');
  } finally {
    loadingOverlay.classList.remove('active');
  }
});

function showError(msg) {
  errorMsg.innerHTML = msg;
  errorMsg.style.display = 'block';
  successMsg.style.display = 'none';
}

function showSuccess(msg) {
  successMsg.innerHTML = msg;
  successMsg.style.display = 'block';
  errorMsg.style.display = 'none';
}

// ===== Confirmation redirect =====
const params = new URLSearchParams(window.location.search);
if (params.get('confirmado') === 'true') {
  showSuccess('E-mail confirmado! Sua conta está ativa. <a href="cadastro.html" style="color:#6ee7b7;font-weight:600;">Fazer login</a>');
}