/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDao;
import org.sonar.db.user.UserGroupDto;
import org.sonar.db.user.UserGroupQuery;
import org.sonar.server.common.SearchResults;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GroupMembershipServiceTest {
  private static final String GROUP_A = "group_a";
  private static final String USER_1 = "user_1";
  private static final String UUID = "1";
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private UserGroupDao userGroupDao;
  @Mock
  private UserDao userDao;
  @Mock
  private GroupDao groupDao;
  @Mock
  private DbSession dbSession;
  @InjectMocks
  private GroupMembershipService groupMembershipService;

  @Before
  public void setup() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  @Test
  public void addMembership_ifGroupAndUserNotFound_shouldThrow() {
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> groupMembershipService.addMembership(GROUP_A, USER_1))
      .withMessage("User 'user_1' not found");
  }

  @Test
  public void addMembership_ifGroupNotFound_shouldThrow() {
    mockUserDto();

    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> groupMembershipService.addMembership(GROUP_A, USER_1))
      .withMessage("Group 'group_a' not found");
  }

  @Test
  public void addMembership_ifGroupAndUserFound_shouldAddMemberToGroup() {
    GroupDto groupDto = mockGroupDto();
    UserDto userDto = mockUserDto();

    UserGroupDto userGroupDto = groupMembershipService.addMembership(GROUP_A, USER_1);

    assertThat(userGroupDto.getGroupUuid()).isEqualTo(groupDto.getUuid());
    assertThat(userGroupDto.getUserUuid()).isEqualTo(userDto.getUuid());

    verify(userGroupDao).insert(dbSession, new UserGroupDto().setGroupUuid(GROUP_A).setUserUuid(USER_1), groupDto.getName(), userDto.getLogin());
    verify(dbSession).commit();
  }

  @Test
  public void removeMembership_ifGroupAndUserNotFound_shouldThrow() {
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> groupMembershipService.removeMembership(GROUP_A, USER_1))
      .withMessage("User 'user_1' not found");
  }

  @Test
  public void removeMembership_ifGroupNotFound_shouldThrow() {
    mockUserDto();

    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> groupMembershipService.removeMembership(GROUP_A, USER_1))
      .withMessage("Group 'group_a' not found");
  }

  @Test
  public void removeMembership_ifLastAdmin_shouldThrow() {
    mockUserDto();
    mockGroupDto();

    assertThatExceptionOfType(BadRequestException.class)
      .isThrownBy(() -> groupMembershipService.removeMembership(GROUP_A, USER_1))
      .withMessage("The last administrator user cannot be removed");
  }

  @Test
  public void removeMembership_ifGroupAndUserFound_shouldRemoveMemberFromGroup() {
    mockAdminInGroup(GROUP_A, USER_1);
    GroupDto groupDto = mockGroupDto();
    UserDto userDto = mockUserDto();

    groupMembershipService.removeMembership(GROUP_A, USER_1);

    verify(userGroupDao).delete(dbSession, groupDto, userDto);
    verify(dbSession).commit();
  }

  @Test
  public void removeMemberByMembershipUuid_ifMembershipNotFound_shouldThrow() {
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> groupMembershipService.removeMembership(UUID))
      .withMessage("Group membership '1' not found");
  }

  @Test
  public void removeMemberByMembershipUuid_ifFound_shouldRemoveMemberFromGroup() {
    mockAdminInGroup(GROUP_A, USER_1);

    GroupDto groupDto = mockGroupDto();
    UserDto userDto = mockUserDto();
    UserGroupDto userGroupDto = new UserGroupDto().setUuid(UUID).setUserUuid(USER_1).setGroupUuid(GROUP_A);
    when(userGroupDao.selectByQuery(any(), any(), anyInt(), anyInt())).thenReturn(List.of(userGroupDto));

    groupMembershipService.removeMembership(UUID);

    verify(userGroupDao).selectByQuery(dbSession, new UserGroupQuery(UUID, null, null), 1, 1);
    verify(userGroupDao).delete(dbSession, groupDto, userDto);
    verify(dbSession).commit();
  }

  @Test
  public void searchMembers_shouldReturnMembers() {
    GroupDto groupDto = mockGroupDto();
    UserDto userDto = mockUserDto();

    UserGroupDto result = mock(UserGroupDto.class);
    when(userGroupDao.selectByQuery(any(), any(), anyInt(), anyInt())).thenReturn(List.of(result));
    when(userGroupDao.countByQuery(any(), any())).thenReturn(10);

    GroupMembershipSearchRequest searchRequest = new GroupMembershipSearchRequest(groupDto.getUuid(), userDto.getUuid());
    SearchResults<UserGroupDto> userGroupDtoSearchResults = groupMembershipService.searchMembers(searchRequest, 1, 10);

    assertThat(userGroupDtoSearchResults.searchResults()).containsOnly(result);
    assertThat(userGroupDtoSearchResults.total()).isEqualTo(10);

    verify(userGroupDao).selectByQuery(dbSession, new UserGroupQuery(null, groupDto.getUuid(), userDto.getUuid()), 1, 10);
    verify(userGroupDao).countByQuery(dbSession, new UserGroupQuery(null, groupDto.getUuid(), userDto.getUuid()));
  }

  @Test
  public void searchMembers_withPageSizeEquals0_shouldOnlyComputeTotal() {
    when(userGroupDao.countByQuery(any(), any())).thenReturn(10);

    GroupMembershipSearchRequest searchRequest = new GroupMembershipSearchRequest(GROUP_A, USER_1);
    SearchResults<UserGroupDto> userGroupDtoSearchResults = groupMembershipService.searchMembers(searchRequest, 1, 0);

    assertThat(userGroupDtoSearchResults.searchResults()).isEmpty();
    assertThat(userGroupDtoSearchResults.total()).isEqualTo(10);

    verify(userGroupDao, never()).selectByQuery(any(), any(), anyInt(), anyInt());
    verify(userGroupDao).countByQuery(dbSession, new UserGroupQuery(null, GROUP_A, USER_1));
  }

  private UserDto mockUserDto() {
    UserDto userDto = mock(UserDto.class);
    when(userDto.getUuid()).thenReturn(USER_1);
    when(userDto.getLogin()).thenReturn("loginA");
    when(userDto.isActive()).thenReturn(true);
    when(userDao.selectByUuid(dbSession, USER_1)).thenReturn(userDto);
    return userDto;
  }

  private GroupDto mockGroupDto() {
    GroupDto groupDto = mock(GroupDto.class);
    when(groupDto.getUuid()).thenReturn(GROUP_A);
    when(groupDto.getName()).thenReturn("name_" + GROUP_A);
    when(groupDao.selectByUuid(dbSession, GROUP_A)).thenReturn(groupDto);
    return groupDto;
  }

  private void mockAdminInGroup(String groupUuid, String userUuid) {
    when(dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroupMember(dbSession,
      GlobalPermission.ADMINISTER.getKey(), groupUuid, userUuid)).thenReturn(1);
  }

}
