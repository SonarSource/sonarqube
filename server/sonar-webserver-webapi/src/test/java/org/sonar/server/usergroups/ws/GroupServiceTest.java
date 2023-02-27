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

import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.security.DefaultGroups;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.AuthorizationDao;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.template.PermissionTemplateDao;
import org.sonar.db.qualitygate.QualityGateGroupPermissionsDao;
import org.sonar.db.qualityprofile.QProfileEditGroupsDao;
import org.sonar.db.scim.ScimGroupDao;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.RoleDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDao;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GroupServiceTest {
  private static final String GROUP_NAME = "GROUP_NAME";
  private static final String GROUP_UUID = "GROUP_UUID";
  @Mock
  private DbSession dbSession;
  @Mock
  private DbClient dbClient;
  @InjectMocks
  private GroupService groupService;

  @Before
  public void setUp() {
    mockNeededDaos();
  }

  @Test
  public void findGroupDtoOrThrow_whenGroupExists_returnsIt() {
    GroupDto groupDto = mockGroupDto();

    when(dbClient.groupDao().selectByName(dbSession, GROUP_NAME))
      .thenReturn(Optional.of(groupDto));

    assertThat(groupService.findGroupDtoOrThrow(dbSession, GROUP_NAME))
      .isEqualTo(groupDto);
  }

  @Test
  public void findGroupDtoOrThrow_whenGroupDoesntExist_throw() {
    when(dbClient.groupDao().selectByName(dbSession, GROUP_NAME))
      .thenReturn(Optional.empty());

    assertThatThrownBy(() -> groupService.findGroupDtoOrThrow(dbSession, GROUP_NAME))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("No group with name '%s'", GROUP_NAME));
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
  public void deleteAllScimUsersByGroup_() {
    GroupDto groupDto = mockGroupDto();
    Set<UserDto> userDtos = Set.of(new UserDto(), new UserDto());

    when(dbClient.userGroupDao().selectScimMembersByGroupUuid(dbSession, groupDto))
      .thenReturn(userDtos);

    groupService.deleteScimMembersByGroup(dbSession, groupDto);

    verify(dbClient.userGroupDao()).deleteFromGroupByUserUuids(dbSession, groupDto, userDtos);
  }

  private void mockNeededDaos() {
    when(dbClient.authorizationDao()).thenReturn(mock(AuthorizationDao.class));
    when(dbClient.roleDao()).thenReturn(mock(RoleDao.class));
    when(dbClient.permissionTemplateDao()).thenReturn(mock(PermissionTemplateDao.class));
    when(dbClient.userGroupDao()).thenReturn(mock(UserGroupDao.class));
    when(dbClient.qProfileEditGroupsDao()).thenReturn(mock(QProfileEditGroupsDao.class));
    when(dbClient.qualityGateGroupPermissionsDao()).thenReturn(mock(QualityGateGroupPermissionsDao.class));
    when(dbClient.scimGroupDao()).thenReturn(mock(ScimGroupDao.class));
    when(dbClient.groupDao()).thenReturn(mock(GroupDao.class));
  }

  private static GroupDto mockGroupDto() {
    GroupDto groupDto = mock(GroupDto.class);
    when(groupDto.getName()).thenReturn(GROUP_NAME);
    when(groupDto.getUuid()).thenReturn(GROUP_UUID);
    return groupDto;
  }

  private void verifyNoGroupDelete(DbSession dbSession, GroupDto groupDto) {
    verify(dbClient.roleDao(), never()).deleteGroupRolesByGroupUuid(dbSession, groupDto.getUuid());
    verify(dbClient.permissionTemplateDao(), never()).deleteByGroup(dbSession, groupDto.getUuid(), groupDto.getName());
    verify(dbClient.userGroupDao(), never()).deleteByGroupUuid(dbSession, groupDto.getUuid(), groupDto.getName());
    verify(dbClient.qProfileEditGroupsDao(), never()).deleteByGroup(dbSession, groupDto);
    verify(dbClient.qualityGateGroupPermissionsDao(), never()).deleteByGroup(dbSession, groupDto);
    verify(dbClient.scimGroupDao(), never()).deleteByGroupUuid(dbSession, groupDto.getUuid());
    verify(dbClient.groupDao(), never()).deleteByUuid(dbSession, groupDto.getUuid(), groupDto.getName());
  }

  private void verifyGroupDelete(DbSession dbSession, GroupDto groupDto) {
    verify(dbClient.roleDao()).deleteGroupRolesByGroupUuid(dbSession, groupDto.getUuid());
    verify(dbClient.permissionTemplateDao()).deleteByGroup(dbSession, groupDto.getUuid(), groupDto.getName());
    verify(dbClient.userGroupDao()).deleteByGroupUuid(dbSession, groupDto.getUuid(), groupDto.getName());
    verify(dbClient.qProfileEditGroupsDao()).deleteByGroup(dbSession, groupDto);
    verify(dbClient.qualityGateGroupPermissionsDao()).deleteByGroup(dbSession, groupDto);
    verify(dbClient.scimGroupDao()).deleteByGroupUuid(dbSession, groupDto.getUuid());
    verify(dbClient.groupDao()).deleteByUuid(dbSession, groupDto.getUuid(), groupDto.getName());
  }
}
