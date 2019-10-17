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
package org.sonar.auth.saml;

import com.onelogin.saml2.Auth;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

@ServerSide
public class SamlIdentityProvider implements OAuth2IdentityProvider {

  private static final String KEY = "saml";

  private static final Logger LOGGER = Loggers.get(SamlIdentityProvider.class);

  private static final String ANY_URL = "http://anyurl";
  private static final String STATE_REQUEST_PARAMETER = "RelayState";

  private final SamlSettings samlSettings;

  public SamlIdentityProvider(SamlSettings samlSettings) {
    this.samlSettings = samlSettings;
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
    try {
      Auth auth = newAuth(initSettings(context.getCallbackUrl()), context.getRequest(), context.getResponse());
      auth.login(context.generateCsrfState());
    } catch (IOException | SettingsException e) {
      throw new IllegalStateException("Fail to intialize SAML authentication plugin", e);
    }
  }

  @Override
  public void callback(CallbackContext context) {
    Auth auth = newAuth(initSettings(null), context.getRequest(), context.getResponse());
    processResponse(auth);
    context.verifyCsrfState(STATE_REQUEST_PARAMETER);

    LOGGER.trace("Name ID : {}", auth.getNameId());
    checkAuthentication(auth);

    LOGGER.trace("Attributes received : {}", auth.getAttributes());
    String login = getNonNullFirstAttribute(auth, samlSettings.getUserLogin());
    UserIdentity.Builder userIdentityBuilder = UserIdentity.builder()
      .setProviderLogin(login)
      .setName(getNonNullFirstAttribute(auth, samlSettings.getUserName()));
    samlSettings.getUserEmail().ifPresent(
      email -> userIdentityBuilder.setEmail(getFirstAttribute(auth, email)));
    samlSettings.getGroupName().ifPresent(
      group -> userIdentityBuilder.setGroups(getGroups(auth, group)));
    context.authenticate(userIdentityBuilder.build());
    context.redirectToRequestedPage();
  }

  private static Auth newAuth(Saml2Settings saml2Settings, HttpServletRequest request, HttpServletResponse response) {
    try {
      return new Auth(saml2Settings, request, response);
    } catch (SettingsException e) {
      throw new IllegalStateException("Fail to create Auth", e);
    }
  }

  private static void processResponse(Auth auth) {
    try {
      auth.processResponse();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to process response", e);
    }
  }

  private static void checkAuthentication(Auth auth) {
    List<String> errors = auth.getErrors();
    if (auth.isAuthenticated() && errors.isEmpty()) {
      return;
    }
    String errorReason = auth.getLastErrorReason();
    throw new UnauthorizedException(errorReason != null && !errorReason.isEmpty() ? errorReason : "Unknown error reason");
  }

  private static String getNonNullFirstAttribute(Auth auth, String key) {
    String attribute = getFirstAttribute(auth, key);
    requireNonNull(attribute, String.format("%s is missing", key));
    return attribute;
  }

  @CheckForNull
  private static String getFirstAttribute(Auth auth, String key) {
    Collection<String> attribute = auth.getAttribute(key);
    if (attribute == null || attribute.isEmpty()) {
      return null;
    }
    return attribute.iterator().next();
  }

  private static Set<String> getGroups(Auth auth, String groupAttribute) {
    Collection<String> attribute = auth.getAttribute(groupAttribute);
    if (attribute == null || attribute.isEmpty()) {
      return emptySet();
    }
    return new HashSet<>(attribute);
  }

  private Saml2Settings initSettings(@Nullable String callbackUrl) {
    Map<String, Object> samlData = new HashMap<>();
    // TODO strict mode is unfortunately not compatible with HTTPS configuration on reverse proxy =>
    // https://jira.sonarsource.com/browse/SQAUTHSAML-8
    samlData.put("onelogin.saml2.strict", false);

    samlData.put("onelogin.saml2.idp.entityid", samlSettings.getProviderId());
    samlData.put("onelogin.saml2.idp.single_sign_on_service.url", samlSettings.getLoginUrl());
    samlData.put("onelogin.saml2.idp.x509cert", samlSettings.getCertificate());

    samlData.put("onelogin.saml2.sp.entityid", samlSettings.getApplicationId());
    // During callback, the callback URL is by definition not needed, but the Saml2Settings does never allow this setting to be empty...
    samlData.put("onelogin.saml2.sp.assertion_consumer_service.url", callbackUrl != null ? callbackUrl : ANY_URL);
    SettingsBuilder builder = new SettingsBuilder();
    return builder
      .fromValues(samlData)
      .build();
  }
}
