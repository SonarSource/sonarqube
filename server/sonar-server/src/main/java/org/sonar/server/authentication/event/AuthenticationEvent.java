/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Objects;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public interface AuthenticationEvent {

  void login(HttpServletRequest request, String login, Source source);

  enum Method {
    BASIC, BASIC_TOKEN, FORM, FORM_TOKEN, SSO, OAUTH2, EXTERNAL
  }

  enum Provider {
    LOCAL, SSO, REALM, EXTERNAL
  }

  class Source {
    private static final String LOCAL_PROVIDER_NAME = "local";
    private static final Source SSO_INSTANCE = new Source(Method.SSO, Provider.SSO, "sso");

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

    public static Source oauth2(String providerName) {
      return new Source(Method.OAUTH2, Provider.EXTERNAL, providerName);
    }

    public static Source realm(Method method, String providerName) {
      return new Source(method, Provider.REALM, providerName);
    }

    public static Source sso() {
      return SSO_INSTANCE;
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
