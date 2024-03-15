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
package org.sonar.server.almsettings.ws.gitlab;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.alm.client.gitlab.GitLabBranch;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.GitlabServerException;
import org.sonar.alm.client.gitlab.Project;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.almsettings.ws.DevOpsProjectDescriptor;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.project.ws.ProjectCreator;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitlabProjectCreatorTest {

  private static final String PROJECT_UUID = "projectUuid";
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;

  @Mock
  private ProjectKeyGenerator projectKeyGenerator;

  @Mock
  private ProjectCreator projectCreator;

  @Mock
  private AlmSettingDto almSettingDto;
  @Mock
  private DevOpsProjectDescriptor devOpsProjectDescriptor;
  @Mock
  private GitlabApplicationClient gitlabApplicationClient;
  @Mock
  private UserSession userSession;

  @InjectMocks
  private GitlabProjectCreator underTest;

  private static final String USER_LOGIN = "userLogin";
  private static final String USER_UUID = "userUuid";

  private static final String GROUP_NAME = "group1";
  private static final String REPOSITORY_PATH_WITH_NAMESPACE = "pathWith/namespace";

  private static final String GITLAB_PROJECT_NAME = "gitlabProjectName";

  private static final String REPOSITORY_ID = "1234";

  private static final String MAIN_BRANCH_NAME = "defaultBranch";

  private static final String ALM_SETTING_KEY = "gitlab_config_1";
  private static final String ALM_SETTING_UUID = "almSettingUuid";

  private static final String USER_PAT = "1234";

  public static final String GITLAB_URL = "http://api.com";
  private static final DevOpsProjectDescriptor DEVOPS_PROJECT_DESCRIPTOR = new DevOpsProjectDescriptor(ALM.GITLAB, GITLAB_URL, REPOSITORY_ID);

  @BeforeEach
  void setup() {
    lenient().when(userSession.getLogin()).thenReturn(USER_LOGIN);
    lenient().when(userSession.getUuid()).thenReturn(USER_UUID);

    lenient().when(almSettingDto.getUrl()).thenReturn(GITLAB_URL);
    lenient().when(almSettingDto.getKey()).thenReturn(ALM_SETTING_KEY);
    lenient().when(almSettingDto.getUuid()).thenReturn(ALM_SETTING_UUID);

    lenient().when(devOpsProjectDescriptor.projectIdentifier()).thenReturn(REPOSITORY_ID);
    lenient().when(devOpsProjectDescriptor.url()).thenReturn(GITLAB_URL);
    lenient().when(devOpsProjectDescriptor.alm()).thenReturn(ALM.GITLAB);
  }

  @Test
  void isScanAllowedUsingPermissionsFromDevopsPlatform_shouldThrowUnsupportedOperationException() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
      .isThrownBy(() -> underTest.isScanAllowedUsingPermissionsFromDevopsPlatform())
      .withMessage("Not Implemented");
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenUserHasNoPat_throws() {
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, false, null, null))
      .withMessage("personal access token for 'gitlab_config_1' is missing");
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenRepoNotFound_throws() {
    mockPatForUser();
    when(gitlabApplicationClient.getProject(DEVOPS_PROJECT_DESCRIPTOR.url(), USER_PAT, Long.valueOf(REPOSITORY_ID))).thenThrow(new GitlabServerException(404, "Not found"));
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, false, null, null))
      .withMessage("Failed to fetch GitLab project with ID '1234' from 'http://api.com'");

  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenRepoFoundOnGitlab_successfullyCreatesProject() {
    mockPatForUser();
    mockGitlabProject();
    mockMainBranch();
    mockProjectCreation("projectKey", "projectName");

    underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, true, "projectKey", "projectName");

    ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingCaptor = ArgumentCaptor.forClass(ProjectAlmSettingDto.class);

    verify(dbClient.projectAlmSettingDao()).insertOrUpdate(any(), projectAlmSettingCaptor.capture(), eq(ALM_SETTING_KEY), eq("projectName"), eq("projectKey"));

    ProjectAlmSettingDto createdProjectAlmSettingDto = projectAlmSettingCaptor.getValue();

    assertThat(createdProjectAlmSettingDto.getAlmSettingUuid()).isEqualTo(ALM_SETTING_UUID);
    assertThat(createdProjectAlmSettingDto.getAlmRepo()).isEqualTo(REPOSITORY_ID);
    assertThat(createdProjectAlmSettingDto.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(createdProjectAlmSettingDto.getMonorepo()).isTrue();

  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenNoKeyAndNameSpecified_generatesOneKeyAndUsersGitlabProjectName() {
    mockPatForUser();
    mockGitlabProject();
    mockMainBranch();

    String generatedProjectKey = "generatedProjectKey";
    when(projectKeyGenerator.generateUniqueProjectKey(REPOSITORY_PATH_WITH_NAMESPACE)).thenReturn(generatedProjectKey);

    mockProjectCreation(generatedProjectKey, GITLAB_PROJECT_NAME);

    underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, true, null, null);

    ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingCaptor = ArgumentCaptor.forClass(ProjectAlmSettingDto.class);

    verify(dbClient.projectAlmSettingDao()).insertOrUpdate(any(), projectAlmSettingCaptor.capture(), eq(ALM_SETTING_KEY), eq(GITLAB_PROJECT_NAME), eq(generatedProjectKey));

    ProjectAlmSettingDto createdProjectAlmSettingDto = projectAlmSettingCaptor.getValue();

    assertThat(createdProjectAlmSettingDto.getAlmSettingUuid()).isEqualTo(ALM_SETTING_UUID);
    assertThat(createdProjectAlmSettingDto.getAlmRepo()).isEqualTo(REPOSITORY_ID);
    assertThat(createdProjectAlmSettingDto.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(createdProjectAlmSettingDto.getMonorepo()).isTrue();
  }

  private void mockPatForUser() {
    AlmPatDto almPatDto = mock();
    when(almPatDto.getPersonalAccessToken()).thenReturn(USER_PAT);
    when(dbClient.almPatDao().selectByUserAndAlmSetting(any(), eq(USER_UUID), eq(almSettingDto))).thenReturn(Optional.of(almPatDto));
  }

  private void mockGitlabProject() {
    Project project = mock(Project.class);
    lenient().when(project.getPathWithNamespace()).thenReturn(REPOSITORY_PATH_WITH_NAMESPACE);
    when(project.getName()).thenReturn(GITLAB_PROJECT_NAME);
    when(gitlabApplicationClient.getProject(DEVOPS_PROJECT_DESCRIPTOR.url(), USER_PAT, Long.valueOf(REPOSITORY_ID))).thenReturn(project);

  }

  private void mockMainBranch() {
    when(gitlabApplicationClient.getBranches(DEVOPS_PROJECT_DESCRIPTOR.url(), USER_PAT, Long.valueOf(REPOSITORY_ID)))
      .thenReturn(List.of(new GitLabBranch("notMain", false), new GitLabBranch(MAIN_BRANCH_NAME, true)));
  }

  private void mockProjectCreation(String projectKey, String projectName) {
    ComponentCreationData componentCreationData = mock();
    ProjectDto projectDto = mock();
    when(componentCreationData.projectDto()).thenReturn(projectDto);
    when(projectDto.getUuid()).thenReturn(PROJECT_UUID);
    when(projectDto.getKey()).thenReturn(projectKey);
    when(projectDto.getName()).thenReturn(projectName);
    when(projectCreator.createProject(any(), eq(projectKey), eq(projectName), eq(MAIN_BRANCH_NAME), eq(CreationMethod.ALM_IMPORT_API)))
      .thenReturn(componentCreationData);
  }

}
