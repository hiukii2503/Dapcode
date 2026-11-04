package com.dapcode.backend_core.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dapcode.backend_core.common.ApiResponse;
import com.dapcode.backend_core.common.ErrorCode;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handlingAppExceptopm(AppException exceptions) {
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
        exception.printStackTrace();
        return ResponseEntity
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.UNCATEGORIZED_EXCEPTION));
    }
}
