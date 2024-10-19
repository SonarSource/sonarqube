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
package org.sonar.server.common.almsettings.gitlab;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.alm.client.gitlab.GitLabBranch;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.Project;
import org.sonar.db.DbClient;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.common.almsettings.DevOpsProjectCreationContext;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitlabDevOpsProjectCreationContextServiceTest {

  private static final AlmSettingDto ALM_SETTING_DTO = mock();
  private static final long GITLAB_PROJECT_ID = 123L;

  private static final DevOpsProjectDescriptor DEV_OPS_PROJECT_DESCRIPTOR = new DevOpsProjectDescriptor(ALM.GITHUB, "project-key", String.valueOf(GITLAB_PROJECT_ID), null);
  private static final String GITLAB_COM = "https://gitlab.com";
  private static final String DEFAULT_BRANCH_NAME = "default-branch";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private UserSession userSession;
  @Mock
  private GitlabApplicationClient gitlabApplicationClient;

  @InjectMocks
  private GitlabDevOpsProjectCreationContextService gitlabDevOpsProjectService;

  @BeforeEach
  public void setUp() {
    when(ALM_SETTING_DTO.getUrl()).thenReturn(GITLAB_COM);
    lenient().when(ALM_SETTING_DTO.getKey()).thenReturn("almKey");
  }

  @Test
  void create_whenGitlabProjectIdIsInvalid_throws() {
    DevOpsProjectDescriptor devOpsProjectDescriptor = mock();
    when(devOpsProjectDescriptor.repositoryIdentifier()).thenReturn("invalid");

    assertThatIllegalArgumentException()
      .isThrownBy(() -> gitlabDevOpsProjectService.create(ALM_SETTING_DTO, devOpsProjectDescriptor))
      .withMessage("GitLab project identifier must be a number, was 'invalid'");
  }

  @Test
  void createDevOpsProject_whenUserUuidIsNull_shouldThrow() {
    assertThatNullPointerException()
      .isThrownBy(() -> gitlabDevOpsProjectService.create(ALM_SETTING_DTO, DEV_OPS_PROJECT_DESCRIPTOR))
      .withMessage("User UUID cannot be null.");
  }

  @Test
  void createDevOpsProject_whenNoPat_shouldThrow() {
    when(userSession.getUuid()).thenReturn("user-uuid");
    when(dbClient.almPatDao().selectByUserAndAlmSetting(dbClient.openSession(false), userSession.getUuid(), ALM_SETTING_DTO)).thenReturn(Optional.empty());

    assertThatIllegalArgumentException()
      .isThrownBy(() -> gitlabDevOpsProjectService.create(ALM_SETTING_DTO, DEV_OPS_PROJECT_DESCRIPTOR))
      .withMessage("Personal access token for 'almKey' is missing");
  }

  @Test
  void create_whenProjectNotFoundOnGitlab_throws() {
    AlmPatDto almPatDto = mockPatExistence();

    when(gitlabApplicationClient.getProject(GITLAB_COM, almPatDto.getPersonalAccessToken(), GITLAB_PROJECT_ID)).thenThrow(new IllegalStateException("error"));

    assertThatIllegalStateException()
      .isThrownBy(() -> gitlabDevOpsProjectService.create(ALM_SETTING_DTO, DEV_OPS_PROJECT_DESCRIPTOR))
      .withMessage("error");
  }

  private static Stream<Arguments> visibilitiesAndExpectedResults() {
    return Stream.of(
      Arguments.of("public", true),
      Arguments.of("private", false),
      Arguments.of("internal", false),
      Arguments.of("other", false)
    );
  }

  @ParameterizedTest
  @MethodSource("visibilitiesAndExpectedResults")
  void create_whenProjectFoundOnGitLab_createCorrectDevOpsProject(String gitlabVisibility, boolean isPublic) {
    AlmPatDto almPatDto = mockPatExistence();

    Project project = mockGitlabProjectAndBranches(gitlabVisibility, almPatDto);

    DevOpsProjectCreationContext devOpsProjectCreationContext = gitlabDevOpsProjectService.create(ALM_SETTING_DTO, DEV_OPS_PROJECT_DESCRIPTOR);
    assertThat(devOpsProjectCreationContext.name()).isEqualTo(project.getName());
    assertThat(devOpsProjectCreationContext.fullName()).isEqualTo(project.getPathWithNamespace());
    assertThat(devOpsProjectCreationContext.devOpsPlatformIdentifier()).isEqualTo(String.valueOf(project.getId()));
    assertThat(devOpsProjectCreationContext.isPublic()).isEqualTo(isPublic);
    assertThat(devOpsProjectCreationContext.defaultBranchName()).isEqualTo(DEFAULT_BRANCH_NAME);
  }

  private AlmPatDto mockPatExistence() {
    when(userSession.getUuid()).thenReturn("user-uuid");

    AlmPatDto almPatDto = mock(AlmPatDto.class);
    when(almPatDto.getPersonalAccessToken()).thenReturn("token");
    when(dbClient.almPatDao().selectByUserAndAlmSetting(dbClient.openSession(false), userSession.getUuid(), ALM_SETTING_DTO)).thenReturn(Optional.of(almPatDto));
    return almPatDto;
  }

  private Project mockGitlabProjectAndBranches(String gitlabVisibility, AlmPatDto almPatDto) {
    Project project = mock(Project.class);
    when(project.getId()).thenReturn(GITLAB_PROJECT_ID);
    when(project.getName()).thenReturn("project-name");
    when(project.getPathWithNamespace()).thenReturn("project-path");
    when(project.getVisibility()).thenReturn(gitlabVisibility);
    when(gitlabApplicationClient.getProject(GITLAB_COM, almPatDto.getPersonalAccessToken(), GITLAB_PROJECT_ID)).thenReturn(project);

    GitLabBranch gitLabBranch = mock();
    GitLabBranch defaultGitlabBranch = mock();
    when(defaultGitlabBranch.getName()).thenReturn(DEFAULT_BRANCH_NAME);
    when(defaultGitlabBranch.isDefault()).thenReturn(true);
    when(gitlabApplicationClient.getBranches(GITLAB_COM, almPatDto.getPersonalAccessToken(), GITLAB_PROJECT_ID)).thenReturn(List.of(gitLabBranch, defaultGitlabBranch));
    return project;
  }

}
