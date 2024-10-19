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
package org.sonar.server.common.almsettings;

import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.UserRole;
import org.sonar.auth.DevOpsPlatformSettings;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.component.ComponentCreationParameters;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.component.NewComponent;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.permission.UserPermissionChange;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.project.CreationMethod.ALM_IMPORT_API;
import static org.sonar.db.project.CreationMethod.SCANNER_API_DEVOPS_AUTO_CONFIG;

@ExtendWith(MockitoExtension.class)
class DefaultDevOpsProjectCreatorTest {

  private static final String ORGANIZATION_NAME = "orga2";
  private static final String REPOSITORY_NAME = "repo1";

  private static final String MAIN_BRANCH_NAME = "defaultBranch";
  private static final DevOpsProjectDescriptor DEVOPS_PROJECT_DESCRIPTOR = new DevOpsProjectDescriptor(ALM.GITHUB, "http://api.com", ORGANIZATION_NAME + "/" + REPOSITORY_NAME,
    null);
  private static final String ALM_SETTING_KEY = "github_config_1";
  private static final String USER_LOGIN = "userLogin";
  private static final String USER_UUID = "userUuid";

  @Mock
  private DbClient dbClient;
  @Mock
  private ProjectKeyGenerator projectKeyGenerator;
  @Mock
  private DevOpsPlatformSettings devOpsPlatformSettings;
  @Mock
  private PermissionUpdater<UserPermissionChange> permissionUpdater;
  @Mock
  private ManagedProjectService managedProjectService;
  @Mock
  private DevOpsProjectCreationContext devOpsProjectCreationContext;

  @InjectMocks
  private DefaultDevOpsProjectCreator defaultDevOpsProjectCreator;

  @Captor
  ArgumentCaptor<ComponentCreationParameters> componentCreationParametersCaptor;
  @Captor
  ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingDtoCaptor;

  @Mock
  private AlmSettingDto almSettingDto;
  @Mock
  private UserSession userSession;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ProjectDefaultVisibility projectDefaultVisibility;
  @Mock
  private ComponentUpdater componentUpdater;

  private final PermissionService permissionService = new PermissionServiceImpl(mock());

  @BeforeEach
  void setup() {
    lenient().when(userSession.getLogin()).thenReturn(USER_LOGIN);
    lenient().when(userSession.getUuid()).thenReturn(USER_UUID);
    lenient().when(devOpsProjectCreationContext.userSession()).thenReturn(userSession);

    lenient().when(almSettingDto.getUrl()).thenReturn(DEVOPS_PROJECT_DESCRIPTOR.url());
    lenient().when(almSettingDto.getKey()).thenReturn(ALM_SETTING_KEY);
    lenient().when(devOpsProjectCreationContext.almSettingDto()).thenReturn(almSettingDto);

    lenient().when(devOpsProjectCreationContext.name()).thenReturn(REPOSITORY_NAME);
    lenient().when(devOpsProjectCreationContext.devOpsPlatformIdentifier()).thenReturn(ORGANIZATION_NAME + "/" + REPOSITORY_NAME);
    lenient().when(devOpsProjectCreationContext.fullName()).thenReturn(ORGANIZATION_NAME + "/" + REPOSITORY_NAME);
    lenient().when(devOpsProjectCreationContext.defaultBranchName()).thenReturn(MAIN_BRANCH_NAME);

    ProjectCreator projectCreator = new ProjectCreator(userSession, projectDefaultVisibility, componentUpdater);
    defaultDevOpsProjectCreator = new DefaultDevOpsProjectCreator(dbClient, devOpsProjectCreationContext, projectKeyGenerator, devOpsPlatformSettings, projectCreator,
      permissionService, permissionUpdater,
      managedProjectService);

  }

  @Test
  void isScanAllowedUsingPermissionsFromDevopsPlatform_throws() {
    assertThatException()
      .isThrownBy(() -> defaultDevOpsProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform())
      .isInstanceOf(UnsupportedOperationException.class)
      .withMessage("Not Implemented");
  }

  @Test
  void createProjectAndBindToDevOpsPlatformFromScanner_whenVisibilitySyncDeactivated_successfullyCreatesProjectAndUseDefaultProjectVisibility() {
    // given
    mockGeneratedProjectKey();

    ComponentCreationData componentCreationData = mockProjectCreation("generated_orga2/repo1");
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);

    // when
    ComponentCreationData actualComponentCreationData = defaultDevOpsProjectCreator.createProjectAndBindToDevOpsPlatform(dbClient.openSession(true),
      null, SCANNER_API_DEVOPS_AUTO_CONFIG, false, null, null);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertComponentCreationParametersContainsCorrectInformation(componentCreationParameters, "generated_orga2/repo1", SCANNER_API_DEVOPS_AUTO_CONFIG);
    assertThat(componentCreationParameters.isManaged()).isFalse();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isTrue();

    verify(projectAlmSettingDao).insertOrUpdate(any(), projectAlmSettingDtoCaptor.capture(), eq(ALM_SETTING_KEY), eq(REPOSITORY_NAME), eq("generated_orga2/repo1"));
    ProjectAlmSettingDto projectAlmSettingDto = projectAlmSettingDtoCaptor.getValue();
    assertAlmSettingsDtoContainsCorrectInformation(almSettingDto, requireNonNull(componentCreationData.projectDto()), projectAlmSettingDto);
  }

  @Test
  void createProjectAndBindToDevOpsPlatformFromScanner_whenVisibilitySynchronizationEnabled_successfullyCreatesProjectAndSetsVisibility() {
    // given
    mockGeneratedProjectKey();
    when(devOpsProjectCreationContext.isPublic()).thenReturn(true);

    ComponentCreationData componentCreationData = mockProjectCreation("generated_orga2/repo1");
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
    when(devOpsPlatformSettings.isProvisioningEnabled()).thenReturn(true);
    when(devOpsPlatformSettings.isProjectVisibilitySynchronizationActivated()).thenReturn(true);

    // when
    ComponentCreationData actualComponentCreationData = defaultDevOpsProjectCreator.createProjectAndBindToDevOpsPlatform(dbClient.openSession(true),
        null, SCANNER_API_DEVOPS_AUTO_CONFIG, false, null, null);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isFalse();
  }

  @Test
  void createProjectAndBindToDevOpsPlatformFromScanner_whenVisibilitySynchronizationDisabled_successfullyCreatesProjectAndMakesProjectPrivate() {
    // given
    mockGeneratedProjectKey();

    ComponentCreationData componentCreationData = mockProjectCreation("generated_orga2/repo1");
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
    when(devOpsPlatformSettings.isProvisioningEnabled()).thenReturn(true);
    when(devOpsPlatformSettings.isProjectVisibilitySynchronizationActivated()).thenReturn(false);

    // when
    ComponentCreationData actualComponentCreationData = defaultDevOpsProjectCreator.createProjectAndBindToDevOpsPlatform(dbClient.openSession(true),
        null, SCANNER_API_DEVOPS_AUTO_CONFIG, false, null, null);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isTrue();
  }

  @Test
  void createProjectAndBindToDevOpsPlatformFromApi_whenRepoFoundOnGitHub_successfullyCreatesProject() {
    // given
    String projectKey = "customProjectKey";
    mockGeneratedProjectKey();

    ComponentCreationData componentCreationData = mockProjectCreation(projectKey);
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);

    // when
    ComponentCreationData actualComponentCreationData = defaultDevOpsProjectCreator.createProjectAndBindToDevOpsPlatform(dbClient.openSession(true), null, ALM_IMPORT_API, false,
      projectKey,
      null);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertComponentCreationParametersContainsCorrectInformation(componentCreationParameters, projectKey, ALM_IMPORT_API);
    assertThat(componentCreationParameters.isManaged()).isFalse();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isTrue();

    verify(projectAlmSettingDao).insertOrUpdate(any(), projectAlmSettingDtoCaptor.capture(), eq(ALM_SETTING_KEY), eq(REPOSITORY_NAME), eq(projectKey));
    ProjectAlmSettingDto projectAlmSettingDto = projectAlmSettingDtoCaptor.getValue();
    assertAlmSettingsDtoContainsCorrectInformation(almSettingDto, requireNonNull(componentCreationData.projectDto()), projectAlmSettingDto);
  }

  @Captor
  private ArgumentCaptor<Collection<UserPermissionChange>> permissionChangesCaptor;

  @Test
  void createProjectAndBindToDevOpsPlatformFromApi_whenAutoProvisioningOnAndRepoPrivate_successfullyCreatesProject() {
    // given
    String projectKey = "customProjectKey";
    mockGeneratedProjectKey();

    ComponentCreationData componentCreationData = mockProjectCreation(projectKey);
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
    when(devOpsPlatformSettings.isProvisioningEnabled()).thenReturn(true);

    // when
    ComponentCreationData actualComponentCreationData = defaultDevOpsProjectCreator.createProjectAndBindToDevOpsPlatform(dbClient.openSession(true), null, ALM_IMPORT_API, false,
      projectKey,
      null);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertComponentCreationParametersContainsCorrectInformation(componentCreationParameters, projectKey, ALM_IMPORT_API);
    assertThat(componentCreationParameters.isManaged()).isTrue();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isTrue();

    verifyScanPermissionWasAddedToUser(actualComponentCreationData);
    verifyProjectSyncTaskWasCreated(actualComponentCreationData);

    verify(projectAlmSettingDao).insertOrUpdate(any(), projectAlmSettingDtoCaptor.capture(), eq(ALM_SETTING_KEY), eq(REPOSITORY_NAME), eq(projectKey));
    ProjectAlmSettingDto projectAlmSettingDto = projectAlmSettingDtoCaptor.getValue();
    assertAlmSettingsDtoContainsCorrectInformation(almSettingDto, requireNonNull(componentCreationData.projectDto()), projectAlmSettingDto);
  }

  private void verifyProjectSyncTaskWasCreated(ComponentCreationData componentCreationData) {
    String projectUuid = requireNonNull(componentCreationData.projectDto()).getUuid();
    String mainBranchUuid = requireNonNull(componentCreationData.mainBranchDto()).getUuid();
    verify(managedProjectService).queuePermissionSyncTask(USER_UUID, mainBranchUuid, projectUuid);
  }

  private void verifyScanPermissionWasAddedToUser(ComponentCreationData actualComponentCreationData) {
    verify(permissionUpdater).apply(any(), permissionChangesCaptor.capture());
    UserPermissionChange permissionChange = permissionChangesCaptor.getValue().iterator().next();
    assertThat(permissionChange.getUserId().getUuid()).isEqualTo(userSession.getUuid());
    assertThat(permissionChange.getUserId().getLogin()).isEqualTo(userSession.getLogin());
    assertThat(permissionChange.getPermission()).isEqualTo(UserRole.SCAN);
    assertThat(permissionChange.getProjectUuid()).isEqualTo(actualComponentCreationData.projectDto().getUuid());
  }

  private void mockGeneratedProjectKey() {
    String generatedKey = "generated_" + devOpsProjectCreationContext.devOpsPlatformIdentifier();
    when(projectKeyGenerator.generateUniqueProjectKey(devOpsProjectCreationContext.fullName())).thenReturn(generatedKey);
  }

  private ComponentCreationData mockProjectCreation(String projectKey) {
    ComponentCreationData componentCreationData = mock();
    ProjectDto projectDto = mockProjectDto(projectKey);
    when(componentCreationData.projectDto()).thenReturn(projectDto);
    BranchDto branchDto = mock();
    when(componentCreationData.mainBranchDto()).thenReturn(branchDto);
    when(componentUpdater.createWithoutCommit(any(), componentCreationParametersCaptor.capture())).thenReturn(componentCreationData);
    return componentCreationData;
  }

  private static ProjectDto mockProjectDto(String projectKey) {
    ProjectDto projectDto = mock();
    when(projectDto.getName()).thenReturn(REPOSITORY_NAME);
    when(projectDto.getKey()).thenReturn(projectKey);
    when(projectDto.getUuid()).thenReturn("project-uuid-1");
    return projectDto;
  }

  private static void assertComponentCreationParametersContainsCorrectInformation(ComponentCreationParameters componentCreationParameters, String expectedKey,
    CreationMethod expectedCreationMethod) {
    assertThat(componentCreationParameters.creationMethod()).isEqualTo(expectedCreationMethod);
    assertThat(componentCreationParameters.mainBranchName()).isEqualTo(MAIN_BRANCH_NAME);
    assertThat(componentCreationParameters.userLogin()).isEqualTo(USER_LOGIN);
    assertThat(componentCreationParameters.userUuid()).isEqualTo(USER_UUID);

    NewComponent newComponent = componentCreationParameters.newComponent();
    assertThat(newComponent.isProject()).isTrue();
    assertThat(newComponent.qualifier()).isEqualTo(Qualifiers.PROJECT);
    assertThat(newComponent.key()).isEqualTo(expectedKey);
    assertThat(newComponent.name()).isEqualTo(REPOSITORY_NAME);
  }

  private static void assertAlmSettingsDtoContainsCorrectInformation(AlmSettingDto almSettingDto, ProjectDto projectDto, ProjectAlmSettingDto projectAlmSettingDto) {
    assertThat(projectAlmSettingDto.getAlmRepo()).isEqualTo(DEVOPS_PROJECT_DESCRIPTOR.repositoryIdentifier());
    assertThat(projectAlmSettingDto.getAlmSlug()).isNull();
    assertThat(projectAlmSettingDto.getAlmSettingUuid()).isEqualTo(almSettingDto.getUuid());
    assertThat(projectAlmSettingDto.getProjectUuid()).isEqualTo(projectDto.getUuid());
    assertThat(projectAlmSettingDto.getMonorepo()).isFalse();
    assertThat(projectAlmSettingDto.getSummaryCommentEnabled()).isTrue();
  }

}

