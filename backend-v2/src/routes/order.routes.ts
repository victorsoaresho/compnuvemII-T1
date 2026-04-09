import { Router } from 'express';
import { OrderController } from '../controllers/order.controller.js';

const router = Router();
const orderController = new OrderController();

router.get('/', orderController.list);
router.get('/:uuid', orderController.getByUuid);

export default router;
