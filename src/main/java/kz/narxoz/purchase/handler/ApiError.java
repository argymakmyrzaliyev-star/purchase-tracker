package kz.narxoz.purchase.handler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public record ApiError(String code, String message, String correlationId) {
    public static String correlationId(HttpServletRequest request) {
        String id = request.getHeader("X-Correlation-Id");
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }

    public static ApiError forRequest(String code, String message, HttpServletRequest request) {
        return new ApiError(code, message, correlationId(request));
    }
}
