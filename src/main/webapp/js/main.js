document.addEventListener('DOMContentLoaded', () => {
  const ctx = document.body.dataset.ctx || '';
  const listEl = document.getElementById('list');
  const ridesEl = document.getElementById('rides');
  const coordsEl = document.getElementById('coords');

  async function loadNearest() {
    listEl.textContent = 'Определяем координаты...';
    try {
      const { posX, posY } = await getUserCoords();
      coordsEl.textContent = `Ваша позиция: posX=${posX.toFixed(5)}, posY=${posY.toFixed(5)}`;

      const scooters = await fetch(ctx + '/api/scooters/nearest', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ posX, posY })
      }).then((r) => r.json());

      if (!Array.isArray(scooters)) {
        listEl.textContent = JSON.stringify(scooters);
        return;
      }

      if (scooters.length === 0) {
        listEl.textContent = 'Рядом нет самокатов со статусом READY';
        return;
      }

      listEl.innerHTML = scooters.map((s) => {
        const plate = s.numberPlate || s.number_plate;
        const dist = s.distance != null ? ` · ${Math.round(s.distance)} м` : '';
        const charge = s.charge != null ? ` · заряд ${s.charge}%` : '';
        const st = s.status ? ` · ${s.status}` : '';
        return `<div class="scooter-item">
          <span><strong>${plate}</strong>${st}${charge}${dist}</span>
          <button type="button" data-plate="${plate}">Заказать</button>
        </div>`;
      }).join('');

      listEl.querySelectorAll('[data-plate]').forEach((btn) => {
        btn.addEventListener('click', () => startRide(btn.dataset.plate));
      });
    } catch (e) {
      listEl.textContent = 'Нужен доступ к геолокации: ' + (e.message || e);
    }
  }

  async function loadRides() {
    const rides = await fetch(ctx + '/api/rides').then((r) => r.json());
    ridesEl.textContent = JSON.stringify(rides, null, 2);

    if (Array.isArray(rides)) {
      const active = rides.find((r) =>
        r.status === 'ACTIVE' || r.status === 'IN_PROGRESS' || r.status === 'STARTED');
      const finishBox = document.getElementById('active-ride');
      if (active && finishBox) {
        finishBox.innerHTML =
          `<p>Активная поездка #${active.id}
             <button type="button" id="btn-finish">Завершить</button></p>`;
        document.getElementById('btn-finish')
          .addEventListener('click', () => finishRide(active.id));
      } else if (finishBox) {
        finishBox.innerHTML = '';
      }
    }
  }

  async function startRide(numberPlate) {
    try {
      const { posX, posY } = await getUserCoords();
      const res = await fetch(ctx + '/api/rides', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ numberPlate, posX, posY })
      });
      alert(res.ok ? 'Поездка начата' : await res.text());
      await loadNearest();
      await loadRides();
    } catch (e) {
      alert(e.message || e);
    }
  }

  async function finishRide(rideId) {
    try {
      const { posX, posY } = await getUserCoords();
      const res = await fetch(ctx + '/api/rides/' + rideId + '/finish', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ posX, posY })
      });
      alert(res.ok ? 'Поездка завершена' : await res.text());
      await loadNearest();
      await loadRides();
    } catch (e) {
      alert(e.message || e);
    }
  }

  document.getElementById('btn-refresh')
    .addEventListener('click', () => {
      loadNearest();
      loadRides();
    });

  loadNearest();
  loadRides();
});
