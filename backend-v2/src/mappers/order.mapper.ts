export const mapOrderToPayload = (order: any) => {
  const items = order.items.map((item: any) => {
    const unitPrice = parseFloat(item.unitPrice.toString());
    const totalItem = unitPrice * item.quantity;
    
    return {
      id: item.id,
      product_id: item.product.productId,
      product_name: item.productName,
      unit_price: unitPrice,
      quantity: item.quantity,
      category: {
        id: item.product.subCategory.category.id,
        name: item.product.subCategory.category.name,
        sub_category: {
          id: item.product.subCategory.id,
          name: item.product.subCategory.name
        }
      },
      total: totalItem
    };
  });

  const totalOrder = items.reduce((sum: number, item: any) => sum + item.total, 0);

  return {
    uuid: order.uuid,
    created_at: order.createdAt,
    channel: order.channel,
    total: totalOrder,
    status: order.status,
    customer: {
      id: order.customer.id,
      name: order.customer.name,
      email: order.customer.email,
      document: order.customer.document
    },
    seller: {
      id: order.seller.id,
      name: order.seller.name,
      city: order.seller.city,
      state: order.seller.state
    },
    items: items,
    shipment: order.shipment ? {
      carrier: order.shipment.carrier,
      service: order.shipment.service,
      status: order.shipment.status,
      tracking_code: order.shipment.trackingCode
    } : null,
    payment: order.payment ? {
      method: order.payment.method,
      status: order.payment.status,
      transaction_id: order.payment.transactionId
    } : null,
    metadata: {
      source: order.source,
      user_agent: order.userAgent,
      ip_address: order.ipAddress
    }
  };
};
