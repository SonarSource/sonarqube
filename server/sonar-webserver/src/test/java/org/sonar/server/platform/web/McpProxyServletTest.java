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
package org.sonar.server.platform.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.server.platform.McpRequestHandler;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class McpProxyServletTest {

  @Test
  void delegates_to_handler_when_present() throws Exception {
    McpRequestHandler handler = mock(McpRequestHandler.class);
    McpProxyServlet servlet = new McpProxyServlet(() -> Optional.of(handler));

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/tools/list");
    MockHttpServletResponse resp = new MockHttpServletResponse();

    servlet.service(req, resp);

    verify(handler).handle(req, resp);
  }

  @Test
  void returns_404_when_no_handler() throws Exception {
    McpProxyServlet servlet = new McpProxyServlet(Optional::empty);

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp");
    MockHttpServletResponse resp = new MockHttpServletResponse();

    servlet.service(req, resp);

    assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  void propagates_io_exception_from_handler() throws Exception {
    McpRequestHandler handler = mock(McpRequestHandler.class);
    org.mockito.Mockito.doThrow(new IOException("handler error")).when(handler).handle(
      org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    McpProxyServlet servlet = new McpProxyServlet(() -> Optional.of(handler));

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp");
    MockHttpServletResponse resp = new MockHttpServletResponse();

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> servlet.service(req, resp))
      .isInstanceOf(IOException.class)
      .hasMessage("handler error");
  }
}
