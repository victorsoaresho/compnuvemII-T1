const API_BASE_URL = 'http://192.168.100.144:3000'; // Ajuste para seu IP se necessário

// Elementos
const filterForm = document.getElementById('filterForm');
const tableBody = document.getElementById('tableBody');
const ordersTable = document.getElementById('ordersTable');
const noResultsDiv = document.getElementById('noResults');
const loadingDiv = document.getElementById('loading');
const errorDiv = document.getElementById('error');
const paginationSection = document.getElementById('pagination');
const modal = document.getElementById('detailsModal');
const modalDetails = document.getElementById('modalDetails');
const closeBtn = document.querySelector('.close-btn');

let currentPage = 0;
const pageSize = 10;
let allOrders = [];

// Eventos
filterForm.addEventListener('submit', (e) => {
    e.preventDefault();
    currentPage = 0;
    fetchOrders();
});

closeBtn.addEventListener('click', closeDetailsModal);
window.onclick = (e) => { if (e.target === modal) closeDetailsModal(); };

async function fetchOrders() {
    showLoading(true);
    hideError();
    try {
        const params = new URLSearchParams();
        
        params.append('page', currentPage + 1);
        params.append('limit', pageSize);

        const clientId = document.getElementById('clientId').value;
        const productId = document.getElementById('productId').value;
        const status = document.getElementById('status').value;

        if (clientId) params.append('custumerId', clientId); 
        if (productId) params.append('productId', productId);
        if (status) params.append('status', status);

        const response = await fetch(`${API_BASE_URL}/orders?${params}`);
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro na requisição');
        }
        
        allOrders = await response.json();
        displayOrders(allOrders);
    } catch (err) {
        showError(err.message);
        console.error("Erro detalhado:", err);
    } finally {
        showLoading(false);
    }
}

function displayOrders(orders) {
    tableBody.innerHTML = '';
    if (!orders.length) {
        ordersTable.classList.add('hidden');
        noResultsDiv.classList.remove('hidden');
        return;
    }
    noResultsDiv.classList.add('hidden');
    ordersTable.classList.remove('hidden');

    orders.forEach(order => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${order.uuid}</td>
            <td>${new Date(order.created_at).toLocaleDateString()}</td>
            <td>${order.customer?.name || '-'}</td>
            <td>-</td>
            <td>R$ ${order.total.toFixed(2)}</td>
            <td><span class="status ${order.status}">${order.status}</span></td>
            <td><button type="button" class="btn btn-detail" onclick="showOrderDetails('${order.uuid}')">Ver Detalhes</button></td>
        `;
        tableBody.appendChild(row);
    });
}

async function showOrderDetails(uuid) {
    if (window.event) window.event.stopPropagation();
    showLoading(true);

    try {
        const response = await fetch(`${API_BASE_URL}/orders/${uuid}`);
        const rawPayload = await response.json();

        modalDetails.innerHTML = `
            <h2 style="margin-bottom:10px; color:#333;"> Payload (JSON)</h2>
            <div style="background:#1e1e1e; color:#d4d4d4; padding:15px; border-radius:8px; overflow:auto; max-height:70vh; text-align:left;">
                <pre style="margin:0; font-family:monospace; font-size:13px;">${JSON.stringify(rawPayload, null, 2)}</pre>
            </div>
        `;
        
        modal.classList.remove('hidden');
        modal.style.display = 'flex'; 
    } catch (err) {
        alert("Erro ao carregar payload.");
    } finally {
        showLoading(false);
    }
}

function closeDetailsModal() {
    modal.classList.add('hidden');
    modal.style.display = 'none';
}

function showLoading(s) { loadingDiv.classList.toggle('hidden', !s); }
function showError(m) { errorDiv.textContent = m; errorDiv.classList.remove('hidden'); }
function hideError() { errorDiv.classList.add('hidden'); }