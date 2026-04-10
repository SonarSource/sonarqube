/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.platform.McpRequestHandler;

public class McpProxyServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(McpProxyServlet.class);

  @SuppressWarnings("java:S1948")
  private final transient Supplier<Optional<McpRequestHandler>> handlerSupplier;

  public McpProxyServlet(Supplier<Optional<McpRequestHandler>> handlerSupplier) {
    this.handlerSupplier = handlerSupplier;
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    McpRequestHandler handler = handlerSupplier.get().orElse(null);
    if (handler == null) {
      LOG.debug("MCP proxy is not available in this edition of SonarQube");
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      resp.setContentType("application/json");
      resp.getWriter().write("{\"error\":\"The MCP proxy is not available in this edition of SonarQube\"}");
      return;
    }
    handler.handle(req, resp);
  }
}
