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
package org.sonar.server.authentication;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.internal.apachecommons.lang.StringEscapeUtils;
import org.sonar.api.platform.Server;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.HttpFilter;
import org.sonar.api.web.UrlPattern;

import static org.sonar.server.authentication.AuthenticationFilter.CALLBACK_PATH;

public class SamlValidationRedirectionFilter extends HttpFilter {

  public static final String VALIDATION_RELAY_STATE = "validation-query";
  public static final String SAML_VALIDATION_CONTROLLER_CONTEXT = "saml";
  public static final String SAML_VALIDATION_KEY = "validation";
  private static final String RELAY_STATE_PARAMETER = "RelayState";
  private static final String SAML_RESPONSE_PARAMETER = "SAMLResponse";
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
  public void init() {
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
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException {
    String relayState = request.getParameter(RELAY_STATE_PARAMETER);

    if (isSamlValidation(relayState)) {

      URI redirectionEndpointUrl = URI.create(server.getContextPath() + "/")
        .resolve(SAML_VALIDATION_CONTROLLER_CONTEXT + "/")
        .resolve(SAML_VALIDATION_KEY);
      String samlResponse = StringEscapeUtils.escapeHtml(request.getParameter(SAML_RESPONSE_PARAMETER));
      String csrfToken = getCsrfTokenFromRelayState(relayState);

      String nonce = SamlValidationCspHeaders.addCspHeadersWithNonceToResponse(response);

      String template = StringUtils.replaceEachRepeatedly(redirectionPageTemplate,
        new String[]{"%NONCE%", "%WEB_CONTEXT%", "%VALIDATION_URL%", "%SAML_RESPONSE%", "%CSRF_TOKEN%"},
        new String[]{nonce, server.getContextPath(), redirectionEndpointUrl.toString(), samlResponse, csrfToken});

      response.setContentType("text/html");
      response.getWriter().print(template);
      return;
    }
    chain.doFilter(request, response);
  }

  private static boolean isSamlValidation(@Nullable String relayState) {
    if (relayState != null) {
      return VALIDATION_RELAY_STATE.equals(relayState.split("/")[0]) && !getCsrfTokenFromRelayState(relayState).isEmpty();
    }
    return false;
  }

  private static String getCsrfTokenFromRelayState(@Nullable String relayState) {
    if (relayState != null && relayState.contains("/")) {
      return StringEscapeUtils.escapeHtml(relayState.split("/")[1]);
    }
    return "";
  }
}
