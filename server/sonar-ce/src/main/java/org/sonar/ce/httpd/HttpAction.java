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
package org.sonar.ce.httpd;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

/**
 * A Http action of the CE's HTTP server handles a request for a specified path.
 */
public interface HttpAction extends HttpRequestHandler {
  /**
   * Provides a context path to be registered on.
   * It must not be empty and start with a '/'.
   * @return the context path as a String
   */
  String getContextPath();

  void handle(HttpRequest request, HttpResponse response);

  default void handle(HttpRequest request, HttpResponse response, HttpContext context)
    throws HttpException {
    try {
      this.handle(request, response);
      // catch Throwable because we want to respond a clean 500 to client even on Error
    } catch (Throwable t) {
      throw new HttpException(t.getMessage(), t);
    }
  }
}
