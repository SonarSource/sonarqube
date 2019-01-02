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
package org.sonar.api.security;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SecurityRealmTest {

  @Test
  public void doGetAuthenticator() {
    final Authenticator authenticator = mock(Authenticator.class);
    SecurityRealm realm = new SecurityRealm() {
      @Override
      public Authenticator doGetAuthenticator() {
        return authenticator;
      }
    };
    assertThat(realm.doGetAuthenticator()).isSameAs(authenticator);
    assertThat(realm.getLoginPasswordAuthenticator()).isNull();
  }

  @Test
  public void getLoginPasswordAuthenticator_deprecated_method_replaced_by_getAuthenticator() {
    final LoginPasswordAuthenticator deprecatedAuthenticator = mock(LoginPasswordAuthenticator.class);
    SecurityRealm realm = new SecurityRealm() {
      @Override
      public LoginPasswordAuthenticator getLoginPasswordAuthenticator() {
        return deprecatedAuthenticator;
      }
    };
    Authenticator proxy = realm.doGetAuthenticator();
    Authenticator.Context context = new Authenticator.Context("foo", "bar", mock(HttpServletRequest.class));
    proxy.doAuthenticate(context);

    verify(deprecatedAuthenticator).authenticate("foo", "bar");
  }
}
