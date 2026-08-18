// ============================================================
// OficJus Drive — Auth (Login + Cadastro toggle)
// ============================================================

const form = document.getElementById('authForm');
const emailInput = document.getElementById('email');
const senhaInput = document.getElementById('senha');
const nomeInput = document.getElementById('nome');
const nomeGroup = document.getElementById('nomeGroup');
const submitBtn = document.getElementById('submitBtn');
const toggleText = document.getElementById('toggleText');
const toggleLink = document.getElementById('toggleLink');
const formTitle = document.getElementById('formTitle');
const errorMsg = document.getElementById('errorMsg');
const successMsg = document.getElementById('successMsg');
const loadingOverlay = document.getElementById('loadingOverlay');

let isLogin = true;

toggleLink.addEventListener('click', (e) => {
  e.preventDefault();
  isLogin = !isLogin;
  if (isLogin) {
    formTitle.textContent = 'Acessar OficJus Drive';
    submitBtn.textContent = 'Entrar';
    nomeGroup.style.display = 'none';
    nomeInput.removeAttribute('required');
    toggleText.innerHTML = 'Ainda não tem conta? <a href="#" id="toggleLink">Cadastre-se grátis por 7 dias</a>';
  } else {
    formTitle.textContent = 'Criar conta — Teste grátis 7 dias';
    submitBtn.textContent = 'Criar conta e testar';
    nomeGroup.style.display = 'block';
    nomeInput.setAttribute('required', '');
    toggleText.innerHTML = 'Já tem conta? <a href="#" id="toggleLink">Fazer login</a>';
  }
  document.getElementById('toggleLink').addEventListener('click', toggleLink.click.bind(toggleLink));
  errorMsg.style.display = 'none';
  successMsg.style.display = 'none';
});

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorMsg.style.display = 'none';
  successMsg.style.display = 'none';
  loadingOverlay.style.display = 'flex';

  const email = emailInput.value.trim();
  const senha = senhaInput.value;
  const nome = nomeInput.value.trim();

  try {
    if (isLogin) {
      const { data, error } = await supabaseClient.auth.signInWithPassword({ email, password: senha });
      if (error) throw error;
      showSuccess('Login realizado! Redirecionando...');
      setTimeout(() => { window.location.href = 'oficjus-drive://open'; }, 1500);
    } else {
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
          emailRedirectTo: window.location.origin + '/backend/web/cadastro.html?confirmado=true',
        },
      });
      if (error) throw error;
      showSuccess(
        'Conta criada! Enviamos um e-mail de confirmação para <strong>' + email + '</strong>.<br>' +
        'Clique no link do e-mail para ativar sua conta e começar o teste grátis de 7 dias.'
      );
      form.reset();
    }
  } catch (err) {
    showError(err.message || 'Erro ao processar. Tente novamente.');
  } finally {
    loadingOverlay.style.display = 'none';
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

const params = new URLSearchParams(window.location.search);
if (params.get('confirmado') === 'true') {
  showSuccess('E-mail confirmado! Sua conta está ativa e o período de teste de 7 dias já começou. <a href="?">Fazer login</a>');
}