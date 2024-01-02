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

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.Test;
import org.sonar.server.v2.api.model.RestError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestResponseEntityExceptionHandlerTest {

  private RestResponseEntityExceptionHandler underTest = new RestResponseEntityExceptionHandler();

  @Test
  public void handleHttpMessageNotReadableException_whenCauseIsNotInvalidFormatException_shouldUseMessage() {

    HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Message not readable", new Exception());

    ResponseEntity<RestError> responseEntity = underTest.handleHttpMessageNotReadableException(exception);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity.getBody().message()).isEqualTo("Message not readable; nested exception is java.lang.Exception");
  }

  @Test
  public void handleHttpMessageNotReadableException_whenCauseIsNotInvalidFormatExceptionAndMessageIsNull_shouldUseEmptyStringAsMessage() {

    HttpMessageNotReadableException exception = new HttpMessageNotReadableException(null, (Exception) null);

    ResponseEntity<RestError> responseEntity = underTest.handleHttpMessageNotReadableException(exception);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity.getBody().message()).isEmpty();
  }

  @Test
  public void handleHttpMessageNotReadableException_whenCauseIsInvalidFormatException_shouldUseMessageFromCause() {

    InvalidFormatException cause = mock(InvalidFormatException.class);
    when(cause.getOriginalMessage()).thenReturn("Cause message");

    HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Message not readable", cause);

    ResponseEntity<RestError> responseEntity = underTest.handleHttpMessageNotReadableException(exception);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity.getBody().message()).isEqualTo("Cause message");
  }
}
