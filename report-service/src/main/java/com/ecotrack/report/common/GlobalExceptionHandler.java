package com.ecotrack.report.common;

import com.ecotrack.report.collection.CollectionRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** All errors are translated here, once, into the ApiError shape with the right HTTP status. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(InvalidTransitionException.class)
    ResponseEntity<ApiError> invalidTransition(InvalidTransitionException e) {
        return build(HttpStatus.CONFLICT, "INVALID_TRANSITION", e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> businessRule(BusinessRuleException e) {
        return build(HttpStatus.CONFLICT, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(InvalidInputException.class)
    ResponseEntity<ApiError> invalidInput(InvalidInputException e) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", e.getMessage());
    }

    @ExceptionHandler(CollectionRejectedException.class)
    ResponseEntity<ApiError> rejectedByCollectionService(CollectionRejectedException e) {
        return build(e.httpStatus(), "COLLECTION_" + e.getGrpcCode(), e.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    ResponseEntity<ApiError> unavailable(ServiceUnavailableException e) {
        log.warn("Downstream unavailable: {}", e.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        var details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of("VALIDATION_FAILED", "request validation failed", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_BODY", "request body is missing or malformed");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "invalid value for parameter '" + e.getName() + "'");
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tooLarge(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "the file is too large (5 MB maximum)");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e) {
        log.error("Unexpected error", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "unexpected error");
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(code, message));
    }
}
