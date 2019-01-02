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
package org.sonar.db.user;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;

public class UserGroupDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();

  private UserGroupDao underTest = dbTester.getDbClient().userGroupDao();

  @Test
  public void insert() {
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup();
    UserGroupDto userGroupDto = new UserGroupDto().setUserId(user.getId()).setGroupId(group.getId());

    underTest.insert(dbTester.getSession(), userGroupDto);
    dbTester.getSession().commit();

    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupIdsByUserId(dbTester.getSession(), user.getId())).containsOnly(group.getId());
  }

  @Test
  public void delete_members_by_group_id() {
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    dbTester.users().insertMember(group1, user1);
    dbTester.users().insertMember(group1, user2);
    dbTester.users().insertMember(group2, user1);
    dbTester.users().insertMember(group2, user2);

    underTest.deleteByGroupId(dbTester.getSession(), group1.getId());
    dbTester.getSession().commit();

    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupIdsByUserId(dbTester.getSession(), user1.getId())).containsOnly(group2.getId());
    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupIdsByUserId(dbTester.getSession(), user2.getId())).containsOnly(group2.getId());
  }

  @Test
  public void delete_organization_member() {
    OrganizationDto organization = dbTester.organizations().insert();
    OrganizationDto anotherOrganization = dbTester.organizations().insert();
    UserDto user = dbTester.users().insertUser();
    UserDto anotherUser = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup(organization);
    GroupDto anotherGroup = dbTester.users().insertGroup(anotherOrganization);
    dbTester.users().insertMembers(group, user, anotherUser);
    dbTester.users().insertMembers(anotherGroup, user, anotherUser);

    underTest.deleteByOrganizationAndUser(dbSession, organization.getUuid(), user.getId());

    assertThat(dbClient.groupMembershipDao().selectGroupIdsByUserId(dbSession, user.getId()))
      .containsOnly(anotherGroup.getId());
    assertThat(dbClient.groupMembershipDao().selectGroupIdsByUserId(dbSession, anotherUser.getId()))
      .containsOnly(group.getId(), anotherGroup.getId());
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

    underTest.deleteByUserId(dbTester.getSession(), user1.getId());
    dbTester.getSession().commit();

    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupIdsByUserId(dbTester.getSession(), user1.getId())).isEmpty();
    assertThat(dbTester.getDbClient().groupMembershipDao().selectGroupIdsByUserId(dbTester.getSession(), user2.getId())).containsOnly(group1.getId(), group2.getId());
  }
}
