// ============================================================
// OficJus Drive — Cadastro
// ============================================================

const form = document.getElementById('cadastroForm');
const nomeInput = document.getElementById('nome');
const emailInput = document.getElementById('email');
const senhaInput = document.getElementById('senha');
const errorMsg = document.getElementById('errorMsg');
const successMsg = document.getElementById('successMsg');
const loadingOverlay = document.getElementById('loadingOverlay');

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorMsg.style.display = 'none';
  successMsg.style.display = 'none';
  loadingOverlay.style.display = 'flex';

  const nome = nomeInput.value.trim();
  const email = emailInput.value.trim();
  const senha = senhaInput.value;

  try {
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
      'Conta criada com sucesso! 🎉<br><br>' +
      'Enviamos um e-mail de confirmação para <strong>' + email + '</strong>.<br><br>' +
      'Passo a passo:<br>' +
      '1. Verifique sua caixa de entrada (e o spam)<br>' +
      '2. Clique no link de confirmação enviado pelo Supabase<br>' +
      '3. Após confirmar, faça <a href="login.html" style="color:#6ee7b7;font-weight:600;">login aqui</a><br><br>' +
      'Seu teste grátis de <strong>7 dias</strong> começa agora!'
    );
    form.reset();
  } catch (err) {
    showError(err.message || 'Erro ao criar conta. Tente novamente.');
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

// Confirmation redirect
const params = new URLSearchParams(window.location.search);
if (params.get('confirmado') === 'true') {
  showSuccess('E-mail confirmado! Sua conta está ativa. <a href="login.html" style="color:#6ee7b7;font-weight:600;">Fazer login</a>');
}