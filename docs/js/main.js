const links = [...document.querySelectorAll('.nav-link')];
const sections = links.map((link) => document.querySelector(link.getAttribute('href'))).filter(Boolean);

const observer = new IntersectionObserver((entries) => {
  const visible = entries.filter((entry) => entry.isIntersecting).sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
  if (!visible) return;
  links.forEach((link) => link.classList.toggle('is-active', link.getAttribute('href') === `#${visible.target.id}`));
}, { rootMargin: '-30% 0px -55% 0px', threshold: [0, 0.2, 0.5] });

sections.forEach((section) => observer.observe(section));
document.getElementById('year').textContent = new Date().getFullYear();
