/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

package org.sonar.server.authentication;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.internal.apachecommons.lang.StringEscapeUtils;
import org.sonar.api.platform.Server;
import org.sonar.api.web.ServletFilter;

import static org.sonar.server.authentication.AuthenticationFilter.CALLBACK_PATH;

public class SamlValidationRedirectionFilter extends ServletFilter {

  public static final String VALIDATION_RELAY_STATE = "validation-query";
  public static final String SAML_VALIDATION_CONTROLLER_CONTEXT = "saml";
  public static final String SAML_VALIDATION_KEY = "validation";
  private String redirectionPageTemplate;
  private final Server server;

  public SamlValidationRedirectionFilter(Server server) {
    this.server = server;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(CALLBACK_PATH + "saml");
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    super.init(filterConfig);
    this.redirectionPageTemplate = extractTemplate("validation-redirection.html");
  }

  String extractTemplate(String templateLocation) {
    try {
      URL url = Resources.getResource(templateLocation);
      return Resources.toString(url, StandardCharsets.UTF_8);
    } catch (IOException | IllegalArgumentException e) {
      throw new IllegalStateException("Cannot read the template " + templateLocation, e);
    }
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    if (isSamlValidation(httpRequest)) {
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      String samlResponse = StringEscapeUtils.escapeHtml(request.getParameter("SAMLResponse"));
      URI redirectionEndpointUrl = URI.create(server.getContextPath() + "/")
        .resolve(SAML_VALIDATION_CONTROLLER_CONTEXT + "/")
        .resolve(SAML_VALIDATION_KEY);

      String template = StringUtils.replaceEachRepeatedly(redirectionPageTemplate,
        new String[]{"%VALIDATION_URL%", "%SAML_RESPONSE%"},
        new String[]{redirectionEndpointUrl.toString(), samlResponse});

      httpResponse.setContentType("text/html");
      httpResponse.getWriter().print(template);
      return;
    }
    chain.doFilter(request, response);
  }

  private static boolean isSamlValidation(HttpServletRequest request) {
    return VALIDATION_RELAY_STATE.equals(request.getParameter("RelayState"));
  }
}
