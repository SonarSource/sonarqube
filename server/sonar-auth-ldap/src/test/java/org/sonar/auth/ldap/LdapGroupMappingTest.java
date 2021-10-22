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

import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapGroupMappingTest {

  @Test
  public void defaults() {
    LdapGroupMapping groupMapping = new LdapGroupMapping(new MapSettings().asConfig(), "ldap");

    assertThat(groupMapping.getBaseDn()).isNull();
    assertThat(groupMapping.getIdAttribute()).isEqualTo("cn");
    assertThat(groupMapping.getRequest()).isEqualTo("(&(objectClass=groupOfUniqueNames)(uniqueMember={0}))");
    assertThat(groupMapping.getRequiredUserAttributes()).isEqualTo(new String[] {"dn"});

    assertThat(groupMapping).hasToString("LdapGroupMapping{" +
      "baseDn=null," +
      " idAttribute=cn," +
      " requiredUserAttributes=[dn]," +
      " request=(&(objectClass=groupOfUniqueNames)(uniqueMember={0}))}");
  }

  @Test
  public void custom_request() {
    MapSettings settings = new MapSettings()
      .setProperty("ldap.group.request", "(&(|(objectClass=posixGroup)(objectClass=groupOfUniqueNames))(|(memberUid={uid})(uniqueMember={dn})))");
    LdapGroupMapping groupMapping = new LdapGroupMapping(settings.asConfig(), "ldap");

    assertThat(groupMapping.getRequest()).isEqualTo("(&(|(objectClass=posixGroup)(objectClass=groupOfUniqueNames))(|(memberUid={0})(uniqueMember={1})))");
    assertThat(groupMapping.getRequiredUserAttributes()).isEqualTo(new String[] {"uid", "dn"});
  }

}
