export interface ListOrdersQueryDto {
  page?: number;
  limit?: number;
  customerId?: number;
  productId?: number;
  status?: string;
}
