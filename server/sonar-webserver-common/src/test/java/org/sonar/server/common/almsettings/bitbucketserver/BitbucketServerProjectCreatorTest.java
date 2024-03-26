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
package org.sonar.server.common.almsettings.bitbucketserver;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.Branch;
import org.sonar.alm.client.bitbucketserver.BranchesList;
import org.sonar.alm.client.bitbucketserver.Project;
import org.sonar.alm.client.bitbucketserver.Repository;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BitbucketServerProjectCreatorTest {

  private static final String USER_LOGIN = "userLogin";
  private static final String USER_UUID = "userUuid";
  private static final String DOP_REPOSITORY_ID = "repository";
  private static final String DOP_PROJECT_ID = "project";
  private static final String URL = "http://rest/api/1.0/projects/projectKey/repos/repoName";
  private static final String ALM_SETTING_KEY = "bitbucketserver_config_1";
  private static final String ALM_SETTING_UUID = "almSettingUuid";
  private static final String USER_PAT = "1234";
  private static final String PROJECT_UUID = "projectUuid";
  private static final String MAIN_BRANCH_NAME = "main";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private AlmSettingDto almSettingDto;
  @Mock
  private DevOpsProjectDescriptor devOpsProjectDescriptor;
  @Mock
  private UserSession userSession;
  @Mock
  private BitbucketServerRestClient bitbucketServerRestClient;
  @Mock
  private ProjectCreator projectCreator;
  @Mock
  private ProjectKeyGenerator projectKeyGenerator;

  @InjectMocks
  private BitbucketServerProjectCreator underTest;

  @Test
  void isScanAllowedUsingPermissionsFromDevopsPlatform_shouldThrowUnsupportedOperationException() {
    assertThatThrownBy(() -> underTest.isScanAllowedUsingPermissionsFromDevopsPlatform())
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("Not Implemented");
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenPatIsMissing_shouldThrow() {
    mockValidUserSession();
    mockValidAlmSettingsDto();
    assertThatThrownBy(() -> underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, false, null, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("personal access token for 'bitbucketserver_config_1' is missing");
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenBitBucketProjectNotProvided_shouldThrow() {
    mockValidUserSession();
    mockValidAlmSettingsDto();
    mockValidPatForUser();

    assertThatThrownBy(() -> underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, false, "projectKey", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The BitBucket project, in which the repository null is located, is mandatory");
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenRepositoryNotFound_shouldThrow() {
    mockValidUserSession();
    mockValidAlmSettingsDto();
    mockValidPatForUser();
    mockValidProjectDescriptor();
    when(bitbucketServerRestClient.getRepo(URL, USER_PAT, DOP_PROJECT_ID, DOP_REPOSITORY_ID)).thenThrow(new IllegalArgumentException("Problem"));

    assertThatThrownBy(() -> underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, false, "projectKey", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Problem");
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenRepoFoundOnBitbucket_successfullyCreatesProject() {
    mockValidUserSession();
    mockValidAlmSettingsDto();
    mockValidPatForUser();
    mockValidProjectDescriptor();
    mockValidBitBucketRepository();
    mockProjectCreation("projectKey", "projectName");

    underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, true, "projectKey", "projectName");

    ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingCaptor = ArgumentCaptor.forClass(ProjectAlmSettingDto.class);
    verify(dbClient.projectAlmSettingDao()).insertOrUpdate(any(), projectAlmSettingCaptor.capture(), eq(ALM_SETTING_KEY), eq("projectName"), eq("projectKey"));
    ProjectAlmSettingDto createdProjectAlmSettingDto = projectAlmSettingCaptor.getValue();

    assertThat(createdProjectAlmSettingDto.getAlmSettingUuid()).isEqualTo(ALM_SETTING_UUID);
    assertThat(createdProjectAlmSettingDto.getAlmRepo()).isEqualTo(DOP_REPOSITORY_ID);
    assertThat(createdProjectAlmSettingDto.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(createdProjectAlmSettingDto.getMonorepo()).isTrue();
  }

  @Test
  void createProjectAndBindToDevOpsPlatform_whenNoKeyAndNameSpecified_generatesKeyAndUsersBitbucketRepositoryName() {
    mockValidUserSession();
    mockValidAlmSettingsDto();
    mockValidPatForUser();
    mockValidProjectDescriptor();
    Repository repository = mockValidBitBucketRepository();
    String generatedProjectKey = "generatedProjectKey";
    when(projectKeyGenerator.generateUniqueProjectKey(repository.getProject().getKey(), repository.getSlug())).thenReturn(generatedProjectKey);
    mockProjectCreation(generatedProjectKey, repository.getName());

    underTest.createProjectAndBindToDevOpsPlatform(mock(DbSession.class), CreationMethod.ALM_IMPORT_API, true, null, null);

    ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingCaptor = ArgumentCaptor.forClass(ProjectAlmSettingDto.class);
    verify(dbClient.projectAlmSettingDao()).insertOrUpdate(any(), projectAlmSettingCaptor.capture(), eq(ALM_SETTING_KEY), eq(repository.getName()), eq(generatedProjectKey));
    ProjectAlmSettingDto createdProjectAlmSettingDto = projectAlmSettingCaptor.getValue();

    assertThat(createdProjectAlmSettingDto.getAlmSettingUuid()).isEqualTo(ALM_SETTING_UUID);
    assertThat(createdProjectAlmSettingDto.getAlmRepo()).isEqualTo(DOP_REPOSITORY_ID);
    assertThat(createdProjectAlmSettingDto.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(createdProjectAlmSettingDto.getMonorepo()).isTrue();
  }

  private void mockValidUserSession() {
    lenient().when(userSession.getLogin()).thenReturn(USER_LOGIN);
    lenient().when(userSession.getUuid()).thenReturn(USER_UUID);
  }

  private void mockValidPatForUser() {
    AlmPatDto almPatDto = mock();
    when(almPatDto.getPersonalAccessToken()).thenReturn(USER_PAT);
    when(dbClient.almPatDao().selectByUserAndAlmSetting(any(), eq(USER_UUID), eq(almSettingDto))).thenReturn(Optional.of(almPatDto));
  }

  private void mockValidAlmSettingsDto() {
    lenient().when(almSettingDto.getUrl()).thenReturn(URL);
    lenient().when(almSettingDto.getKey()).thenReturn(ALM_SETTING_KEY);
    lenient().when(almSettingDto.getUuid()).thenReturn(ALM_SETTING_UUID);
  }

  private void mockValidProjectDescriptor() {
    lenient().when(devOpsProjectDescriptor.alm()).thenReturn(ALM.BITBUCKET);
    lenient().when(devOpsProjectDescriptor.url()).thenReturn(URL);
    lenient().when(devOpsProjectDescriptor.repositoryIdentifier()).thenReturn(DOP_REPOSITORY_ID);
    lenient().when(devOpsProjectDescriptor.projectIdentifier()).thenReturn(DOP_PROJECT_ID);
  }

  private Repository mockValidBitBucketRepository() {
    Repository repository = new Repository(DOP_REPOSITORY_ID, "Repository name", 12L, new Project(DOP_PROJECT_ID, "Project name", 42L));
    when(bitbucketServerRestClient.getRepo(URL, USER_PAT, DOP_PROJECT_ID, DOP_REPOSITORY_ID)).thenReturn(repository);

    BranchesList branches = new BranchesList(List.of(
      new Branch(MAIN_BRANCH_NAME, true),
      new Branch("feature", false)));
    when(bitbucketServerRestClient.getBranches(URL, USER_PAT, DOP_PROJECT_ID, DOP_REPOSITORY_ID)).thenReturn(branches);

    return repository;
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
