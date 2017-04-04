/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.user.index;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;

public class UserResultSetIteratorTest {

  @Rule
  public DbTester db = DbTester.create();

  @Test
  public void iterator_over_users() {
    UserDto userDto1 = db.users().insertUser(u -> u
      .setName("User1")
      .setLogin("user1")
      .setEmail("user1@mail.com")
      .setScmAccounts(Arrays.asList("user_1", "u1"))
      .setCreatedAt(1_500_000_000_000L)
      .setUpdatedAt(1_500_000_000_000L));
    UserDto userDto2 = db.users().insertUser(u -> u
      .setName("User2")
      .setLogin("user2")
      .setEmail("user2@mail.com")
      .setScmAccounts(Arrays.asList("user,2", "user_2"))
      .setCreatedAt(1_500_000_000_000L)
      .setUpdatedAt(1_500_000_000_000L));
    UserDto inactiveUser = db.users().insertUser(u -> u
      .setName("User3")
      .setLogin("user3")
      .setEmail(null)
      .setActive(false)
      .setScmAccounts((String) null)
      .setCreatedAt(1_500_000_000_000L)
      .setUpdatedAt(1_550_000_000_000L));
    OrganizationDto org1 = db.organizations().insertForUuid("ORG_1");
    OrganizationDto org2 = db.organizations().insertForUuid("ORG_2");
    db.organizations().addMember(org1, userDto1);
    db.organizations().addMember(org1, userDto2);
    db.organizations().addMember(org2, userDto1);

    UserResultSetIterator it = UserResultSetIterator.create(db.getDbClient(), db.getSession(), null);
    Map<String, UserDoc> usersByLogin = Maps.uniqueIndex(it, UserDoc::login);
    it.close();

    assertThat(usersByLogin).hasSize(3);

    UserDoc user1 = usersByLogin.get("user1");
    assertThat(user1.name()).isEqualTo("User1");
    assertThat(user1.email()).isEqualTo("user1@mail.com");
    assertThat(user1.active()).isTrue();
    assertThat(user1.scmAccounts()).containsOnly("user_1", "u1");
    assertThat(user1.createdAt()).isEqualTo(1_500_000_000_000L);
    assertThat(user1.updatedAt()).isEqualTo(1_500_000_000_000L);
    assertThat(user1.organizationUuids()).containsOnly("ORG_1", "ORG_2");

    UserDoc user2 = usersByLogin.get("user2");
    assertThat(user2.name()).isEqualTo("User2");
    assertThat(user2.email()).isEqualTo("user2@mail.com");
    assertThat(user2.active()).isTrue();
    assertThat(user2.scmAccounts()).containsOnly("user,2", "user_2");
    assertThat(user2.createdAt()).isEqualTo(1_500_000_000_000L);
    assertThat(user2.updatedAt()).isEqualTo(1_500_000_000_000L);
    assertThat(user2.organizationUuids()).containsOnly("ORG_1");

    UserDoc user3 = usersByLogin.get("user3");
    assertThat(user3.name()).isEqualTo("User3");
    assertThat(user3.email()).isNull();
    assertThat(user3.active()).isFalse();
    assertThat(user3.scmAccounts()).isEmpty();
    assertThat(user3.createdAt()).isEqualTo(1500000000000L);
    assertThat(user3.updatedAt()).isEqualTo(1550000000000L);
    assertThat(user3.organizationUuids()).isEmpty();
  }
}
