/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ServerException;

import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;

public class RequestVerifier {
  private RequestVerifier() {
    // static methods only
  }

  public static void verifyRequest(WebService.Action action, Request request) {
    switch (request.method()) {
      case "GET":
        if (action.isPost()) {
          throw new ServerException(SC_METHOD_NOT_ALLOWED, "HTTP method POST is required");
        }
        return;
      case "PUT":
      case "DELETE":
        throw new ServerException(SC_METHOD_NOT_ALLOWED, String.format("HTTP method %s is not allowed", request.method()));
      default:
        // Nothing to do
    }
  }
}
