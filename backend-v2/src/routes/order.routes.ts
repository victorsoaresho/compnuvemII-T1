import { Router } from 'express';
import { OrderController } from '../controllers/order.controller.js';
import { OrderService } from '../services/order.service.js';

const router = Router();

const orderService = new OrderService();
const orderController = new OrderController(orderService);

router.get('/', orderController.list);
router.get('/:uuid', orderController.getByUuid);

export default router;
