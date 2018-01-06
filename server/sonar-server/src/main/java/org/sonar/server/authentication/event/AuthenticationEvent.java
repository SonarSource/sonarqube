/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.authentication.event;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public interface AuthenticationEvent {

  void loginSuccess(HttpServletRequest request, String login, Source source);

  void loginFailure(HttpServletRequest request, AuthenticationException e);

  void logoutSuccess(HttpServletRequest request, @Nullable String login);

  void logoutFailure(HttpServletRequest request, String errorMessage);

  enum Method {
    /**
     * HTTP basic authentication with a login and password.
     */
    BASIC,
    /**
     * HTTP basic authentication with a security token.
     */
    BASIC_TOKEN,
    /**
     * SQ login form authentication with a login and password.
     */
    FORM,
    /**
     * SSO authentication (ie. with HTTP headers)
     */
    SSO,
    /**
     * OAUTH2 authentication.
     */
    OAUTH2,
    /**
     * JWT authentication (ie. with a session token).
     */
    JWT,
    /**
     * External authentication (ie. fully implemented out of SQ's core code, see {@link BaseIdentityProvider}).
     */
    EXTERNAL
  }

  enum Provider {
    /**
     * User authentication made against data in SQ's User table.
     */
    LOCAL,
    /**
     * User authentication made by SSO provider.
     */
    SSO,
    /**
     * User authentication made by Realm based provider (eg. LDAP).
     */
    REALM,
    /**
     * User authentication made by JWT token information.
     */
    JWT,
    /**
     * User authentication made by external provider (see {@link BaseIdentityProvider}).
     */
    EXTERNAL
  }

  final class Source implements Serializable {
    private static final String LOCAL_PROVIDER_NAME = "local";
    private static final Source SSO_INSTANCE = new Source(Method.SSO, Provider.SSO, "sso");
    private static final Source JWT_INSTANCE = new Source(Method.JWT, Provider.JWT, "jwt");

    private final Method method;
    private final Provider provider;
    private final String providerName;

    private Source(Method method, Provider provider, String providerName) {
      this.method = requireNonNull(method, "method can't be null");
      this.provider = requireNonNull(provider, "provider can't be null");
      this.providerName = requireNonNull(providerName, "provider name can't be null");
      checkArgument(!providerName.isEmpty(), "provider name can't be empty");
    }

    public static Source local(Method method) {
      return new Source(method, Provider.LOCAL, LOCAL_PROVIDER_NAME);
    }

    public static Source oauth2(OAuth2IdentityProvider identityProvider) {
      return new Source(
        Method.OAUTH2, Provider.EXTERNAL,
        requireNonNull(identityProvider, "identityProvider can't be null").getName());
    }

    public static Source realm(Method method, String providerName) {
      return new Source(method, Provider.REALM, providerName);
    }

    public static Source sso() {
      return SSO_INSTANCE;
    }

    public static Source jwt() {
      return JWT_INSTANCE;
    }

    public static Source external(IdentityProvider identityProvider) {
      return new Source(
        Method.EXTERNAL, Provider.EXTERNAL,
        requireNonNull(identityProvider, "identityProvider can't be null").getName());
    }

    public Method getMethod() {
      return method;
    }

    public Provider getProvider() {
      return provider;
    }

    public String getProviderName() {
      return providerName;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Source source = (Source) o;
      return method == source.method &&
        provider == source.provider &&
        providerName.equals(source.providerName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(method, provider, providerName);
    }

    @Override
    public String toString() {
      return "Source{" +
        "method=" + method +
        ", provider=" + provider +
        ", providerName='" + providerName + '\'' +
        '}';
    }
  }

}
