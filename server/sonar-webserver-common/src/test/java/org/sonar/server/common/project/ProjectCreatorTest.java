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
package org.sonar.server.common.project;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectCreatorTest {

  private static final String PROJECT_KEY = "project-key";
  private static final String PROJECT_NAME = "Project Name";
  private static final String MAIN_BRANCH = "main";
  private static final String PROJECT_UUID = "project-uuid";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;

  @Mock
  private UserSession userSession;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ProjectDefaultVisibility projectDefaultVisibility;

  @Mock
  private ComponentUpdater componentUpdater;

  @Mock
  private DbSession dbSession;

  private ProjectCreator projectCreator;

  @BeforeEach
  void setUp() {
    projectCreator = new ProjectCreator(dbClient, userSession, projectDefaultVisibility, componentUpdater);
  }

  @Test
  void getOrCreateProject_whenProjectDoesNotExist_createsNewProject() {
    when(dbClient.projectDao().selectProjectByKey(dbSession, PROJECT_KEY)).thenReturn(Optional.empty());

    ProjectCreationRequest request = new ProjectCreationRequest(
      PROJECT_KEY,
      PROJECT_NAME,
      MAIN_BRANCH,
      CreationMethod.ALM_IMPORT_API,
      true,
      false,
      true);

    ComponentDto componentDto = mock(ComponentDto.class);
    ProjectDto projectDto = mock(ProjectDto.class);
    BranchDto branchDto = mock(BranchDto.class);
    ComponentCreationData expectedData = new ComponentCreationData(componentDto, null, branchDto, projectDto, true);
    when(componentUpdater.createWithoutCommit(any(), any())).thenReturn(expectedData);

    ComponentCreationData result = projectCreator.getOrCreateProject(dbSession, request);

    assertThat(result).isEqualTo(expectedData);
    assertThat(result.newProjectCreated()).isTrue();
    verify(componentUpdater).createWithoutCommit(any(), any());
  }

  @Test
  void getOrCreateProject_whenProjectExistsAndAllowExistingTrue_andNameMatches_returnsExistingProject() {
    ProjectDto existingProject = mock(ProjectDto.class);
    when(existingProject.getName()).thenReturn(PROJECT_NAME);
    when(existingProject.getUuid()).thenReturn(PROJECT_UUID);
    when(dbClient.projectDao().selectProjectByKey(dbSession, PROJECT_KEY))
      .thenReturn(Optional.of(existingProject));

    ComponentDto componentDto = mock(ComponentDto.class);
    when(dbClient.componentDao().selectByKey(dbSession, PROJECT_KEY))
      .thenReturn(Optional.of(componentDto));

    BranchDto branchDto = mock(BranchDto.class);
    when(dbClient.branchDao().selectMainBranchByProjectUuid(dbSession, PROJECT_UUID))
      .thenReturn(Optional.of(branchDto));

    ProjectCreationRequest request = new ProjectCreationRequest(
      PROJECT_KEY,
      PROJECT_NAME,
      MAIN_BRANCH,
      CreationMethod.ALM_IMPORT_API,
      true,
      false,
      true); // allowExisting = true

    ComponentCreationData result = projectCreator.getOrCreateProject(dbSession, request);

    assertThat(result.newProjectCreated()).isFalse();
    assertThat(result.projectDto()).isEqualTo(existingProject);
    assertThat(result.mainBranchComponent()).isEqualTo(componentDto);
    assertThat(result.mainBranchDto()).isEqualTo(branchDto);
    verify(componentUpdater, never()).createWithoutCommit(any(), any());
  }

  @Test
  void getOrCreateProject_whenProjectExistsAndAllowExistingTrue_andNameMismatch_throwsException() {
    ProjectDto existingProject = mock(ProjectDto.class);
    when(existingProject.getName()).thenReturn("Different Name");
    when(dbClient.projectDao().selectProjectByKey(dbSession, PROJECT_KEY))
      .thenReturn(Optional.of(existingProject));

    ProjectCreationRequest request = new ProjectCreationRequest(
      PROJECT_KEY,
      PROJECT_NAME,
      MAIN_BRANCH,
      CreationMethod.ALM_IMPORT_API,
      true,
      false,
      true); // allowExisting = true

    assertThatThrownBy(() -> projectCreator.getOrCreateProject(dbSession, request))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Project with key '" + PROJECT_KEY + "' cannot be bound - configuration mismatch");

    verify(componentUpdater, never()).createWithoutCommit(any(), any());
  }

  @Test
  void getOrCreateProject_whenProjectExistsAndAllowExistingFalse_throwsException() {
    ProjectDto existingProject = mock(ProjectDto.class);
    when(dbClient.projectDao().selectProjectByKey(dbSession, PROJECT_KEY))
      .thenReturn(Optional.of(existingProject));

    ProjectCreationRequest request = new ProjectCreationRequest(
      PROJECT_KEY,
      PROJECT_NAME,
      MAIN_BRANCH,
      CreationMethod.ALM_IMPORT_API,
      true,
      false,
      false); // allowExisting = false

    assertThatThrownBy(() -> projectCreator.getOrCreateProject(dbSession, request))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"" + PROJECT_KEY + "\". A similar key already exists: \"" + PROJECT_KEY + "\"");

    verify(componentUpdater, never()).createWithoutCommit(any(), any());
  }
}
