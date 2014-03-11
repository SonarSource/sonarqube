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
import org.sonar.core.permission.PermissionTemplateDao;
import org.sonar.core.permission.PermissionTemplateDto;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.MockUserSession;

import static org.mockito.Mockito.*;

public class PermissionTemplateUpdaterTest {

  private static final UserDto DEFAULT_USER = new UserDto().setId(1L).setLogin("user").setName("user");
  private static final GroupDto DEFAULT_GROUP = new GroupDto().setId(1L).setName("group");

  private UserDao userDao;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void setUpCommonMocks() {
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    userDao = mock(UserDao.class);
    stub(userDao.selectActiveUserByLogin("user")).toReturn(DEFAULT_USER);
    stub(userDao.selectGroupByName("group")).toReturn(DEFAULT_GROUP);
  }

  @Test
  public void should_execute_on_valid_parameters() throws Exception {

    final PermissionTemplateDao permissionTemplateDao = mock(PermissionTemplateDao.class);
    when(permissionTemplateDao.selectTemplateByKey("my_template")).thenReturn(new PermissionTemplateDto().setId(1L));

    PermissionTemplateUpdater updater =
      new PermissionTemplateUpdater("my_template", UserRole.USER, "user", permissionTemplateDao, userDao) {
      @Override
      void doExecute(Long templateId, String permission) {
        permissionTemplateDao.addUserPermission(1L, 1L, UserRole.USER);
      }
    };
    updater.executeUpdate();

    verify(permissionTemplateDao, times(1)).addUserPermission(1L, 1L, UserRole.USER);
  }

  @Test
  public void should_validate_template_reference() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Unknown template:");

    final PermissionTemplateDao permissionTemplateDao = mock(PermissionTemplateDao.class);
    when(permissionTemplateDao.selectTemplateByKey("my_template")).thenReturn(null);

    PermissionTemplateUpdater updater =
      new PermissionTemplateUpdater("my_template", UserRole.USER, "user", permissionTemplateDao, userDao) {
        @Override
        void doExecute(Long templateId, String permission) {
        }
      };
    updater.executeUpdate();
  }

  @Test
  public void should_validate_permission_reference() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Invalid permission:");

    final PermissionTemplateDao permissionTemplateDao = mock(PermissionTemplateDao.class);
    when(permissionTemplateDao.selectTemplateByKey("my_template")).thenReturn(new PermissionTemplateDto().setId(1L));

    PermissionTemplateUpdater updater =
      new PermissionTemplateUpdater("my_template", "invalid", "user", permissionTemplateDao, userDao) {
        @Override
        void doExecute(Long templateId, String permission) {
        }
      };
    updater.executeUpdate();
  }

  @Test
  public void should_check_that_user_is_logged_in() throws Exception {
    expected.expect(UnauthorizedException.class);
    expected.expectMessage("Authentication is required");

    MockUserSession.set();

    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(null, null, null, null, null) {
        @Override
        void doExecute(Long templateId, String permission) {
        }
      };
    updater.executeUpdate();
  }

  @Test
  public void should_check_that_user_is_a_system_admin() throws Exception {
    expected.expect(ForbiddenException.class);
    expected.expectMessage("Insufficient privileges");

    MockUserSession.set().setLogin("user").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(null, null, null, null, null) {
      @Override
      void doExecute(Long templateId, String permission) {
      }
    };
    updater.executeUpdate();
  }
}
