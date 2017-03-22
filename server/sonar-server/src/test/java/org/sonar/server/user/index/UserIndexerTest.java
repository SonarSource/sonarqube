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

import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class UserIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(new MapSettings()));

  private UserIndexer underTest = new UserIndexer(db.getDbClient(), es.client());

  @Test
  public void index_nothing_on_startup() {
    underTest.indexOnStartup(null);

    assertThat(es.countDocuments(UserIndexDefinition.INDEX_TYPE_USER)).isEqualTo(0L);
  }

  @Test
  public void index_everything_on_startup() {
    db.prepareDbUnit(getClass(), "index.xml");

    underTest.indexOnStartup(null);

    List<UserDoc> docs = es.getDocuments(UserIndexDefinition.INDEX_TYPE_USER, UserDoc.class);
    assertThat(docs).hasSize(1);
    UserDoc doc = docs.get(0);
    assertThat(doc.login()).isEqualTo("user1");
    assertThat(doc.name()).isEqualTo("User1");
    assertThat(doc.email()).isEqualTo("user1@mail.com");
    assertThat(doc.active()).isTrue();
    assertThat(doc.scmAccounts()).containsOnly("user_1", "u1");
    assertThat(doc.createdAt()).isEqualTo(1500000000000L);
    assertThat(doc.updatedAt()).isEqualTo(1500000000000L);
  }

  @Test
  public void index_single_user_on_startup() {
    UserDto user = db.users().insertUser();

    underTest.indexOnStartup(null);

    List<UserDoc> docs = es.getDocuments(UserIndexDefinition.INDEX_TYPE_USER, UserDoc.class);
    assertThat(docs).hasSize(1);
    assertThat(docs).extracting(UserDoc::login).containsExactly(user.getLogin());
  }

  @Test
  public void index_single_user() {
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();

    underTest.index(user.getLogin());

    List<UserDoc> docs = es.getDocuments(UserIndexDefinition.INDEX_TYPE_USER, UserDoc.class);
    assertThat(docs).hasSize(1);
    assertThat(docs).extracting(UserDoc::login)
      .containsExactly(user.getLogin())
      .doesNotContain(anotherUser.getLogin());
  }

  @Test
  public void index_several_users() {
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();

    underTest.index(Arrays.asList(user.getLogin(), anotherUser.getLogin()));

    List<UserDoc> docs = es.getDocuments(UserIndexDefinition.INDEX_TYPE_USER, UserDoc.class);
    assertThat(docs).hasSize(2);
    assertThat(docs).extracting(UserDoc::login).containsOnly(user.getLogin(), anotherUser.getLogin());
  }
}
