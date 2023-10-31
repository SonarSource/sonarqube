/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.server.http.JavaxHttpRequest;
import org.sonar.server.http.JavaxHttpResponse;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static org.sonar.auth.saml.SamlAuthStatusPageGenerator.getSamlAuthStatusHtml;
import static org.sonar.auth.saml.SamlIdentityProvider.RSA_SHA_256_URL;
import static org.sonar.auth.saml.SamlStatusChecker.getSamlAuthenticationStatus;

public class SamlAuthenticator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SamlAuthenticator.class);

  private static final String ANY_URL = "http://anyurl";
  private static final String STATE_REQUEST_PARAMETER = "RelayState";

  private final SamlSettings samlSettings;
  private final SamlMessageIdChecker samlMessageIdChecker;

  public SamlAuthenticator(SamlSettings samlSettings, SamlMessageIdChecker samlMessageIdChecker) {
    this.samlSettings = samlSettings;
    this.samlMessageIdChecker = samlMessageIdChecker;
  }

  public UserIdentity buildUserIdentity(OAuth2IdentityProvider.CallbackContext context, HttpRequest processedRequest) {
    Auth auth = this.initSamlAuth(processedRequest, context.getHttpResponse());
    processResponse(auth);
    context.verifyCsrfState(STATE_REQUEST_PARAMETER);

    LOGGER.trace("Name ID : {}", getNameId(auth));
    checkAuthentication(auth);
    this.checkMessageId(auth);


    LOGGER.trace("Attributes received : {}", getAttributes(auth));
    String login = this.getLogin(auth);
    UserIdentity.Builder userIdentityBuilder = UserIdentity.builder()
      .setProviderLogin(login)
      .setName(this.getName(auth));
    this.getEmail(auth).ifPresent(userIdentityBuilder::setEmail);
    this.getGroups(auth).ifPresent(userIdentityBuilder::setGroups);

    return userIdentityBuilder.build();
  }

  public void initLogin(String callbackUrl, String relayState, HttpRequest request, HttpResponse response) {
    Auth auth = this.initSamlAuth(callbackUrl, request, response);
    login(auth, relayState);
  }

  private Auth initSamlAuth(HttpRequest request, HttpResponse response) {
    return initSamlAuth(null, request, response);
  }

  private Auth initSamlAuth(@Nullable String callbackUrl, HttpRequest request, HttpResponse response) {
    try {
      //no way around this as onelogin requires javax request/response
      HttpServletRequest httpServletRequest = ((JavaxHttpRequest) request).getDelegate();
      HttpServletResponse httpServletResponse = ((JavaxHttpResponse) response).getDelegate();

      return new Auth(initSettings(callbackUrl), httpServletRequest, httpServletResponse);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create a SAML Auth", e);
    }
  }

  private Saml2Settings initSettings(@Nullable String callbackUrl) {
    Map<String, Object> samlData = new HashMap<>();
    samlData.put("onelogin.saml2.strict", true);

    samlData.put("onelogin.saml2.idp.entityid", samlSettings.getProviderId());
    samlData.put("onelogin.saml2.idp.single_sign_on_service.url", samlSettings.getLoginUrl());
    samlData.put("onelogin.saml2.idp.x509cert", samlSettings.getCertificate());

    // Service Provider configuration
    samlData.put("onelogin.saml2.sp.entityid", samlSettings.getApplicationId());
    if (samlSettings.isSignRequestsEnabled()) {
      samlData.put("onelogin.saml2.security.authnrequest_signed", true);
      samlData.put("onelogin.saml2.security.logoutrequest_signed", true);
      samlData.put("onelogin.saml2.security.logoutresponse_signed", true);
      samlData.put("onelogin.saml2.sp.x509cert", samlSettings.getServiceProviderCertificate());
      samlData.put("onelogin.saml2.sp.privatekey",
        samlSettings.getServiceProviderPrivateKey().orElseThrow(() -> new IllegalArgumentException("Service provider private key is missing")));
    } else {
      samlSettings.getServiceProviderPrivateKey().ifPresent(privateKey -> samlData.put("onelogin.saml2.sp.privatekey", privateKey));
    }
    samlData.put("onelogin.saml2.security.signature_algorithm", RSA_SHA_256_URL);

    // During callback, the callback URL is by definition not needed, but the Saml2Settings does never allow this setting to be empty...
    samlData.put("onelogin.saml2.sp.assertion_consumer_service.url", callbackUrl != null ? callbackUrl : ANY_URL);

    var saml2Settings = new SettingsBuilder().fromValues(samlData).build();
    if (samlSettings.getServiceProviderPrivateKey().isPresent() && saml2Settings.getSPkey() == null) {
      final String pkcs8ErrorMessage = "Error in parsing service provider private key, please make sure that it is in PKCS 8 format.";
      LOGGER.error(pkcs8ErrorMessage);
      // If signature is enabled then we need to throw an exception because the authentication will never work with a missing private key
      if (samlSettings.isSignRequestsEnabled()) {
        throw new IllegalStateException(pkcs8ErrorMessage);
      }
    }
    return saml2Settings;
  }

  private static void login(Auth auth, String relayState) {
    try {
      auth.login(relayState);
    } catch (IOException | SettingsException e) {
      throw new IllegalStateException("Failed to initialize SAML authentication plugin", e);
    }
  }

  private static void processResponse(Auth auth) {
    try {
      auth.processResponse();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to process the authentication response", e);
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

  private String getLogin(Auth auth) {
    return getNonNullFirstAttribute(auth, samlSettings.getUserLogin());
  }

  private String getName(Auth auth) {
    return getNonNullFirstAttribute(auth, samlSettings.getUserName());
  }

  private Optional<String> getEmail(Auth auth) {
    return samlSettings.getUserEmail().map(userEmailField -> getFirstAttribute(auth, userEmailField));
  }

  private Optional<Set<String>> getGroups(Auth auth) {
    return samlSettings.getGroupName().map(groupsField -> getGroups(auth, groupsField));
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

  private static String getNameId(Auth auth) {
    return auth.getNameId();
  }

  private static Map<String, List<String>> getAttributes(Auth auth) {
    return auth.getAttributes();
  }

  private void checkMessageId(Auth auth) {
    samlMessageIdChecker.check(auth);
  }

  public String getAuthenticationStatusPage(HttpRequest request, HttpResponse response) {
    try {
      Auth auth = initSamlAuth(request, response);
      return getSamlAuthStatusHtml(request, getSamlAuthenticationStatus(auth, samlSettings));
    } catch (IllegalStateException e) {
      return getSamlAuthStatusHtml(request, getSamlAuthenticationStatus(String.format("%s due to: %s", e.getMessage(), e.getCause().getMessage())));
    }
  }
}
