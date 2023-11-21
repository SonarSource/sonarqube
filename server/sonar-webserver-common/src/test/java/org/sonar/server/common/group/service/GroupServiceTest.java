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
package org.sonar.server.common.group.service;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.sonar.api.security.DefaultGroups;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.AuthorizationDao;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.template.PermissionTemplateDao;
import org.sonar.db.provisioning.GithubOrganizationGroupDao;
import org.sonar.db.qualitygate.QualityGateGroupPermissionsDao;
import org.sonar.db.qualityprofile.QProfileEditGroupsDao;
import org.sonar.db.scim.ScimGroupDao;
import org.sonar.db.user.ExternalGroupDao;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.RoleDao;
import org.sonar.db.user.UserGroupDao;
import org.sonar.server.exceptions.BadRequestException;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class GroupServiceTest {

  private static final String GROUP_NAME = "GROUP_NAME";
  private static final String GROUP_UUID = "GROUP_UUID";
  private static final String DEFAULT_GROUP_NAME = "sonar-users";
  private static final String DEFAULT_GROUP_UUID = "DEFAULT_GROUP_UUID";
  @Mock
  private DbSession dbSession;
  @Mock
  private DbClient dbClient;
  @Mock
  private UuidFactory uuidFactory;
  @InjectMocks
  private GroupService groupService;

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    mockNeededDaos();
  }

  private void mockNeededDaos() {
    when(dbClient.authorizationDao()).thenReturn(mock(AuthorizationDao.class));
    when(dbClient.roleDao()).thenReturn(mock(RoleDao.class));
    when(dbClient.permissionTemplateDao()).thenReturn(mock(PermissionTemplateDao.class));
    when(dbClient.userGroupDao()).thenReturn(mock(UserGroupDao.class));
    when(dbClient.qProfileEditGroupsDao()).thenReturn(mock(QProfileEditGroupsDao.class));
    when(dbClient.qualityGateGroupPermissionsDao()).thenReturn(mock(QualityGateGroupPermissionsDao.class));
    when(dbClient.scimGroupDao()).thenReturn(mock(ScimGroupDao.class));
    when(dbClient.externalGroupDao()).thenReturn(mock(ExternalGroupDao.class));
    when(dbClient.groupDao()).thenReturn(mock(GroupDao.class));
    when(dbClient.githubOrganizationGroupDao()).thenReturn(mock(GithubOrganizationGroupDao.class));
  }

  @Test
  public void findGroup_whenGroupExists_returnsIt() {
    GroupDto groupDto = mockGroupDto();

    when(dbClient.groupDao().selectByName(dbSession, GROUP_NAME))
      .thenReturn(Optional.of(groupDto));

    assertThat(groupService.findGroup(dbSession, GROUP_NAME)).contains(groupDto);
  }

  @Test
  public void findGroup_whenGroupDoesntExist_returnsEmtpyOptional() {
    when(dbClient.groupDao().selectByName(dbSession, GROUP_NAME))
      .thenReturn(Optional.empty());

    assertThat(groupService.findGroup(dbSession, GROUP_NAME)).isEmpty();
  }

  @Test
  public void findGroupByUuid_whenGroupExists_returnsIt() {
    GroupDto groupDto = mockGroupDto();

    when(dbClient.groupDao().selectByUuid(dbSession, GROUP_UUID))
      .thenReturn(groupDto);

    assertThat(groupService.findGroupByUuid(dbSession, GROUP_UUID)).contains(groupDto);
  }

  @Test
  public void findGroupByUuid_whenGroupDoesntExist_returnsEmptyOptional() {
    when(dbClient.groupDao().selectByUuid(dbSession, GROUP_UUID))
      .thenReturn(null);

    assertThat(groupService.findGroupByUuid(dbSession, GROUP_UUID)).isEmpty();
  }

  @Test
  public void delete_whenNotDefaultAndNotLastAdminGroup_deleteGroup() {
    GroupDto groupDto = mockGroupDto();

    when(dbClient.groupDao().selectByName(dbSession, DefaultGroups.USERS))
      .thenReturn(Optional.of(new GroupDto().setUuid("another_group_uuid")));
    when(dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroup(dbSession, GlobalPermission.ADMINISTER.getKey(), groupDto.getUuid()))
      .thenReturn(2);

    groupService.delete(dbSession, groupDto);

    verifyGroupDelete(dbSession, groupDto);
  }

  @Test
  public void delete_whenDefaultGroup_throwAndDontDeleteGroup() {
    GroupDto groupDto = mockGroupDto();

    when(dbClient.groupDao().selectByName(dbSession, DefaultGroups.USERS))
      .thenReturn(Optional.of(groupDto));

    assertThatThrownBy(() -> groupService.delete(dbSession, groupDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Default group '%s' cannot be used to perform this action", GROUP_NAME));

    verifyNoGroupDelete(dbSession, groupDto);
  }

  @Test
  public void delete_whenLastAdminGroup_throwAndDontDeleteGroup() {
    GroupDto groupDto = mockGroupDto();

    when(dbClient.groupDao().selectByName(dbSession, DefaultGroups.USERS))
      .thenReturn(Optional.of(new GroupDto().setUuid("another_group_uuid"))); // We must pass the default group check
    when(dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroup(dbSession, GlobalPermission.ADMINISTER.getKey(), groupDto.getUuid()))
      .thenReturn(0);

    assertThatThrownBy(() -> groupService.delete(dbSession, groupDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The last system admin group cannot be deleted");

    verifyNoGroupDelete(dbSession, groupDto);
  }

  @Test
  public void updateGroup_updatesGroupNameAndDescription() {
    GroupDto group = mockGroupDto();
    GroupDto groupWithUpdatedName = mockGroupDto();
    mockDefaultGroup();
    when(dbClient.groupDao().update(dbSession, group)).thenReturn(groupWithUpdatedName);

    groupService.updateGroup(dbSession, group, "new-name", "New Description");
    verify(group).setName("new-name");
    verify(groupWithUpdatedName).setDescription("New Description");
    verify(dbClient.groupDao()).update(dbSession, group);
    verify(dbClient.groupDao()).update(dbSession, groupWithUpdatedName);
  }

  @Test
  public void updateGroup_updatesGroupName() {
    GroupDto group = mockGroupDto();
    mockDefaultGroup();

    groupService.updateGroup(dbSession, group, "new-name");
    verify(group).setName("new-name");
    verify(dbClient.groupDao()).update(dbSession, group);
  }

  @Test
  public void updateGroup_whenGroupIsDefault_throws() {
    GroupDto defaultGroup = mockDefaultGroup();
    when(dbClient.groupDao().selectByName(dbSession, DEFAULT_GROUP_NAME)).thenReturn(Optional.of(defaultGroup));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> groupService.updateGroup(dbSession, defaultGroup, "new-name", "New Description"))
      .withMessage("Default group 'sonar-users' cannot be used to perform this action");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> groupService.updateGroup(dbSession, defaultGroup, "new-name"))
      .withMessage("Default group 'sonar-users' cannot be used to perform this action");
  }

  @Test
  public void updateGroup_whenGroupNameDoesntChange_succeedsWithDescription() {
    GroupDto group = mockGroupDto();
    mockDefaultGroup();

    groupService.updateGroup(dbSession, group, group.getName(), "New Description");
    verify(group).setDescription("New Description");
    verify(dbClient.groupDao()).update(dbSession, group);
  }

  @Test
  public void updateGroup_whenGroupNameDoesntChange_succeeds() {
    GroupDto group = mockGroupDto();
    mockDefaultGroup();

    assertThatNoException()
      .isThrownBy(() -> groupService.updateGroup(dbSession, group, group.getName()));

    verify(dbClient.groupDao(), never()).update(dbSession, group);
  }

  @Test
  public void updateGroup_whenGroupExist_throws() {
    GroupDto group = mockGroupDto();
    GroupDto group2 = mockGroupDto();
    mockDefaultGroup();
    String group2Name = GROUP_NAME + "2";

    when(dbClient.groupDao().selectByName(dbSession, group2Name)).thenReturn(Optional.of(group2));

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> groupService.updateGroup(dbSession, group, group2Name, "New Description"))
      .withMessage("Group '" + group2Name + "' already exists");

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> groupService.updateGroup(dbSession, group, group2Name))
      .withMessage("Group '" + group2Name + "' already exists");
  }

  @Test
  @UseDataProvider("invalidGroupNames")
  public void updateGroup_whenGroupNameIsInvalid_throws(String groupName, String errorMessage) {
    GroupDto group = mockGroupDto();
    mockDefaultGroup();

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> groupService.updateGroup(dbSession, group, groupName, "New Description"))
      .withMessage(errorMessage);

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> groupService.updateGroup(dbSession, group, groupName))
      .withMessage(errorMessage);
  }

  @Test
  public void createGroup_whenNameAndDescriptionIsProvided_createsGroup() {

    when(uuidFactory.create()).thenReturn("1234");
    groupService.createGroup(dbSession, "Name", "Description");

    ArgumentCaptor<GroupDto> groupCaptor = ArgumentCaptor.forClass(GroupDto.class);
    verify(dbClient.groupDao()).insert(eq(dbSession), groupCaptor.capture());
    GroupDto createdGroup = groupCaptor.getValue();
    assertThat(createdGroup.getName()).isEqualTo("Name");
    assertThat(createdGroup.getDescription()).isEqualTo("Description");
    assertThat(createdGroup.getUuid()).isEqualTo("1234");
  }

  @Test
  public void createGroup_whenGroupExist_throws() {
    GroupDto group = mockGroupDto();

    when(dbClient.groupDao().selectByName(dbSession, GROUP_NAME)).thenReturn(Optional.of(group));

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> groupService.createGroup(dbSession, GROUP_NAME, "New Description"))
      .withMessage("Group '" + GROUP_NAME + "' already exists");

  }

  @Test
  @UseDataProvider("invalidGroupNames")
  public void createGroup_whenGroupNameIsInvalid_throws(String groupName, String errorMessage) {
    mockDefaultGroup();

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> groupService.createGroup(dbSession, groupName, "Description"))
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

  private static GroupDto mockGroupDto() {
    GroupDto groupDto = mock(GroupDto.class);
    when(groupDto.getName()).thenReturn(GROUP_NAME);
    when(groupDto.getUuid()).thenReturn(GROUP_UUID);
    return groupDto;
  }

  private GroupDto mockDefaultGroup() {
    GroupDto defaultGroup = mock(GroupDto.class);
    when(defaultGroup.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(defaultGroup.getUuid()).thenReturn(DEFAULT_GROUP_UUID);
    when(dbClient.groupDao().selectByName(dbSession, DEFAULT_GROUP_NAME)).thenReturn(Optional.of(defaultGroup));
    return defaultGroup;
  }

  private void verifyNoGroupDelete(DbSession dbSession, GroupDto groupDto) {
    verify(dbClient.roleDao(), never()).deleteGroupRolesByGroupUuid(dbSession, groupDto.getUuid());
    verify(dbClient.permissionTemplateDao(), never()).deleteByGroup(dbSession, groupDto.getUuid(), groupDto.getName());
    verify(dbClient.userGroupDao(), never()).deleteByGroupUuid(dbSession, groupDto.getUuid(), groupDto.getName());
    verify(dbClient.qProfileEditGroupsDao(), never()).deleteByGroup(dbSession, groupDto);
    verify(dbClient.qualityGateGroupPermissionsDao(), never()).deleteByGroup(dbSession, groupDto);
    verify(dbClient.scimGroupDao(), never()).deleteByGroupUuid(dbSession, groupDto.getUuid());
    verify(dbClient.groupDao(), never()).deleteByUuid(dbSession, groupDto.getUuid(), groupDto.getName());
    verify(dbClient.githubOrganizationGroupDao(), never()).deleteByGroupUuid(dbSession, groupDto.getUuid());
  }

  private void verifyGroupDelete(DbSession dbSession, GroupDto groupDto) {
    verify(dbClient.roleDao()).deleteGroupRolesByGroupUuid(dbSession, groupDto.getUuid());
    verify(dbClient.permissionTemplateDao()).deleteByGroup(dbSession, groupDto.getUuid(), groupDto.getName());
    verify(dbClient.userGroupDao()).deleteByGroupUuid(dbSession, groupDto.getUuid(), groupDto.getName());
    verify(dbClient.qProfileEditGroupsDao()).deleteByGroup(dbSession, groupDto);
    verify(dbClient.qualityGateGroupPermissionsDao()).deleteByGroup(dbSession, groupDto);
    verify(dbClient.scimGroupDao()).deleteByGroupUuid(dbSession, groupDto.getUuid());
    verify(dbClient.externalGroupDao()).deleteByGroupUuid(dbSession, groupDto.getUuid());
    verify(dbClient.groupDao()).deleteByUuid(dbSession, groupDto.getUuid(), groupDto.getName());
    verify(dbClient.githubOrganizationGroupDao()).deleteByGroupUuid(dbSession, groupDto.getUuid());
  }
}
