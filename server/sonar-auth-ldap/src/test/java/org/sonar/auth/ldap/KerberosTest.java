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

import java.io.File;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.auth.ldap.server.LdapServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.process.ProcessProperties.Property.SONAR_SECURITY_REALM;

public class KerberosTest {

  static {
    System.setProperty("java.security.krb5.conf", new File("target/krb5.conf").getAbsolutePath());
  }

  @ClassRule
  public static LdapServer server = new LdapServer("/krb.ldif");

  LdapAuthenticator authenticator;
  LdapRealm ldapRealm;

  @Before
  public void before() {
    MapSettings settings = configure();
    ldapRealm = new LdapRealm(new LdapSettingsManager(settings.asConfig()), settings.asConfig());
    authenticator = ldapRealm.getAuthenticator();
  }

  @Test
  public void test_wrong_password() {
    LdapAuthenticator.Context wrongPasswordContext = new LdapAuthenticator.Context("Godin@EXAMPLE.ORG", "wrong_user_password", Mockito.mock(HttpServletRequest.class));
    assertThat(authenticator.doAuthenticate(wrongPasswordContext).isSuccess()).isFalse();
  }

  @Test
  public void test_correct_password() {

    LdapAuthenticator.Context correctPasswordContext = new LdapAuthenticator.Context("Godin@EXAMPLE.ORG", "user_password", Mockito.mock(HttpServletRequest.class));
    assertThat(authenticator.doAuthenticate(correctPasswordContext).isSuccess()).isTrue();

  }

  @Test
  public void test_default_realm() {

    // Using default realm from krb5.conf:
    LdapAuthenticator.Context defaultRealmContext = new LdapAuthenticator.Context("Godin", "user_password", Mockito.mock(HttpServletRequest.class));
    assertThat(authenticator.doAuthenticate(defaultRealmContext).isSuccess()).isTrue();
  }

  @Test
  public void test_groups() {
    LdapGroupsProvider groupsProvider = ldapRealm.getGroupsProvider();
    LdapGroupsProvider.Context groupsContext = new LdapGroupsProvider.Context("default", "godin", Mockito.mock(HttpServletRequest.class));
    assertThat(groupsProvider.doGetGroups(groupsContext))
      .containsOnly("sonar-users");
  }

  @Test
  public void wrong_bind_password() {
    MapSettings settings = configure()
      .setProperty("ldap.bindPassword", "wrong_bind_password");

    Configuration config = settings.asConfig();
    LdapSettingsManager settingsManager = new LdapSettingsManager(config);
    assertThatThrownBy(() -> new LdapRealm(settingsManager, config))
      .isInstanceOf(LdapException.class)
      .hasMessage("LDAP realm failed to start: Unable to open LDAP connection");

  }

  private static MapSettings configure() {
    return new MapSettings()
      .setProperty("ldap.url", server.getUrl())
      .setProperty("ldap.authentication", LdapContextFactory.AUTH_METHOD_GSSAPI)
      .setProperty("ldap.bindDn", "SonarQube@EXAMPLE.ORG")
      .setProperty("ldap.bindPassword", "bind_password")
      .setProperty("ldap.user.baseDn", "ou=users,dc=example,dc=org")
      .setProperty("ldap.group.baseDn", "ou=groups,dc=example,dc=org")
      .setProperty("ldap.group.request", "(&(objectClass=groupOfUniqueNames)(uniqueMember={dn}))")
      .setProperty(SONAR_SECURITY_REALM.getKey(), LdapRealm.LDAP_SECURITY_REALM);
  }

}
