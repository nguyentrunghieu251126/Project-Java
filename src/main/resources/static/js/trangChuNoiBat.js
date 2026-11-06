document.addEventListener('DOMContentLoaded', () => {
  const filter = document.getElementById('bestSellerFilters');
  const grid = document.getElementById('bestSellerGrid');
  if (!filter || !grid) return;
  const empty = document.getElementById('bestSellerEmpty');
  const cards = Array.from(grid.querySelectorAll(':scope > .col'));
  filter.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-nhom]');
    if (!button || !filter.contains(button)) return;
    const nhom = button.dataset.nhom;
    filter.querySelectorAll('button[data-nhom]').forEach(b => {
      b.classList.toggle('active', b === button);
    });
    let count = 0;
    cards.forEach(card => {
      const visible = nhom === 'tat-ca' || card.dataset.nhom === nhom;
      card.hidden = !visible;
      card.style.display = visible ? '' : 'none';
      if (visible) count++;
    });
    if (empty) empty.hidden = count > 0;
  });
});
