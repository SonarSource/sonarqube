/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.batch;

import com.google.common.collect.ImmutableMap;
import java.util.Date;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualityprofile.DefaultQProfileDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.source.FileSourceDao;
import org.sonar.db.source.FileSourceDto;
import org.sonar.db.source.FileSourceDto.Type;
import org.sonar.scanner.protocol.input.FileData;
import org.sonar.scanner.protocol.input.ProjectRepositories;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.server.qualityprofile.QProfileTesting.newQProfileDto;

public class ProjectDataLoaderMediumTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes().addXoo();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbSession dbSession;
  DbClient dbClient;

  ProjectDataLoader underTest;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbClient = tester.get(DbClient.class);
    dbSession = dbClient.openSession(false);
    underTest = tester.get(ProjectDataLoader.class);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void return_project_settings_with_global_scan_permission() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));
    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(project.key()));

    Map<String, String> projectSettings = ref.settings(project.key());
    assertThat(projectSettings).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"));
  }

  @Test
  public void return_project_settings_with_project_scan_permission() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    userSessionRule.logIn("john").addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));
    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(project.key()));

    Map<String, String> projectSettings = ref.settings(project.key());
    assertThat(projectSettings).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"));
  }

  @Test
  public void not_returned_secured_settings_when_lgged_but_no_scan_permission() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    userSessionRule.logIn("john").addProjectPermission(UserRole.USER, project);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));
    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(project.key()).setIssuesMode(true));
    Map<String, String> projectSettings = ref.settings(project.key());
    assertThat(projectSettings).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR"));
  }

  @Test
  public void return_project_with_module_settings() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    dbClient.componentDao().insert(dbSession, module);

    // Module properties
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module.getId()));
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(module.getId()));

    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(project.key()));
    assertThat(ref.settings(project.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"));
    assertThat(ref.settings(module.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-SERVER",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"));
  }

  @Test
  public void return_project_with_module_settings_inherited_from_project() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    dbClient.componentDao().insert(dbSession, module);

    // No property on module -> should have the same as project

    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(project.key()));
    assertThat(ref.settings(project.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"));
    assertThat(ref.settings(module.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"));
  }

  @Test
  public void return_project_with_module_with_sub_module() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    dbClient.componentDao().insert(dbSession, module);

    // Module properties
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module.getId()));
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(module.getId()));

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    dbClient.componentDao().insert(dbSession, subModule);

    // Sub module properties
    dbClient.propertiesDao().saveProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER-DAO").setResourceId(subModule.getId()));

    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(project.key()));
    assertThat(ref.settings(project.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"));
    assertThat(ref.settings(module.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-SERVER",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"));
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-SERVER-DAO",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"));
  }

  @Test
  public void return_project_with_two_modules() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));

    ComponentDto module1 = ComponentTesting.newModuleDto(project);
    dbClient.componentDao().insert(dbSession, module1);

    // Module 1 properties
    dbClient.propertiesDao()
      .saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module1.getId()));
    // This property should not be found on the other module
    dbClient.propertiesDao()
      .saveProperty(dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(module1.getId()));

    ComponentDto module2 = ComponentTesting.newModuleDto(project);
    dbClient.componentDao().insert(dbSession, module2);

    // Module 2 property
    dbClient.propertiesDao()
      .saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-APPLICATION").setResourceId(module2.getId()));

    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(project.key()));
    assertThat(ref.settings(project.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"));
    assertThat(ref.settings(module1.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-SERVER",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"));
    assertThat(ref.settings(module2.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-APPLICATION",
      "sonar.jira.login.secured", "john"));
  }

  @Test
  public void return_provisioned_project_settings() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    // No snapshot attached on the project -> provisioned project
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));

    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(project.key()));
    assertThat(ref.settings(project.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"));
  }

  @Test
  public void return_sub_module_settings() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();
    // No project properties

    ComponentDto module = ComponentTesting.newModuleDto(project);
    dbClient.componentDao().insert(dbSession, module);
    // No module properties

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, subModule);

    // Sub module properties
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(subModule.getId()));
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(subModule.getId()));
    dbClient.propertiesDao()
      .saveProperty(dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(subModule.getId()));

    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"));
  }

  @Test
  public void return_sub_module_settings_including_settings_from_parent_modules() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project property
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    dbClient.componentDao().insert(dbSession, module);

    // Module property
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(module.getId()));

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, subModule);

    // Sub module properties
    dbClient.propertiesDao()
      .saveProperty(dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(subModule.getId()));

    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"));
  }

  @Test
  public void return_sub_module_settings_only_inherited_from_project() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));
    dbClient.propertiesDao()
      .saveProperty(dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    dbClient.componentDao().insert(dbSession, module);
    // No module property

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, subModule);
    // No sub module property

    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"));
  }

  @Test
  public void return_sub_module_settings_inherited_from_project_and_module() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));
    dbClient.propertiesDao()
      .saveProperty(dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    dbClient.componentDao().insert(dbSession, module);

    // Module property
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module.getId()));

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, subModule);
    // No sub module property

    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-SERVER",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"));
  }

  @Test
  public void fail_when_no_browse_permission_nor_scan_permission() {
    userSessionRule.logIn();

    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();

    try {
      underTest.load(ProjectDataQuery.create().setModuleKey(project.key()));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class).hasMessage(Messages.NO_PERMISSION);
    }
  }

  @Test
  public void fail_when_not_preview_and_only_browse_permission_without_scan_permission() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();

    userSessionRule.logIn("john").addProjectPermission(UserRole.USER, project);

    thrown.expect(ForbiddenException.class);
    thrown.expectMessage("You're only authorized to execute a local (preview) SonarQube analysis without pushing the results to the SonarQube server. " +
      "Please contact your SonarQube administrator.");
    underTest.load(ProjectDataQuery.create().setModuleKey(project.key()).setIssuesMode(false));
  }

  @Test
  public void fail_when_preview_and_only_scan_permission_without_browse_permission() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();

    userSessionRule.logIn("john").addProjectPermission(GlobalPermissions.SCAN_EXECUTION, project);

    thrown.expect(ForbiddenException.class);
    thrown.expectMessage("You don't have the required permissions to access this project. Please contact your SonarQube administrator.");
    underTest.load(ProjectDataQuery.create().setModuleKey(project.key()).setIssuesMode(true));
  }

  @Test
  public void return_file_data_from_single_project() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    ComponentDto file = ComponentTesting.newFileDto(project, null, "file");
    dbClient.componentDao().insert(dbSession, file);
    tester.get(FileSourceDao.class).insert(dbSession, newFileSourceDto(file).setSrcHash("123456"));
    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(project.key()));
    assertThat(ref.fileDataByPath(project.key())).hasSize(1);
    FileData fileData = ref.fileData(project.key(), file.path());
    assertThat(fileData.hash()).isEqualTo("123456");
  }

  @Test
  public void return_file_data_from_multi_modules() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // File on project
    ComponentDto projectFile = ComponentTesting.newFileDto(project, null, "projectFile");
    dbClient.componentDao().insert(dbSession, projectFile);
    tester.get(FileSourceDao.class).insert(dbSession, newFileSourceDto(projectFile).setSrcHash("123456"));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    dbClient.componentDao().insert(dbSession, module);

    // File on module
    ComponentDto moduleFile = ComponentTesting.newFileDto(module, null, "moduleFile");
    dbClient.componentDao().insert(dbSession, moduleFile);
    tester.get(FileSourceDao.class).insert(dbSession, newFileSourceDto(moduleFile).setSrcHash("789456"));

    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(project.key()));
    assertThat(ref.fileData(project.key(), projectFile.path()).hash()).isEqualTo("123456");
    assertThat(ref.fileData(module.key(), moduleFile.path()).hash()).isEqualTo("789456");
  }

  @Test
  public void return_file_data_from_module() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    dbClient.componentDao().insert(dbSession, project);
    addDefaultProfile();

    // File on project
    ComponentDto projectFile = ComponentTesting.newFileDto(project, null, "projectFile");
    dbClient.componentDao().insert(dbSession, projectFile);
    tester.get(FileSourceDao.class).insert(dbSession, newFileSourceDto(projectFile).setSrcHash("123456").setRevision("987654321"));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    userSessionRule.logIn().addProjectPermission(SCAN_EXECUTION, project);
    dbClient.componentDao().insert(dbSession, module);

    // File on module
    ComponentDto moduleFile = ComponentTesting.newFileDto(module, null, "moduleFile");
    dbClient.componentDao().insert(dbSession, moduleFile);
    tester.get(FileSourceDao.class).insert(dbSession, newFileSourceDto(moduleFile).setSrcHash("789456").setRevision("123456789"));

    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setModuleKey(module.key()));
    assertThat(ref.fileData(module.key(), moduleFile.path()).hash()).isEqualTo("789456");
    assertThat(ref.fileData(module.key(), moduleFile.path()).revision()).isEqualTo("123456789");
    assertThat(ref.fileData(project.key(), projectFile.path())).isNull();
  }

  private void addDefaultProfile() {
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    dbClient.organizationDao().insert(dbSession, organizationDto, false);
    QProfileDto profileDto = newQProfileDto(organizationDto, QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(
      formatDateTime(new Date()));
    dbClient.qualityProfileDao().insert(dbSession, profileDto);
    dbClient.defaultQProfileDao().insertOrUpdate(dbSession, DefaultQProfileDto.from(profileDto));
  }

  private static FileSourceDto newFileSourceDto(ComponentDto file) {
    return new FileSourceDto()
      .setFileUuid(file.uuid())
      .setProjectUuid(file.projectUuid())
      .setDataHash("0263047cd758c68c27683625f072f010")
      .setLineHashes("8d7b3d6b83c0a517eac07e1aac94b773")
      .setCreatedAt(System.currentTimeMillis())
      .setUpdatedAt(System.currentTimeMillis())
      .setDataType(Type.SOURCE)
      .setRevision("123456789")
      .setSrcHash("123456");
  }

}
