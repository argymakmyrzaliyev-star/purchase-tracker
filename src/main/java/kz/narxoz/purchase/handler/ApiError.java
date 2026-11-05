package kz.narxoz.purchase.handler;

public record ApiError(String code, String message, String correlationId) {}
