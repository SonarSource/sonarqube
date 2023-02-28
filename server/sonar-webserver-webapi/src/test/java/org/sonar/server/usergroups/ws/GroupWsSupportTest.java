/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.usergroups.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

@RunWith(DataProviderRunner.class)
public class GroupWsSupportTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final GroupWsSupport groupWsSupport = new GroupWsSupport(db.getDbClient(), new DefaultGroupFinder(db.getDbClient()));

  @Test
  public void updateGroup_updatesGroupNameAndDescription() {
    db.users().insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    groupWsSupport.updateGroup(db.getSession(), group, "new-name", "New Description");
    GroupDto updatedGroup = db.getDbClient().groupDao().selectByUuid(db.getSession(), group.getUuid());
    assertThat(updatedGroup.getName()).isEqualTo("new-name");
    assertThat(updatedGroup.getDescription()).isEqualTo("New Description");
  }

  public void updateGroup_updatesGroupName() {
    db.users().insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    groupWsSupport.updateGroup(db.getSession(), group, "new-name");
    GroupDto updatedGroup = db.getDbClient().groupDao().selectByUuid(db.getSession(), group.getUuid());
    assertThat(updatedGroup.getName()).isEqualTo("new-name");
  }

  @Test
  public void updateGroup_whenGroupIsDefault_throws() {
    GroupDto defaultGroup = db.users().insertDefaultGroup();
    DbSession session = db.getSession();
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> groupWsSupport.updateGroup(session, defaultGroup, "new-name", "New Description"))
      .withMessage("Default group 'sonar-users' cannot be used to perform this action");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> groupWsSupport.updateGroup(session, defaultGroup, "new-name"))
      .withMessage("Default group 'sonar-users' cannot be used to perform this action");
  }

  @Test
  public void updateGroup_whenGroupNameDoesntChange_succeedsWithDescription() {
    db.users().insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    groupWsSupport.updateGroup(db.getSession(), group, group.getName(), "New Description");
    GroupDto updatedGroup = db.getDbClient().groupDao().selectByUuid(db.getSession(), group.getUuid());
    assertThat(updatedGroup.getDescription()).isEqualTo("New Description");
  }

  public void updateGroup_whenGroupNameDoesntChange_succeeds() {
    db.users().insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    assertThatNoException()
      .isThrownBy(() -> groupWsSupport.updateGroup(db.getSession(), group, group.getName()));
  }

  @Test
  public void updateGroup_whenGroupExist_throws() {
    db.users().insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    DbSession session = db.getSession();
    String group2Name = group2.getName();

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> groupWsSupport.updateGroup(session, group, group2Name, "New Description"))
      .withMessage("Group '" + group2Name + "' already exists");

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> groupWsSupport.updateGroup(session, group, group2Name))
      .withMessage("Group '" + group2Name + "' already exists");
  }

  @Test
  @UseDataProvider("invalidGroupNames")
  public void updateGroup_whenGroupNameIsInvalid_throws(String groupName, String errorMessage) {
    db.users().insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    DbSession session = db.getSession();

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> groupWsSupport.updateGroup(session, group, groupName, "New Description"))
      .withMessage(errorMessage);

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> groupWsSupport.updateGroup(session, group, groupName))
      .withMessage(errorMessage);
  }

  @DataProvider
  public static Object[][] invalidGroupNames() {
    return new Object[][] {
      {"", "Group name cannot be empty"},
      {randomAlphanumeric(256), "Group name cannot be longer than 255 characters"},
      {"Anyone", "Anyone group cannot be used"},
    };
  }

}
