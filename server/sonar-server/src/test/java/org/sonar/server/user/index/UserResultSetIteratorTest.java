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
package org.sonar.server.user.index;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class UserResultSetIteratorTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Test
  public void iterator_over_users() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    UserResultSetIterator it = UserResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 0L);
    Map<String, UserDoc> usersByLogin = Maps.uniqueIndex(it, new Function<UserDoc, String>() {
      @Override
      public String apply(UserDoc user) {
        return user.login();
      }
    });
    it.close();

    assertThat(usersByLogin).hasSize(3);

    UserDoc user1 = usersByLogin.get("user1");
    assertThat(user1.name()).isEqualTo("User1");
    assertThat(user1.email()).isEqualTo("user1@mail.com");
    assertThat(user1.active()).isTrue();
    assertThat(user1.scmAccounts()).containsOnly("user_1", "u1");
    assertThat(user1.createdAt()).isEqualTo(1500000000000L);
    assertThat(user1.updatedAt()).isEqualTo(1500000000000L);

    UserDoc user2 = usersByLogin.get("user2");
    assertThat(user2.name()).isEqualTo("User2");
    assertThat(user2.email()).isEqualTo("user2@mail.com");
    assertThat(user2.active()).isTrue();
    assertThat(user2.scmAccounts()).containsOnly("user,2", "user_2");
    assertThat(user2.createdAt()).isEqualTo(1500000000000L);
    assertThat(user2.updatedAt()).isEqualTo(1500000000000L);

    UserDoc user3 = usersByLogin.get("user3");
    assertThat(user3.name()).isEqualTo("User3");
    assertThat(user3.email()).isNull();
    assertThat(user3.active()).isFalse();
    assertThat(user3.scmAccounts()).isEmpty();
    assertThat(user3.createdAt()).isEqualTo(1500000000000L);
    assertThat(user3.updatedAt()).isEqualTo(1550000000000L);
  }

  @Test
  public void select_after_date() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    UserResultSetIterator it = UserResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), 1520000000000L);

    assertThat(it.hasNext()).isTrue();
    UserDoc user = it.next();
    assertThat(user.login()).isEqualTo("user3");

    assertThat(it.hasNext()).isFalse();
    it.close();
  }
}
