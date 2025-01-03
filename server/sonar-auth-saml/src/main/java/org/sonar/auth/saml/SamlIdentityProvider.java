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
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;

@ServerSide
public class SamlIdentityProvider implements OAuth2IdentityProvider {

  private static final Pattern HTTPS_PATTERN = Pattern.compile("https?://");
  public static final String KEY = "saml";

  public static final String RSA_SHA_256_URL = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

  private final SamlAuthenticator samlAuthenticator;
  private final SamlSettings samlSettings;

  public SamlIdentityProvider(SamlSettings samlSettings, SamlAuthenticator samlAuthenticator) {
    this.samlSettings = samlSettings;
    this.samlAuthenticator = samlAuthenticator;
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
    samlAuthenticator.initLogin(context.getCallbackUrl(), context.generateCsrfState(),
      context.getHttpRequest(), context.getHttpResponse());
  }

  @Override
  public void callback(CallbackContext context) {

    UserIdentity userIdentity = samlAuthenticator.onCallback(context, context.getHttpRequest());
    context.authenticate(userIdentity);
    context.redirectToRequestedPage();

  }

}
