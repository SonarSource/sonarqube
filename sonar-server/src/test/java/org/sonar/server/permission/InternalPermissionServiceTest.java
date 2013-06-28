/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.user.*;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.MockUserSession;

import java.util.Map;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.sonar.server.permission.InternalPermissionServiceTest.MatchesGroupRole.matchesRole;
import static org.sonar.server.permission.InternalPermissionServiceTest.MatchesUserRole.matchesRole;

public class InternalPermissionServiceTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  private Map<String, Object> params;
  private InternalPermissionService service;
  private RoleDao roleDao;
  private UserDao userDao;

  @Before
  public void setUp() {
    MockUserSession.set().setLogin("admin").setPermissions(Permissions.SYSTEM_ADMIN);

    UserDto targetedUser = new UserDto().setId(2L).setLogin("user").setActive(true);
    GroupDto targetedGroup = new GroupDto().setId(2L).setName("group");

    roleDao = mock(RoleDao.class);
    when(roleDao.selectUserPermissions("user")).thenReturn(Lists.newArrayList(Permissions.QUALITY_PROFILE_ADMIN));
    when(roleDao.selectGroupPermissions("group")).thenReturn(Lists.newArrayList(Permissions.QUALITY_PROFILE_ADMIN));

    userDao = mock(UserDao.class);
    when(userDao.selectActiveUserByLogin("user")).thenReturn(targetedUser);
    when(userDao.selectGroupByName("group")).thenReturn(targetedGroup);

    service = new InternalPermissionService(roleDao, userDao);
  }

  @Test
  public void should_add_user_permission() throws Exception {
    params = buildParams("user", null, Permissions.DASHBOARD_SHARING);
    UserRoleDto roleToInsert = new UserRoleDto().setUserId(2L).setRole(Permissions.DASHBOARD_SHARING);

    service.addPermission(params);

    verify(roleDao).insertUserRole(argThat(matchesRole(roleToInsert)));
  }

  @Test
  public void should_remove_user_permission() throws Exception {
    params = buildParams("user", null, Permissions.QUALITY_PROFILE_ADMIN);
    UserRoleDto roleToRemove = new UserRoleDto().setUserId(2L).setRole(Permissions.QUALITY_PROFILE_ADMIN);

    service.removePermission(params);

    verify(roleDao).deleteUserRole(argThat(matchesRole(roleToRemove)));
  }

  @Test
  public void should_add_group_permission() throws Exception {
    params = buildParams(null, "group", Permissions.DASHBOARD_SHARING);
    GroupRoleDto roleToInsert = new GroupRoleDto().setGroupId(2L).setRole(Permissions.DASHBOARD_SHARING);

    service.addPermission(params);

    verify(roleDao).insertGroupRole(argThat(matchesRole(roleToInsert)));
  }

  @Test
  public void should_remove_group_permission() throws Exception {
    params = buildParams(null, "group", Permissions.QUALITY_PROFILE_ADMIN);
    GroupRoleDto roleToRemove = new GroupRoleDto().setGroupId(2L).setRole(Permissions.QUALITY_PROFILE_ADMIN);

    service.removePermission(params);

    verify(roleDao).deleteGroupRole(argThat(matchesRole(roleToRemove)));
  }

  @Test
  public void should_skip_redundant_permission_change() throws Exception {
    params = buildParams("user", null, Permissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(roleDao, never()).insertUserRole(any(UserRoleDto.class));
  }

  @Test
  public void should_fail_on_invalid_request() throws Exception {
    throwable.expect(IllegalArgumentException.class);
    params = buildParams("user", "group", Permissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);
  }

  @Test
  public void should_fail_on_insufficient_rights() throws Exception {
    throwable.expect(ForbiddenException.class);
    params = buildParams("user", null, Permissions.QUALITY_PROFILE_ADMIN);

    MockUserSession.set().setLogin("unauthorized").setPermissions(Permissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);
  }

  @Test
  public void should_prevent_last_admin_removal() throws Exception {
    throwable.expect(BadRequestException.class);
    params = buildParams("admin", null, Permissions.SYSTEM_ADMIN);
    when(roleDao.countSystemAdministrators(null)).thenReturn(0);

    service.removePermission(params);
  }

  @Test
  public void should_prevent_last_admin_group_removal() throws Exception {
    throwable.expect(BadRequestException.class);
    params = buildParams(null, "sonar-administrators", Permissions.SYSTEM_ADMIN);
    GroupDto adminGroups = new GroupDto().setId(2L).setName("sonar-administrators");

    roleDao = mock(RoleDao.class);
    when(roleDao.selectGroupPermissions("sonar-administrators")).thenReturn(Lists.newArrayList(Permissions.SYSTEM_ADMIN));
    when(roleDao.countSystemAdministrators("sonar-administrators")).thenReturn(0);

    userDao = mock(UserDao.class);
    when(userDao.selectGroupByName("sonar-administrators")).thenReturn(adminGroups);

    service = new InternalPermissionService(roleDao, userDao);
    service.removePermission(params);
  }

  @Test
  public void should_fail_on_anonymous_access() throws Exception {
    throwable.expect(ForbiddenException.class);
    params = buildParams("user", null, Permissions.QUALITY_PROFILE_ADMIN);

    MockUserSession.set();

    service.addPermission(params);
  }

  protected static class MatchesUserRole extends BaseMatcher<UserRoleDto> {

    private final UserRoleDto referenceDto;

    private MatchesUserRole(UserRoleDto referenceDto) {
      this.referenceDto = referenceDto;
    }

    public static MatchesUserRole matchesRole(UserRoleDto referenceDto) {
      return new MatchesUserRole(referenceDto);
    }

    @Override
    public boolean matches(Object o) {
      if(o != null && o instanceof UserRoleDto) {
        UserRoleDto otherDto = (UserRoleDto) o;
        return ObjectUtils.equals(referenceDto.getResourceId(), otherDto.getResourceId()) &&
          ObjectUtils.equals(referenceDto.getRole(), otherDto.getRole()) &&
          ObjectUtils.equals(referenceDto.getUserId(), otherDto.getUserId());
      }
      return false;
    }

    @Override
    public void describeTo(Description description) {
    }
  }

  protected static class MatchesGroupRole extends BaseMatcher<GroupRoleDto> {

    private final GroupRoleDto referenceDto;

    private MatchesGroupRole(GroupRoleDto referenceDto) {
      this.referenceDto = referenceDto;
    }

    public static MatchesGroupRole matchesRole(GroupRoleDto referenceDto) {
      return new MatchesGroupRole(referenceDto);
    }

    @Override
    public boolean matches(Object o) {
      if(o != null && o instanceof GroupRoleDto) {
        GroupRoleDto otherDto = (GroupRoleDto) o;
        return ObjectUtils.equals(referenceDto.getResourceId(), otherDto.getResourceId()) &&
          ObjectUtils.equals(referenceDto.getRole(), otherDto.getRole()) &&
          ObjectUtils.equals(referenceDto.getGroupId(), otherDto.getGroupId());
      }
      return false;
    }

    @Override
    public void describeTo(Description description) {
    }
  }


  private Map<String, Object> buildParams(String login, String group, String role) {
    Map<String, Object> params = Maps.newHashMap();
    params.put("user", login);
    params.put("group", group);
    params.put("permission", role);
    return params;
  }
}
