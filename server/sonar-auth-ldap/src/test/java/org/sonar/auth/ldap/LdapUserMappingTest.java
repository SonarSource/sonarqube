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
package org.sonar.auth.ldap;

import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapUserMappingTest {

  @Test
  public void defaults() {
    LdapUserMapping userMapping = new LdapUserMapping(new MapSettings(), "ldap");
    assertThat(userMapping.getBaseDn()).isNull();
    assertThat(userMapping.getRequest()).isEqualTo("(&(objectClass=inetOrgPerson)(uid={0}))");
    assertThat(userMapping.getRealNameAttribute()).isEqualTo("cn");
    assertThat(userMapping.getEmailAttribute()).isEqualTo("mail");

    assertThat(userMapping.toString()).isEqualTo("LdapUserMapping{" +
      "baseDn=null," +
      " request=(&(objectClass=inetOrgPerson)(uid={0}))," +
      " realNameAttribute=cn," +
      " emailAttribute=mail}");
  }

  @Test
  public void activeDirectory() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.user.baseDn", "cn=users")
      .setProperty("ldap.user.request", "(&(objectClass=user)(sAMAccountName={0}))");

    LdapUserMapping userMapping = new LdapUserMapping(settings, "ldap");
    LdapSearch search = userMapping.createSearch(null, "tester");
    assertThat(search.getBaseDn()).isEqualTo("cn=users");
    assertThat(search.getRequest()).isEqualTo("(&(objectClass=user)(sAMAccountName={0}))");
    assertThat(search.getParameters()).isEqualTo(new String[] {"tester"});
    assertThat(search.getReturningAttributes()).isNull();

    assertThat(userMapping.toString()).isEqualTo("LdapUserMapping{" +
      "baseDn=cn=users," +
      " request=(&(objectClass=user)(sAMAccountName={0}))," +
      " realNameAttribute=cn," +
      " emailAttribute=mail}");
  }

  @Test
  public void realm() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.realm", "example.org")
      .setProperty("ldap.userObjectClass", "user")
      .setProperty("ldap.loginAttribute", "sAMAccountName");

    LdapUserMapping userMapping = new LdapUserMapping(settings, "ldap");
    assertThat(userMapping.getBaseDn()).isEqualTo("dc=example,dc=org");
  }

}
