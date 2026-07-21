const API = '/api';
let currentUserId = null;
let users = [];
let resources = [];
let waitlistByResource = {}; // resourceId -> list of entries

async function api(path, options = {}) {
  const res = await fetch(API + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    const message = (body && body.message) ? body.message : 'Something went wrong';
    throw new Error(message);
  }
  return body;
}

function showToast(message, isError = false) {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className = 'toast show' + (isError ? ' error' : '');
  setTimeout(() => { toast.className = 'toast'; }, 2600);
}

async function loadUsers() {
  users = await api('/users');
  const select = document.getElementById('userSelect');
  select.innerHTML = users.map(u => `<option value="${u.id}">${u.name}</option>`).join('');
  currentUserId = Number(select.value);
  select.addEventListener('change', () => {
    currentUserId = Number(select.value);
    renderBoard();
    renderMyBookings();
  });
}

async function loadResources() {
  resources = await api('/resources');
}

async function loadWaitlists() {
  waitlistByResource = {};
  for (const r of resources) {
    waitlistByResource[r.id] = await api(`/waitlist/resource/${r.id}`);
  }
}

function formatCountdown(expiresAt) {
  const ms = new Date(expiresAt) - new Date();
  if (ms <= 0) return 'expiring…';
  const s = Math.floor(ms / 1000);
  return `${Math.floor(s / 60)}m ${s % 60}s left to confirm`;
}

function renderBoard() {
  const board = document.getElementById('board');
  board.innerHTML = resources.map(r => {
    const waitlist = waitlistByResource[r.id] || [];
    const waiting = waitlist.filter(w => w.status === 'WAITING');
    const notified = waitlist.find(w => w.status === 'NOTIFIED');
    const myEntry = waitlist.find(w => w.user.id === currentUserId && (w.status === 'WAITING' || w.status === 'NOTIFIED'));

    let actionHtml = '';
    if (r.status === 'AVAILABLE') {
      actionHtml = `<button class="btn-book" onclick="bookResource(${r.id})">Book now</button>`;
    } else if (r.status === 'RESERVED' && notified && notified.user.id === currentUserId) {
      actionHtml = `<button class="btn-confirm" onclick="confirmClaim(${notified.id})">Claim it — it's your turn</button>
                    <div class="countdown" data-expires="${notified.expiresAt}">${formatCountdown(notified.expiresAt)}</div>`;
    } else if (myEntry) {
      actionHtml = `<button class="btn-disabled" disabled>You're #${myEntry.position} in line</button>`;
    } else {
      actionHtml = `<button class="btn-waitlist" onclick="joinWaitlist(${r.id})">Join waitlist</button>`;
    }

    const waitlistInfo = waiting.length > 0
      ? `<div class="waitlist-info"><strong>${waiting.length}</strong> ${waiting.length === 1 ? 'person' : 'people'} waiting</div>`
      : '';

    return `
      <div class="card">
        <div class="card-top">
          <div class="card-title">${r.name}</div>
          <span class="status-pill status-${r.status}">${r.status}</span>
        </div>
        <div class="card-desc">${r.description || ''}</div>
        <div class="card-action">${actionHtml}</div>
        ${waitlistInfo}
      </div>
    `;
  }).join('');
}

async function renderMyBookings() {
  const bookings = await api(`/bookings/user/${currentUserId}`);
  const active = bookings.filter(b => b.status === 'ACTIVE');
  const container = document.getElementById('myBookings');

  if (active.length === 0) {
    container.innerHTML = `<div class="empty-note">No active bookings.</div>`;
    return;
  }

  container.innerHTML = active.map(b => `
    <div class="booking-row">
      <span>${b.resource.name} — due ${new Date(b.dueAt).toLocaleDateString()}</span>
      <button onclick="returnBooking(${b.id})">Return</button>
    </div>
  `).join('');
}

async function refreshAll() {
  await loadResources();
  await loadWaitlists();
  renderBoard();
  await renderMyBookings();
}

async function bookResource(resourceId) {
  try {
    await api('/bookings', { method: 'POST', body: JSON.stringify({ resourceId, userId: currentUserId }) });
    showToast('Booked successfully.');
    await refreshAll();
  } catch (e) {
    showToast(e.message, true);
    await refreshAll(); // someone likely beat us to it - refresh to show real status
  }
}

async function joinWaitlist(resourceId) {
  try {
    const res = await api('/waitlist', { method: 'POST', body: JSON.stringify({ resourceId, userId: currentUserId }) });
    showToast(res.message);
    await refreshAll();
  } catch (e) {
    showToast(e.message, true);
  }
}

async function confirmClaim(waitlistEntryId) {
  try {
    await api(`/waitlist/${waitlistEntryId}/confirm`, { method: 'POST' });
    showToast('Claimed! It\'s now booked under your name.');
    await refreshAll();
  } catch (e) {
    showToast(e.message, true);
    await refreshAll();
  }
}

async function returnBooking(bookingId) {
  try {
    await api(`/bookings/${bookingId}/return`, { method: 'POST' });
    showToast('Returned. Next person in line (if any) has been notified.');
    await refreshAll();
  } catch (e) {
    showToast(e.message, true);
  }
}

// Live countdown tick for anyone currently NOTIFIED, and periodic refresh
// so the board reflects the background expiry job without a manual reload.
setInterval(() => {
  document.querySelectorAll('.countdown').forEach(el => {
    el.textContent = formatCountdown(el.dataset.expires);
  });
}, 1000);

setInterval(refreshAll, 8000);

(async function init() {
  await loadUsers();
  await refreshAll();
})();
