// ============================================================
// OficJus Drive — Completar Perfil (popula public.profiles)
// ============================================================

const SUPABASE_URL = 'https://weaqkaaqalvpbxkxrfee.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ';

const form = document.getElementById('profileForm');
const errorMsg = document.getElementById('errorMsg');
const successMsg = document.getElementById('successMsg');
const loadingOverlay = document.getElementById('loadingOverlay');

// Máscaras
document.getElementById('cpf').addEventListener('input', function() {
  let v = this.value.replace(/\D/g, '').slice(0, 11);
  if (v.length > 9) v = v.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4');
  else if (v.length > 6) v = v.replace(/^(\d{3})(\d{3})(\d{1,3})$/, '$1.$2.$3');
  else if (v.length > 3) v = v.replace(/^(\d{3})(\d{1,3})$/, '$1.$2');
  this.value = v;
});

document.getElementById('telefone').addEventListener('input', function() {
  let v = this.value.replace(/\D/g, '').slice(0, 11);
  if (v.length > 10) v = v.replace(/^(\d{2})(\d{5})(\d{4})$/, '($1) $2-$3');
  else if (v.length > 6) v = v.replace(/^(\d{2})(\d{4})(\d{1,4})$/, '($1) $2-$3');
  else if (v.length > 2) v = v.replace(/^(\d{2})(\d{1,5})$/, '($1) $2');
  this.value = v;
});

document.getElementById('cep').addEventListener('input', function() {
  let v = this.value.replace(/\D/g, '').slice(0, 8);
  if (v.length > 5) v = v.replace(/^(\d{5})(\d{1,3})$/, '$1-$2');
  this.value = v;
});

// Buscar CEP
document.getElementById('btnBuscarCEP').addEventListener('click', async () => {
  const cep = document.getElementById('cep').value.replace(/\D/g, '');
  if (cep.length !== 8) return;
  try {
    const r = await fetch('https://viacep.com.br/ws/' + cep + '/json/');
    const d = await r.json();
    if (!d.erro) {
      document.getElementById('rua').value = d.logradouro || '';
      document.getElementById('bairro').value = d.bairro || '';
      document.getElementById('cidade').value = d.localidade || '';
      document.getElementById('uf').value = d.uf || '';
    }
  } catch {}
});

// Preview da foto
document.getElementById('foto').addEventListener('change', function() {
  const file = this.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = e => {
    const img = document.getElementById('fotoPreview');
    img.src = e.target.result;
    img.style.display = 'block';
  };
  reader.readAsDataURL(file);
});

// Submit
form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorMsg.style.display = 'none';
  successMsg.style.display = 'none';
  loadingOverlay.classList.add('active');

  const nome = document.getElementById('nome').value.trim();
  const cpf = document.getElementById('cpf').value.replace(/\D/g, '');
  const telefone = document.getElementById('telefone').value;
  const matricula = document.getElementById('matricula').value.trim();
  const cep = document.getElementById('cep').value.replace(/\D/g, '');
  const rua = document.getElementById('rua').value.trim();
  const numero = document.getElementById('numero').value.trim();
  const bairro = document.getElementById('bairro').value.trim();
  const cidade = document.getElementById('cidade').value.trim();
  const uf = document.getElementById('uf').value.trim().toUpperCase();

  if (!nome) { errorMsg.textContent = 'Preencha o nome.'; errorMsg.style.display = 'block'; loadingOverlay.classList.remove('active'); return; }
  if (cpf.length !== 11) { errorMsg.textContent = 'CPF inválido.'; errorMsg.style.display = 'block'; loadingOverlay.classList.remove('active'); return; }

  try {
    // Obtém o token do usuário logado (vindo da confirmação de e-mail)
    const { data: { session } } = await supabaseClient.auth.getSession();
    if (!session) throw new Error('Sessão expirada. Faça login novamente.');

    // Atualiza user_metadata + profile
    const { error: updateError } = await supabaseClient.auth.updateUser({
      data: {
        full_name: nome,
        cpf,
        telefone,
        matricula,
        cep,
        logradouro: rua,
        numero,
        bairro,
        cidade,
        estado: uf,
        profile_complete: true,
      },
    });
    if (updateError) throw updateError;

    // Atualiza public.profiles diretamente via REST
    const { error: profileError } = await supabaseClient
      .from('profiles')
      .upsert({
        id: session.user.id,
        full_name: nome,
        cpf,
        telefone,
        matricula,
        cep,
        logradouro: rua,
        numero,
        bairro,
        cidade,
        estado: uf,
        updated_at: new Date().toISOString(),
      });
    if (profileError) throw profileError;

    form.style.display = 'none';
    successMsg.innerHTML = 'Cadastro completo! 🎉<br><br>Agora faça login no aplicativo OficJus Drive.';
    successMsg.style.display = 'block';
  } catch (err) {
    errorMsg.innerHTML = err.message || 'Erro ao salvar. Tente novamente.';
    errorMsg.style.display = 'block';
  } finally {
    loadingOverlay.classList.remove('active');
  }
});