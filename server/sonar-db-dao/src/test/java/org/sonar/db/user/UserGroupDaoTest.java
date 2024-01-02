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

import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class UserGroupDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = dbTester.getSession();
  private final UserGroupDao underTest = dbTester.getDbClient().userGroupDao();

  @Test
  public void insert() {
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup();
    UserGroupDto userGroupDto = new UserGroupDto().setUserUuid(user.getUuid()).setGroupUuid(group.getUuid());

    underTest.insert(dbTester.getSession(), userGroupDto, group.getName(), user.getLogin());
    dbTester.getSession().commit();

    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(dbTester.getSession(), user.getUuid())).containsOnly(group.getUuid());
  }

  @Test
  public void select_user_uuids_in_group() {
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    UserDto user3 = dbTester.users().insertUser();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    UserGroupDto userGroupDto1 = new UserGroupDto().setUserUuid(user1.getUuid()).setGroupUuid(group1.getUuid());
    UserGroupDto userGroupDto2 = new UserGroupDto().setUserUuid(user2.getUuid()).setGroupUuid(group2.getUuid());
    UserGroupDto userGroupDto3 = new UserGroupDto().setUserUuid(user3.getUuid()).setGroupUuid(group2.getUuid());
    underTest.insert(dbSession, userGroupDto1, group1.getName(), user1.getLogin());
    underTest.insert(dbSession, userGroupDto2, group2.getName(), user2.getLogin());
    underTest.insert(dbSession, userGroupDto3, group2.getName(), user3.getLogin());
    dbTester.getSession().commit();

    Set<String> userUuids = underTest.selectUserUuidsInGroup(dbTester.getSession(), group2.getUuid());

    assertThat(userUuids).containsExactlyInAnyOrder(user2.getUuid(), user3.getUuid());
  }

  @Test
  public void select_user_uuids_in_group_returns_empty_set_when_nothing_found() {
    UserDto user1 = dbTester.users().insertUser();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    UserGroupDto userGroupDto1 = new UserGroupDto().setUserUuid(user1.getUuid()).setGroupUuid(group1.getUuid());
    underTest.insert(dbSession, userGroupDto1, group1.getName(), user1.getLogin());
    dbTester.getSession().commit();

    Set<String> userUuids = underTest.selectUserUuidsInGroup(dbTester.getSession(), group2.getUuid());

    assertThat(userUuids).isEmpty();
  }

  @Test
  public void delete_members_by_group_uuid() {
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    dbTester.users().insertMember(group1, user1);
    dbTester.users().insertMember(group1, user2);
    dbTester.users().insertMember(group2, user1);
    dbTester.users().insertMember(group2, user2);

    underTest.deleteByGroupUuid(dbTester.getSession(), group1.getUuid(), group1.getName());
    dbTester.getSession().commit();

    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(dbTester.getSession(), user1.getUuid())).containsOnly(group2.getUuid());
    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(dbTester.getSession(), user2.getUuid())).containsOnly(group2.getUuid());
  }

  @Test
  public void delete_by_user() {
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    dbTester.users().insertMember(group1, user1);
    dbTester.users().insertMember(group1, user2);
    dbTester.users().insertMember(group2, user1);
    dbTester.users().insertMember(group2, user2);

    underTest.deleteByUserUuid(dbTester.getSession(), user1);
    dbTester.getSession().commit();

    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(dbTester.getSession(), user1.getUuid())).isEmpty();
    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(dbTester.getSession(), user2.getUuid())).containsOnly(group1.getUuid(), group2.getUuid());
  }
}
