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
package org.sonar.server.user.index;

import java.util.HashSet;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.user.index.UserIndexDefinition.TYPE_USER;

public class UserIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = EsTester.create();

  private UserIndexer underTest = new UserIndexer(db.getDbClient(), es.client());

  @Test
  public void index_nothing_on_startup() {
    underTest.indexOnStartup(new HashSet<>());

    assertThat(es.countDocuments(TYPE_USER)).isEqualTo(0L);
  }

  @Test
  public void indexOnStartup_adds_all_users_to_index() {
    UserDto user = db.users().insertUser(u -> u.setScmAccounts(asList("user_1", "u1")));

    underTest.indexOnStartup(new HashSet<>());

    List<UserDoc> docs = es.getDocuments(TYPE_USER, UserDoc.class);
    assertThat(docs).hasSize(1);
    UserDoc doc = docs.get(0);
    assertThat(doc.uuid()).isEqualTo(user.getUuid());
    assertThat(doc.login()).isEqualTo(user.getLogin());
    assertThat(doc.name()).isEqualTo(user.getName());
    assertThat(doc.email()).isEqualTo(user.getEmail());
    assertThat(doc.active()).isEqualTo(user.isActive());
    assertThat(doc.scmAccounts()).isEqualTo(user.getScmAccountsAsList());
    assertThat(doc.organizationUuids()).isEmpty();
  }

  @Test
  public void indexOnStartup_adds_all_users_with_organizations() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization1, user);
    db.organizations().addMember(organization2, user);

    underTest.indexOnStartup(new HashSet<>());

    List<UserDoc> docs = es.getDocuments(TYPE_USER, UserDoc.class);
    assertThat(docs).hasSize(1);
    UserDoc doc = docs.get(0);
    assertThat(doc.uuid()).isEqualTo(user.getUuid());
    assertThat(doc.login()).isEqualTo(user.getLogin());
    assertThat(doc.organizationUuids()).containsExactlyInAnyOrder(organization1.getUuid(), organization2.getUuid());
  }

  @Test
  public void commitAndIndex_single_user() {
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();

    underTest.commitAndIndex(db.getSession(), user);

    List<UserDoc> docs = es.getDocuments(TYPE_USER, UserDoc.class);
    assertThat(docs)
      .extracting(UserDoc::uuid)
      .containsExactlyInAnyOrder(user.getUuid())
    .doesNotContain(anotherUser.getUuid());
  }

  @Test
  public void commitAndIndex_single_user_belonging_to_organizations() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization1, user);
    db.organizations().addMember(organization2, user);
    UserDto anotherUser = db.users().insertUser();
    db.organizations().addMember(organization1, anotherUser);

    underTest.commitAndIndex(db.getSession(), user);

    List<UserDoc> docs = es.getDocuments(TYPE_USER, UserDoc.class);
    assertThat(docs)
      .extracting(UserDoc::uuid, UserDoc::organizationUuids)
      .containsExactlyInAnyOrder(tuple(user.getUuid(), asList(organization1.getUuid(), organization2.getUuid())));
  }

  @Test
  public void commitAndIndex_multiple_users() {
    OrganizationDto organization1 = db.organizations().insert();
    UserDto user1 = db.users().insertUser();
    db.organizations().addMember(organization1, user1);
    OrganizationDto organization2 = db.organizations().insert();
    UserDto user2 = db.users().insertUser();
    db.organizations().addMember(organization2, user2);

    underTest.commitAndIndex(db.getSession(), asList(user1, user2));

    List<UserDoc> docs = es.getDocuments(TYPE_USER, UserDoc.class);
    assertThat(docs)
      .extracting(UserDoc::login, UserDoc::organizationUuids)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), singletonList(organization1.getUuid())),
        tuple(user2.getLogin(), singletonList(organization2.getUuid())));
    assertThat(db.countRowsOfTable(db.getSession(), "users")).isEqualTo(2);
  }
}
