// ============================================================
// OficJus Driver — Completar Cadastro (public.profiles)
// ============================================================

const API_URL = 'https://oficjus-backend.onrender.com';
const SUPABASE_URL = 'https://weaqkaaqalvpbxkxrfee.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndlYXFrYWFxYWx2cGJ4a3hyZmVlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzUyNzU4NjIsImV4cCI6MjA1MDg1MTg2Mn0.BsW5BdsOXIqqlxKD5sgJ8g8tT5Q1LfIW7A5WZR340qE';

// XHR helper
function xhrRequest(method, url, options = {}) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(method, url, true);
    if (options.headers) {
      Object.entries(options.headers).forEach(([k, v]) => xhr.setRequestHeader(k, v));
    }
    xhr.onload = function () {
      let data = null;
      try { data = xhr.responseText ? JSON.parse(xhr.responseText) : null; } catch { data = null; }
      resolve({ status: xhr.status, ok: xhr.status >= 200 && xhr.status < 300, data, text: xhr.responseText });
    };
    xhr.onerror = function () { reject(new Error('Erro de conexao com o servidor.')); };
    if (options.body) xhr.send(typeof options.body === 'string' ? options.body : JSON.stringify(options.body));
    else xhr.send();
  });
}

// Estado
let accessToken = '';
let userId = '';
let userEmail = '';
let userName = '';
let fotoDataUrl = '';

// Elementos
const form = document.getElementById('profileForm');
const nomeInput = document.getElementById('nome');
const emailInput = document.getElementById('email');
const submitBtn = document.getElementById('submitBtn');
const errorMsg = document.getElementById('errorMsg');
const successMsg = document.getElementById('successMsg');
const loadingOverlay = document.getElementById('loadingOverlay');
const fotoInput = document.getElementById('fotoInput');
const fotoPreview = document.getElementById('fotoPreview');

// Foto preview
fotoInput.addEventListener('change', function (e) {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (ev) => {
    fotoDataUrl = ev.target.result;
    fotoPreview.innerHTML = `<img src="${fotoDataUrl}" alt="foto">`;
  };
  reader.readAsDataURL(file);
});

// Buscar CEP
document.getElementById('cep').addEventListener('blur', async function () {
  const cep = this.value.replace(/\D/g, '');
  if (cep.length !== 8) return;
  try {
    const { ok, data } = await xhrRequest('GET', `https://viacep.com.br/ws/${cep}/json/`);
    if (ok && data && !data.erro) {
      document.getElementById('rua').value = data.logradouro || '';
      document.getElementById('bairro').value = data.bairro || '';
      document.getElementById('cidade').value = data.localidade || '';
      document.getElementById('uf').value = data.uf || '';
    }
  } catch { /* ignore */ }
});

// Inicializar — pegar token da URL ou localStorage
(async function init() {
  const hash = window.location.hash.replace('#', '&');
  const params = new URLSearchParams(hash);
  accessToken = params.get('access_token') || '';
  const type = params.get('type') || '';

  // Salva token pra usar depois
  if (accessToken && type === 'signup') {
    try { localStorage.setItem('sb-access-token', accessToken); } catch { /* */ }
  }
  if (!accessToken) {
    try { accessToken = localStorage.getItem('sb-access-token') || ''; } catch { /* */ }
  }

  if (!accessToken) {
    nomeInput.removeAttribute('readonly');
    emailInput.removeAttribute('readonly');
    errorMsg.innerHTML = 'Sessão expirada. Preencha manualmente ou <a href="login.html" style="color:#3b82f6;">faça login</a>.';
    errorMsg.style.display = 'block';
    submitBtn.disabled = false;
    return;
  }

  // Buscar dados do usuario
  try {
    const { ok, data } = await xhrRequest('GET', `${SUPABASE_URL}/auth/v1/user`, {
      headers: { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${accessToken}` },
    });
    if (ok && data) {
      userId = data.id || '';
      userEmail = data.email || '';
      userName = data.user_metadata?.full_name || '';
      nomeInput.value = userName;
      emailInput.value = userEmail;
    } else {
      nomeInput.removeAttribute('readonly');
      emailInput.removeAttribute('readonly');
      errorMsg.innerHTML = 'Sessão expirada. Preencha manualmente.';
      errorMsg.style.display = 'block';
    }
  } catch (err) {
    nomeInput.removeAttribute('readonly');
    emailInput.removeAttribute('readonly');
    errorMsg.innerHTML = 'Erro ao carregar. Preencha manualmente.';
    errorMsg.style.display = 'block';
  }
})();
})();

// Submit
form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorMsg.style.display = 'none';
  successMsg.style.display = 'none';
  loadingOverlay.classList.add('active');

  try {
    const cpf = document.getElementById('cpf').value.replace(/\D/g, '');
    if (cpf.length !== 11) throw new Error('CPF inválido.');
    if (!document.getElementById('rua').value.trim()) throw new Error('Preencha o logradouro.');
    if (!document.getElementById('cidade').value.trim()) throw new Error('Preencha a cidade.');
    if (!document.getElementById('uf').value) throw new Error('Selecione a UF.');

    const profile = {
      user_id: userId,
      nome: nomeInput.value.trim(),
      email: userEmail,
      cpf: cpf,
      telefone: document.getElementById('telefone').value.replace(/\D/g, ''),
      matricula: document.getElementById('matricula').value.trim(),
      cep: document.getElementById('cep').value.replace(/\D/g, ''),
      rua: document.getElementById('rua').value.trim(),
      numero: document.getElementById('numero').value.trim(),
      bairro: document.getElementById('bairro').value.trim(),
      cidade: document.getElementById('cidade').value.trim(),
      uf: document.getElementById('uf').value,
      cnj_j: document.getElementById('j').value,
      cnj_tr: document.getElementById('tr').value.trim(),
      cnj_oooo: document.getElementById('oooo').value.trim(),
      valor_urbano: document.getElementById('valor_urbano').value ? parseFloat(document.getElementById('valor_urbano').value.replace(/\./g, '').replace(',', '.')) : null,
      valor_rural: document.getElementById('valor_rural').value ? parseFloat(document.getElementById('valor_rural').value.replace(/\./g, '').replace(',', '.')) : null,
      foto_data_url: fotoDataUrl || '',
    };

    // Salva via backend
    const { ok, data } = await xhrRequest('POST', `${API_URL}/api/complete-profile`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`,
      },
      body: JSON.stringify(profile),
    });

    if (!ok) {
      throw new Error(data?.error || 'Erro ao salvar cadastro.');
    }

    loadingOverlay.classList.remove('active');
    // Redireciona para download
    window.location.href = 'download.html';
  } catch (err) {
    errorMsg.textContent = err.message;
    errorMsg.style.display = 'block';
    loadingOverlay.classList.remove('active');
  }
});