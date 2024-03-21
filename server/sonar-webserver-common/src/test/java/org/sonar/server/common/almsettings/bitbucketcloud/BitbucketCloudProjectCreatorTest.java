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
package org.sonar.server.common.almsettings.bitbucketcloud;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Repository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.component.ComponentCreationData;
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
class BitbucketCloudProjectCreatorTest {

  private static final String USER_LOGIN = "userLogin";
  private static final String USER_UUID = "userUuid";
  private static final String REPOSITORY_SLUG = "projectSlug";
  private static final String REPOSITORY_NAME = "repositoryName";
  private static final String ALM_SETTING_KEY = "bitbucketcloud_config_1";
  private static final String ALM_SETTING_UUID = "almSettingUuid";
  private static final String USER_PAT = "1234";
  private static final String PROJECT_UUID = "projectUuid";
  private static final String WORKSPACE = "workspace";
  private static final String MAIN_BRANCH_NAME = "defaultBranch";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private AlmSettingDto almSettingDto;
  @Mock
  private DevOpsProjectDescriptor devOpsProjectDescriptor;
  @Mock
  private UserSession userSession;
  @Mock
  private BitbucketCloudRestClient bitbucketCloudRestClient;
  @Mock
  private ProjectCreator projectCreator;
  @Mock
  private ProjectKeyGenerator projectKeyGenerator;

  @InjectMocks
  private BitbucketCloudProjectCreator underTest;

  @BeforeEach
  void setup() {
    lenient().when(userSession.getLogin()).thenReturn(USER_LOGIN);
    lenient().when(userSession.getUuid()).thenReturn(USER_UUID);

    lenient().when(almSettingDto.getKey()).thenReturn(ALM_SETTING_KEY);
    lenient().when(almSettingDto.getUuid()).thenReturn(ALM_SETTING_UUID);

    lenient().when(devOpsProjectDescriptor.projectIdentifier()).thenReturn(REPOSITORY_SLUG);
    lenient().when(devOpsProjectDescriptor.alm()).thenReturn(ALM.BITBUCKET_CLOUD);
  }

  @Test
  void isScanAllowedUsingPermissionsFromDevopsPlatform_shouldThrowUnsupportedOperationException() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
      .isThrownBy(() -> underTest.isScanAllowedUsingPermissionsFromDevopsPlatform())
      .withMessage("Not Implemented");
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenPatIsMissing_shouldThrow() {
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, false, null, null))
      .withMessage("personal access token for 'bitbucketcloud_config_1' is missing");
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenWorkspaceIsNotDefined_shouldThrow() {
    mockPatForUser();
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, false, null, null))
      .withMessage("workspace for alm setting bitbucketcloud_config_1 is missing");
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenRepositoryNotFound_shouldThrow() {
    mockPatForUser();
    when(almSettingDto.getAppId()).thenReturn("workspace");
    when(bitbucketCloudRestClient.getRepo(USER_PAT, "workspace", REPOSITORY_SLUG)).thenThrow(new IllegalStateException("Problem fetching repository from Bitbucket Cloud"));
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, false, null, null))
      .withMessage("Problem fetching repository from Bitbucket Cloud");
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenRepoFoundOnBitbucket_successfullyCreatesProject() {
    mockPatForUser();

    when(almSettingDto.getAppId()).thenReturn(WORKSPACE);

    mockBitbucketCloudRepository();
    mockProjectCreation("projectKey", "projectName");

    underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, true, "projectKey", "projectName");

    ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingCaptor = ArgumentCaptor.forClass(ProjectAlmSettingDto.class);

    verify(dbClient.projectAlmSettingDao()).insertOrUpdate(any(), projectAlmSettingCaptor.capture(), eq(ALM_SETTING_KEY), eq("projectName"), eq("projectKey"));

    ProjectAlmSettingDto createdProjectAlmSettingDto = projectAlmSettingCaptor.getValue();

    assertThat(createdProjectAlmSettingDto.getAlmSettingUuid()).isEqualTo(ALM_SETTING_UUID);
    assertThat(createdProjectAlmSettingDto.getAlmRepo()).isEqualTo(REPOSITORY_SLUG);
    assertThat(createdProjectAlmSettingDto.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(createdProjectAlmSettingDto.getMonorepo()).isTrue();
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenNoKeyAndNameSpecified_generatesKeyAndUsersBitbucketRepositoryName() {
    mockPatForUser();

    when(almSettingDto.getAppId()).thenReturn(WORKSPACE);

    mockBitbucketCloudRepository();
    String generatedProjectKey = "generatedProjectKey";
    when(projectKeyGenerator.generateUniqueProjectKey(WORKSPACE, REPOSITORY_SLUG)).thenReturn(generatedProjectKey);
    mockProjectCreation(generatedProjectKey, REPOSITORY_NAME);

    underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, true, null, null);

    ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingCaptor = ArgumentCaptor.forClass(ProjectAlmSettingDto.class);

    verify(dbClient.projectAlmSettingDao()).insertOrUpdate(any(), projectAlmSettingCaptor.capture(), eq(ALM_SETTING_KEY), eq(REPOSITORY_NAME), eq(generatedProjectKey));

    ProjectAlmSettingDto createdProjectAlmSettingDto = projectAlmSettingCaptor.getValue();

    assertThat(createdProjectAlmSettingDto.getAlmSettingUuid()).isEqualTo(ALM_SETTING_UUID);
    assertThat(createdProjectAlmSettingDto.getAlmRepo()).isEqualTo(REPOSITORY_SLUG);
    assertThat(createdProjectAlmSettingDto.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(createdProjectAlmSettingDto.getMonorepo()).isTrue();
  }

  private void mockPatForUser() {
    AlmPatDto almPatDto = mock();
    when(almPatDto.getPersonalAccessToken()).thenReturn(USER_PAT);
    when(dbClient.almPatDao().selectByUserAndAlmSetting(any(), eq(USER_UUID), eq(almSettingDto))).thenReturn(Optional.of(almPatDto));
  }

  private void mockBitbucketCloudRepository() {
    Repository repository = mock(Repository.class, Answers.RETURNS_DEEP_STUBS);
    when(repository.getMainBranch().getName()).thenReturn(MAIN_BRANCH_NAME);
    when(repository.getSlug()).thenReturn(REPOSITORY_SLUG);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(bitbucketCloudRestClient.getRepo(USER_PAT, WORKSPACE, REPOSITORY_SLUG)).thenReturn(repository);
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
