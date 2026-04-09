import { prisma } from '../prisma.js';

export class OrderService {
  async getOrders(params: {
    page?: number;
    limit?: number;
    uuid?: string;
    customerId?: number;
    productId?: number;
    status?: string;
  }) {
    const { page = 1, limit = 10, uuid, customerId, productId, status } = params;
    const skip = (page - 1) * limit;

    const where: any = {};

    if (uuid) {
      where.uuid = uuid;
    }

    if (customerId) {
      where.customerId = customerId;
    }

    if (status) {
      where.status = status;
    }

    if (productId) {
      where.items = {
        some: {
          product: {
            productId: productId
          }
        }
      };
    }

    const orders = await prisma.order.findMany({
      where,
      skip,
      take: limit,
      orderBy: {
        createdAt: 'desc'
      },
      include: {
        customer: true,
        seller: true,
        shipment: true,
        payment: true,
        items: {
          include: {
            product: {
              include: {
                subCategory: {
                  include: {
                    category: true
                  }
                }
              }
            }
          }
        }
      }
    });

    return orders;
  }

  async getOrderByUuid(uuid: string) {
    return prisma.order.findUnique({
      where: { uuid },
      include: {
        customer: true,
        seller: true,
        shipment: true,
        payment: true,
        items: {
          include: {
            product: {
              include: {
                subCategory: {
                  include: {
                    category: true
                  }
                }
              }
            }
          }
        }
      }
    });
  }
}
