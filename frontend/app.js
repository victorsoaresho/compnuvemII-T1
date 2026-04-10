const API_BASE_URL = 'http://192.168.100.144:3000'; 

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
    
    const uuidValue = document.getElementById('orderUuid').value.trim();
    const clientValue = document.getElementById('clientId').value.trim();
    const productValue = document.getElementById('productId').value.trim();
    const statusValue = document.getElementById('status').value;

    try {
        let response;
        let data;

        if (uuidValue) {
            response = await fetch(`${API_BASE_URL}/orders/${uuidValue}`);
            
            if (response.status === 404) {
                data = [];
            } else if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || 'Erro ao buscar UUID');
            } else {
                const order = await response.json();
                data = [order]; 
            }
        } else {
            const params = new URLSearchParams();
            params.append('page', currentPage + 1);
            params.append('limit', pageSize);

            if (clientValue)  params.append('custumerId', clientValue); 
            if (productValue) params.append('productId', productValue);
            if (statusValue)  params.append('status', statusValue);

            response = await fetch(`${API_BASE_URL}/orders?${params}`);
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || 'Erro na filtragem');
            }
            data = await response.json();
        }

        allOrders = data; 
        displayOrders(allOrders); 

    } catch (err) {
        showError(err.message);
        console.error("Erro na busca:", err);
        if (ordersTable) ordersTable.classList.add('hidden');
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