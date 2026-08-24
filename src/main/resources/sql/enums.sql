CREATE TYPE "reservation_status" AS ENUM (
    'pending',
    'completed',
    'canceled'
    );

CREATE TYPE "payment_method" AS ENUM (
    'cash',
    'transfer',
    'bank card'
    );

CREATE TYPE "order_status" AS ENUM (
    'PENDING_PAYMENT',
    'PAID_PENDING_DELIVERY',
    'COMPLETED',
    'CANCELLED'
    );

CREATE TYPE "sale_type" AS ENUM (
    'PRE_ORDER',
    'SALE_ON_DELIVERY'
    );

CREATE TYPE "message_type" AS ENUM (
    'TEXT',
    'JSON'
    );