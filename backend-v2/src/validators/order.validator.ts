import { ListOrdersQueryDto } from '../dtos/order.dto.js';

export interface ValidationResult<T> {
  isValid: boolean;
  data?: T;
  error?: string | object;
}

export class OrderValidator {
  static validateListQuery(query: any): ValidationResult<ListOrdersQueryDto> {
    const allowedQueries = ['page', 'limit', 'custumerId', 'productId', 'status'];
    const invalidParams = Object.keys(query).filter(key => !allowedQueries.includes(key));

    if (invalidParams.length > 0) {
      return { 
        isValid: false, 
        error: { 
          error: "Parâmetros de filtro inválidos identificados.",
          invalidParameters: invalidParams
        } 
      };
    }

    const { page, limit, custumerId, productId, status } = query;

    const parsedPage = page ? Number(page) : 1;
    const parsedLimit = limit ? Number(limit) : 10;
    const parsedCustomerId = custumerId ? Number(custumerId) : undefined;
    const parsedProductId = productId ? Number(productId) : undefined;

    if (page && (!Number.isInteger(parsedPage) || parsedPage <= 0)) {
      return { isValid: false, error: "O parâmetro 'page' deve ser um número inteiro positivo." };
    }
    if (limit && (!Number.isInteger(parsedLimit) || parsedLimit <= 0)) {
      return { isValid: false, error: "O parâmetro 'limit' deve ser um número inteiro positivo." };
    }
    if (custumerId && (!Number.isInteger(parsedCustomerId) || parsedCustomerId! <= 0)) {
      return { isValid: false, error: "O parâmetro 'custumerId' deve ser um número inteiro positivo." };
    }
    if (productId && (!Number.isInteger(parsedProductId) || parsedProductId! <= 0)) {
      return { isValid: false, error: "O parâmetro 'productId' deve ser um número inteiro positivo." };
    }

    if (status) {
      const validStatuses = ['created', 'paid', 'shipped', 'delivered', 'canceled'];
      if (!validStatuses.includes(status as string)) {
        return { 
          isValid: false, 
          error: { 
            error: "O status fornecido é inválido.", 
            validStatuses 
          } 
        };
      }
    }

    return {
      isValid: true,
      data: {
        page: parsedPage,
        limit: parsedLimit,
        customerId: parsedCustomerId,
        productId: parsedProductId,
        status: status as string,
      }
    };
  }

  static validateUuid(uuid: string): ValidationResult<string> {
    if (!uuid || typeof uuid !== 'string') {
      return { isValid: false, error: "UUID é obrigatório e deve ser válido." };
    }
    return { isValid: true, data: uuid };
  }
}
