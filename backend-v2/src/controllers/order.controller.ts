import { Request, Response } from 'express';
import { OrderService } from '../services/order.service.js';
import { mapOrderToPayload } from '../mappers/order.mapper.js';

const orderService = new OrderService();

export class OrderController {
  async list(req: Request, res: Response) {
    try {
      const { page, limit, uuid, customerId, productId, status } = req.query;

      const orders = await orderService.getOrders({
        page: page ? parseInt(page as string) : undefined,
        limit: limit ? parseInt(limit as string) : undefined,
        uuid: uuid as string,
        customerId: customerId ? parseInt(customerId as string) : undefined,
        productId: productId ? parseInt(productId as string) : undefined,
        status: status as string,
      });

      const payload = orders.map(mapOrderToPayload);
      return res.json(payload);
    } catch (error: any) {
      return res.status(404).json({ error: error.message });
    }
  }

  async getByUuid(req: Request, res: Response) {
    try {
      const { uuid } = req.params;
      const order = await orderService.getOrderByUuid(uuid as string);

      if (!order) {
        return res.status(404).json({ error: 'Order not found' });
      }

      const payload = mapOrderToPayload(order);
      return res.json(payload);
    } catch (error: any) {
      return res.status(500).json({ error: error.message });
    }
  }
}
