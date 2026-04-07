import path from 'path';
import dotenv from 'dotenv';
import { PrismaClient } from '@prisma/client';

dotenv.config({ path: path.join(process.cwd(), '.env') });

export const prisma = new PrismaClient({
  accelerateUrl: process.env.DATABASE_URL,
});
