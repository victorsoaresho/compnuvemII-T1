import { Request, Response } from 'express';
import { OrderService } from '../services/order.service.js';
import { mapOrderToPayload } from '../mappers/order.mapper.js';

const orderService = new OrderService();

export class OrderController {
  async list(req: Request, res: Response) {
    try {
      const allowedQueries = ['page', 'limit', 'custumerId', 'productId', 'status'];
      const invalidParams = Object.keys(req.query).filter(key => !allowedQueries.includes(key));

      if (invalidParams.length > 0) {
        return res.status(400).json({ 
          error: "Parâmetros de filtro inválidos identificados.",
          invalidParameters: invalidParams
        });
      }

      const { page, limit, custumerId, productId, status } = req.query;

      const parsedPage = page ? Number(page) : 1;
      const parsedLimit = limit ? Number(limit) : 10;
      const parsedCustomerId = custumerId ? Number(custumerId) : undefined;
      const parsedProductId = productId ? Number(productId) : undefined;

      if (page && (!Number.isInteger(parsedPage) || parsedPage <= 0)) {
        return res.status(400).json({ error: "O parâmetro 'page' deve ser um número inteiro positivo." });
      }
      if (limit && (!Number.isInteger(parsedLimit) || parsedLimit <= 0)) {
        return res.status(400).json({ error: "O parâmetro 'limit' deve ser um número inteiro positivo." });
      }

      if (custumerId && (!Number.isInteger(parsedCustomerId) || parsedCustomerId! <= 0)) {
        return res.status(400).json({ error: "O parâmetro 'custumerId' deve ser um número inteiro positivo." });
      }
      if (productId && (!Number.isInteger(parsedProductId) || parsedProductId! <= 0)) {
        return res.status(400).json({ error: "O parâmetro 'productId' deve ser um número inteiro positivo." });
      }

      if (status) {
        const validStatuses = ['created', 'paid', 'shipped', 'delivered', 'canceled'];
        if (!validStatuses.includes(status as string)) {
          return res.status(400).json({ 
            error: "O status fornecido é inválido.", 
            validStatuses 
          });
        }
      }

      const orders = await orderService.getOrders({
        page: parsedPage,
        limit: parsedLimit,
        customerId: parsedCustomerId,
        productId: parsedProductId,
        status: status as string,
      });

      const payload = orders.map(mapOrderToPayload);
      return res.json(payload);

    } catch (error: any) {
      console.error("[OrderController.list] Error:", error);
      return res.status(500).json({ error: "Erro interno no servidor ao listar pedidos." });
    }
  }

  async getByUuid(req: Request, res: Response) {
    try {
      const uuid = req.params.uuid as string;

      const order = await orderService.getOrderByUuid(uuid);

      if (!order) {
        return res.status(404).json({ error: "Pedido não encontrado com o UUID especificado." });
      }

      const payload = mapOrderToPayload(order);
      return res.json(payload);

    } catch (error: any) {
      console.error("[OrderController.getByUuid] Error:", error);
      return res.status(500).json({ error: "Erro interno no servidor ao buscar o pedido." });
    }
  }
}
