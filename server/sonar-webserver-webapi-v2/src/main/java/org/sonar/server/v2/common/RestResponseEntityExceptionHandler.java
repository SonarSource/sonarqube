/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.v2.common;

import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.v2.api.model.RestError;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class RestResponseEntityExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

  // region client

  @ExceptionHandler(HttpMessageNotReadableException.class)
  protected ResponseEntity<RestError> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
    LOGGER.warn(ErrorMessages.INVALID_REQUEST_FORMAT.getMessage(), ex);
    return buildResponse(HttpStatus.BAD_REQUEST, ErrorMessages.INVALID_REQUEST_FORMAT);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<RestError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
    String validationErrors = ex.getFieldErrors().stream()
      .map(RestResponseEntityExceptionHandler::handleFieldError)
      .collect(Collectors.joining());
    LOGGER.info("{}\n{}", ErrorMessages.VALIDATION_ERROR.getMessage(), validationErrors);
    return buildResponse(HttpStatus.BAD_REQUEST, validationErrors);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  protected ResponseEntity<RestError> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
    LOGGER.warn(ErrorMessages.INVALID_PARAMETER_TYPE.getMessage(), ex);
    return buildResponse(HttpStatus.BAD_REQUEST, ErrorMessages.INVALID_PARAMETER_TYPE);
  }

  @ExceptionHandler(ConversionFailedException.class)
  protected ResponseEntity<RestError> handleConversionFailedException(ConversionFailedException ex) {
    LOGGER.info(ErrorMessages.CONVERSION_FAILED.getMessage(), ex);
    return buildResponse(HttpStatus.BAD_REQUEST, ErrorMessages.CONVERSION_FAILED);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  protected ResponseEntity<RestError> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
    LOGGER.warn(ErrorMessages.METHOD_NOT_SUPPORTED.getMessage(), ex);
    return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ErrorMessages.METHOD_NOT_SUPPORTED);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  protected ResponseEntity<RestError> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
    LOGGER.warn(ErrorMessages.UNSUPPORTED_MEDIA_TYPE.getMessage(), ex);
    return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorMessages.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  protected ResponseEntity<RestError> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException ex) {
    LOGGER.warn(ErrorMessages.UNACCEPTABLE_MEDIA_TYPE.getMessage(), ex);
    return buildResponse(HttpStatus.NOT_ACCEPTABLE, ErrorMessages.UNACCEPTABLE_MEDIA_TYPE);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  protected ResponseEntity<RestError> handleIllegalArgumentException(IllegalArgumentException ex) {
    LOGGER.warn(ErrorMessages.INVALID_INPUT_PROVIDED.getMessage(), ex);
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(BindException.class)
  protected ResponseEntity<RestError> handleBindException(BindException ex) {
    String validationErrors = ex.getFieldErrors().stream()
      .map(RestResponseEntityExceptionHandler::handleFieldError)
      .collect(Collectors.joining());
    LOGGER.info("{}\n{}", ErrorMessages.BIND_ERROR.getMessage(), validationErrors);
    return buildResponse(HttpStatus.BAD_REQUEST, validationErrors);
  }

  @ExceptionHandler({
    NoSuchElementException.class,
    ServletRequestBindingException.class,
  })
  protected ResponseEntity<RestError> handleBadRequests(Exception ex) {
    LOGGER.warn(ErrorMessages.BAD_REQUEST.getMessage(), ex);
    return buildResponse(HttpStatus.BAD_REQUEST, ErrorMessages.BAD_REQUEST);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  protected ResponseEntity<RestError> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
    LOGGER.warn(ErrorMessages.SIZE_EXCEEDED.getMessage(), ex);
    return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, ErrorMessages.SIZE_EXCEEDED);
  }

  // endregion client

  // region security

  @ExceptionHandler(AccessDeniedException.class)
  protected ResponseEntity<RestError> handleAccessDeniedException(AccessDeniedException ex) {
    LOGGER.error(ErrorMessages.ACCESS_DENIED.getMessage(), ex);
    return buildResponse(HttpStatus.FORBIDDEN, ErrorMessages.ACCESS_DENIED);
  }

  @ExceptionHandler(AuthenticationException.class)
  protected ResponseEntity<RestError> handleAuthenticationException(AuthenticationException ex) {
    LOGGER.warn(ex.getPublicMessage());
    return buildResponse(HttpStatus.UNAUTHORIZED, ErrorMessages.AUTHENTICATION_FAILED);
  }

  // endregion security

  // region server

  @ExceptionHandler(IllegalStateException.class)
  protected ResponseEntity<RestError> handleIllegalStateException(IllegalStateException ex) {
    LOGGER.error(ErrorMessages.INVALID_STATE.getMessage(), ex);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
  }

  @ExceptionHandler({
    FileNotFoundException.class,
    NoHandlerFoundException.class,
  })
  protected ResponseEntity<RestError> handleResourceNotFoundException(Exception ex) {
    LOGGER.error(ErrorMessages.RESOURCE_NOT_FOUND.getMessage(), ex);
    return buildResponse(HttpStatus.NOT_FOUND, ErrorMessages.RESOURCE_NOT_FOUND);
  }

  @ExceptionHandler(AsyncRequestTimeoutException.class)
  protected ResponseEntity<RestError> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex) {
    LOGGER.error(ErrorMessages.REQUEST_TIMEOUT.getMessage(), ex);
    return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ErrorMessages.REQUEST_TIMEOUT);
  }

  @ExceptionHandler(ServerException.class)
  protected ResponseEntity<RestError> handleServerException(ServerException ex) {
    final HttpStatus httpStatus = Optional.ofNullable(HttpStatus.resolve(ex.httpCode())).orElse(HttpStatus.INTERNAL_SERVER_ERROR);
    final String errorMessage = Optional.ofNullable(ex.getMessage()).orElse(ErrorMessages.INTERNAL_SERVER_ERROR.getMessage());
    return buildResponse(httpStatus, errorMessage);
  }

  @ExceptionHandler({Exception.class})
  protected ResponseEntity<RestError> handleUnhandledException(Exception ex) {
    LOGGER.error(ErrorMessages.UNEXPECTED_ERROR.getMessage(), ex);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.UNEXPECTED_ERROR);
  }

  // endregion server

  private static ResponseEntity<RestError> buildResponse(HttpStatus httpStatus, ErrorMessages errorMessages) {
    return buildResponse(httpStatus, errorMessages.getMessage());
  }

  private static ResponseEntity<RestError> buildResponse(HttpStatus httpStatus, String errorMessage) {
    return ResponseEntity.status(httpStatus).body(new RestError(errorMessage));
  }

  private static String handleFieldError(FieldError fieldError) {
    String fieldName = fieldError.getField();
    String rejectedValueAsString = Optional.ofNullable(fieldError.getRejectedValue()).map(Object::toString).orElse("{}");
    String defaultMessage = fieldError.getDefaultMessage();
    return String.format("Value %s for field %s was rejected. Error: %s.", rejectedValueAsString, fieldName, defaultMessage);
  }
}
