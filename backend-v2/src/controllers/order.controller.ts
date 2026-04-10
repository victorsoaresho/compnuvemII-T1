import { Request, Response } from 'express';
import { OrderService } from '../services/order.service.js';
import { mapOrderToPayload, mapOrderListToPayload } from '../mappers/order.mapper.js';
import { OrderValidator } from '../validators/order.validator.js';

export class OrderController {
  constructor(private readonly orderService: OrderService) {}

  list = async (req: Request, res: Response) => {
    try {
      const validation = OrderValidator.validateListQuery(req.query);
      
      if (!validation.isValid) {
        if (typeof validation.error === 'string') {
          return res.status(400).json({ error: validation.error });
        }
        return res.status(400).json(validation.error);
      }

      const orders = await this.orderService.getOrders(validation.data!);

      const payload = orders.map(mapOrderListToPayload);
      return res.json(payload);

    } catch (error: any) {
      console.error("[OrderController.list] Error:", error);
      return res.status(500).json({ error: "Erro interno no servidor ao listar pedidos." });
    }
  };

  getByUuid = async (req: Request, res: Response) => {
    try {
      const validation = OrderValidator.validateUuid(req.params.uuid as string);

      if (!validation.isValid) {
        return res.status(400).json({ error: validation.error as string });
      }

      const order = await this.orderService.getOrderByUuid(validation.data!);

      if (!order) {
        return res.status(404).json({ error: "Pedido não encontrado com o UUID especificado." });
      }

      const payload = mapOrderToPayload(order);
      return res.json(payload);

    } catch (error: any) {
      console.error("[OrderController.getByUuid] Error:", error);
      return res.status(500).json({ error: "Erro interno no servidor ao buscar o pedido." });
    }
  };
}
