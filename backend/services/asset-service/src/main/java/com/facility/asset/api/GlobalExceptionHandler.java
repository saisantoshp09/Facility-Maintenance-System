package com.facility.asset.api;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.facility.asset.api.dto.ApiError;
import com.facility.asset.application.ConflictException;
import com.facility.asset.application.DependencyUnavailableException;
import com.facility.asset.application.InvalidRequestException;
import com.facility.asset.application.NotFoundException;
import com.facility.asset.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream().map(e -> new ApiError.FieldIssue(e.getField(), e.getDefaultMessage())).toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", req, issues);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, PropertyReferenceException.class, InvalidRequestException.class})
    ResponseEntity<ApiError> badRequest(Exception ex, HttpServletRequest req) { return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex instanceof InvalidRequestException ? ex.getMessage() : "Malformed or invalid request", req, List.of()); }
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<ApiError> forbidden(HttpServletRequest req) { return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access is denied", req, List.of()); }
    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class}) ResponseEntity<ApiError> notFound(Exception ex, HttpServletRequest req) { return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex instanceof NotFoundException ? ex.getMessage() : "Resource not found", req, List.of()); }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class) ResponseEntity<ApiError> method(HttpServletRequest req) { return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed", req, List.of()); }
    @ExceptionHandler({ConflictException.class, ObjectOptimisticLockingFailureException.class}) ResponseEntity<ApiError> conflict(Exception ex, HttpServletRequest req) { return build(HttpStatus.CONFLICT, "CONFLICT", ex instanceof ConflictException ? ex.getMessage() : "The resource was modified concurrently; retry", req, List.of()); }
    @ExceptionHandler(DependencyUnavailableException.class) ResponseEntity<ApiError> unavailable(HttpServletRequest req) { return build(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE", "A required service is temporarily unavailable", req, List.of()); }
    @ExceptionHandler(Exception.class) ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) { log.error("Unhandled exception", ex); return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req, List.of()); }
    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, HttpServletRequest req, List<ApiError.FieldIssue> issues) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code, message, req.getRequestURI(), MDC.get(CorrelationIdFilter.MDC_KEY), issues));
    }
}
