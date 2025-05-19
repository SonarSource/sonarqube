/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.exceptions.BadConfigurationException;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.exceptions.TemplateMatchingKeyException;
import org.sonar.server.exceptions.TooManyRequestsException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.v2.api.model.RestError;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestResponseEntityExceptionHandlerTest {

  private final RestResponseEntityExceptionHandler underTest = new RestResponseEntityExceptionHandler();

  @RegisterExtension
  private final LogTesterJUnit5 logs = new LogTesterJUnit5();

  // region client

  @Test
  void handleHttpMessageNotReadableException_shouldReturnBadRequest() {
    var ex = new HttpMessageNotReadableException("Invalid format", new MockHttpInputMessage(InputStream.nullInputStream()));
    ResponseEntity<RestError> response = underTest.handleHttpMessageNotReadableException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.INVALID_REQUEST_FORMAT.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.WARN)).contains(ErrorMessages.INVALID_REQUEST_FORMAT.getMessage());
  }

  @Test
  void handleMethodArgumentNotValidException_shouldReturnBadRequest() {
    var fieldError = mock(FieldError.class);
    var ex = mock(MethodArgumentNotValidException.class);

    when(fieldError.getDefaultMessage()).thenReturn("<defaultMessage>");
    when(fieldError.getField()).thenReturn("<field>");
    when(fieldError.getRejectedValue()).thenReturn("<rejectedValue>");
    when(ex.getFieldErrors()).thenReturn(List.of(fieldError));

    ResponseEntity<RestError> response = underTest.handleMethodArgumentNotValidException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Value <rejectedValue> for field <field> was rejected. Error: <defaultMessage>.");

    // Verify logging
    assertThat(logs.logs(Level.INFO)).anyMatch(log -> log.startsWith(ErrorMessages.VALIDATION_ERROR.getMessage()));
  }

  @Test
  void handleMethodArgumentTypeMismatchException_shouldReturnBadRequest() {
    MethodParameter methodParameter = mock(MethodParameter.class);
    Throwable throwable = mock(Throwable.class);

    var ex = new MethodArgumentTypeMismatchException(null, null, "name", methodParameter, throwable);

    ResponseEntity<RestError> response = underTest.handleMethodArgumentTypeMismatchException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.INVALID_PARAMETER_TYPE.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.WARN)).contains(ErrorMessages.INVALID_PARAMETER_TYPE.getMessage());
  }

  @Test
  void handleConversionFailedException_shouldReturnBadRequest() {
    var ex = new ConversionFailedException(null, TypeDescriptor.valueOf(Byte.class), null, mock(Throwable.class));

    ResponseEntity<RestError> response = underTest.handleConversionFailedException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.CONVERSION_FAILED.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.INFO)).contains(ErrorMessages.CONVERSION_FAILED.getMessage());
  }

  @Test
  void handleHttpRequestMethodNotSupportedException_shouldReturnMethodNotAllowed() {
    var ex = new HttpRequestMethodNotSupportedException("GET");

    ResponseEntity<RestError> response = underTest.handleHttpRequestMethodNotSupportedException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.METHOD_NOT_SUPPORTED.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.WARN)).contains(ErrorMessages.METHOD_NOT_SUPPORTED.getMessage());
  }

  @Test
  void handleHttpMediaTypeNotSupportedException_shouldReturnUnsupportedMediaType() {
    var ex = new HttpMediaTypeNotSupportedException("Unsupported media type");

    ResponseEntity<RestError> response = underTest.handleHttpMediaTypeNotSupportedException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.UNSUPPORTED_MEDIA_TYPE.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.WARN)).contains(ErrorMessages.UNSUPPORTED_MEDIA_TYPE.getMessage());
  }

  @Test
  void handleHttpMediaTypeNotAcceptableException_shouldReturnNotAcceptable() {
    var ex = new HttpMediaTypeNotAcceptableException("Not acceptable");

    ResponseEntity<RestError> response = underTest.handleHttpMediaTypeNotAcceptableException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.UNACCEPTABLE_MEDIA_TYPE.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.WARN)).contains(ErrorMessages.UNACCEPTABLE_MEDIA_TYPE.getMessage());
  }

  @Test
  void handleIllegalArgumentException_shouldReturnBadRequest() {
    var ex = new IllegalArgumentException("Invalid argument");

    ResponseEntity<RestError> response = underTest.handleIllegalArgumentException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ex.getMessage() /* ErrorMessages.INVALID_INPUT_PROVIDED.getMessage() */);

    // Verify logging
    assertThat(logs.logs(Level.WARN)).contains(ErrorMessages.INVALID_INPUT_PROVIDED.getMessage());
  }

  @Test
  void handleBindException_shouldReturnBadRequest() {
    var ex = new BindException(new Object(), "target");
    ex.addError(new FieldError("target", "field", "Field error"));

    ResponseEntity<RestError> response = underTest.handleBindException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).contains("Value {} for field field was rejected. Error: Field error.");

    // Verify logging
    assertThat(logs.logs(Level.INFO)).anyMatch(log -> log.startsWith(ErrorMessages.BIND_ERROR.getMessage()));
  }

  @ParameterizedTest
  @MethodSource("badRequestsProvider")
  void handleBadRequests_shouldReturnBadRequest(Exception ex) {
    ResponseEntity<RestError> response = underTest.handleBadRequests(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.BAD_REQUEST.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.WARN)).contains(ErrorMessages.BAD_REQUEST.getMessage());
  }

  static Stream<Arguments> badRequestsProvider() {
    return Stream.of(
      Arguments.of(new NoSuchElementException("Element not found")),
      Arguments.of(new ServletRequestBindingException("Binding error")));
  }

  // endregion client

  // region security

  @Test
  void handleAccessDeniedException_shouldReturnForbidden() {
    var ex = new AccessDeniedException("Access denied");
    ResponseEntity<RestError> response = underTest.handleAccessDeniedException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.ACCESS_DENIED.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.ERROR)).contains(ErrorMessages.ACCESS_DENIED.getMessage());
  }

  @Test
  void handleAuthenticationException_shouldReturnUnauthorized() {
    var ex = AuthenticationException.newBuilder()
      .setSource(AuthenticationEvent.Source.sso())
      .setLogin("mockLogin")
      .setPublicMessage("Authentication failed.")
      .build();
    ResponseEntity<RestError> response = underTest.handleAuthenticationException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.AUTHENTICATION_FAILED.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.WARN)).contains(ErrorMessages.AUTHENTICATION_FAILED.getMessage());
  }

  // endregion security

  // region server

  @Test
  void handleIllegalStateException_shouldReturnInternalServerError() {
    var ex = new IllegalStateException();
    ResponseEntity<RestError> response = underTest.handleIllegalStateException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ex.getMessage() /* ErrorMessages.INVALID_STATE.getMessage() */);

    // Verify logging
    assertThat(logs.logs(Level.ERROR)).contains(ErrorMessages.INVALID_STATE.getMessage());
  }

  @ParameterizedTest
  @MethodSource("resourceNotFoundExceptionProvider")
  void handleResourceNotFoundException_shouldReturnNotFound(Exception ex) {
    ResponseEntity<RestError> response = underTest.handleResourceNotFoundException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.RESOURCE_NOT_FOUND.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.ERROR)).contains(ErrorMessages.RESOURCE_NOT_FOUND.getMessage());
  }

  static Stream<Arguments> resourceNotFoundExceptionProvider() {
    return Stream.of(
      Arguments.of(new FileNotFoundException("File not found")),
      Arguments.of(new NoHandlerFoundException("GET", "URL", HttpHeaders.EMPTY)));
  }

  @Test
  void handleAsyncRequestTimeoutException_shouldReturnServiceUnavailable() {
    var ex = new AsyncRequestTimeoutException();
    ResponseEntity<RestError> response = underTest.handleAsyncRequestTimeoutException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.REQUEST_TIMEOUT.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.ERROR)).contains(ErrorMessages.REQUEST_TIMEOUT.getMessage());
  }

  @Test
  void handleMaxUploadSizeExceededException_shouldReturnPayloadTooLarge() {
    var ex = new MaxUploadSizeExceededException(0);
    ResponseEntity<RestError> response = underTest.handleMaxUploadSizeExceededException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.SIZE_EXCEEDED.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.WARN)).contains(ErrorMessages.SIZE_EXCEEDED.getMessage());
  }

  @Test
  void handleTooManyRequestsException_shouldReturnCorrectHttpStatus(){
    var ex = new TooManyRequestsException("Too many requests");
    ResponseEntity<RestError> response = underTest.handleTooManyRequestsException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ex.getMessage());
  }

  @ParameterizedTest
  @MethodSource("serverExceptionsProvider")
  void handleServerException_shouldReturnCorrectHttpStatus(ServerException ex, HttpStatus expectedStatus) {
    ResponseEntity<RestError> response = underTest.handleServerException(ex);

    assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ex.getMessage());
    assertThat(response.getBody().relatedField()).isNull();
  }

  static Stream<Arguments> serverExceptionsProvider() {
    return Stream.of(
      Arguments.of(new BadConfigurationException("Scope", "Bad config"), HttpStatus.BAD_REQUEST),
      Arguments.of(BadRequestException.create("Bad request"), HttpStatus.BAD_REQUEST),
      Arguments.of(new ForbiddenException("Access forbidden"), HttpStatus.FORBIDDEN),
      Arguments.of(new NotFoundException("Not found"), HttpStatus.NOT_FOUND),
      Arguments.of(new TemplateMatchingKeyException("Template matching error"), HttpStatus.BAD_REQUEST),
      Arguments.of(new UnauthorizedException("Unauthorized access"), HttpStatus.UNAUTHORIZED));
  }

  @Test
  void handleUnhandledException_shouldReturnInternalServerError() {
    var ex = new Exception("Some unexpected error");
    ResponseEntity<RestError> response = underTest.handleUnhandledException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo(ErrorMessages.UNEXPECTED_ERROR.getMessage());

    // Verify logging
    assertThat(logs.logs(Level.ERROR)).contains(ErrorMessages.UNEXPECTED_ERROR.getMessage());
  }

  @Test
  void handleBadRequestException_shouldReturnRelatedField_whenItIsProvided() {
    ResponseEntity<RestError> response = underTest.handleBadRequestException(BadRequestException.createWithRelatedField("Bad request message", "related field"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Bad request message");
    assertThat(response.getBody().relatedField()).isEqualTo("related field");
  }

  // endregion server

}
