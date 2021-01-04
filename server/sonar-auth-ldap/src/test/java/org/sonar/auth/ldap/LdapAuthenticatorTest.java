/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.auth.ldap.server.LdapServer;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapAuthenticatorTest {

  /**
   * A reference to the original ldif file
   */
  public static final String USERS_EXAMPLE_ORG_LDIF = "/users.example.org.ldif";
  /**
   * A reference to an aditional ldif file.
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
      LdapSettingsManager settingsManager = new LdapSettingsManager(LdapSettingsFactory.generateAuthenticationSettings(exampleServer, null, LdapContextFactory.AUTH_METHOD_SIMPLE).asConfig(),
        new LdapAutodiscovery());
      LdapAuthenticator authenticator = new LdapAuthenticator(settingsManager.getContextFactories(), settingsManager.getUserMappings());
      authenticator.authenticate("godin", "secret1");
    } finally {
      exampleServer.enableAnonymousAccess();
    }
  }

  @Test
  public void testSimple() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(LdapSettingsFactory.generateAuthenticationSettings(exampleServer, null, LdapContextFactory.AUTH_METHOD_SIMPLE).asConfig(),
      new LdapAutodiscovery());
    LdapAuthenticator authenticator = new LdapAuthenticator(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    assertThat(authenticator.authenticate("godin", "secret1")).isTrue();
    assertThat(authenticator.authenticate("godin", "wrong")).isFalse();

    assertThat(authenticator.authenticate("tester", "secret2")).isTrue();
    assertThat(authenticator.authenticate("tester", "wrong")).isFalse();

    assertThat(authenticator.authenticate("notfound", "wrong")).isFalse();
    // SONARPLUGINS-2493
    assertThat(authenticator.authenticate("godin", "")).isFalse();
    assertThat(authenticator.authenticate("godin", null)).isFalse();
  }

  @Test
  public void testSimpleMultiLdap() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      LdapSettingsFactory.generateAuthenticationSettings(exampleServer, infosupportServer, LdapContextFactory.AUTH_METHOD_SIMPLE).asConfig(), new LdapAutodiscovery());
    LdapAuthenticator authenticator = new LdapAuthenticator(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    assertThat(authenticator.authenticate("godin", "secret1")).isTrue();
    assertThat(authenticator.authenticate("godin", "wrong")).isFalse();

    assertThat(authenticator.authenticate("tester", "secret2")).isTrue();
    assertThat(authenticator.authenticate("tester", "wrong")).isFalse();

    assertThat(authenticator.authenticate("notfound", "wrong")).isFalse();
    // SONARPLUGINS-2493
    assertThat(authenticator.authenticate("godin", "")).isFalse();
    assertThat(authenticator.authenticate("godin", null)).isFalse();

    // SONARPLUGINS-2793
    assertThat(authenticator.authenticate("robby", "secret1")).isTrue();
    assertThat(authenticator.authenticate("robby", "wrong")).isFalse();
  }

  @Test
  public void testSasl() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(LdapSettingsFactory.generateAuthenticationSettings(exampleServer, null, LdapContextFactory.AUTH_METHOD_CRAM_MD5).asConfig(),
      new LdapAutodiscovery());
    LdapAuthenticator authenticator = new LdapAuthenticator(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    assertThat(authenticator.authenticate("godin", "secret1")).isTrue();
    assertThat(authenticator.authenticate("godin", "wrong")).isFalse();

    assertThat(authenticator.authenticate("tester", "secret2")).isTrue();
    assertThat(authenticator.authenticate("tester", "wrong")).isFalse();

    assertThat(authenticator.authenticate("notfound", "wrong")).isFalse();
  }

  @Test
  public void testSaslMultipleLdap() {
    LdapSettingsManager settingsManager = new LdapSettingsManager(
      LdapSettingsFactory.generateAuthenticationSettings(exampleServer, infosupportServer, LdapContextFactory.AUTH_METHOD_CRAM_MD5).asConfig(), new LdapAutodiscovery());
    LdapAuthenticator authenticator = new LdapAuthenticator(settingsManager.getContextFactories(), settingsManager.getUserMappings());

    assertThat(authenticator.authenticate("godin", "secret1")).isTrue();
    assertThat(authenticator.authenticate("godin", "wrong")).isFalse();

    assertThat(authenticator.authenticate("tester", "secret2")).isTrue();
    assertThat(authenticator.authenticate("tester", "wrong")).isFalse();

    assertThat(authenticator.authenticate("notfound", "wrong")).isFalse();

    assertThat(authenticator.authenticate("robby", "secret1")).isTrue();
    assertThat(authenticator.authenticate("robby", "wrong")).isFalse();
  }

}
