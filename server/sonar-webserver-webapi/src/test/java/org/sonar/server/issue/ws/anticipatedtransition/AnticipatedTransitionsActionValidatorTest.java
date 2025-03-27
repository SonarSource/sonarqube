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
package org.sonar.server.issue.ws.anticipatedtransition;

import java.util.Set;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.AuthorizationDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.db.permission.ProjectPermission.ISSUE_ADMIN;

public class AnticipatedTransitionsActionValidatorTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final ComponentFinder componentFinder = mock(ComponentFinder.class);
  private final UserSession userSession = mock(UserSession.class);

  AnticipatedTransitionsActionValidator underTest = new AnticipatedTransitionsActionValidator(dbClient, componentFinder, userSession);

  @Test
  public void givenValidProjectKey_whenValidateProjectKey_thenReturnProjectDto() {
    // given
    String projectKey = "validProjectKey";
    DbSession dbSession = mockDbSession();
    ProjectDto projectDto = new ProjectDto();
    doReturn(projectDto).when(componentFinder).getProjectByKey(dbSession, projectKey);

    // when
    ProjectDto returnedProjectDto = underTest.validateProjectKey(projectKey);

    // then
    assertThat(projectDto).isEqualTo(returnedProjectDto);
  }

  @Test
  public void givenInvalidProjectKey_whenValidateProjectKey_thenThrowForbiddenException() {
    // given
    String projectKey = "invalidProjectKey";
    DbSession dbSession = mockDbSession();
    doThrow(NotFoundException.class).when(componentFinder).getProjectByKey(dbSession, projectKey);

    // when then
    assertThatThrownBy(() -> underTest.validateProjectKey(projectKey))
      .withFailMessage("Insufficient privileges")
      .isInstanceOf(ForbiddenException.class);

  }

  @Test
  public void givenUserLoggedIn_whenValidateUserLoggedIn_thenReturnUserUuid() {
    // given
    String userUuid = "userUuid";
    doReturn(userUuid).when(userSession).getUuid();

    // when
    String returnedUserUuid = underTest.validateUserLoggedIn();

    // then
    assertThat(returnedUserUuid).isEqualTo(userUuid);
  }

  @Test
  public void givenUserHasAdministerIssuesPermission_whenValidateUserHasAdministerIssuesPermission_thenDoNothing() {
    // given
    String userUuid = "userUuid";
    doReturn(userUuid).when(userSession).getUuid();
    String projectUuid = "projectUuid";
    DbSession dbSession = mockDbSession();
    AuthorizationDao authorizationDao = mockAuthorizationDao();
    doReturn(Set.of("permission1", ISSUE_ADMIN.getKey())).when(authorizationDao).selectEntityPermissions(dbSession, projectUuid, userUuid);

    // when, then
    assertThatCode(() -> underTest.validateUserHasAdministerIssuesPermission(projectUuid))
      .doesNotThrowAnyException();
    verify(dbClient, times(1)).authorizationDao();
  }

  @Test
  public void givenUserDoesNotHaveAdministerIssuesPermission_whenValidateUserHasAdministerIssuesPermission_thenThrowForbiddenException() {
    // given
    String userUuid = "userUuid";
    doReturn(userUuid).when(userSession).getUuid();
    String projectUuid = "projectUuid";
    DbSession dbSession = mockDbSession();
    AuthorizationDao authorizationDao = mockAuthorizationDao();
    doReturn(Set.of("permission1")).when(authorizationDao).selectEntityPermissions(dbSession, projectUuid, userUuid);

    // when, then
    assertThatThrownBy(() -> underTest.validateUserHasAdministerIssuesPermission(projectUuid))
      .withFailMessage("Insufficient privileges")
      .isInstanceOf(ForbiddenException.class);
    verify(dbClient, times(1)).authorizationDao();
  }

  private AuthorizationDao mockAuthorizationDao() {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    doReturn(authorizationDao).when(dbClient).authorizationDao();
    return authorizationDao;
  }

  private DbSession mockDbSession() {
    DbSession dbSession = mock(DbSession.class);
    doReturn(dbSession).when(dbClient).openSession(false);
    return dbSession;
  }
}