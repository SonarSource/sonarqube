/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PermissionTemplateUpdaterTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private static final UserDto DEFAULT_USER = new UserDto().setId(1L).setLogin("user").setName("user");
  private static final GroupDto DEFAULT_GROUP = new GroupDto().setId(1L).setName("group");

  private DbClient dbClient = mock(DbClient.class);
  private UserDao userDao = mock(UserDao.class);
  private GroupDao groupDao = mock(GroupDao.class);

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void setUpCommonMocks() {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    stub(userDao.selectActiveUserByLogin("user")).toReturn(DEFAULT_USER);
    stub(groupDao.selectByName(any(DbSession.class), eq("group"))).toReturn(DEFAULT_GROUP);

    when(dbClient.userDao()).thenReturn(userDao);
    when(dbClient.groupDao()).thenReturn(groupDao);
    when(dbClient.permissionTemplateDao()).thenReturn(mock(PermissionTemplateDao.class));
  }

  @Test
  public void should_execute_on_valid_parameters() {

    final PermissionTemplateDao permissionTemplateDao = mock(PermissionTemplateDao.class);
    when(permissionTemplateDao.selectByUuid("my_template")).thenReturn(new PermissionTemplateDto().setId(1L));
    when(dbClient.permissionTemplateDao()).thenReturn(permissionTemplateDao);

    PermissionTemplateUpdater updater =
      new PermissionTemplateUpdater(dbClient, userSessionRule, "my_template", UserRole.USER, "user") {
        @Override
        void doExecute(Long templateId, String permission) {
          permissionTemplateDao.insertUserPermission(1L, 1L, UserRole.USER);
        }
      };
    updater.executeUpdate();

    verify(permissionTemplateDao, times(1)).insertUserPermission(1L, 1L, UserRole.USER);
  }

  @Test
  public void should_validate_template_reference() {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Unknown template:");

    final PermissionTemplateDao permissionTemplateDao = mock(PermissionTemplateDao.class);
    when(permissionTemplateDao.selectByUuid("my_template")).thenReturn(null);

    PermissionTemplateUpdater updater =
      new PermissionTemplateUpdater(dbClient, userSessionRule, "my_template", UserRole.USER, "user") {
        @Override
        void doExecute(Long templateId, String permission) {
        }
      };
    updater.executeUpdate();
  }

  @Test
  public void should_validate_permission_reference() {
    expected.expect(BadRequestException.class);

    final PermissionTemplateDao permissionTemplateDao = mock(PermissionTemplateDao.class);
    when(permissionTemplateDao.selectByUuid("my_template")).thenReturn(new PermissionTemplateDto().setId(1L));
    when(dbClient.permissionTemplateDao()).thenReturn(permissionTemplateDao);

    PermissionTemplateUpdater updater =
      new PermissionTemplateUpdater(dbClient, userSessionRule, "my_template", "invalid_permission", "user") {
        @Override
        void doExecute(Long templateId, String permission) {
        }
      };
    updater.executeUpdate();
  }

  @Test
  public void should_check_that_user_is_logged_in() {
    expected.expect(UnauthorizedException.class);
    expected.expectMessage("Authentication is required");

    userSessionRule.anonymous();

    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(dbClient, userSessionRule, null, null, null) {
      @Override
      void doExecute(Long templateId, String permission) {
      }
    };
    updater.executeUpdate();
  }

  @Test
  public void should_check_that_user_is_a_system_admin() {
    expected.expect(ForbiddenException.class);
    expected.expectMessage("Insufficient privileges");

    userSessionRule.login("user").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(dbClient, userSessionRule, null, null, null) {
      @Override
      void doExecute(Long templateId, String permission) {
      }
    };
    updater.executeUpdate();
  }
}
