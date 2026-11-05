package kz.narxoz.purchase.handler;

import jakarta.servlet.http.HttpServletRequest;
import kz.narxoz.purchase.domain.DuplicatePurchase;
import kz.narxoz.purchase.domain.MissingPurchase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var field = ex.getBindingResult().getFieldError();
        String name = "businessKey".equals(field.getField()) ? "business key" : field.getField();
        return validation(name + " " + field.getDefaultMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidId(HttpServletRequest request) {
        return validation("id must be a UUID", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> invalidBody(HttpServletRequest request) {
        return validation("body must be JSON", request);
    }

    @ExceptionHandler(MissingPurchase.class)
    ResponseEntity<ApiError> missing(MissingPurchase ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), correlationId(request));
    }

    @ExceptionHandler(DuplicatePurchase.class)
    ResponseEntity<ApiError> duplicate(DuplicatePurchase ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DUPLICATE", ex.getMessage(), correlationId(request));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupported(HttpServletRequest request) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED",
                "Content-Type must be application/json", correlationId(request));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiError> routeNotFound(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "resource not found", correlationId(request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                              HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(ex.getHeaders())
                .contentType(APPLICATION_JSON)
                .body(new ApiError("METHOD_NOT_ALLOWED", "method not allowed", correlationId(request)));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ApiError> notAcceptable(HttpServletRequest request) {
        return error(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE",
                "response is available as application/json", correlationId(request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        String id = correlationId(request);
        LOG.error("Unexpected API error, correlationId={}", id, ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", "unexpected error", id);
    }

    private static ResponseEntity<ApiError> validation(String message, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION", message, correlationId(request));
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message, String id) {
        return ResponseEntity.status(status).contentType(APPLICATION_JSON)
                .body(new ApiError(code, message, id));
    }

    private static String correlationId(HttpServletRequest request) {
        String id = request.getHeader("X-Correlation-Id");
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }
}
