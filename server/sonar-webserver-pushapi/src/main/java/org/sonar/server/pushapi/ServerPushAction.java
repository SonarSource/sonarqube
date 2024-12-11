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
package org.sonar.server.pushapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.sonar.server.ws.ServletRequest;
import org.sonar.server.ws.ServletResponse;
import org.sonar.server.ws.WsAction;

public abstract class ServerPushAction implements WsAction {

  protected boolean isServerSideEventsRequest(ServletRequest request) {
    Map<String, String> headers = request.getHeaders();
    String accept = headers.get("accept");
    if (accept != null) {
      return accept.contains("text/event-stream");
    }
    return false;
  }

  protected void setHeadersForResponse(ServletResponse response) throws IOException {
    response.stream().setStatus(HttpServletResponse.SC_OK);
    response.stream().setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.stream().setMediaType("text/event-stream");
    // By adding this header, and not closing the connection,
    // we disable HTTP chunking, and we can use write()+flush()
    // to send data in the text/event-stream protocol
    response.setHeader("Connection", "close");
    response.stream().flushBuffer();
  }


}
