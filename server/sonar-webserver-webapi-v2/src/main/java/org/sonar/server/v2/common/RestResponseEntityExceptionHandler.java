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

import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.v2.api.model.RestError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class RestResponseEntityExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

  @ExceptionHandler({IllegalStateException.class})
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  protected ResponseEntity<RestError> handleIllegalStateException(IllegalStateException illegalStateException) {
    return new ResponseEntity<>(new RestError(illegalStateException.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler({IllegalArgumentException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  protected ResponseEntity<RestError> handleIllegalArgumentException(IllegalArgumentException illegalArgumentException) {
    return new ResponseEntity<>(new RestError(illegalArgumentException.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(BindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  protected ResponseEntity<RestError> handleBindException(BindException bindException) {
    String validationErrors = bindException.getFieldErrors().stream()
      .map(RestResponseEntityExceptionHandler::handleFieldError)
      .collect(Collectors.joining());
    return new ResponseEntity<>(new RestError(validationErrors), HttpStatus.BAD_REQUEST);
  }

  private static String handleFieldError(FieldError fieldError) {
    String fieldName = fieldError.getField();
    String rejectedValueAsString = Optional.ofNullable(fieldError.getRejectedValue()).map(Object::toString).orElse("{}");
    String defaultMessage = fieldError.getDefaultMessage();
    return String.format("Value %s for field %s was rejected. Error: %s.", rejectedValueAsString, fieldName, defaultMessage);
  }

  @ExceptionHandler({ServerException.class, ForbiddenException.class, UnauthorizedException.class, BadRequestException.class})
  protected ResponseEntity<RestError> handleServerException(ServerException serverException) {
    return new ResponseEntity<>(new RestError(serverException.getMessage()),
      Optional.ofNullable(HttpStatus.resolve(serverException.httpCode())).orElse(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @ExceptionHandler({NotFoundException.class})
  @ResponseStatus(HttpStatus.NOT_FOUND)
  protected ResponseEntity<RestError> handleNotFoundException(NotFoundException notFoundException) {
    return new ResponseEntity<>(new RestError(notFoundException.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    HttpRequestMethodNotSupportedException.class,
    ServletRequestBindingException.class,
    NoHandlerFoundException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  protected ResponseEntity<RestError> handleClientSideException(Exception exception) {
    LOGGER.error("Error processing request", exception);
    return new ResponseEntity<>(new RestError("An error occurred while processing the request."), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({Exception.class})
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  protected ResponseEntity<RestError> handleUnhandledException(Exception exception) {
    LOGGER.error("Unhandled exception", exception);
    return new ResponseEntity<>(new RestError("An unexpected error occurred. Please contact support if the problem persists."), HttpStatus.INTERNAL_SERVER_ERROR);
  }

}
