// Gerenciamento de Telas do Simulador de Smartphone
function switchScreen(screenId, btnElement) {
    // Desativar todas as telas do mockup
    document.querySelectorAll('.app-screen').forEach(screen => {
        screen.classList.remove('active');
    });
    
    // Ativar a tela correspondente
    const targetScreen = document.getElementById(`screen-${screenId}`);
    if (targetScreen) targetScreen.classList.add('active');

    // Atualizar estado visual dos botões de controle
    document.querySelectorAll('.control-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    btnElement.classList.add('active');
}

// Lógica de Negócio: Calculadora de Produtividade Dinâmica
const rangeInput = document.getElementById('mandados-range');
const rangeValDisplay = document.getElementById('range-val');
const horasRuaDisplay = document.getElementById('horas-rua');
const horasCertidaoDisplay = document.getElementById('horas-certidao');

if (rangeInput) {
    rangeInput.addEventListener('input', (e) => {
        const volumeMandados = parseInt(e.target.value, 10);
        rangeValDisplay.textContent = volumeMandados;

        // Regras matemáticas baseadas nas métricas operacionais coletadas:
        // 1. Economia de rota/trânsito: média de 9 minutos (0.15 horas) poupados por mandado devido ao cálculo inteligente
        // 2. Economia de certidão: média de 12 minutos (0.20 horas) poupados por uso do ditado de voz e automação Gemini IA
        const economiaTransito = Math.round(volumeMandados * 0.15);
        const economiaCertidao = Math.round(volumeMandados * 0.20);

        horasRuaDisplay.textContent = `${economiaTransito}h`;
        horasCertidaoDisplay.textContent = `${economiaCertidao}h`;
    });
}

// Comportamento do FAQ (Accordion Sanfona)
document.querySelectorAll('.accordion-header').forEach(header => {
    header.addEventListener('click', () => {
        const item = header.parentElement;
        const isActive = item.classList.contains('active');
        
        // Fecha todos os outros itens abertos
        document.querySelectorAll('.accordion-item').forEach(i => i.classList.remove('active'));
        
        // Se não estava ativo, abre o atual
        if (!isActive) {
            item.classList.add('active');
        }
    });
});

// Controle do Modal de Captação / Demonstração
const modal = document.getElementById('contactModal');

function openModal() {
    if (modal) modal.classList.add('active');
}

function closeModal() {
    if (modal) modal.classList.remove('active');
}

// Fechar modal ao clicar fora da área do card
window.addEventListener('click', (e) => {
    if (e.target === modal) {
        closeModal();
    }
});

// Feedback de envio do formulário de demonstração
function handleFormSubmit(event) {
    event.preventDefault();
    alert('Obrigado pelo interesse! Nossa equipe de engenharia judicial entrará em contato com a sua comarca em até 24 horas para liberar o ambiente de testes.');
    closeModal();
    document.getElementById('demoForm').reset();
}
