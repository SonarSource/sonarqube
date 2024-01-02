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
package org.sonar.db.user;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserGroupNewValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class UserGroupDaoWithPersisterIT {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE, auditPersister);

  private final ArgumentCaptor<UserGroupNewValue> newValueCaptor = ArgumentCaptor.forClass(UserGroupNewValue.class);

  private final DbClient dbClient = db.getDbClient();
  private final UserGroupDao underTest = dbClient.userGroupDao();

  @Test
  public void insertUserGroupIsPersisted() {
    UserDto user = db.users().insertUser();

    verify(auditPersister).addUser(eq(db.getSession()), any());

    GroupDto group = db.users().insertGroup();
    UserGroupDto userGroupDto = new UserGroupDto().setUserUuid(user.getUuid()).setGroupUuid(group.getUuid());
    underTest.insert(db.getSession(), userGroupDto, group.getName(), user.getLogin());
    db.getSession().commit();

    verify(auditPersister).addUserToGroup(eq(db.getSession()), newValueCaptor.capture());
    assertThat(db.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(db.getSession(), user.getUuid())).containsOnly(group.getUuid());
    assertThat(newValueCaptor.getValue())
      .extracting(UserGroupNewValue::getGroupUuid, UserGroupNewValue::getName, UserGroupNewValue::getUserUuid, UserGroupNewValue::getUserLogin)
      .containsExactly(group.getUuid(), group.getName(), user.getUuid(), user.getLogin());
  }

  @Test
  public void deleteUserGroupByGroupIsPersisted() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertMember(group1, user1);
    db.users().insertMember(group1, user2);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group2, user2);
    underTest.deleteByGroupUuid(db.getSession(), group1.getUuid(), group1.getName());
    db.getSession().commit();

    assertThat(db.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(db.getSession(), user1.getUuid())).containsOnly(group2.getUuid());
    assertThat(db.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(db.getSession(), user2.getUuid())).containsOnly(group2.getUuid());
    verify(auditPersister).deleteUserFromGroup(eq(db.getSession()), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue())
      .extracting(UserGroupNewValue::getGroupUuid, UserGroupNewValue::getName)
      .containsExactly(group1.getUuid(), group1.getName());
  }

  @Test
  public void deleteUserGroupByGroupWithoutAffectedRowsIsNotPersisted() {
    GroupDto group1 = db.users().insertGroup();
    underTest.deleteByGroupUuid(db.getSession(), group1.getUuid(), group1.getName());
    db.getSession().commit();

    verify(auditPersister).addUserGroup(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void deleteUserGroupByUserIsPersisted() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertMember(group1, user1);
    db.users().insertMember(group1, user2);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group2, user2);
    underTest.deleteByUserUuid(db.getSession(), user1);
    db.getSession().commit();

    assertThat(db.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(db.getSession(), user1.getUuid())).isEmpty();
    assertThat(db.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(db.getSession(), user2.getUuid())).containsOnly(group1.getUuid(), group2.getUuid());
    verify(auditPersister).deleteUserFromGroup(eq(db.getSession()), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue())
      .extracting(UserGroupNewValue::getUserUuid, UserGroupNewValue::getUserLogin)
      .containsExactly(user1.getUuid(), user1.getLogin());
  }

  @Test
  public void deleteUserGroupByUserWithoutAffectedRowsIsNotPersisted() {
    UserDto user1 = db.users().insertUser();
    underTest.deleteByUserUuid(db.getSession(), user1);
    db.getSession().commit();

    verify(auditPersister).addUser(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void delete_by_user_and_group() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    db.users().insertMember(group1, user1);
    db.users().insertMember(group1, user2);
    underTest.delete(db.getSession(), group1, user1);
    db.getSession().commit();

    assertThat(db.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(db.getSession(), user1.getUuid())).isEmpty();
    assertThat(db.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(db.getSession(), user2.getUuid())).containsOnly(group1.getUuid());
    verify(auditPersister).deleteUserFromGroup(eq(db.getSession()), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue())
      .extracting(UserGroupNewValue::getGroupUuid, UserGroupNewValue::getName, UserGroupNewValue::getUserUuid, UserGroupNewValue::getUserLogin)
      .containsExactly(group1.getUuid(), group1.getName(), user1.getUuid(), user1.getLogin());
  }

  @Test
  public void deleteByUserAndGroupWithoutAffectedRowsIsNotPersisted() {
    UserDto user1 = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    underTest.delete(db.getSession(), group1, user1);
    db.getSession().commit();

    verify(auditPersister).addUser(any(), any());
    verify(auditPersister).addUserGroup(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }
}
