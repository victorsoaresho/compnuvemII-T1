const API_BASE_URL = 'http://localhost:3000';

const filterForm = document.getElementById('filterForm');
const uuidInput = document.getElementById('orderUuid');
const clientIdInput = document.getElementById('clientId');
const productIdInput = document.getElementById('productId');
const statusSelect = document.getElementById('status');

const tableBody = document.getElementById('ordersTable').querySelector('tbody');
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

// Ao carregar a página
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
    
    try {
        const params = new URLSearchParams();
        
        if (currentFilters.orderUuid) {
            params.append('uuid', currentFilters.orderUuid);
        }
        if (currentFilters.clientId) {
            params.append('customerId', currentFilters.clientId);
        }
        if (currentFilters.productId) {
            params.append('productId', currentFilters.productId);
        }
        if (currentFilters.status) {
            params.append('status', currentFilters.status);
        }
        
        // API usa 0-based pagination
        params.append('page', currentPage);
        params.append('limit', pageSize);
        
        const url = `${API_BASE_URL}/orders?${params.toString()}`;
        console.log('Buscando:', url);
        
        const response = await fetch(url);
        
        if (!response.ok) {
            throw new Error(`Erro na API: ${response.status}`);
        }
        
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
    hideNoResults();
}

function createOrderRow(order) {
    const row = document.createElement('tr');
    
    const uuid = order.uuid || order.id || '-';
    const createdAt = formatDate(order.created_at || order.createdAt || new Date());
    const customerName = order.customer?.name || '-';
    const status = order.status || '-';
    const total = order.total || 0;
    
    const products = order.items || order.products || [];
    const productNames = products.map(p => p.product_name || p.productName || p.name).join(', ');
    
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
    
    if (!order) {
        showError('Pedido não encontrado');
        return;
    }
    
    displayOrderModal(order);
    modal.classList.remove('hidden');
}

function displayOrderModal(order) {
    const customer = order.customer || {};
    const seller = order.seller || {};
    const shipment = order.shipment || {};
    const payment = order.payment || {};
    const items = order.items || order.products || [];
    
    let html = `
        <h2>Detalhes do Pedido</h2>
        
        <div class="detail-section">
            <h3>Informações Gerais</h3>
            <div class="detail-grid">
                <div class="detail-item">
                    <strong>UUID:</strong>
                    <span>${order.uuid || order.id || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>Data de Criação:</strong>
                    <span>${formatDate(order.created_at || order.createdAt)}</span>
                </div>
                <div class="detail-item">
                    <strong>Status:</strong>
                    <span class="status ${order.status}">${formatStatus(order.status)}</span>
                </div>
                <div class="detail-item">
                    <strong>Canal:</strong>
                    <span>${order.channel || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>Total:</strong>
                    <span class="price">R$ ${formatCurrency(order.total || 0)}</span>
                </div>
            </div>
        </div>
        
        <div class="detail-section">
            <h3>Cliente</h3>
            <div class="detail-grid">
                <div class="detail-item">
                    <strong>ID:</strong>
                    <span>${customer.id || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>Nome:</strong>
                    <span>${customer.name || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>Email:</strong>
                    <span>${customer.email || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>CPF:</strong>
                    <span>${customer.document || '-'}</span>
                </div>
            </div>
        </div>
        
        <div class="detail-section">
            <h3>Vendedor</h3>
            <div class="detail-grid">
                <div class="detail-item">
                    <strong>ID:</strong>
                    <span>${seller.id || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>Nome:</strong>
                    <span>${seller.name || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>Cidade:</strong>
                    <span>${seller.city || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>Estado:</strong>
                    <span>${seller.state || '-'}</span>
                </div>
            </div>
        </div>
        
        <div class="detail-section">
            <h3>Itens do Pedido</h3>
            <table class="items-table">
                <thead>
                    <tr>
                        <th>Produto</th>
                        <th>Categoria</th>
                        <th>Preço Unit.</th>
                        <th>Quantidade</th>
                        <th>Total</th>
                    </tr>
                </thead>
                <tbody>
    `;
    
    items.forEach(item => {
        const itemTotal = (item.unit_price || 0) * (item.quantity || 1);
        const category = item.category || {};
        const subCategory = category.sub_category || {};
        
        html += `
            <tr>
                <td>${item.product_name || item.productName || '-'}</td>
                <td>${category.name || '-'} → ${subCategory.name || '-'}</td>
                <td>R$ ${formatCurrency(item.unit_price || 0)}</td>
                <td>${item.quantity || 0}</td>
                <td>R$ ${formatCurrency(itemTotal)}</td>
            </tr>
        `;
    });
    
    html += `
                </tbody>
            </table>
        </div>
        
        <div class="detail-section">
            <h3>Envio</h3>
            <div class="detail-grid">
                <div class="detail-item">
                    <strong>Transportadora:</strong>
                    <span>${shipment.carrier || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>Serviço:</strong>
                    <span>${shipment.service || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>Status Envio:</strong>
                    <span>${shipment.status || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>Rastreamento:</strong>
                    <span>${shipment.tracking_code || '-'}</span>
                </div>
            </div>
        </div>
        
        <div class="detail-section">
            <h3>Pagamento</h3>
            <div class="detail-grid">
                <div class="detail-item">
                    <strong>Método:</strong>
                    <span>${payment.method || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>Status:</strong>
                    <span>${payment.status || '-'}</span>
                </div>
                <div class="detail-item">
                    <strong>ID Transação:</strong>
                    <span>${payment.transaction_id || '-'}</span>
                </div>
            </div>
        </div>
    `;
    
    modalDetails.innerHTML = html;
}

function closeDetailsModal() {
    modal.classList.add('hidden');
}

// ========== Funções de Paginação ==========
function updatePagination() {
    currentPageSpan.textContent = currentPage + 1;
    totalPagesSpan.textContent = totalPages;
    
    prevBtn.disabled = currentPage === 0;
    nextBtn.disabled = currentPage >= totalPages - 1 || allOrders.length < pageSize;
    
    if (totalPages > 1 && allOrders.length > 0) {
        paginationSection.classList.remove('hidden');
    } else {
        paginationSection.classList.add('hidden');
    }
}

function previousPage() {
    if (currentPage > 0) {
        currentPage--;
        fetchOrders();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function nextPage() {
    if (allOrders.length === pageSize) {
        currentPage++;
        fetchOrders();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

// ========== Funções de Visibilidade ==========
function showLoading(show) {
    if (show) {
        loadingDiv.classList.remove('hidden');
    } else {
        loadingDiv.classList.add('hidden');
    }
}

function showTable() {
    ordersTable.classList.remove('hidden');
}

function hideTable() {
    ordersTable.classList.add('hidden');
}

function showNoResults() {
    noResultsDiv.classList.remove('hidden');
}

function hideNoResults() {
    noResultsDiv.classList.add('hidden');
}

function showError(message) {
    errorDiv.textContent = message;
    errorDiv.classList.remove('hidden');
}

function hideError() {
    errorDiv.classList.add('hidden');
}

// ========== Funções de Formatação ==========
function formatDate(date) {
    if (!date) return '-';
    
    const d = new Date(date);
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    
    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

function formatCurrency(value) {
    return parseFloat(value || 0).toLocaleString('pt-BR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function formatStatus(status) {
    const statusMap = {
        'created': ' Criado',
        'paid': ' Pago',
        'shipped': ' Enviado',
        'delivered': ' Entregue',
        'canceled': ' Cancelado'
    };
    
    return statusMap[status] || status;
}
