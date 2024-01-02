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
package org.sonar.auth.saml;

import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;

@ServerSide
public class SamlIdentityProvider implements OAuth2IdentityProvider {

  private static final Pattern HTTPS_PATTERN = Pattern.compile("https?://");
  public static final String KEY = "saml";

  public static final String RSA_SHA_256_URL = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

  private final SamlSettings samlSettings;
  private final SamlMessageIdChecker samlMessageIdChecker;

  public SamlIdentityProvider(SamlSettings samlSettings, SamlMessageIdChecker samlMessageIdChecker) {
    this.samlSettings = samlSettings;
    this.samlMessageIdChecker = samlMessageIdChecker;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public String getName() {
    return samlSettings.getProviderName();
  }

  @Override
  public Display getDisplay() {
    return Display.builder()
      .setIconPath("/images/saml.png")
      .setBackgroundColor("#444444")
      .build();
  }

  @Override
  public boolean isEnabled() {
    return samlSettings.isEnabled();
  }

  @Override
  public boolean allowsUsersToSignUp() {
    return true;
  }

  @Override
  public void init(InitContext context) {
    SamlAuthenticator samlAuthenticator = new SamlAuthenticator(samlSettings, samlMessageIdChecker);
    samlAuthenticator.initLogin(context.getCallbackUrl(), context.generateCsrfState(), context.getRequest(), context.getResponse());
  }

  @Override
  public void callback(CallbackContext context) {
    //
    // Workaround for onelogin/java-saml validation not taking into account running a reverse proxy configuration. This change
    // makes the validation take into account 'X-Forwarded-Proto' and 'Host' headers set by the reverse proxy
    // More details here:
    // - https://github.com/onelogin/java-saml/issues/198
    // - https://github.com/onelogin/java-saml/issues/95
    //
    HttpServletRequest processedRequest = useProxyHeadersInRequest(context.getRequest());

    SamlAuthenticator samlAuthenticator = new SamlAuthenticator(samlSettings, samlMessageIdChecker);
    UserIdentity userIdentity = samlAuthenticator.buildUserIdentity(context, processedRequest);
    context.authenticate(userIdentity);
    context.redirectToRequestedPage();

  }

  private static HttpServletRequest useProxyHeadersInRequest(HttpServletRequest request) {
    String forwardedScheme = request.getHeader("X-Forwarded-Proto");
    if (forwardedScheme != null) {
      request = new HttpServletRequestWrapper(request) {
        @Override
        public String getScheme() {
          return forwardedScheme;
        }

        @Override
        public StringBuffer getRequestURL() {
          StringBuffer originalURL = ((HttpServletRequest) getRequest()).getRequestURL();
          return new StringBuffer(HTTPS_PATTERN.matcher(originalURL.toString()).replaceFirst(forwardedScheme + "://"));
        }
      };
    }

    return request;
  }
}
