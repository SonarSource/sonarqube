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
package org.sonar.server.authentication;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Set;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.http.Cookie;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.server.authentication.event.AuthenticationException;

import static java.lang.String.format;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sonar.server.authentication.Cookies.SAMESITE_LAX;
import static org.sonar.server.authentication.Cookies.SET_COOKIE;
import static org.sonar.server.authentication.Cookies.findCookie;
import static org.sonar.server.authentication.Cookies.newCookieBuilder;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class OAuthCsrfVerifier {

  private static final String CSRF_STATE_COOKIE = "OAUTHSTATE";
  private static final String DEFAULT_STATE_PARAMETER_NAME = "state";

  /**
   * Provider keys known to use a cross-site GET redirect callback, where an explicit SameSite=Lax cookie
   * is correct and safe (this closes SSF-1190 for them).
   *
   * `OAuth2IdentityProvider` is a public plugin extension API with no binding-type metadata, so this MUST
   * be an allowlist of known-safe providers rather than a denylist of known-unsafe ones: any provider key
   * NOT in this set (including third-party plugins we don't control, e.g. an OIDC provider configured with
   * `response_mode=form_post`) falls through to the legacy no-explicit-SameSite cookie below. That legacy
   * cookie still relies on the browser's implicit "Lax+POST" compatibility exception for POST-binding
   * callbacks — the same limitation SAML has (see SONAR-30979) — but that is the pre-existing, working
   * behavior for any provider we don't specifically know is GET-based. Defaulting new/unknown providers to
   * explicit SameSite=Lax instead would silently break any of them that use a POST binding, with no way
   * for an installed plugin to opt out.
   *
   * These literals must stay equal to {@code GitHubIdentityProvider.KEY}, {@code GitLabIdentityProvider.KEY}
   * and {@code BitbucketIdentityProvider.KEY} respectively (kept as literals, not references, to avoid a
   * sonar-webserver-auth -> sonar-auth-{github,gitlab,bitbucket} dependency) — check those if this ever
   * looks out of sync.
   */
  private static final Set<String> GET_BINDING_PROVIDER_KEYS = Set.of("github", "gitlab", "bitbucket");

  public String generateState(HttpRequest request, HttpResponse response, String providerKey) {
    // Create a state token to prevent request forgery.
    // Store it in the session for later validation.
    String state = new BigInteger(130, new SecureRandom()).toString(32);
    if (GET_BINDING_PROVIDER_KEYS.contains(providerKey)) {
      response.addHeader(SET_COOKIE, newCookieBuilder(request)
        .setName(CSRF_STATE_COOKIE)
        .setValue(sha256Hex(state))
        .setHttpOnly(true)
        .setExpiry(-1)
        .setSameSite(SAMESITE_LAX)
        .toValueString());
    } else {
      // Legacy behavior for SAML and any provider not known to be GET-based (see the javadoc above).
      response.addCookie(newCookieBuilder(request).setName(CSRF_STATE_COOKIE).setValue(sha256Hex(state)).setHttpOnly(true).setExpiry(-1).build());
    }
    return state;
  }

  public void verifyState(HttpRequest request, HttpResponse response, OAuth2IdentityProvider provider) {
    verifyState(request, response, provider, DEFAULT_STATE_PARAMETER_NAME);
  }

  public void verifyState(HttpRequest request, HttpResponse response, OAuth2IdentityProvider provider, String parameterName) {
    Cookie cookie = findCookie(CSRF_STATE_COOKIE, request)
      .orElseThrow(AuthenticationException.newBuilder()
        .setSource(Source.oauth2(provider))
        .setMessage(format("Cookie '%s' is missing", CSRF_STATE_COOKIE))::build);
    String hashInCookie = cookie.getValue();

    // remove cookie
    response.addCookie(newCookieBuilder(request).setName(CSRF_STATE_COOKIE).setValue(null).setHttpOnly(true).setExpiry(0).build());

    String stateInRequest = request.getParameter(parameterName);
    if (isBlank(stateInRequest) || !sha256Hex(stateInRequest).equals(hashInCookie)) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.oauth2(provider))
        .setMessage("CSRF state value is invalid")
        .build();
    }
  }

}
