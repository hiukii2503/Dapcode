package com.dapcode.backend_core.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.dapcode.backend_core.common.ApiResponse;
import com.dapcode.backend_core.common.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exceptions) {
        ErrorCode errorCode = exceptions.getErrorCode();

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handlingValidation(MethodArgumentNotValidException exception) {
        String defaultMessage = exception.getFieldError() != null
                ? exception.getFieldError().getDefaultMessage()
                : ErrorCode.INVALID_REQUEST.getMessage();
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .code(ErrorCode.INVALID_REQUEST.getCode())
                .message(defaultMessage)
                .build();
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(apiResponse);
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handlingIllegalArgument(IllegalArgumentException exception) {
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .code(ErrorCode.INVALID_REQUEST.getCode())
                .message(exception.getMessage())
                .build();
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(apiResponse);
    }

    /** Body JSON sai cú pháp hoặc sai kiểu dữ liệu → lỗi của client (400), không phải 500. */
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handlingUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST));
    }

    /** Tham số path/query sai kiểu (ví dụ id không phải UUID) → 400. */
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handlingTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(value = org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlingNotFound(org.springframework.web.servlet.resource.NoResourceFoundException exception) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .code(404)
                        .message("Đường dẫn không tồn tại: " + exception.getResourcePath())
                        .build());
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<Void>> handlingGenericException(Exception exception) {
        log.error("Lỗi hệ thống chưa phân loại: {}", exception.getMessage(), exception);
        return ResponseEntity
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.UNCATEGORIZED_EXCEPTION));
    }
}
