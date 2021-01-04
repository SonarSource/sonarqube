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

import java.io.File;
import javax.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.security.Authenticator;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.auth.ldap.server.LdapServer;

import static org.assertj.core.api.Assertions.assertThat;

public class KerberosTest {

  static {
    System.setProperty("java.security.krb5.conf", new File("target/krb5.conf").getAbsolutePath());
  }

  @ClassRule
  public static LdapServer server = new LdapServer("/krb.ldif");

  @Test
  public void test() {
    MapSettings settings = configure();
    LdapRealm ldapRealm = new LdapRealm(new LdapSettingsManager(settings.asConfig(), new LdapAutodiscovery()));

    ldapRealm.init();

    assertThat(ldapRealm.doGetAuthenticator().doAuthenticate(new Authenticator.Context("Godin@EXAMPLE.ORG", "wrong_user_password", Mockito.mock(HttpServletRequest.class))))
      .isFalse();
    assertThat(ldapRealm.doGetAuthenticator().doAuthenticate(new Authenticator.Context("Godin@EXAMPLE.ORG", "user_password", Mockito.mock(HttpServletRequest.class)))).isTrue();
    // Using default realm from krb5.conf:
    assertThat(ldapRealm.doGetAuthenticator().doAuthenticate(new Authenticator.Context("Godin", "user_password", Mockito.mock(HttpServletRequest.class)))).isTrue();

    assertThat(ldapRealm.getGroupsProvider().doGetGroups(new ExternalGroupsProvider.Context("godin", Mockito.mock(HttpServletRequest.class)))).containsOnly("sonar-users");
  }

  @Test
  public void wrong_bind_password() {
    MapSettings settings = configure()
      .setProperty("ldap.bindPassword", "wrong_bind_password");
    LdapRealm ldapRealm = new LdapRealm(new LdapSettingsManager(settings.asConfig(), new LdapAutodiscovery()));
    try {
      ldapRealm.init();
      Assert.fail();
    } catch (LdapException e) {
      assertThat(e.getMessage()).isEqualTo("Unable to open LDAP connection");
    }
  }

  private static MapSettings configure() {
    return new MapSettings()
      .setProperty("ldap.url", server.getUrl())
      .setProperty("ldap.authentication", LdapContextFactory.AUTH_METHOD_GSSAPI)
      .setProperty("ldap.bindDn", "SonarQube@EXAMPLE.ORG")
      .setProperty("ldap.bindPassword", "bind_password")
      .setProperty("ldap.user.baseDn", "ou=users,dc=example,dc=org")
      .setProperty("ldap.group.baseDn", "ou=groups,dc=example,dc=org")
      .setProperty("ldap.group.request", "(&(objectClass=groupOfUniqueNames)(uniqueMember={dn}))");
  }

}
