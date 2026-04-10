const API_BASE_URL = 'http://localhost:3000:3000';

const filterForm = document.getElementById('filterForm');
const uuidInput = document.getElementById('orderUuid');
const clientIdInput = document.getElementById('clientId');
const productIdInput = document.getElementById('productId');
const statusSelect = document.getElementById('status');

const tableBody = document.getElementById('tableBody');
const ordersTable = document.getElementById('ordersTable');
const noResultsDiv = document.getElementById('noResults');
const loadingDiv = document.getElementById('loading');
const errorDiv = document.getElementById('error');
const paginationSection = document.getElementById('pagination');

const currentPageSpan = document.getElementById('currentPage');
const totalPagesSpan = document.getElementById('totalPages');
const prevBtn = document.getElementById('prevBtn');
const nextBtn = document.getElementById('nextBtn');

const modal = document.getElementById('detailsModal');
const modalDetails = document.getElementById('modalDetails');
const closeBtn = document.querySelector('.close-btn');

let currentPage = 0;
const pageSize = 10;
let totalPages = 1;
let allOrders = [];
let currentFilters = {
    orderUuid: '',
    clientId: '',
    productId: '',
    status: ''
};

filterForm.addEventListener('submit', handleFilterSubmit);
prevBtn.addEventListener('click', previousPage);
nextBtn.addEventListener('click', nextPage);
closeBtn.addEventListener('click', closeDetailsModal);

window.onclick = (event) => {
    if (event.target == modal) closeDetailsModal();
};

window.addEventListener('DOMContentLoaded', () => {
    fetchOrders();
});

function handleFilterSubmit(e) {
    e.preventDefault();
    currentFilters = {
        orderUuid: uuidInput.value.trim(),
        clientId: clientIdInput.value.trim(),
        productId: productIdInput.value.trim(),
        status: statusSelect.value
    };
    currentPage = 0;
    fetchOrders();
}

async function fetchOrders() {
    showLoading(true);
    hideError();
    hideNoResults();
    
    try {
        const params = new URLSearchParams();
        if (currentFilters.orderUuid) params.append('uuid', currentFilters.orderUuid);
        if (currentFilters.clientId) params.append('customerId', currentFilters.clientId);
        if (currentFilters.productId) params.append('productId', currentFilters.productId);
        if (currentFilters.status) params.append('status', currentFilters.status);
        
        params.append('page', currentPage + 1); 
        params.append('limit', pageSize);
        
        const response = await fetch(`${API_BASE_URL}/orders?${params.toString()}`);
        
        if (!response.ok) throw new Error(`Erro na API: ${response.status}`);
        
        const data = await response.json();
        allOrders = data || [];

        if (allOrders.length < pageSize) {
            totalPages = currentPage + 1;
        } else {
            totalPages = currentPage + 2; 
        }
        
        displayOrders(allOrders);
        updatePagination();
        
    } catch (error) {
        console.error('Erro ao buscar pedidos:', error);
        showError(`Erro ao buscar pedidos: ${error.message}`);
        hideTable();
    } finally {
        showLoading(false);
    }
}

function displayOrders(orders) {
    tableBody.innerHTML = '';
    
    if (!orders || orders.length === 0) {
        hideTable();
        showNoResults();
        return;
    }
    
    orders.forEach(order => {
        const row = createOrderRow(order);
        tableBody.appendChild(row);
    });
    
    showTable();
}

function createOrderRow(order) {
    const row = document.createElement('tr');
    const uuid = order.uuid || order.id || '-';
    const createdAt = formatDate(order.created_at || order.createdAt);
    const customerName = order.customer?.name || '-';
    const status = order.status || '-';
    const total = order.total || 0;
    
    const items = order.items || order.products || [];
    const productNames = items.map(p => p.product_name || p.productName || p.name).join(', ');
    
    row.innerHTML = `
        <td>${uuid}</td>
        <td>${createdAt}</td>
        <td>${customerName}</td>
        <td class="products-list">${productNames || '-'}</td>
        <td class="price">R$ ${formatCurrency(total)}</td>
        <td><span class="status ${status}">${formatStatus(status)}</span></td>
        <td><button class="btn btn-detail" onclick="showOrderDetails('${uuid}')">Ver Detalhes</button></td>
    `;
    return row;
}

function showOrderDetails(orderUuid) {
    const order = allOrders.find(o => (o.uuid || o.id) === orderUuid);
    if (!order) return;
    
    displayOrderModal(order);
    modal.classList.remove('hidden');
}

function displayOrderModal(order) {
    const customer = order.customer || {};
    const seller = order.seller || {};
    const shipment = order.shipment || {};
    const payment = order.payment || {};
    const items = order.items || order.products || [];
    
    let itemsHtml = items.map(item => {
        const itemTotal = (item.unit_price || 0) * (item.quantity || 1);
        const category = item.category || {};
        const subCategory = category.sub_category || {};
        return `
            <tr>
                <td>${item.product_name || item.productName || '-'}</td>
                <td>${category.name || '-'} → ${subCategory.name || '-'}</td>
                <td>R$ ${formatCurrency(item.unit_price || 0)}</td>
                <td>${item.quantity || 0}</td>
                <td>R$ ${formatCurrency(itemTotal)}</td>
            </tr>`;
    }).join('');

    modalDetails.innerHTML = `
        <h2>Detalhes do Pedido</h2>
        <div class="detail-section">
            <h3>Informações Gerais</h3>
            <div class="detail-grid">
                <div class="detail-item"><strong>UUID:</strong> <span>${order.uuid || order.id || '-'}</span></div>
                <div class="detail-item"><strong>Data:</strong> <span>${formatDate(order.created_at || order.createdAt)}</span></div>
                <div class="detail-item"><strong>Status:</strong> <span class="status ${order.status}">${formatStatus(order.status)}</span></div>
                <div class="detail-item"><strong>Total:</strong> <span class="price">R$ ${formatCurrency(order.total || 0)}</span></div>
            </div>
        </div>
        <div class="detail-section">
            <h3>Cliente</h3>
            <div class="detail-grid">
                <div class="detail-item"><strong>Nome:</strong> <span>${customer.name || '-'}</span></div>
                <div class="detail-item"><strong>Email:</strong> <span>${customer.email || '-'}</span></div>
            </div>
        </div>
        <div class="detail-section">
            <h3>Itens</h3>
            <table class="items-table">
                <thead><tr><th>Produto</th><th>Categoria</th><th>Preço</th><th>Qtd</th><th>Total</th></tr></thead>
                <tbody>${itemsHtml}</tbody>
            </table>
        </div>
    `;
}

function closeDetailsModal() {
    modal.classList.add('hidden');
}

function updatePagination() {
    currentPageSpan.textContent = currentPage + 1;
    totalPagesSpan.textContent = totalPages;
    prevBtn.disabled = currentPage === 0;
    nextBtn.disabled = allOrders.length < pageSize;
    
    paginationSection.classList.toggle('hidden', allOrders.length === 0 && currentPage === 0);
}

function previousPage() {
    if (currentPage > 0) {
        currentPage--;
        fetchOrders();
    }
}

function nextPage() {
    if (allOrders.length === pageSize) {
        currentPage++;
        fetchOrders();
    }
}

// Auxiliares de Visualização
function showLoading(show) { loadingDiv.classList.toggle('hidden', !show); }
function showTable() { ordersTable.classList.remove('hidden'); }
function hideTable() { ordersTable.classList.add('hidden'); }
function showNoResults() { noResultsDiv.classList.remove('hidden'); }
function hideNoResults() { noResultsDiv.classList.add('hidden'); }
function showError(msg) { errorDiv.textContent = msg; errorDiv.classList.remove('hidden'); }
function hideError() { errorDiv.classList.add('hidden'); }

// Formatação
function formatDate(date) {
    if (!date) return '-';
    const d = new Date(date);
    return d.toLocaleString('pt-BR');
}

function formatCurrency(value) {
    return parseFloat(value || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 });
}

function formatStatus(status) {
    const statusMap = {
        'created': 'Criado', 'paid': 'Pago', 'shipped': 'Enviado',
        'delivered': 'Entregue', 'canceled': 'Cancelado'
    };
    return statusMap[status] || status;
}