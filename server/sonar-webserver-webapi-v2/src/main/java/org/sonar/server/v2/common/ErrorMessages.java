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

enum ErrorMessages {
  ACCESS_DENIED("Access denied."),
  AUTHENTICATION_FAILED("Authentication failed."),
  BAD_REQUEST("Bad request."),
  BIND_ERROR("Bind error."),
  CONVERSION_FAILED("Failed to convert value."),
  INTERNAL_SERVER_ERROR("An internal server error occurred."),
  INVALID_INPUT_PROVIDED("Invalid input provided."),
  INVALID_PARAMETER_TYPE("Invalid parameter type."),
  INVALID_REQUEST_FORMAT("Invalid request format."),
  INVALID_STATE("Invalid state."),
  METHOD_NOT_SUPPORTED("Method not supported."),
  RESOURCE_NOT_FOUND("Resource not found."),
  REQUEST_TIMEOUT("The request timed out."),
  SIZE_EXCEEDED("The uploaded file exceeds the maximum allowed size."),
  UNEXPECTED_ERROR("An unexpected error occurred. Please contact support if the problem persists."),
  UNACCEPTABLE_MEDIA_TYPE("The requested media type is not acceptable."),
  UNSUPPORTED_MEDIA_TYPE("Unsupported media type."),
  VALIDATION_ERROR("Validation error. Please check your input."),
  ;

  private final String message;

  ErrorMessages(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
