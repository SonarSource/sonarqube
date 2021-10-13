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
package org.sonar.db.qualitygate;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.user.SearchUserMembershipDto;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.qualitygate.SearchQualityGatePermissionQuery.builder;
import static org.sonar.db.user.SearchPermissionQuery.ANY;
import static org.sonar.db.user.SearchPermissionQuery.IN;
import static org.sonar.db.user.SearchPermissionQuery.OUT;

public class QualityGateUserPermissionsDaoTest {
  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final UserDbTester userDbTester = new UserDbTester(db);
  private final QualityGateDbTester qualityGateDbTester = new QualityGateDbTester(db);
  private final QualityGateUserPermissionsDao underTest = db.getDbClient().qualityGateUserPermissionDao();

  @Test
  public void insert() {
    UserDto user = userDbTester.insertUser();
    QualityGateDto qualityGate = qualityGateDbTester.insertQualityGate();
    QualityGateUserPermissionsDto qualityGateUserPermissions = new QualityGateUserPermissionsDto("uuid", user.getUuid(), qualityGate.getUuid());
    underTest.insert(dbSession, qualityGateUserPermissions);
    dbSession.commit();

    QualityGateUserPermissionsDto fromDB = underTest.selectByQualityGateAndUser(dbSession, qualityGate.getUuid(), user.getUuid());
    assertThat(fromDB.getQualityGateUuid()).isEqualTo(qualityGate.getUuid());
    assertThat(fromDB.getUserUuid()).isEqualTo(user.getUuid());
    assertThat(fromDB.getUuid()).isEqualTo("uuid");
  }

  @Test
  public void exist() {
    UserDto user1 = userDbTester.insertUser();
    UserDto user2 = userDbTester.insertUser();
    QualityGateDto qualityGate = qualityGateDbTester.insertQualityGate();
    QualityGateUserPermissionsDto qualityGateUserPermissions = new QualityGateUserPermissionsDto("uuid", user1.getUuid(), qualityGate.getUuid());
    underTest.insert(dbSession, qualityGateUserPermissions);
    dbSession.commit();

    assertThat(underTest.exists(dbSession, qualityGate.getUuid(), user1.getUuid())).isTrue();
    assertThat(underTest.exists(dbSession, qualityGate, user1)).isTrue();
    assertThat(underTest.exists(dbSession, qualityGate.getUuid(), user2.getUuid())).isFalse();
    assertThat(underTest.exists(dbSession, qualityGate, user2)).isFalse();
  }

  @Test
  public void exist_can_handle_null_param_and_return_false() {
    assertThat(underTest.exists(dbSession, "uuid", null)).isFalse();
    assertThat(underTest.exists(dbSession, null, "uuid")).isFalse();
  }

  @Test
  public void countByQuery() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, user1);
    db.qualityGates().addUserPermission(qualityGate, user2);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setQualityGate(qualityGate)
      .setMembership(ANY).build()))
      .isEqualTo(3);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setQualityGate(qualityGate)
      .setMembership(IN).build()))
      .isEqualTo(2);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setQualityGate(qualityGate)
      .setMembership(OUT).build()))
      .isEqualTo(1);
  }

  @Test
  public void selectByQuery() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, user1);
    db.qualityGates().addUserPermission(qualityGate, user2);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
      .setQualityGate(qualityGate)
      .setMembership(ANY).build(), Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid, SearchUserMembershipDto::isSelected)
      .containsExactlyInAnyOrder(
        tuple(user1.getUuid(), true),
        tuple(user2.getUuid(), true),
        tuple(user3.getUuid(), false));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setQualityGate(qualityGate)
        .setMembership(IN).build(),
      Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid, SearchUserMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(user1.getUuid(), true), tuple(user2.getUuid(), true));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setQualityGate(qualityGate)
        .setMembership(OUT).build(),
      Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid, SearchUserMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(user3.getUuid(), false));
  }

  @Test
  public void selectByQuery_search_by_name_or_login() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser(u -> u.setLogin("user1").setName("John Doe"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("user2").setName("John Smith"));
    UserDto user3 = db.users().insertUser(u -> u.setLogin("user3").setName("Jane Doe"));
    db.qualityGates().addUserPermission(qualityGate, user1);
    db.qualityGates().addUserPermission(qualityGate, user2);
    db.qualityGates().addUserPermission(qualityGate, user3);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setQualityGate(qualityGate)
        .setMembership(IN)
        .setQuery("user2").build(),
      Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactlyInAnyOrder(user2.getUuid());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setQualityGate(qualityGate)
        .setMembership(IN)
        .setQuery("joh").build(),
      Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setQualityGate(qualityGate)
        .setMembership(IN)
        .setQuery("Doe").build(),
      Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactlyInAnyOrder(user1.getUuid(), user3.getUuid());
  }

  @Test
  public void selectByQuery_with_paging() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser(u -> u.setName("user1"));
    UserDto user2 = db.users().insertUser(u -> u.setName("user2"));
    UserDto user3 = db.users().insertUser(u -> u.setName("user3"));
    db.qualityGates().addUserPermission(qualityGate, user1);
    db.qualityGates().addUserPermission(qualityGate, user2);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setQualityGate(qualityGate)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(1)))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactly(user1.getUuid());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setQualityGate(qualityGate)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(3).andSize(1)))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactly(user3.getUuid());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setQualityGate(qualityGate)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(10)))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactly(user1.getUuid(), user2.getUuid(), user3.getUuid());
  }

}