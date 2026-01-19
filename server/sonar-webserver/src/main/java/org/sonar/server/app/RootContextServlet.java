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
package org.sonar.server.app;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for the ROOT context that redirects to the main webapp to show the React 404 page.
 * This handles requests that fall outside the configured web context path.
 */
public class RootContextServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(RootContextServlet.class);

  @Serial
  private static final long serialVersionUID = 1L;
  private static final String NOT_FOUND = "/not-found";

  private String webContext = "";

  public RootContextServlet() {
    // Default constructor with default value for webContext
  }

  @Override
  public void init(ServletConfig config) {
    webContext = config.getInitParameter("webContext");
    if (webContext == null || webContext.isEmpty()) {
      webContext = "";
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Redirect to a non-existent page in the main webapp context
    // This will be handled by the React app and show the proper 404 page
    try {
      String redirectUrl = webContext + NOT_FOUND;
      response.sendRedirect(redirectUrl);
    } catch (IOException e) {
      LOG.error("Failed to redirect to not-found page", e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      doGet(request, response);
    } catch (IOException e) {
      LOG.error("Failed to handle POST request", e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doHead(HttpServletRequest request, HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
  }
}
