document.addEventListener('DOMContentLoaded', () => {
  const ctx = document.body.dataset.ctx || '';
  const form = document.getElementById('f');
  const err = document.getElementById('err');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const res = await fetch(ctx + '/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        login: fd.get('login'),
        password: fd.get('password'),
        fullName: fd.get('fullName'),
        birthDate: fd.get('birthDate'),
        role: 'COMMON'
      })
    });
    if (res.ok) {
      location.href = ctx + '/login.jsp';
    } else {
      err.textContent = await res.text();
    }
  });
});
