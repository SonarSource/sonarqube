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
package org.sonar.server.app;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serial;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.authentication.McpOAuthDiscovery;

/**
 * Servlet for the ROOT context, which handles requests that fall outside the configured web context path.
 * <ul>
 *   <li>OAuth authorization server metadata requested at the RFC 8414 location of an issuer with a path component
 *   ({@code /.well-known/oauth-authorization-server/<issuer path>}) is forwarded to the main webapp, which matches the path
 *   against the public issuer. The issuer path can differ from the web context when a reverse proxy rewrites it.</li>
 *   <li>Other {@code /.well-known/} requests get a 404, so that discovery clients can try their next candidate URL.</li>
 *   <li>Any other request is redirected to the main webapp to show the React 404 page.</li>
 * </ul>
 */
public class RootContextServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(RootContextServlet.class);

  @Serial
  private static final long serialVersionUID = 1L;
  private static final String NOT_FOUND = "/not-found";
  private static final String WELL_KNOWN_PREFIX = "/.well-known/";
  /**
   * Matched against the raw request URI: no percent-encoding, path parameters or dot segments are accepted, so the forwarded
   * path cannot resolve outside the authorization server metadata endpoint.
   */
  private static final Pattern AUTHORIZATION_METADATA_PATTERN = Pattern.compile(
    Pattern.quote(McpOAuthDiscovery.AUTHORIZATION_METADATA_PATH) + "(/[A-Za-z0-9_~-][A-Za-z0-9._~-]*)+");

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
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    String path = request.getRequestURI();
    if (isAuthorizationServerMetadata(path)) {
      forwardToWebapp(path, request, response);
    } else if (isWellKnown(path)) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } else {
      redirectToNotFoundPage(response);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }

  @Override
  protected void doHead(HttpServletRequest request, HttpServletResponse response) {
    String path = request.getRequestURI();
    if (isAuthorizationServerMetadata(path)) {
      forwardToWebapp(path, request, response);
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private boolean isAuthorizationServerMetadata(String path) {
    return !webContext.isEmpty() && AUTHORIZATION_METADATA_PATTERN.matcher(path).matches();
  }

  private static boolean isWellKnown(String path) {
    return path.startsWith(WELL_KNOWN_PREFIX);
  }

  private void redirectToNotFoundPage(HttpServletResponse response) {
    try {
      response.sendRedirect(webContext + NOT_FOUND);
    } catch (IOException e) {
      LOG.error("Failed to redirect to not-found page", e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private void forwardToWebapp(String path, HttpServletRequest request, HttpServletResponse response) {
    RequestDispatcher dispatcher = webappDispatcher(request, path);
    if (dispatcher == null) {
      LOG.warn("Failed to forward {} to the web context {}: the web context is not available", path, webContext);
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }
    try {
      dispatcher.forward(request, response);
    } catch (ServletException | IOException e) {
      LOG.error("Failed to forward {} to the web context {}", path, webContext, e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * When the webapp is not running, Tomcat resolves the web context to the closest running context, which is this ROOT
   * context: forwarding there would loop back into this servlet.
   */
  @CheckForNull
  private RequestDispatcher webappDispatcher(HttpServletRequest request, String path) {
    ServletContext webapp = request.getServletContext().getContext(webContext);
    if (webapp == null || !webContext.equals(webapp.getContextPath())) {
      return null;
    }
    return webapp.getRequestDispatcher(path);
  }
}
