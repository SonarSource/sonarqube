/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.HashSet;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.es.EsTester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class UserIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(new MapSettings().asConfig()));

  private UserIndexer underTest = new UserIndexer(db.getDbClient(), es.client());

  @Test
  public void index_nothing_on_startup() {
    underTest.indexOnStartup(new HashSet<>());

    assertThat(es.countDocuments(UserIndexDefinition.INDEX_TYPE_USER)).isEqualTo(0L);
  }

  @Test
  public void indexOnStartup_adds_all_users_to_index() {
    UserDto user = db.users().insertUser(u -> u
      .setScmAccounts(asList("user_1", "u1")));

    underTest.indexOnStartup(new HashSet<>());

    List<UserDoc> docs = es.getDocuments(UserIndexDefinition.INDEX_TYPE_USER, UserDoc.class);
    assertThat(docs).hasSize(1);
    UserDoc doc = docs.get(0);
    assertThat(doc.login()).isEqualTo(user.getLogin());
    assertThat(doc.name()).isEqualTo(user.getName());
    assertThat(doc.email()).isEqualTo(user.getEmail());
    assertThat(doc.active()).isEqualTo(user.isActive());
    assertThat(doc.scmAccounts()).isEqualTo(user.getScmAccountsAsList());
  }

  @Test
  public void commitAndIndex_single_user() {
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();

    underTest.commitAndIndex(db.getSession(), user);

    List<UserDoc> docs = es.getDocuments(UserIndexDefinition.INDEX_TYPE_USER, UserDoc.class);
    assertThat(docs).hasSize(1);
    assertThat(docs).extracting(UserDoc::login)
      .containsExactly(user.getLogin())
      .doesNotContain(anotherUser.getLogin());
  }

  @Test
  public void commitAndIndex_multiple_users() {
    UserDto user1 = db.getDbClient().userDao().insert(db.getSession(), UserTesting.newUserDto());
    UserDto user2 = db.getDbClient().userDao().insert(db.getSession(), UserTesting.newUserDto());

    underTest.commitAndIndex(db.getSession(), asList(user1, user2));

    List<UserDoc> docs = es.getDocuments(UserIndexDefinition.INDEX_TYPE_USER, UserDoc.class);
    assertThat(docs).extracting(UserDoc::login).containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin());
    assertThat(db.countRowsOfTable(db.getSession(), "users")).isEqualTo(2);
  }
}
