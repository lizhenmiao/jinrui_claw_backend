package com.jinrui.xlxadmin.controller;

import com.jinrui.xlxadmin.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<?> validation(MethodArgumentNotValidException e) {
    // 参数校验失败：例如缺少licenseKey、usbId等必填字段。
    return ApiResponse.fail("BAD_REQUEST", "请求参数不完整");
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<?> error(Exception e) {
    // 统一异常返回：避免接口直接抛出堆栈给客户端。
    return ApiResponse.fail("ERROR", e.getMessage());
  }
}
