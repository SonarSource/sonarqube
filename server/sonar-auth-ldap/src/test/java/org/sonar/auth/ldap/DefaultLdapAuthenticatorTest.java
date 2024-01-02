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
package org.sonar.auth.ldap;

import javax.servlet.http.HttpServletRequest;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.auth.ldap.server.LdapServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultLdapAuthenticatorTest {

  /**
   * A reference to the original ldif file
   */
  public static final String USERS_EXAMPLE_ORG_LDIF = "/users.example.org.ldif";
  /**
   * A reference to an additional ldif file.
   */
  public static final String USERS_INFOSUPPORT_COM_LDIF = "/users.infosupport.com.ldif";
  @ClassRule
  public static LdapServer exampleServer = new LdapServer(USERS_EXAMPLE_ORG_LDIF);
  @ClassRule
  public static LdapServer infosupportServer = new LdapServer(USERS_INFOSUPPORT_COM_LDIF, "infosupport.com", "dc=infosupport,dc=com");

  @Test
  public void testNoConnection() {
    exampleServer.disableAnonymousAccess();
    try {
      LdapSettingsManager settingsManager = new LdapSettingsManager(
        LdapSettingsFactory.generateAuthenticationSettings(exampleServer, null, LdapContextFactory.AUTH_METHOD_SIMPLE).asConfig());
      DefaultLdapAuthenticator authenticator = new DefaultLdapAuthenticator(settingsManager.getContextFactories(), settingsManager.getUserMappings());
      boolean isAuthenticationSuccessful = authenticator.doAuthenticate(createContext("godin", "secret1")).isSuccess();
      assertThat(isAuthenticationSuccessful).isTrue();
    } finally {
      exampleServer.enableAnonymousAccess();
    }
  }

  @Test
  public void testSimple() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      LdapSettingsFactory.generateAuthenticationSettings(exampleServer, null, LdapContextFactory.AUTH_METHOD_SIMPLE).asConfig());
    DefaultLdapAuthenticator authenticator = new DefaultLdapAuthenticator(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    LdapAuthenticationResult user1Success = authenticator.doAuthenticate(createContext("godin", "secret1"));
    assertThat(user1Success.isSuccess()).isTrue();
    assertThat(user1Success.getServerKey()).isEqualTo("default");

    assertThat(authenticator.doAuthenticate(createContext("godin", "wrong")).isSuccess()).isFalse();

    LdapAuthenticationResult user2Success = authenticator.doAuthenticate(createContext("tester", "secret2"));
    assertThat(user2Success.isSuccess()).isTrue();
    assertThat(user2Success.getServerKey()).isEqualTo("default");

    assertThat(authenticator.doAuthenticate(createContext("tester", "wrong")).isSuccess()).isFalse();

    assertThat(authenticator.doAuthenticate(createContext("notfound", "wrong")).isSuccess()).isFalse();
    // SONARPLUGINS-2493
    assertThat(authenticator.doAuthenticate(createContext("godin", "")).isSuccess()).isFalse();
    assertThat(authenticator.doAuthenticate(createContext("godin", null)).isSuccess()).isFalse();
  }

  @Test
  public void testSimpleMultiLdap() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      LdapSettingsFactory.generateAuthenticationSettings(exampleServer, infosupportServer, LdapContextFactory.AUTH_METHOD_SIMPLE).asConfig());
    DefaultLdapAuthenticator authenticator = new DefaultLdapAuthenticator(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    LdapAuthenticationResult user1Success = authenticator.doAuthenticate(createContext("godin", "secret1"));
    assertThat(user1Success.isSuccess()).isTrue();
    assertThat(user1Success.getServerKey()).isEqualTo("example");
    assertThat(authenticator.doAuthenticate(createContext("godin", "wrong")).isSuccess()).isFalse();

    LdapAuthenticationResult user2Server1Success = authenticator.doAuthenticate(createContext("tester", "secret2"));
    assertThat(user2Server1Success.isSuccess()).isTrue();
    assertThat(user2Server1Success.getServerKey()).isEqualTo("example");

    LdapAuthenticationResult user2Server2Success = authenticator.doAuthenticate(createContext("tester", "secret3"));
    assertThat(user2Server2Success.isSuccess()).isTrue();
    assertThat(user2Server2Success.getServerKey()).isEqualTo("infosupport");

    assertThat(authenticator.doAuthenticate(createContext("tester", "wrong")).isSuccess()).isFalse();

    assertThat(authenticator.doAuthenticate(createContext("notfound", "wrong")).isSuccess()).isFalse();
    // SONARPLUGINS-2493
    assertThat(authenticator.doAuthenticate(createContext("godin", "")).isSuccess()).isFalse();
    assertThat(authenticator.doAuthenticate(createContext("godin", null)).isSuccess()).isFalse();

    // SONARPLUGINS-2793
    LdapAuthenticationResult user3Success = authenticator.doAuthenticate(createContext("robby", "secret1"));
    assertThat(user3Success.isSuccess()).isTrue();
    assertThat(user3Success.getServerKey()).isEqualTo("infosupport");
    assertThat(authenticator.doAuthenticate(createContext("robby", "wrong")).isSuccess()).isFalse();
  }

  @Test
  public void testSasl() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      LdapSettingsFactory.generateAuthenticationSettings(exampleServer, null, LdapContextFactory.AUTH_METHOD_CRAM_MD5).asConfig());
    DefaultLdapAuthenticator authenticator = new DefaultLdapAuthenticator(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    LdapAuthenticationResult user1Success = authenticator.doAuthenticate(createContext("godin", "secret1"));
    assertThat(user1Success.isSuccess()).isTrue();
    assertThat(user1Success.getServerKey()).isEqualTo("default");

    assertThat(authenticator.doAuthenticate(createContext("godin", "wrong")).isSuccess()).isFalse();

    LdapAuthenticationResult user2Success = authenticator.doAuthenticate(createContext("tester", "secret2"));
    assertThat(user2Success.isSuccess()).isTrue();
    assertThat(user2Success.getServerKey()).isEqualTo("default");

    assertThat(authenticator.doAuthenticate(createContext("tester", "wrong")).isSuccess()).isFalse();

    assertThat(authenticator.doAuthenticate(createContext("notfound", "wrong")).isSuccess()).isFalse();
  }

  @Test
  public void testSaslMultipleLdap() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      LdapSettingsFactory.generateAuthenticationSettings(exampleServer, infosupportServer, LdapContextFactory.AUTH_METHOD_CRAM_MD5).asConfig());
    DefaultLdapAuthenticator authenticator = new DefaultLdapAuthenticator(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    LdapAuthenticationResult user1Success = authenticator.doAuthenticate(createContext("godin", "secret1"));
    assertThat(user1Success.isSuccess()).isTrue();
    assertThat(authenticator.doAuthenticate(createContext("godin", "wrong")).isSuccess()).isFalse();

    LdapAuthenticationResult user2Server1Success = authenticator.doAuthenticate(createContext("tester", "secret2"));
    assertThat(user2Server1Success.isSuccess()).isTrue();
    assertThat(user2Server1Success.getServerKey()).isEqualTo("example");

    LdapAuthenticationResult user2Server2Success = authenticator.doAuthenticate(createContext("tester", "secret3"));
    assertThat(user2Server2Success.isSuccess()).isTrue();
    assertThat(user2Server2Success.getServerKey()).isEqualTo("infosupport");

    assertThat(authenticator.doAuthenticate(createContext("tester", "wrong")).isSuccess()).isFalse();

    assertThat(authenticator.doAuthenticate(createContext("notfound", "wrong")).isSuccess()).isFalse();

    LdapAuthenticationResult user3Success = authenticator.doAuthenticate(createContext("robby", "secret1"));
    assertThat(user3Success.isSuccess()).isTrue();

    assertThat(authenticator.doAuthenticate(createContext("robby", "wrong")).isSuccess()).isFalse();
  }

  private static LdapAuthenticator.Context createContext(String username, String password) {
    return new LdapAuthenticator.Context(username, password, mock(HttpServletRequest.class));
  }

}
