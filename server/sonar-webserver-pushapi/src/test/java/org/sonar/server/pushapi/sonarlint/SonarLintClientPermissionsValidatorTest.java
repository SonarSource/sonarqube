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
package org.sonar.server.pushapi.sonarlint;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserSessionFactory;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarLintClientPermissionsValidatorTest {

  private final static String USER_UUID = "USER_UUID";

  private final Set<String> exampleProjectuuids = Set.of("project1", "project2");
  private final List<ProjectDto> projectDtos = List.of(mock(ProjectDto.class), mock(ProjectDto.class));
  private final DbClient dbClient = mock(DbClient.class);
  private final UserSessionFactory userSessionFactory = mock(UserSessionFactory.class);
  private final UserDao userDao = mock(UserDao.class);
  private final ProjectDao projectDao = mock(ProjectDao.class);
  private final UserSession userSession = mock(UserSession.class);

  private SonarLintClientPermissionsValidator underTest = new SonarLintClientPermissionsValidator(dbClient, userSessionFactory);

  @Before
  public void before() {
    when(dbClient.userDao()).thenReturn(userDao);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(userSessionFactory.create(any())).thenReturn(userSession);
    when(projectDao.selectProjectsByKeys(any(), any())).thenReturn(projectDtos);
    when(projectDao.selectByUuids(any(), any())).thenReturn(projectDtos);
  }

  @Test
  public void validate_givenUserActivatedAndWithRequiredPermissions_dontThrowException() {
    UserDto userDto = new UserDto();
    when(userDao.selectByUuid(any(), any())).thenReturn(userDto);
    when(userSession.isActive()).thenReturn(true);

    assertThatCode(() -> underTest.validateUserCanReceivePushEventForProjectUuids(USER_UUID, exampleProjectuuids))
      .doesNotThrowAnyException();
  }

  @Test
  public void validate_givenUserNotActivated_throwException() {
    UserDto userDto = new UserDto();
    when(userDao.selectByUuid(any(), any())).thenReturn(userDto);
    when(userSession.isActive()).thenReturn(false);

    assertThrows(ForbiddenException.class,
      () -> underTest.validateUserCanReceivePushEventForProjectUuids(USER_UUID, exampleProjectuuids));
  }

  @Test
  public void validate_givenUserNotGrantedProjectPermissions_throwException() {
    UserDto userDto = new UserDto();
    when(userDao.selectByUuid(any(), any())).thenReturn(userDto);
    when(userSession.isActive()).thenReturn(true);
    when(userSession.checkProjectPermission(any(), any())).thenThrow(ForbiddenException.class);

    assertThrows(ForbiddenException.class,
      () -> underTest.validateUserCanReceivePushEventForProjectUuids(USER_UUID, exampleProjectuuids));
  }
}
