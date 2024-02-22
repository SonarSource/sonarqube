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
package org.sonar.db.qualityprofile;

import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserEditorNewValue;
import org.sonar.db.user.SearchUserMembershipDto;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.ANY;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.IN;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.OUT;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.builder;

class QProfileEditUsersDaoIT {

  private static final long NOW = 10_000_000_000L;

  private final System2 system2 = new TestSystem2().setNow(NOW);
  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final ArgumentCaptor<UserEditorNewValue> newValueCaptor = ArgumentCaptor.forClass(UserEditorNewValue.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2, auditPersister);

  private final QProfileEditUsersDao underTest = db.getDbClient().qProfileEditUsersDao();

  @Test
  void exists() {
    QProfileDto profile = db.qualityProfiles().insert();
    QProfileDto anotherProfile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);

    verify(auditPersister).addQualityProfileEditor(eq(db.getSession()), newValueCaptor.capture());

    UserEditorNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(UserEditorNewValue::getQualityProfileName, UserEditorNewValue::getQualityProfileUuid,
        UserEditorNewValue::getUserLogin, UserEditorNewValue::getUserUuid)
      .containsExactly(profile.getName(), profile.getKee(), user.getLogin(), user.getUuid());
    assertThat(newValue.toString()).contains("\"qualityProfileName\"").contains("\"userLogin\"");

    assertThat(underTest.exists(db.getSession(), profile, user)).isTrue();
    assertThat(underTest.exists(db.getSession(), profile, anotherUser)).isFalse();
    assertThat(underTest.exists(db.getSession(), anotherProfile, user)).isFalse();
    assertThat(underTest.exists(db.getSession(), anotherProfile, anotherUser)).isFalse();
  }

  @Test
  void countByQuery() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user1);
    db.qualityProfiles().addUserPermission(profile, user2);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setProfile(profile)
      .setMembership(ANY).build()))
      .isEqualTo(3);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setProfile(profile)
      .setMembership(IN).build()))
      .isEqualTo(2);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setProfile(profile)
      .setMembership(OUT).build()))
      .isOne();
  }

  @Test
  void selectByQuery() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user1);
    db.qualityProfiles().addUserPermission(profile, user2);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
      .setProfile(profile)
      .setMembership(ANY).build(), Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid, SearchUserMembershipDto::isSelected)
      .containsExactlyInAnyOrder(
        tuple(user1.getUuid(), true),
        tuple(user2.getUuid(), true),
        tuple(user3.getUuid(), false));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(IN).build(),
      Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid, SearchUserMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(user1.getUuid(), true), tuple(user2.getUuid(), true));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(OUT).build(),
      Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid, SearchUserMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(user3.getUuid(), false));
  }

  @Test
  void selectByQuery_search_by_name_or_login() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user1 = db.users().insertUser(u -> u.setLogin("user1").setName("John Doe"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("user2").setName("John Smith"));
    UserDto user3 = db.users().insertUser(u -> u.setLogin("user3").setName("Jane Doe"));
    db.qualityProfiles().addUserPermission(profile, user1);
    db.qualityProfiles().addUserPermission(profile, user2);
    db.qualityProfiles().addUserPermission(profile, user3);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(IN)
        .setQuery("user2").build(),
      Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactlyInAnyOrder(user2.getUuid());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(IN)
        .setQuery("joh").build(),
      Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(IN)
        .setQuery("Doe").build(),
      Pagination.all()))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactlyInAnyOrder(user1.getUuid(), user3.getUuid());
  }

  @Test
  void selectByQuery_with_paging() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user1 = db.users().insertUser(u -> u.setName("user1"));
    UserDto user2 = db.users().insertUser(u -> u.setName("user2"));
    UserDto user3 = db.users().insertUser(u -> u.setName("user3"));
    db.qualityProfiles().addUserPermission(profile, user1);
    db.qualityProfiles().addUserPermission(profile, user2);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(1)))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactly(user1.getUuid());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(3).andSize(1)))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactly(user3.getUuid());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(10)))
      .extracting(SearchUserMembershipDto::getUserUuid)
      .containsExactly(user1.getUuid(), user2.getUuid(), user3.getUuid());
  }

  @Test
  void selectQProfileUuidsByUser() {
    QProfileDto profile1 = db.qualityProfiles().insert();
    QProfileDto profile2 = db.qualityProfiles().insert();
    UserDto user1 = db.users().insertUser(u -> u.setName("user1"));
    UserDto user2 = db.users().insertUser(u -> u.setName("user2"));
    db.qualityProfiles().addUserPermission(profile1, user1);
    db.qualityProfiles().addUserPermission(profile2, user1);

    assertThat(underTest.selectQProfileUuidsByUser(db.getSession(), user1))
      .containsExactlyInAnyOrder(profile1.getKee(), profile2.getKee());
    assertThat(underTest.selectQProfileUuidsByUser(db.getSession(), user2)).isEmpty();
  }

  @Test
  void insert() {
    String qualityProfileUuid = "QPROFILE";
    String qualityProfileName = "QPROFILE_NAME";
    String userUuid = "100";
    String userLogin = "USER_LOGIN";
    underTest.insert(db.getSession(), new QProfileEditUsersDto()
        .setUuid("ABCD")
        .setUserUuid(userUuid)
        .setQProfileUuid(qualityProfileUuid),
      qualityProfileName, userLogin);

    assertThat(db.selectFirst(db.getSession(),
      "select uuid as \"uuid\", user_uuid as \"userUuid\", qprofile_uuid as \"qProfileUuid\", created_at as \"createdAt\" from " +
        "qprofile_edit_users")).contains(
      entry("uuid", "ABCD"),
      entry("userUuid", userUuid),
      entry("qProfileUuid", qualityProfileUuid),
      entry("createdAt", NOW));
  }

  @Test
  void fail_to_insert_same_row_twice() {
    String qualityProfileUuid = "QPROFILE";
    String qualityProfileName = "QPROFILE_NAME";
    String userUuid = "100";
    String userLogin = "USER_LOGIN";
    underTest.insert(db.getSession(), new QProfileEditUsersDto()
        .setUuid("UUID-1")
        .setUserUuid(userUuid)
        .setQProfileUuid(qualityProfileUuid),
      qualityProfileName, userLogin);

    assertThatThrownBy(() -> {
      underTest.insert(db.getSession(), new QProfileEditUsersDto()
          .setUuid("UUID-2")
          .setUserUuid(userUuid)
          .setQProfileUuid(qualityProfileUuid),
        qualityProfileName, userLogin);
    })
      .hasCauseInstanceOf(SQLException.class);
  }

  @Test
  void deleteByQProfileAndUser() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    assertThat(underTest.exists(db.getSession(), profile, user)).isTrue();

    underTest.deleteByQProfileAndUser(db.getSession(), profile, user);

    verify(auditPersister).deleteQualityProfileEditor(eq(db.getSession()), newValueCaptor.capture());

    UserEditorNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(UserEditorNewValue::getQualityProfileName, UserEditorNewValue::getQualityProfileUuid,
        UserEditorNewValue::getUserLogin, UserEditorNewValue::getUserUuid)
      .containsExactly(profile.getName(), profile.getKee(), user.getLogin(), user.getUuid());
    assertThat(newValue.toString()).contains("\"qualityProfileName\"").contains("\"userLogin\"");

    assertThat(underTest.exists(db.getSession(), profile, user)).isFalse();
  }

  @Test
  void deleteByQProfiles() {
    QProfileDto profile1 = db.qualityProfiles().insert();
    QProfileDto profile2 = db.qualityProfiles().insert();
    QProfileDto profile3 = db.qualityProfiles().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile1, user1);
    db.qualityProfiles().addUserPermission(profile2, user2);
    db.qualityProfiles().addUserPermission(profile3, user1);

    underTest.deleteByQProfiles(db.getSession(), asList(profile1, profile2));

    verify(auditPersister, times(2)).deleteQualityProfileEditor(eq(db.getSession()), newValueCaptor.capture());

    Map<String, UserEditorNewValue> newValues = newValueCaptor.getAllValues().stream()
      .collect(toMap(UserEditorNewValue::getQualityProfileName, identity()));
    assertThat(newValues.get(profile1.getName()))
      .extracting(UserEditorNewValue::getQualityProfileUuid, UserEditorNewValue::getUserLogin, UserEditorNewValue::getUserUuid)
      .containsExactly(profile1.getKee(), null, null);
    assertThat(newValues.get(profile1.getName()).toString()).contains("\"qualityProfileName\"").doesNotContain("\"groupName\"");
    assertThat(newValues.get(profile2.getName()))
      .extracting(UserEditorNewValue::getQualityProfileUuid, UserEditorNewValue::getUserLogin, UserEditorNewValue::getUserUuid)
      .containsExactly(profile2.getKee(), null, null);
    assertThat(newValues.get(profile2.getName()).toString()).contains("\"qualityProfileName\"").doesNotContain("\"groupName\"");

    assertThat(underTest.exists(db.getSession(), profile1, user1)).isFalse();
    assertThat(underTest.exists(db.getSession(), profile2, user2)).isFalse();
    assertThat(underTest.exists(db.getSession(), profile3, user1)).isTrue();
  }

  @Test
  void deleteByUser() {
    QProfileDto profile1 = db.qualityProfiles().insert();
    QProfileDto profile3 = db.qualityProfiles().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile1, user1);
    db.qualityProfiles().addUserPermission(profile3, user2);

    underTest.deleteByUser(db.getSession(), user1);

    verify(auditPersister).deleteQualityProfileEditor(eq(db.getSession()), newValueCaptor.capture());

    UserEditorNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(UserEditorNewValue::getQualityProfileName, UserEditorNewValue::getQualityProfileUuid,
        UserEditorNewValue::getUserLogin, UserEditorNewValue::getUserUuid)
      .containsExactly(null, null, user1.getLogin(), user1.getUuid());
    assertThat(newValue.toString()).doesNotContain("\"qualityProfileName\"").contains("\"userLogin\"");

    assertThat(underTest.exists(db.getSession(), profile1, user1)).isFalse();
    assertThat(underTest.exists(db.getSession(), profile3, user2)).isTrue();
  }

}
