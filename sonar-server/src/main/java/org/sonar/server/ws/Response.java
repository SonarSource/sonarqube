/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * HTTP response
 *
 * @since 4.2
 */
public class Response {

  /**
   * HTTP Status-Code 200: OK.
   */
  public static final int HTTP_OK = 200;

  /**
   * HTTP Status-Code 400: Bad Request.
   */
  public static final int HTTP_BAD_REQUEST = 400;

  /**
   * HTTP Status-Code 401: Unauthorized.
   */
  public static final int HTTP_UNAUTHORIZED = 401;

  /**
   * HTTP Status-Code 403: Forbidden.
   */
  public static final int HTTP_FORBIDDEN = 403;

  /**
   * HTTP Status-Code 404: Not Found.
   */
  public static final int HTTP_NOT_FOUND = 404;

  /**
   * HTTP Status-Code 500: Internal Server Error.
   */
  public static final int HTTP_INTERNAL_ERROR = 500;


  private int httpStatus = HTTP_OK;
  private String body;

  @CheckForNull
  public String body() {
    return body;
  }

  public Response setBody(@Nullable String body) {
    this.body = body;
    return this;
  }

  public int status() {
    return httpStatus;
  }

  public Response setStatus(int httpStatus) {
    this.httpStatus = httpStatus;
    return this;
  }
}
