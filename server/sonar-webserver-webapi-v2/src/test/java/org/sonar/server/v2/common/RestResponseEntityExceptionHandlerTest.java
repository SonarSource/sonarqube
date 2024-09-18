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

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.server.v2.api.model.RestError;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class RestResponseEntityExceptionHandlerTest {

  private final RestResponseEntityExceptionHandler underTest = new RestResponseEntityExceptionHandler();

  @ParameterizedTest
  @MethodSource("clientSideExceptionsProvider")
  void handleClientSideException_shouldUseGenericMessage(Exception exception) {

    ResponseEntity<RestError> responseEntity = underTest.handleClientSideException(exception);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity.getBody().message()).isEqualTo("An error occurred while processing the request.");
  }

  private static Stream<Arguments> clientSideExceptionsProvider() {
    return Stream.of(
      Arguments.of(new HttpMessageNotReadableException("", (HttpInputMessage) null)),
      Arguments.of(new MethodArgumentTypeMismatchException(null, null, null, null, null)),
      Arguments.of(new HttpRequestMethodNotSupportedException("")),
      Arguments.of(new ServletRequestBindingException("")),
      Arguments.of(new NoHandlerFoundException("", null, null)));
  }

  @Test
  void handleUnhandledException_shouldUseGenericMessage() {

    Exception exception = new Exception();

    ResponseEntity<RestError> responseEntity = underTest.handleUnhandledException(exception);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(responseEntity.getBody().message()).isEqualTo("An unexpected error occurred. Please contact support if the problem persists.");
  }

}
