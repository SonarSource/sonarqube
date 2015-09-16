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
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.batch.protocol.input.QProfile;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.source.FileSourceDao;
import org.sonar.db.source.FileSourceDto;
import org.sonar.db.source.FileSourceDto.Type;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.qualityprofile.QProfileTesting.newQProfileDto;

public class ProjectDataLoaderMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().addXoo();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbSession dbSession;

  ProjectDataLoader loader;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbSession = tester.get(DbClient.class).openSession(false);
    loader = tester.get(ProjectDataLoader.class);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void return_project_settings() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId())
    );
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId())
    );
    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    Map<String, String> projectSettings = ref.settings(project.key());
    assertThat(projectSettings).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"
    ));
  }

  @Test
  public void not_returned_secured_settings_with_only_preview_permission() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION).addProjectUuidPermissions(UserRole.USER, project.uuid());
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId())
    );
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId())
    );
    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()).setPreview(true));
    Map<String, String> projectSettings = ref.settings(project.key());
    assertThat(projectSettings).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR"
    ));
  }

  @Test
  public void return_project_with_module_settings() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);

    // Module properties
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(module.getId()));

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
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
  public void return_project_with_module_settings_inherited_from_project() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);

    // No property on module -> should have the same as project

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
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
  public void return_project_with_module_with_sub_module() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);

    // Module properties
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(module.getId()));

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    tester.get(DbClient.class).componentDao().insert(dbSession, subModule);

    // Sub module properties
    tester.get(DbClient.class).propertiesDao().insertProperty(
      dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER-DAO").setResourceId(subModule.getId()));

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
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
  public void return_project_with_two_modules() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));

    ComponentDto module1 = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module1);

    // Module 1 properties
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module1.getId()));
    // This property should not be found on the other module
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(module1.getId()));

    ComponentDto module2 = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module2);

    // Module 2 property
    tester.get(DbClient.class).propertiesDao()
      .insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-APPLICATION").setResourceId(module2.getId()));

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
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
  public void return_provisioned_project_settings() {
    // No snapshot attached on the project -> provisioned project
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    assertThat(ref.settings(project.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john"
    ));
  }

  @Test
  public void return_sub_module_settings() {

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();
    // No project properties

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);
    // No module properties

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, subModule);

    // Sub module properties
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(subModule.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(subModule.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(subModule.getId()));

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
  }

  @Test
  public void return_sub_module_settings_including_settings_from_parent_modules() {
    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project property
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);

    // Module property
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(module.getId()));

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, subModule);

    // Sub module properties
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(subModule.getId()));

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
  }

  @Test
  public void return_sub_module_settings_only_inherited_from_project() {
    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR").setResourceId(project.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);
    // No module property

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, subModule);
    // No sub module property

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
  }

  @Test
  public void return_sub_module_settings_inherited_from_project_and_module() {
    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // Project properties
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.login.secured").setValue("john").setResourceId(project.getId()));
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java").setResourceId(project.getId()));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);

    // Module property
    tester.get(DbClient.class).propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR-SERVER").setResourceId(module.getId()));

    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, subModule);
    // No sub module property

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(subModule.key()));
    assertThat(ref.settings(project.key())).isEmpty();
    assertThat(ref.settings(module.key())).isEmpty();
    assertThat(ref.settings(subModule.key())).isEqualTo(ImmutableMap.of(
      "sonar.jira.project.key", "SONAR-SERVER",
      "sonar.jira.login.secured", "john",
      "sonar.coverage.exclusions", "**/*.java"
    ));
  }

  @Test
  public void return_quality_profile_from_project_profile() {
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);

    QualityProfileDto profileDto = newQProfileDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(
      formatDateTime(ruleUpdatedAt));
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);
    tester.get(DbClient.class).qualityProfileDao().insertProjectProfileAssociation(project.uuid(), profileDto.getKee(), dbSession);

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    List<QProfile> profiles = newArrayList(ref.qProfiles());
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).key()).isEqualTo("abcd");
    assertThat(profiles.get(0).name()).isEqualTo("SonarQube way");
    assertThat(profiles.get(0).language()).isEqualTo("xoo");
    assertThat(profiles.get(0).rulesUpdatedAt()).isEqualTo(ruleUpdatedAt);
  }

  @Test
  public void return_quality_profile_from_default_profile() {
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);

    QualityProfileDto profileDto = newQProfileDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(
      formatDateTime(ruleUpdatedAt)).setDefault(true);
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    List<QProfile> profiles = newArrayList(ref.qProfiles());
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).key()).isEqualTo("abcd");
    assertThat(profiles.get(0).name()).isEqualTo("SonarQube way");
    assertThat(profiles.get(0).language()).isEqualTo("xoo");
    assertThat(profiles.get(0).rulesUpdatedAt()).isEqualTo(ruleUpdatedAt);
  }

  @Test
  public void return_quality_profile_from_given_profile_name() {
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);

    QualityProfileDto profileDto = newQProfileDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(
      formatDateTime(ruleUpdatedAt)).setDefault(true);
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()).setProfileName("SonarQube way"));
    List<QProfile> profiles = newArrayList(ref.qProfiles());
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).key()).isEqualTo("abcd");
    assertThat(profiles.get(0).name()).isEqualTo("SonarQube way");
    assertThat(profiles.get(0).language()).isEqualTo("xoo");
    assertThat(profiles.get(0).rulesUpdatedAt()).isEqualTo(ruleUpdatedAt);
  }

  @Test
  public void return_quality_profiles_even_when_project_does_not_exists() {
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    QualityProfileDto profileDto = newQProfileDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(
      formatDateTime(ruleUpdatedAt)).setDefault(true);
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey("project"));
    List<QProfile> profiles = newArrayList(ref.qProfiles());
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).key()).isEqualTo("abcd");
    assertThat(profiles.get(0).name()).isEqualTo("SonarQube way");
    assertThat(profiles.get(0).language()).isEqualTo("xoo");
    assertThat(profiles.get(0).rulesUpdatedAt()).isEqualTo(ruleUpdatedAt);
  }

  @Test
  public void return_provisioned_project_profile() {
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    // No snapshot attached on the project -> provisioned project
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);

    QualityProfileDto profileDto = newQProfileDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(
      formatDateTime(ruleUpdatedAt));
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);
    tester.get(DbClient.class).qualityProfileDao().insertProjectProfileAssociation(project.uuid(), profileDto.getKee(), dbSession);

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    List<QProfile> profiles = newArrayList(ref.qProfiles());
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).key()).isEqualTo("abcd");
    assertThat(profiles.get(0).name()).isEqualTo("SonarQube way");
    assertThat(profiles.get(0).language()).isEqualTo("xoo");
    assertThat(profiles.get(0).rulesUpdatedAt()).isEqualTo(ruleUpdatedAt);
  }

  @Test
  public void fail_when_no_quality_profile_for_a_language() {
    ComponentDto project = ComponentTesting.newProjectDto().setKey("org.codehaus.sonar:sonar");
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    dbSession.commit();

    try {
      loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("No quality profile can been found on language 'xoo' for project 'org.codehaus.sonar:sonar'");
    }
  }

  @Test
  public void return_active_rules() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);

    QualityProfileDto profileDto = newQProfileDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(
      formatDateTime(DateUtils.parseDateTime("2014-01-14T13:00:00+0100"))).setDefault(true);
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);

    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    RuleDto rule = RuleTesting.newDto(ruleKey).setName("Avoid Cycle").setConfigKey("squid-1").setLanguage(ServerTester.Xoo.KEY);
    tester.get(DbClient.class).deprecatedRuleDao().insert(dbSession, rule);
    tester.get(DbClient.class).deprecatedRuleDao().insertRuleParam(dbSession, rule, RuleParamDto.createFor(rule)
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));

    RuleActivation activation = new RuleActivation(ruleKey);
    activation.setSeverity(Severity.MINOR);
    activation.setParameter("max", "2");
    tester.get(RuleActivator.class).activate(dbSession, activation, profileDto.getKey());

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    List<ActiveRule> activeRules = newArrayList(ref.activeRules());
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).repositoryKey()).isEqualTo("squid");
    assertThat(activeRules.get(0).ruleKey()).isEqualTo("AvoidCycle");
    assertThat(activeRules.get(0).name()).isEqualTo("Avoid Cycle");
    assertThat(activeRules.get(0).language()).isEqualTo("xoo");
    assertThat(activeRules.get(0).severity()).isEqualTo("MINOR");
    assertThat(activeRules.get(0).internalKey()).isEqualTo("squid-1");
    assertThat(activeRules.get(0).params()).isEqualTo(ImmutableMap.of("max", "2"));
  }

  @Test
  public void return_only_active_rules_from_project_profile() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);

    RuleKey ruleKey1 = RuleKey.of("squid", "AvoidCycle");
    RuleKey ruleKey2 = RuleKey.of("squid", "AvoidNPE");
    tester.get(DbClient.class).deprecatedRuleDao().insert(dbSession,
      RuleTesting.newDto(ruleKey1).setName("Avoid Cycle").setLanguage(ServerTester.Xoo.KEY),
      RuleTesting.newDto(ruleKey2).setName("Avoid NPE").setLanguage(ServerTester.Xoo.KEY)
    );

    QualityProfileDto profileDto1 = newQProfileDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd");
    QualityProfileDto profileDto2 = newQProfileDto(QProfileName.createFor(ServerTester.Xoo.KEY, "Another profile"), "efgh");
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto1, profileDto2);

    // The first profile is the profile used but the project
    tester.get(DbClient.class).qualityProfileDao().insertProjectProfileAssociation(project.uuid(), profileDto1.getKee(), dbSession);

    tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(ruleKey1).setSeverity(Severity.MINOR), profileDto1.getKey());

    // Active rule from 2nd profile
    tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(ruleKey1).setSeverity(Severity.BLOCKER), profileDto2.getKey());
    tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(ruleKey2).setSeverity(Severity.BLOCKER), profileDto2.getKey());

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    List<ActiveRule> activeRules = newArrayList(ref.activeRules());
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).repositoryKey()).isEqualTo("squid");
    assertThat(activeRules.get(0).ruleKey()).isEqualTo("AvoidCycle");
    assertThat(activeRules.get(0).name()).isEqualTo("Avoid Cycle");
    assertThat(activeRules.get(0).language()).isEqualTo("xoo");
    assertThat(activeRules.get(0).severity()).isEqualTo("MINOR");
  }

  @Test
  public void return_more_than_10_active_rules() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);

    QualityProfileDto profileDto = newQProfileDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd")
      .setRulesUpdatedAt(formatDateTime(DateUtils.parseDateTime("2014-01-14T13:00:00+0100"))).setDefault(true);
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);

    for (int i = 0; i < 20; i++) {
      RuleKey ruleKey = RuleKey.of("squid", "Rule" + i);
      tester.get(DbClient.class).deprecatedRuleDao().insert(dbSession, RuleTesting.newDto(ruleKey).setName("Rule" + i).setLanguage(ServerTester.Xoo.KEY));
      tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(ruleKey).setSeverity(Severity.MINOR), profileDto.getKey());
    }

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    assertThat(ref.activeRules()).hasSize(20);
  }

  @Test
  public void return_custom_rule() {
    Date ruleUpdatedAt = DateUtils.parseDateTime("2014-01-14T13:00:00+0100");

    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);

    QualityProfileDto profileDto = newQProfileDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(
      formatDateTime(ruleUpdatedAt)).setDefault(true);
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);

    RuleKey ruleKey = RuleKey.of("squid", "ArchitecturalConstraint");
    RuleDto templateRule = RuleTesting.newTemplateRule(ruleKey).setName("Architectural Constraint").setLanguage(ServerTester.Xoo.KEY);
    tester.get(DbClient.class).deprecatedRuleDao().insert(dbSession, templateRule);

    RuleDto customRule = RuleTesting.newCustomRule(templateRule);
    tester.get(DbClient.class).deprecatedRuleDao().insert(dbSession, customRule);
    tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(customRule.getKey()).setSeverity(Severity.MINOR), profileDto.getKey());

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    List<ActiveRule> activeRules = newArrayList(ref.activeRules());
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).repositoryKey()).isEqualTo("squid");
    assertThat(activeRules.get(0).ruleKey()).startsWith("ArchitecturalConstraint_");
    assertThat(activeRules.get(0).templateRuleKey()).isEqualTo("ArchitecturalConstraint");
    assertThat(activeRules.get(0).language()).isEqualTo("xoo");
    assertThat(activeRules.get(0).severity()).isEqualTo("MINOR");
  }

  @Test
  public void return_manual_rules() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    RuleDto rule = RuleTesting.newManualRule("manualRuleKey").setName("Name manualRuleKey");
    tester.get(DbClient.class).deprecatedRuleDao().insert(dbSession, rule);

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    List<ActiveRule> activeRules = newArrayList(ref.activeRules());
    assertThat(activeRules).extracting("repositoryKey").containsOnly(RuleKey.MANUAL_REPOSITORY_KEY);
    assertThat(activeRules).extracting("ruleKey").containsOnly("manualRuleKey");
    assertThat(activeRules).extracting("name").containsOnly("Name manualRuleKey");
    assertThat(activeRules).extracting("language").containsNull();
    assertThat(activeRules).extracting("severity").containsNull();
  }

  @Test
  public void fail_if_no_permission() {
    userSessionRule.login("john").setGlobalPermissions();

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    dbSession.commit();

    try {
      loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class).hasMessage("You're not authorized to execute any SonarQube analysis. Please contact your SonarQube administrator.");
    }
  }

  @Test
  public void fail_when_not_preview_and_only_dry_run_permission() {
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION);

    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    dbSession.commit();

    try {
      loader.load(ProjectDataQuery.create().setModuleKey(project.key()).setPreview(false));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class).hasMessage(
        "You're only authorized to execute a local (preview) SonarQube analysis without pushing the results to the SonarQube server. " +
          "Please contact your SonarQube administrator.");
    }
  }

  @Test
  public void return_file_data_from_single_project() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    ComponentDto file = ComponentTesting.newFileDto(project, "file");
    tester.get(DbClient.class).componentDao().insert(dbSession, file);
    tester.get(FileSourceDao.class).insert(newFileSourceDto(file).setSrcHash("123456"));

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    assertThat(ref.fileDataByPath(project.key())).hasSize(1);
    FileData fileData = ref.fileData(project.key(), file.path());
    assertThat(fileData.hash()).isEqualTo("123456");
  }

  @Test
  public void return_file_data_from_multi_modules() {
    ComponentDto project = ComponentTesting.newProjectDto();
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // File on project
    ComponentDto projectFile = ComponentTesting.newFileDto(project, "projectFile");
    tester.get(DbClient.class).componentDao().insert(dbSession, projectFile);
    tester.get(FileSourceDao.class).insert(newFileSourceDto(projectFile).setSrcHash("123456"));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);

    // File on module
    ComponentDto moduleFile = ComponentTesting.newFileDto(module, "moduleFile");
    tester.get(DbClient.class).componentDao().insert(dbSession, moduleFile);
    tester.get(FileSourceDao.class).insert(newFileSourceDto(moduleFile).setSrcHash("789456"));

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(project.key()));
    assertThat(ref.fileData(project.key(), projectFile.path()).hash()).isEqualTo("123456");
    assertThat(ref.fileData(module.key(), moduleFile.path()).hash()).isEqualTo("789456");
  }

  @Test
  public void return_file_data_from_module() {
    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(DbClient.class).componentDao().insert(dbSession, project);
    addDefaultProfile();

    // File on project
    ComponentDto projectFile = ComponentTesting.newFileDto(project, "projectFile");
    tester.get(DbClient.class).componentDao().insert(dbSession, projectFile);
    tester.get(FileSourceDao.class).insert(newFileSourceDto(projectFile).setSrcHash("123456"));

    ComponentDto module = ComponentTesting.newModuleDto(project);
    userSessionRule.login("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    tester.get(DbClient.class).componentDao().insert(dbSession, module);

    // File on module
    ComponentDto moduleFile = ComponentTesting.newFileDto(module, "moduleFile");
    tester.get(DbClient.class).componentDao().insert(dbSession, moduleFile);
    tester.get(FileSourceDao.class).insert(newFileSourceDto(moduleFile).setSrcHash("789456"));

    dbSession.commit();

    ProjectRepositories ref = loader.load(ProjectDataQuery.create().setModuleKey(module.key()));
    assertThat(ref.fileData(module.key(), moduleFile.path()).hash()).isEqualTo("789456");
    assertThat(ref.fileData(project.key(), projectFile.path())).isNull();
  }

  private void addDefaultProfile() {
    QualityProfileDto profileDto = newQProfileDto(QProfileName.createFor(ServerTester.Xoo.KEY, "SonarQube way"), "abcd").setRulesUpdatedAt(
      formatDateTime(new Date())).setDefault(true);
    tester.get(DbClient.class).qualityProfileDao().insert(dbSession, profileDto);
  }

  private FileSourceDto newFileSourceDto(ComponentDto file) {
    return new FileSourceDto()
      .setFileUuid(file.uuid())
      .setProjectUuid(file.projectUuid())
        //.setSourceData(",,,,,,,,,,,,,,,unchanged&#13;&#10;,,,,,,,,,,,,,,,content&#13;&#10;")
      .setDataHash("0263047cd758c68c27683625f072f010")
      .setLineHashes("8d7b3d6b83c0a517eac07e1aac94b773")
      .setCreatedAt(System.currentTimeMillis())
      .setUpdatedAt(System.currentTimeMillis())
      .setDataType(Type.SOURCE)
      .setSrcHash("123456");
  }

}
