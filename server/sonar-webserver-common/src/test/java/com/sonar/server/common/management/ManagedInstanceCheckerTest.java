/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package com.sonar.server.common.management;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.management.ManagedProjectService;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ManagedInstanceCheckerTest {

  private static final String INSTANCE_EXCEPTION_MESSAGE = "Operation not allowed when the instance is externally managed.";
  private static final String PROJECT_EXCEPTION_MESSAGE = "Operation not allowed when the project is externally managed.";

  @Mock
  private DbSession dbSession;
  @Mock
  private ManagedInstanceService managedInstanceService;
  @Mock
  private ManagedProjectService managedProjectService;
  @InjectMocks
  private ManagedInstanceChecker managedInstanceChecker;

  @Test
  public void throwIfInstanceIsManaged_whenInstanceExternallyManaged_shouldThrow() {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);

    assertThatThrownBy(() -> managedInstanceChecker.throwIfInstanceIsManaged())
      .isInstanceOf(BadRequestException.class)
      .hasMessage(INSTANCE_EXCEPTION_MESSAGE);
  }

  @Test
  public void throwIfInstanceIsManaged_whenCustomErrorMessage_shouldThrowWithCustomError() {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);

    String customErrorMessage = "Custom error message";

    assertThatThrownBy(() -> managedInstanceChecker.throwIfInstanceIsManaged(customErrorMessage))
      .isInstanceOf(BadRequestException.class)
      .hasMessage(customErrorMessage);
  }

  @Test
  public void throwIfInstanceIsManaged_whenCustomErrorMessageAndInstanceManaged_shouldNotThrow() {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(false);

    String customErrorMessage = "Custom error message";

    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfInstanceIsManaged(customErrorMessage));
  }

  @Test
  public void throwIfInstanceIsManaged_whenInstanceNotExternallyManaged_shouldNotThrow() {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(false);

    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfInstanceIsManaged());
  }

  @Test
  public void throwIfProjectIsManaged_whenProjectIsManaged_shouldThrow() {
    ProjectDto projectDto = mockManagedProject();

    String projectUuid = projectDto.getUuid();
    assertThatThrownBy(() -> managedInstanceChecker.throwIfProjectIsManaged(dbSession, projectUuid))
      .isInstanceOf(BadRequestException.class)
      .hasMessage(PROJECT_EXCEPTION_MESSAGE);
  }

  @Test
  public void throwIfProjectIsManaged_whenProjectIsNotManaged_shouldNotThrow() {
    ProjectDto projectDto = mockNotManagedProject();

    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfProjectIsManaged(dbSession, projectDto.getUuid()));
  }

  @Test
  public void throwIfUserIsManaged_whenUserIsManaged_shouldThrow() {
    UserDto userDto = mockManagedUser();

    String userUuid = userDto.getUuid();
    assertThatThrownBy(() -> managedInstanceChecker.throwIfUserIsManaged(dbSession, userUuid))
      .isInstanceOf(BadRequestException.class)
      .hasMessage(INSTANCE_EXCEPTION_MESSAGE);
  }

  @Test
  public void throwIfUserIsManaged_whenUserIsNotManaged_shouldNotThrow() {
    UserDto userDto = mockNotManagedUser();

    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfUserIsManaged(dbSession, userDto.getUuid()));
  }

  @Test
  public void throwIfGroupIsManaged_whenGroupIsManaged_shouldThrow() {
    GroupDto groupDto = mockManagedGroup();

    String groupUuid = groupDto.getUuid();
    assertThatThrownBy(() -> managedInstanceChecker.throwIfGroupIsManaged(dbSession, groupUuid))
      .isInstanceOf(BadRequestException.class)
      .hasMessage(INSTANCE_EXCEPTION_MESSAGE);
  }

  @Test
  public void throwIfGroupIsManaged_whenGroupIsNotManaged_shouldNotThrow() {
    GroupDto groupDto = mockNotManagedGroup();

    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfGroupIsManaged(dbSession, groupDto.getUuid()));
  }

  @Test
  public void throwIfUserAndProjectAreManaged_whenUserAndProjectAreManaged_shouldThrow() {
    ProjectDto projectDto = mockManagedProject();
    UserDto userDto = mockManagedUser();

    String userUuid = userDto.getUuid();
    String projectUuid = projectDto.getUuid();
    assertThatThrownBy(() -> managedInstanceChecker.throwIfUserAndProjectAreManaged(dbSession, userUuid, projectUuid))
      .isInstanceOf(BadRequestException.class)
      .hasMessage(PROJECT_EXCEPTION_MESSAGE);
  }

  @Test
  public void throwIfUserAndProjectAreManaged_whenOnlyUserIsManaged_shouldNotThrow() {
    ProjectDto projectDto = mockNotManagedProject();
    UserDto userDto = mockManagedUser();

    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfUserAndProjectAreManaged(dbSession, userDto.getUuid(), projectDto.getUuid()));
  }

  @Test
  public void throwIfUserAndProjectAreManaged_whenOnlyProjectIsManaged_shouldNotThrow() {
    ProjectDto projectDto = mockManagedProject();
    UserDto userDto = mockNotManagedUser();

    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfUserAndProjectAreManaged(dbSession, userDto.getUuid(), projectDto.getUuid()));
  }

  @Test
  public void throwIfUserAndProjectAreManaged_whenNothingIsManaged_shouldNotThrow() {
    ProjectDto projectDto = mockNotManagedProject();
    UserDto userDto = mockNotManagedUser();

    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfUserAndProjectAreManaged(dbSession, userDto.getUuid(), projectDto.getUuid()));
  }

  @Test
  public void throwIfGroupAndProjectAreManaged_whenGroupAndProjectAreManaged_shouldThrow() {
    ProjectDto projectDto = mockManagedProject();
    GroupDto groupDto = mockManagedGroup();

    String groupDtoUuid = groupDto.getUuid();
    String projectDtoUuid = projectDto.getUuid();
    assertThatThrownBy(() -> managedInstanceChecker.throwIfGroupAndProjectAreManaged(dbSession, groupDtoUuid, projectDtoUuid))
      .isInstanceOf(BadRequestException.class)
      .hasMessage(PROJECT_EXCEPTION_MESSAGE);
  }

  @Test
  public void throwIfGroupAndProjectAreManaged_whenOnlyGroupIsManaged_shouldNotThrow() {
    ProjectDto projectDto = mockNotManagedProject();
    GroupDto groupDto = mockManagedGroup();

    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfGroupAndProjectAreManaged(dbSession, groupDto.getUuid(), projectDto.getUuid()));
  }

  @Test
  public void throwIfGroupAndProjectAreManaged_whenOnlyProjectIsManaged_shouldNotThrow() {
    ProjectDto projectDto = mockManagedProject();
    GroupDto groupDto = mockNotManagedGroup();

    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfGroupAndProjectAreManaged(dbSession, groupDto.getUuid(), projectDto.getUuid()));
  }

  @Test
  public void throwIfGroupAndProjectAreManaged_whenNothingIsManaged_shouldNotThrow() {
    ProjectDto projectDto = mockNotManagedProject();
    GroupDto groupDto = mockNotManagedGroup();

    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfGroupAndProjectAreManaged(dbSession, groupDto.getUuid(), projectDto.getUuid()));
  }

  private ProjectDto mockManagedProject() {
    return mockProject(true);
  }

  private ProjectDto mockNotManagedProject() {
    return mockProject(false);
  }

  private ProjectDto mockProject(boolean isManaged) {
    ProjectDto projectDto = mock(ProjectDto.class);
    when(managedProjectService.isProjectManaged(dbSession, projectDto.getUuid())).thenReturn(isManaged);
    return projectDto;
  }

  private UserDto mockManagedUser() {
    return mockUser(true);
  }

  private UserDto mockNotManagedUser() {
    return mockUser(false);
  }

  private UserDto mockUser(boolean isManaged) {
    UserDto userDto = mock(UserDto.class);
    when(managedInstanceService.isUserManaged(dbSession, userDto.getUuid())).thenReturn(isManaged);
    return userDto;
  }

  private GroupDto mockManagedGroup() {
    return mockGroup(true);
  }

  private GroupDto mockNotManagedGroup() {
    return mockGroup(false);
  }

  private GroupDto mockGroup(boolean isManaged) {
    GroupDto groupDto = mock(GroupDto.class);
    when(managedInstanceService.isGroupManaged(dbSession, groupDto.getUuid())).thenReturn(isManaged);
    return groupDto;
  }

}
