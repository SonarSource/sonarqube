/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.v2.api.model.RestError;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class ServerRestResponseEntityExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerRestResponseEntityExceptionHandler.class);

  @ExceptionHandler(HandlerMethodValidationException.class)
  protected ResponseEntity<RestError> handleHandlerMethodValidationException(HandlerMethodValidationException exception) {
    String message = Stream.concat(
        exception.getParameterValidationResults().stream()
          .flatMap(result -> result.getResolvableErrors().stream()
            .map(error -> formatValidationError(result.getMethodParameter(), error))),
        exception.getCrossParameterValidationResults().stream()
          .map(ServerRestResponseEntityExceptionHandler::formatValidationError))
      .flatMap(Optional::stream)
      .distinct()
      .sorted()
      .collect(Collectors.joining(", "));
    if (message.isEmpty()) {
      message = ErrorMessages.VALIDATION_ERROR.getMessage();
    }
    LOGGER.info("{}\n{}", ErrorMessages.VALIDATION_ERROR.getMessage(), message);
    return ResponseEntity.badRequest().body(new RestError(message));
  }

  private static Optional<String> formatValidationError(MethodParameter methodParameter, MessageSourceResolvable error) {
    return Optional.ofNullable(error.getDefaultMessage())
      .map(message -> {
        String parameterName = getParameterName(methodParameter);
        if (error instanceof FieldError fieldError) {
          parameterName += "." + fieldError.getField();
        }
        return parameterName + ": " + message;
      });
  }

  private static Optional<String> formatValidationError(MessageSourceResolvable error) {
    return Optional.ofNullable(error.getDefaultMessage());
  }

  private static String getParameterName(MethodParameter methodParameter) {
    RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
    if (requestParam != null) {
      if (!requestParam.name().isEmpty()) {
        return requestParam.name();
      }
      if (!requestParam.value().isEmpty()) {
        return requestParam.value();
      }
    }
    return Optional.ofNullable(methodParameter.getParameterName())
      .orElse("parameter " + methodParameter.getParameterIndex());
  }

  @ExceptionHandler(AuthenticationException.class)
  protected ResponseEntity<RestError> handleAuthenticationException(AuthenticationException ex) {
    LOGGER.warn(ex.getPublicMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new RestError(ErrorMessages.AUTHENTICATION_FAILED.getMessage()));
  }
}
