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
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.almsettings.DevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectCreatorFactory;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.project.CreationMethod.ALM_IMPORT_API;
import static org.sonar.db.project.CreationMethod.ALM_IMPORT_MONOREPO_API;

class ImportProjectServiceTest {

  private static final String API_URL = "https://api.com";
  private static final String PROJECT_UUID = "project-uuid";
  private static final String DOP_REPOSITORY_ID = "repository-id";
  private static final String DOP_PROJECT_ID = "project-id";
  private static final String PROJECT_KEY = "project-key";
  private static final String PROJECT_NAME = "project-name";
  private static final String MAIN_BRANCH_UUID = "main-branch-uuid";
  private static final String MAIN_BRANCH_KEY = "main-branch-key";
  private static final String ALM_SETTING_ID = "alm-setting-id";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DevOpsProjectCreatorFactory devOpsProjectCreatorFactory = mock();

  private final DbClient dbClient = mock(Answers.RETURNS_DEEP_STUBS);
  private final NewCodeDefinitionResolver newCodeDefinitionResolver = mock();
  private final ComponentUpdater componentUpdater = mock();

  private final ImportProjectService importProjectService = new ImportProjectService(dbClient, devOpsProjectCreatorFactory, userSession, componentUpdater,
    newCodeDefinitionResolver);;

  @Test
  void createdImportedProject_whenAlmSettingDoesntExist_throws() {
    userSession.logIn().addPermission(PROVISION_PROJECTS);
    DbSession dbSession = mockDbSession();
    when(dbClient.almSettingDao().selectByUuid(dbSession, ALM_SETTING_ID)).thenReturn(Optional.empty());

    ImportProjectRequest request = new ImportProjectRequest(PROJECT_KEY, PROJECT_NAME, ALM_SETTING_ID, DOP_REPOSITORY_ID, DOP_PROJECT_ID, null, null, true);

    assertThatThrownBy(() -> importProjectService.importProject(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("ALM setting not found");

  }

  @Test
  void createImportedProject_whenAlmIsNotSupported_throws() {
    userSession.logIn().addPermission(PROVISION_PROJECTS);

    DbSession dbSession = mockDbSession();
    AlmSettingDto almSetting = mockAlmSetting(dbSession);

    when(devOpsProjectCreatorFactory.getDevOpsProjectCreator(eq(almSetting), any()))
      .thenReturn(Optional.empty());

    ImportProjectRequest request = new ImportProjectRequest(PROJECT_KEY, PROJECT_NAME, ALM_SETTING_ID, DOP_REPOSITORY_ID, DOP_PROJECT_ID, null, null, true);

    assertThatThrownBy(() -> importProjectService.importProject(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Platform GITHUB not supported");
  }

  @Test
  void createImportedProject_whenAlmIsSupportedAndNoNewCodeDefinitionDefined_shouldCreateProject() {
    userSession.logIn().addPermission(PROVISION_PROJECTS);
    DbSession dbSession = mockDbSession();
    AlmSettingDto almSetting = mockAlmSetting(dbSession);

    DevOpsProjectCreator devOpsProjectCreator = mockDevOpsProjectCreator(almSetting);

    ComponentCreationData componentCreationData = mockProjectCreation(devOpsProjectCreator, ALM_IMPORT_MONOREPO_API, true, dbSession);

    ProjectDto projectDto = mockProjectDto(componentCreationData);
    when(componentCreationData.mainBranchDto()).thenReturn(mock(BranchDto.class));

    ProjectAlmSettingDto projectAlmSettingDto = mockProjectAlmSetting(dbSession, projectDto);

    ImportProjectRequest request = new ImportProjectRequest(PROJECT_KEY, PROJECT_NAME, ALM_SETTING_ID, DOP_REPOSITORY_ID, DOP_PROJECT_ID, null, null, true);

    ImportedProject importedProject = importProjectService.importProject(request);

    assertThat(importedProject.projectDto()).isEqualTo(projectDto);
    assertThat(importedProject.projectAlmSettingDto()).isEqualTo(projectAlmSettingDto);

    verify(componentUpdater).commitAndIndex(dbSession, componentCreationData);
  }

  @Test
  void createImportedProject_whenAlmIsSupportedAndNewCodeDefinitionDefined_shouldCreateProjectAndNewCodeDefinition() {
    userSession.logIn().addPermission(PROVISION_PROJECTS);
    DbSession dbSession = mockDbSession();
    AlmSettingDto almSetting = mockAlmSetting(dbSession);

    DevOpsProjectCreator devOpsProjectCreator = mockDevOpsProjectCreator(almSetting);

    ComponentCreationData componentCreationData = mockProjectCreation(devOpsProjectCreator, ALM_IMPORT_API, false, dbSession);

    ProjectDto projectDto = mockProjectDto(componentCreationData);
    mockBranchDto(componentCreationData);

    ProjectAlmSettingDto projectAlmSettingDto = mockProjectAlmSetting(dbSession, projectDto);

    ImportProjectRequest request = new ImportProjectRequest(PROJECT_KEY, PROJECT_NAME, ALM_SETTING_ID, DOP_REPOSITORY_ID, DOP_PROJECT_ID, "NUMBER_OF_DAYS", "10", false);

    ImportedProject importedProject = importProjectService.importProject(request);

    assertThat(importedProject.projectDto()).isEqualTo(projectDto);
    assertThat(importedProject.projectAlmSettingDto()).isEqualTo(projectAlmSettingDto);

    verify(newCodeDefinitionResolver).createNewCodeDefinition(
      dbSession,
      PROJECT_UUID,
      MAIN_BRANCH_UUID,
      MAIN_BRANCH_KEY,
      "NUMBER_OF_DAYS",
      "10");
    verify(componentUpdater).commitAndIndex(dbSession, componentCreationData);
  }

  private DbSession mockDbSession() {
    DbSession dbSession = mock(DbSession.class);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    return dbSession;
  }

  private AlmSettingDto mockAlmSetting(DbSession dbSession) {
    AlmSettingDto almSetting = mock(AlmSettingDto.class);
    when(almSetting.getAlm()).thenReturn(ALM.GITHUB);
    when(almSetting.getUrl()).thenReturn(API_URL);
    when(dbClient.almSettingDao().selectByUuid(dbSession, ALM_SETTING_ID)).thenReturn(Optional.of(almSetting));
    return almSetting;
  }

  private DevOpsProjectCreator mockDevOpsProjectCreator(AlmSettingDto almSetting) {
    DevOpsProjectCreator devOpsProjectCreator = mock(DevOpsProjectCreator.class);
    DevOpsProjectDescriptor projectDescriptor = new DevOpsProjectDescriptor(ALM.GITHUB, API_URL, DOP_REPOSITORY_ID, DOP_PROJECT_ID);
    when(devOpsProjectCreatorFactory.getDevOpsProjectCreator(almSetting, projectDescriptor))
      .thenReturn(Optional.of(devOpsProjectCreator));
    return devOpsProjectCreator;
  }

  private static ComponentCreationData mockProjectCreation(DevOpsProjectCreator devOpsProjectCreator, CreationMethod creationMethod, boolean monorepo, DbSession dbSession) {
    ComponentCreationData componentCreationData = mock(ComponentCreationData.class);
    when(devOpsProjectCreator.createProjectAndBindToDevOpsPlatform(dbSession, creationMethod, monorepo, PROJECT_KEY, PROJECT_NAME))
      .thenReturn(componentCreationData);
    return componentCreationData;
  }

  private static ProjectDto mockProjectDto(ComponentCreationData componentCreationData) {
    ProjectDto projectDto = mock(ProjectDto.class);
    lenient().when(projectDto.getUuid()).thenReturn(PROJECT_UUID);
    when(componentCreationData.projectDto()).thenReturn(projectDto);
    return projectDto;
  }

  private static void mockBranchDto(ComponentCreationData componentCreationData) {
    BranchDto mainBrainDto = mock(BranchDto.class);
    when(mainBrainDto.getUuid()).thenReturn(MAIN_BRANCH_UUID);
    when(mainBrainDto.getKey()).thenReturn(MAIN_BRANCH_KEY);
    when(componentCreationData.mainBranchDto()).thenReturn(mainBrainDto);
  }

  private ProjectAlmSettingDto mockProjectAlmSetting(DbSession dbSession, ProjectDto projectDto) {
    ProjectAlmSettingDto projectAlmSetting = mock(ProjectAlmSettingDto.class);
    when(dbClient.projectAlmSettingDao().selectByProject(dbSession, projectDto))
      .thenReturn(Optional.of(projectAlmSetting));
    return projectAlmSetting;
  }

}
