/* ========================================
   OficJus Drive — Site Scripts
   ======================================== */

// ---------- Mobile menu toggle ----------
const menuToggle = document.getElementById('menuToggle');
const navMobile = document.getElementById('navMobile');

if (menuToggle && navMobile) {
  menuToggle.addEventListener('click', function () {
    navMobile.classList.toggle('open');
  });
}

function closeMenu() {
  if (navMobile) {
    navMobile.classList.remove('open');
  }
}

// ---------- Smooth scroll para links internos ----------
document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
  anchor.addEventListener('click', function (e) {
    const targetId = this.getAttribute('href');
    if (targetId === '#') return;
    const target = document.querySelector(targetId);
    if (target) {
      e.preventDefault();
      closeMenu();
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  });
});

// ---------- Reveal animation on scroll ----------
const observerOptions = {
  threshold: 0.1,
  rootMargin: '0px 0px -50px 0px',
};

const observer = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      entry.target.style.animationPlayState = 'running';
      observer.unobserve(entry.target);
    }
  });
}, observerOptions);

document.querySelectorAll('.feature-card, .tech-card, .pillar').forEach((el) => {
  el.style.animationPlayState = 'paused';
  observer.observe(el);
});

console.log('OficJus Drive site loaded.');