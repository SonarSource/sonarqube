/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.batch.protocol.input.ProjectReferentials;
import org.sonar.batch.protocol.input.QProfile;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ProjectReferentialsLoaderMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().addXoo();

  DbSession dbSession;

  ProjectReferentialsLoader loader;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbSession = tester.get(DbClient.class).openSession(false);
    loader = tester.get(ProjectReferentialsLoader.class);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void return_project_settings() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForProject(project));
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()),
      dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()),
      dbSession);
    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
    Map<String, String> projectSettings = ref.settings(project.key());
    assertThat(projectSettings).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"
    ));
  }

  @Test
  public void not_returned_secured_settings_with_only_preview_permission() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.DRY_RUN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForProject(project));
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()),
      dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()),
      dbSession);
    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()).setPreview(true));
    Map<String, String> projectSettings = ref.settings(project.key());
    assertThat(projectSettings).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR"
    ));
  }

  @Test
  public void return_project_with_module_settings() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, projectSnapshot);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()), dbSession);

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForComponent(module, project, projectSnapshot));

    // Module properties
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(module.getId()), dbSession);

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
    assertThat(ref.settings(project.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"
    ));
    assertThat(ref.settings(module.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-SERVER",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
  }

  @Test
  public void return_project_with_module_settings_inherited_from_project() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, projectSnapshot);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()), dbSession);

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForComponent(module, project, projectSnapshot));

    // No property on module -> should have the same as project

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
    assertThat(ref.settings(project.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"
    ));
    assertThat(ref.settings(module.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"
    ));
  }

  @Test
  public void return_project_with_module_with_sub_module() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, projectSnapshot);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()), dbSession);

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);
    SnapshotDto moduleSnapshot = SnapshotTesting.createForComponent(module, project, projectSnapshot);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, moduleSnapshot);

    // Module properties
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(module.getId()), dbSession);

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    tester.get(DbClient.class).componentDao().insert(dbSession, subModule);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForComponent(subModule, module, moduleSnapshot));

    // Sub module properties
    tester.get(DbClient.class).propertiesDao().setProperty(
      new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER-DAO").setResourceId(subModule.getId()), dbSession);

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
    assertThat(ref.settings(project.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"
    ));
    assertThat(ref.settings(module.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-SERVER",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-SERVER-DAO",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
  }

  @Test
  public void return_project_with_two_modules() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, projectSnapshot);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()), dbSession);

    ComponentDto module1 = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module1);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForComponent(module1, project, projectSnapshot));

    // Module 1 properties
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module1.getId()), dbSession);
    // This property should not be found on the other module
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(module1.getId()), dbSession);

    ComponentDto module2 = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module2);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForComponent(module2, project, projectSnapshot));

    // Module 2 property
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-APPLICATION").setResourceId(module2.getId()), dbSession);

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
    assertThat(ref.settings(project.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"
    ));
    assertThat(ref.settings(module1.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-SERVER",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
    assertThat(ref.settings(module2.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-APPLICATION",
      "sonar.jira.login.secured", "john"
    ));
  }

  @Test
  public void return_provisioned_project_settings() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    // No snapshot attached on the project -> provisioned project
    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()), dbSession);

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
    assertThat(ref.settings(project.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"
    ));
  }

  @Test
  public void return_sub_module_settings() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, projectSnapshot);
    addDefaultProfile();
    // No project properties

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);
    SnapshotDto moduleSnapshot = SnapshotTesting.createForComponent(module, project, projectSnapshot);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, moduleSnapshot);
    // No module properties

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    tester.get(DbClient.class).componentDao().insert(dbSession, subModule);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForComponent(subModule, module, moduleSnapshot));

    // Sub module properties
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(subModule.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(subModule.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(subModule.getId()), dbSession);

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
  }

  @Test
  public void return_sub_module_settings_including_settings_from_parent_modules() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, projectSnapshot);
    addDefaultProfile();

    // Project property
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()), dbSession);

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);
    SnapshotDto moduleSnapshot = SnapshotTesting.createForComponent(module, project, projectSnapshot);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, moduleSnapshot);

    // Module property
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(module.getId()), dbSession);

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    tester.get(DbClient.class).componentDao().insert(dbSession, subModule);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForComponent(subModule, module, moduleSnapshot));

    // Sub module properties
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(subModule.getId()), dbSession);

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
  }

  @Test
  public void return_sub_module_settings_only_inherited_from_project() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, projectSnapshot);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(project.getId()), dbSession);

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);
    SnapshotDto moduleSnapshot = SnapshotTesting.createForComponent(module, project, projectSnapshot);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, moduleSnapshot);
    // No module property

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    tester.get(DbClient.class).componentDao().insert(dbSession, subModule);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForComponent(subModule, module, moduleSnapshot));
    // No sub module property

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
  }

  @Test
  public void return_sub_module_settings_inherited_from_project_and_module() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, projectSnapshot);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()), dbSession);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(project.getId()), dbSession);

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);
    SnapshotDto moduleSnapshot = SnapshotTesting.createForComponent(module, project, projectSnapshot);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, moduleSnapshot);

    // Module property
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module.getId()), dbSession);

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    tester.get(DbClient.class).componentDao().insert(dbSession, subModule);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForComponent(subModule, module, moduleSnapshot));
    // No sub module property

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-SERVER",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
  }

  @Test
  public void return_quality_profile_from_project_profile() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForProject(project));

    QualityProfileDto profileDto = QProfileTesting.newDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(DateUtils.formatDateTime(ruleUpdatedAt));
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.profile.xoo").setValue("SonarQube way").setResourceId(project.getId()), dbSession);

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
    List<QProfile> profiles = newArrayList(ref.qProfiles());
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).key()).isEqualTo("abcd");
    assertThat(profiles.get(0).name()).isEqualTo("SonarQube way");
    assertThat(profiles.get(0).language()).isEqualTo("xoo");
    assertThat(profiles.get(0).rulesUpdatedAt()).isEqualTo(ruleUpdatedAt);
  }

  @Test
  public void return_quality_profile_from_default_profile() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForProject(project));

    QualityProfileDto profileDto = QProfileTesting.newDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(DateUtils.formatDateTime(ruleUpdatedAt));
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.profile.xoo").setValue("SonarQube way"), dbSession);

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
    List<QProfile> profiles = newArrayList(ref.qProfiles());
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).key()).isEqualTo("abcd");
    assertThat(profiles.get(0).name()).isEqualTo("SonarQube way");
    assertThat(profiles.get(0).language()).isEqualTo("xoo");
    assertThat(profiles.get(0).rulesUpdatedAt()).isEqualTo(ruleUpdatedAt);
  }

  @Test
  public void return_quality_profile_from_given_profile_name() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForProject(project));

    QualityProfileDto profileDto = QProfileTesting.newDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(DateUtils.formatDateTime(ruleUpdatedAt));
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.profile.xoo").setValue("SonarQube way"), dbSession);

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()).setProfileName("SonarQube way"));
    List<QProfile> profiles = newArrayList(ref.qProfiles());
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).key()).isEqualTo("abcd");
    assertThat(profiles.get(0).name()).isEqualTo("SonarQube way");
    assertThat(profiles.get(0).language()).isEqualTo("xoo");
    assertThat(profiles.get(0).rulesUpdatedAt()).isEqualTo(ruleUpdatedAt);
  }

  @Test
  public void return_quality_profiles_even_when_project_does_not_exists() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    QualityProfileDto profileDto = QProfileTesting.newDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(DateUtils.formatDateTime(ruleUpdatedAt));
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.profile.xoo").setValue("SonarQube way"), dbSession);

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey("project"));
    List<QProfile> profiles = newArrayList(ref.qProfiles());
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).key()).isEqualTo("abcd");
    assertThat(profiles.get(0).name()).isEqualTo("SonarQube way");
    assertThat(profiles.get(0).language()).isEqualTo("xoo");
    assertThat(profiles.get(0).rulesUpdatedAt()).isEqualTo(ruleUpdatedAt);
  }


  @Test
  public void return_provisioned_project_profile() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    // No snapshot attached on the project -> provisioned project
    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);

    QualityProfileDto profileDto = QProfileTesting.newDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(DateUtils.formatDateTime(ruleUpdatedAt));
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.profile.xoo").setValue("SonarQube way").setResourceId(project.getId()), dbSession);

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
    List<QProfile> profiles = newArrayList(ref.qProfiles());
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).key()).isEqualTo("abcd");
    assertThat(profiles.get(0).name()).isEqualTo("SonarQube way");
    assertThat(profiles.get(0).language()).isEqualTo("xoo");
    assertThat(profiles.get(0).rulesUpdatedAt()).isEqualTo(ruleUpdatedAt);
  }

  @Test
  public void fail_when_no_quality_profile_for_a_language() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto().setKey("org.codehaus.sonar:sonar");
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    dbSession.commit();

    try {
      loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("No quality profile can been found on language 'xoo' for project 'org.codehaus.sonar:sonar'");
    }
  }

  @Test
  public void return_active_rules() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    tester.get(DbClient.class).snapshotDao().insert(dbSession, SnapshotTesting.createForProject(project));

    QualityProfileDto profileDto = QProfileTesting.newDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(DateUtils.formatDateTime(ruleUpdatedAt));
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.profile.xoo").setValue("SonarQube way"), dbSession);

    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    RuleDto rule = RuleTesting.newDto(ruleKey).setName("Avoid Cycle").setConfigKey("squid-1").setLanguage(ServerTester.Xoo.KEY);
    tester.get(DbClient.class).ruleDao().insert(dbSession, rule);
    tester.get(DbClient.class).ruleDao().addRuleParam(dbSession, rule, RuleParamDto.createFor(rule)
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));

    RuleActivation activation = new RuleActivation(ruleKey);
    activation.setSeverity(Severity.MINOR);
    activation.setParameter("max", "2");
    tester.get(RuleActivator.class).activate(dbSession, activation, profileDto.getKey());

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
    List<ActiveRule> activeRules = newArrayList(ref.activeRules());
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).repositoryKey()).isEqualTo("squid");
    assertThat(activeRules.get(0).ruleKey()).isEqualTo("AvoidCycle");
    assertThat(activeRules.get(0).name()).isEqualTo("Avoid Cycle");
    assertThat(activeRules.get(0).language()).isEqualTo("xoo");
    assertThat(activeRules.get(0).severity()).isEqualTo("MINOR");
    assertThat(activeRules.get(0).internalKey()).isEqualTo("squid-1");
    assertThat(activeRules.get(0).language()).isEqualTo("xoo");
    assertThat(activeRules.get(0).params()).isEqualTo(ImmutableMap.of("max", "2"));
  }

  @Test
  public void return_custom_rule() throws Exception {
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    ComponentDto project = ComponentTesting.newProjectDto();
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION).addComponentPermission(UserRole.USER, project.key(), project.key());
    tester.get(DbClient.class).componentDao().insert(dbSession, project);

    QualityProfileDto profileDto = QProfileTesting.newDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(
      DateUtils.formatDateTime(ruleUpdatedAt));
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.profile.xoo").setValue("SonarQube way"), dbSession);

    RuleKey ruleKey = RuleKey.of("squid", "ArchitecturalConstraint");
    RuleDto templateRule = RuleTesting.newTemplateRule(ruleKey).setName("Architectural Constraint").setLanguage(ServerTester.Xoo.KEY);
    tester.get(DbClient.class).ruleDao().insert(dbSession, templateRule);

    RuleDto customRule = RuleTesting.newCustomRule(templateRule);
    tester.get(DbClient.class).ruleDao().insert(dbSession, customRule);
    tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(customRule.getKey()).setSeverity(Severity.MINOR), profileDto.getKey());

    dbSession.commit();

    ProjectReferentials ref = loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
    List<ActiveRule> activeRules = newArrayList(ref.activeRules());
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).repositoryKey()).isEqualTo("squid");
    assertThat(activeRules.get(0).ruleKey()).startsWith("ArchitecturalConstraint_");
    assertThat(activeRules.get(0).templateRuleKey()).isEqualTo("ArchitecturalConstraint");
    assertThat(activeRules.get(0).language()).isEqualTo("xoo");
    assertThat(activeRules.get(0).severity()).isEqualTo("MINOR");
  }

  @Test
  public void fail_if_no_permission() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions();

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    dbSession.commit();

    try {
      loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class).hasMessage("You're not authorized to execute any SonarQube analysis. Please contact your SonarQube administrator.");
    }
  }

  @Test
  public void fail_when_not_preview_and_only_dry_run_permission() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.DRY_RUN_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    dbSession.commit();

    try {
      loader.load(ProjectReferentialsQuery.create().setModuleKey(project.key()).setPreview(false));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class).hasMessage(
        "You're only authorized to execute a local (dry run) SonarQube analysis without pushing the results to the SonarQube server. " +
          "Please contact your SonarQube administrator.");
    }
  }

  private void addDefaultProfile() {
    QualityProfileDto profileDto = QProfileTesting.newDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(DateUtils.formatDateTime(new Date()));
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);
    tester.get(DbClient.class).propertiesDao().setProperty(new PropertyDto().setKey("sonar.profile.xoo").setValue("SonarQube way"), dbSession);
  }

}
