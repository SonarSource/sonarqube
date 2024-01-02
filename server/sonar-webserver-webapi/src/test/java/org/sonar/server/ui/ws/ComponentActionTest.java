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
package org.sonar.server.ui.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.Page.Qualifier;
import org.sonar.api.web.page.PageDefinition;
import org.sonar.core.component.DefaultResourceTypes;
import org.sonar.core.extension.CoreExtensionRepository;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.updatecenter.common.Version;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES_KEY;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.page.Page.Scope.COMPONENT;
import static org.sonar.db.component.ComponentDbTester.toProjectDto;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.ui.ws.ComponentAction.PARAM_COMPONENT;
import static org.sonar.test.JsonAssert.assertJson;

public class ComponentActionTest {

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final ComponentDbTester componentDbTester = db.components();
  private final PropertyDbTester propertyDbTester = new PropertyDbTester(db);
  private final ResourceTypes resourceTypes = mock(ResourceTypes.class);
  private final Configuration config = mock(Configuration.class);

  private WsActionTester ws;

  @Before
  public void setup() {
    ResourceType resourceType = mock(ResourceType.class);
    when(resourceType.getBooleanProperty(any())).thenReturn(true);
    when(resourceTypes.get(any())).thenReturn(resourceType);
  }

  @Test
  public void return_info_if_user_has_browse_permission_on_project() {
    ComponentDto project = insertProject();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    init();

    verifySuccess(project.getKey());
  }

  @Test
  public void return_info_if_user_has_administration_permission_on_project() {
    ComponentDto project = insertProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    init();

    verifySuccess(project.getKey());
  }

  @Test
  public void return_info_if_user_is_system_administrator() {
    ComponentDto project = insertProject();
    userSession.logIn().setSystemAdministrator();
    init();

    verifySuccess(project.getKey());
  }

  @Test
  public void return_component_info_when_anonymous_no_snapshot() {
    ComponentDto project = insertProject();
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getKey(), "return_component_info_when_anonymous_no_snapshot.json");
  }

  @Test
  public void return_component_info_with_favourite() {
    ComponentDto project = insertProject();
    UserDto user = db.users().insertUser("obiwan");
    propertyDbTester.insertProperty(new PropertyDto().setKey("favourite").setComponentUuid(project.uuid()).setUserUuid(user.getUuid()),
      project.getKey(), project.name(), project.qualifier(), user.getLogin());
    userSession.logIn(user).addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getKey(), "return_component_info_with_favourite.json");
  }

  @Test
  public void return_favourite_for_branch() {
    ComponentDto project = insertProject();
    String branchName = "feature1";
    componentDbTester.insertProjectBranch(project, b -> b.setKey(branchName).setUuid("xyz"));
    UserDto user = db.users().insertUser("obiwan");
    propertyDbTester.insertProperty(new PropertyDto().setKey("favourite").setComponentUuid(project.uuid()).setUserUuid(user.getUuid()),
      project.getKey(), project.name(), project.qualifier(), user.getLogin());
    userSession.logIn(user).addProjectPermission(UserRole.USER, project);
    init();

    String json = ws.newRequest()
      .setParam("component", project.getKey())
      .setParam("branch", branchName)
      .execute()
      .getInput();

    assertJson(json).isSimilarTo("{\n" +
      "  \"key\": \"polop\",\n" +
      "  \"isFavorite\": true,\n" +
      "  \"id\": \"xyz\",\n" +
      "  \"branch\": \"feature1\"," +
      "  \"name\": \"Polop\",\n" +
      "  \"description\": \"test project\"\n" +
      "}\n");
  }

  @Test
  public void return_favourite_for_subportfolio() {
    db.qualityGates().createDefaultQualityGate();
    ComponentDto portfolio = componentDbTester.insertPrivatePortfolio();
    ComponentDto subportfolio = componentDbTester.insertComponent(newSubPortfolio(portfolio));
    UserDto user = db.users().insertUser("obiwan");

    // set favourite for sub portfolio
    propertyDbTester.insertProperty(new PropertyDto().setKey("favourite").setComponentUuid(subportfolio.uuid()).setUserUuid(user.getUuid()),
      subportfolio.getKey(), subportfolio.name(), subportfolio.qualifier(), user.getLogin());

    userSession.logIn(user).addProjectPermission(UserRole.USER, subportfolio);
    init();

    String json = ws.newRequest()
      .setParam("component", subportfolio.getKey())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo("{" +
      "  \"key\": \"" + subportfolio.getKey() + "\"," +
      "  \"isFavorite\": true," +
      "  \"id\": \"" + subportfolio.uuid() + "\"," +
      "  \"name\": \"" + subportfolio.name() + "\"" +
      "}");
  }

  @Test
  public void return_favourite_for_portfolio() {
    db.qualityGates().createDefaultQualityGate();
    ComponentDto portfolio = componentDbTester.insertPrivatePortfolio();
    ComponentDto subportfolio = componentDbTester.insertComponent(newSubPortfolio(portfolio));
    UserDto user = db.users().insertUser("obiwan");

    // set favourite for sub portfolio
    propertyDbTester.insertProperty(new PropertyDto().setKey("favourite").setComponentUuid(portfolio.uuid()).setUserUuid(user.getUuid()),
      subportfolio.getKey(), portfolio.name(), portfolio.qualifier(), user.getLogin());

    userSession.logIn(user).addProjectPermission(UserRole.USER, portfolio);
    init();

    String json = ws.newRequest()
      .setParam("component", portfolio.getKey())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo("{" +
      "  \"key\": \"" + portfolio.getKey() + "\"," +
      "  \"isFavorite\": true," +
      "  \"id\": \"" + portfolio.uuid() + "\"," +
      "  \"name\": \"" + portfolio.name() + "\"" +
      "}");
  }

  @Test
  public void return_canBrowseAllChildProjects_when_component_is_an_application() {
    db.qualityGates().createDefaultQualityGate();
    ComponentDto application1 = db.components().insertPrivateApplication();
    ComponentDto project11 = db.components().insertPrivateProject();
    ComponentDto project12 = db.components().insertPrivateProject();
    userSession.registerApplication(
      toProjectDto(application1, 1L),
      toProjectDto(project11, 1L),
      toProjectDto(project12, 1L));
    userSession.addProjectPermission(UserRole.USER, application1, project11, project12);

    ComponentDto application2 = db.components().insertPrivateApplication();
    ComponentDto project21 = db.components().insertPrivateProject();
    ComponentDto project22 = db.components().insertPrivateProject();
    userSession.registerApplication(
      toProjectDto(application2, 1L),
      toProjectDto(project21, 1L),
      toProjectDto(project22, 1L));
    userSession.addProjectPermission(UserRole.USER, application2, project21);

    init();

    // access to all projects (project11, project12)
    String json = execute(application1.getKey());
    assertJson(json).isSimilarTo("{" +
      "\"canBrowseAllChildProjects\":true" +
      "}");

    // access to some projects (project11)
    json = execute(application2.getKey());
    assertJson(json).isSimilarTo("{" +
      "\"canBrowseAllChildProjects\":false" +
      "}");
  }

  @Test
  public void return_component_info_when_snapshot() {
    ComponentDto project = insertProject();
    db.components().insertSnapshot(project, snapshot -> snapshot
      .setCreatedAt(parseDateTime("2015-04-22T11:44:00+0200").getTime())
      .setProjectVersion("3.14"));
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getKey(), "return_component_info_when_snapshot.json");
  }

  @Test
  public void return_component_info_when_file_on_master() {
    db.qualityGates().createDefaultQualityGate();
    ComponentDto main = componentDbTester.insertPrivateProject(p -> p.setName("Sample").setKey("sample"));
    userSession.addProjectPermission(UserRole.USER, main);
    init();

    ComponentDto dirDto = componentDbTester.insertComponent(newDirectory(main, "src"));

    ComponentDto fileDto = componentDbTester.insertComponent(newFileDto(main, dirDto)
      .setUuid("abcd")
      .setName("Main.xoo")
      .setKey("sample:src/Main.xoo"));

    executeAndVerify(fileDto.getKey(), "return_component_info_when_file_on_master.json");
  }

  @Test
  public void return_component_info_when_file_on_branch() {
    db.qualityGates().createDefaultQualityGate();
    ComponentDto project = componentDbTester.insertPrivateProject(p -> p.setName("Sample").setKey("sample"));
    String branchName = "feature1";
    ComponentDto branch = componentDbTester.insertProjectBranch(project, b -> b.setKey(branchName));
    userSession.addProjectPermission(UserRole.USER, project);
    init();
    ComponentDto dirDto = componentDbTester.insertComponent(newDirectory(branch, "src"));
    ComponentDto fileDto = componentDbTester.insertComponent(newFileDto(branch, dirDto)
      .setUuid("abcd")
      .setName("Main.xoo"));

    String json = ws.newRequest()
      .setParam("component", fileDto.getKey())
      .setParam("branch", branchName)
      .execute()
      .getInput();

    assertJson(json).isSimilarTo("{\n" +
      "  \"key\": \"" + fileDto.getKey() + "\",\n" +
      "  \"branch\": \"feature1\",\n" +
      "  \"name\": \"Main.xoo\",\n" +
      "  \"breadcrumbs\": [\n" +
      "    {\n" +
      "      \"key\": \"sample\",\n" +
      "      \"name\": \"Sample\",\n" +
      "      \"qualifier\": \"TRK\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"key\": \"sample:src\",\n" +
      "      \"name\": \"src\",\n" +
      "      \"qualifier\": \"DIR\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"key\": \"" + fileDto.getKey() + "\",\n" +
      "      \"name\": \"Main.xoo\",\n" +
      "      \"qualifier\": \"FIL\"\n" +
      "    }\n" +
      "  ]\n" +
      "}\n");
  }

  @Test
  public void return_quality_profiles_and_supports_deleted_ones() {
    ComponentDto project = insertProject();
    QProfileDto qp1 = db.qualityProfiles().insert(t -> t.setKee("qp1").setName("Sonar Way Java").setLanguage("java"));
    QProfileDto qp2 = db.qualityProfiles().insert(t -> t.setKee("qp2").setName("Sonar Way Xoo").setLanguage("xoo"));
    addQualityProfiles(project,
      new QualityProfile(qp1.getKee(), qp1.getName(), qp1.getLanguage(), new Date()),
      new QualityProfile(qp2.getKee(), qp2.getName(), qp2.getLanguage(), new Date()));
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getKey(), "return_quality_profiles.json");

    db.getDbClient().qualityProfileDao().deleteOrgQProfilesByUuids(db.getSession(), ImmutableSet.of(qp1.getKee(), qp2.getKee()));
    db.commit();

    executeAndVerify(project.getKey(), "return_deleted_quality_profiles.json");
  }

  @Test
  public void return_empty_quality_profiles_when_no_measure() {
    ComponentDto project = insertProject();
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getKey(), "return_empty_quality_profiles_when_no_measure.json");
  }

  @Test
  public void return_quality_gate_defined_on_project() {
    db.qualityGates().createDefaultQualityGate();
    ProjectDto project = db.components().insertPrivateProjectDto();
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way"));
    db.qualityGates().associateProjectToQualityGate(project, qualityGateDto);
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getKey(), "return_quality_gate.json");
  }

  @Test
  public void quality_gate_for_a_branch() {
    db.qualityGates().createDefaultQualityGate();
    ProjectDto project = db.components().insertPrivateProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way"));
    db.qualityGates().associateProjectToQualityGate(project, qualityGateDto);
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    String json = ws.newRequest()
      .setParam("component", project.getKey())
      .setParam("branch", branch.getKey())
      .execute()
      .getInput();

    verify(json, "return_quality_gate.json");
  }

  @Test
  public void return_default_quality_gate() {
    ComponentDto project = db.components().insertPrivateProject();
    db.qualityGates().createDefaultQualityGate(qg -> qg.setName("Sonar way"));
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getKey(), "return_default_quality_gate.json");
  }

  @Test
  public void return_extensions() {
    ComponentDto project = insertProject();
    userSession.anonymous().addProjectPermission(UserRole.USER, project);
    init(createPages());

    executeAndVerify(project.getKey(), "return_extensions.json");
  }

  @Test
  public void return_extensions_for_application() {
    db.qualityGates().createDefaultQualityGate();
    ProjectDto project = db.components().insertPrivateProjectDto();
    Page page = Page.builder("my_plugin/app_page")
      .setName("App Page")
      .setScope(COMPONENT)
      .setComponentQualifiers(Qualifier.VIEW, Qualifier.APP)
      .build();
    ComponentDto application = componentDbTester.insertPublicApplication();
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way"));
    db.qualityGates().associateProjectToQualityGate(project, qualityGateDto);
    userSession.registerComponents(application);
    init(page);

    String result = ws.newRequest()
      .setParam(PARAM_COMPONENT, application.getKey())
      .execute().getInput();

    assertThat(result).contains("my_plugin/app_page");
  }

  @Test
  public void return_extensions_for_admin() {
    ComponentDto project = insertProject();
    userSession.anonymous()
      .addProjectPermission(UserRole.USER, project)
      .addProjectPermission(UserRole.ADMIN, project);
    init(createPages());

    executeAndVerify(project.getKey(), "return_extensions_for_admin.json");
  }

  @Test
  public void return_configuration_for_admin() {
    ComponentDto project = insertProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user)
      .addProjectPermission(UserRole.USER, project)
      .addProjectPermission(UserRole.ADMIN, project);
    Page page1 = Page.builder("my_plugin/first_page")
      .setName("First Page")
      .setAdmin(true)
      .setScope(COMPONENT)
      .setComponentQualifiers(Qualifier.PROJECT)
      .build();
    Page page2 = Page.builder("my_plugin/second_page")
      .setName("Second Page")
      .setAdmin(true)
      .setScope(COMPONENT)
      .setComponentQualifiers(Qualifier.PROJECT)
      .build();
    init(page1, page2);

    executeAndVerify(project.getKey(), "return_configuration_for_admin.json");
  }

  @Test
  public void return_configuration_with_all_properties() {
    ComponentDto project = insertProject();
    userSession.anonymous()
      .addProjectPermission(UserRole.USER, project)
      .addProjectPermission(UserRole.ADMIN, project);
    ResourceType projectResourceType = ResourceType.builder(project.qualifier())
      .setProperty("comparable", true)
      .setProperty("configurable", true)
      .setProperty("hasRolePolicy", true)
      .setProperty("modifiable_history", true)
      .setProperty("updatable_key", true)
      .setProperty("deletable", true)
      .build();
    when(resourceTypes.get(project.qualifier()))
      .thenReturn(projectResourceType);
    init();

    executeAndVerify(project.getKey(), "return_configuration_with_all_properties.json");
  }

  @Test
  public void return_configuration_for_quality_profile_admin() {
    ComponentDto project = insertProject();
    userSession.logIn()
      .addProjectPermission(UserRole.USER, project)
      .addPermission(ADMINISTER_QUALITY_PROFILES);
    init();

    executeAndVerify(project.getKey(), "return_configuration_for_quality_profile_admin.json");
  }

  @Test
  public void return_configuration_for_quality_gate_admin() {
    ComponentDto project = insertProject();
    userSession.logIn()
      .addProjectPermission(UserRole.USER, project)
      .addPermission(ADMINISTER_QUALITY_GATES);
    init();

    executeAndVerify(project.getKey(), "return_configuration_for_quality_gate_admin.json");
  }

  @Test
  public void return_configuration_for_private_projects_for_user_with_project_administer_permission_when_permission_management_is_enabled_for_project_admins() {
    ComponentDto project = insertProject();
    UserSessionRule userSessionRule = userSession.logIn();
    init();
    userSessionRule.addProjectPermission(UserRole.USER, project);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    String json = execute(project.getKey());

    assertJson(json).isSimilarTo("{\n" +
      "  \"configuration\": {\n" +
      "    \"showSettings\": true,\n" +
      "    \"showQualityProfiles\": true,\n" +
      "    \"showQualityGates\": true,\n" +
      "    \"showLinks\": true,\n" +
      "    \"showPermissions\": true,\n" +
      "    \"showHistory\": true,\n" +
      "    \"showUpdateKey\": true,\n" +
      "    \"showBackgroundTasks\": true,\n" +
      "    \"canApplyPermissionTemplate\": false,\n" +
      "    \"canBrowseProject\": true,\n" +
      "    \"canUpdateProjectVisibilityToPrivate\": true\n" +
      "  }\n" +
      "}");
  }

  @Test
  public void return_configuration_for_private_projects_for_user_with_project_administer_permission_when_permission_management_is_disabled_for_project_admins() {
    when(config.getBoolean(CoreProperties.CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY)).thenReturn(of(false));
    ComponentDto project = insertProject();
    UserSessionRule userSessionRule = userSession.logIn();
    init();
    userSessionRule.addProjectPermission(UserRole.USER, project);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    String json = execute(project.getKey());

    assertJson(json).isSimilarTo("{\n" +
      "  \"configuration\": {\n" +
      "    \"showSettings\": true,\n" +
      "    \"showQualityProfiles\": true,\n" +
      "    \"showQualityGates\": true,\n" +
      "    \"showLinks\": true,\n" +
      "    \"showPermissions\": false,\n" +
      "    \"showHistory\": true,\n" +
      "    \"showUpdateKey\": true,\n" +
      "    \"showBackgroundTasks\": true,\n" +
      "    \"canApplyPermissionTemplate\": false,\n" +
      "    \"canBrowseProject\": true,\n" +
      "    \"canUpdateProjectVisibilityToPrivate\": true\n" +
      "  }\n" +
      "}");
  }

  @Test
  public void do_not_return_configuration_for_private_projects_for_user_with_view_permission_only() {
    ComponentDto project = insertProject();
    UserSessionRule userSessionRule = userSession.logIn();
    init();
    userSessionRule.addProjectPermission(UserRole.USER, project);

    String json = execute(project.getKey());

    assertThat(json).doesNotContain("\"configuration\"");
  }

  @Test
  public void return_bread_crumbs_on_several_levels() {
    ComponentDto project = insertProject();
    ComponentDto module = componentDbTester.insertComponent(newModuleDto("bcde", project).setKey("palap").setName("Palap"));
    ComponentDto directory = componentDbTester.insertComponent(newDirectory(module, "src/main/xoo"));
    ComponentDto file = componentDbTester.insertComponent(newFileDto(directory, directory, "cdef").setName("Source.xoo")
      .setKey("palap:src/main/xoo/Source.xoo")
      .setPath(directory.path()));
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(file.getKey(), "return_bread_crumbs_on_several_levels.json");
  }

  @Test
  public void project_administrator_is_allowed_to_get_information() {
    ComponentDto project = insertProject();
    userSession.addProjectPermission(UserRole.ADMIN, project);
    init(createPages());

    execute(project.getKey());
  }

  @Test
  public void should_return_private_flag_for_project() {
    db.qualityGates().createDefaultQualityGate();
    ComponentDto project = db.components().insertPrivateProject();
    init();

    userSession.logIn()
      .addProjectPermission(UserRole.ADMIN, project)
      .addPermission(GlobalPermission.ADMINISTER);
    assertJson(execute(project.getKey())).isSimilarTo("{\"visibility\": \"private\"}");
  }

  @Test
  public void should_return_public_flag_for_project() {
    db.qualityGates().createDefaultQualityGate();
    ComponentDto project = db.components().insertPublicProject();
    init();

    userSession.logIn()
      .addProjectPermission(UserRole.ADMIN, project)
      .addPermission(GlobalPermission.ADMINISTER);
    assertJson(execute(project.getKey())).isSimilarTo("{\"visibility\": \"public\"}");
  }

  @Test
  public void canApplyPermissionTemplate_is_true_if_logged_in_as_administrator() {
    db.qualityGates().createDefaultQualityGate();
    ComponentDto project = db.components().insertPrivateProject();
    init(createPages());

    userSession.logIn()
      .addProjectPermission(UserRole.ADMIN, project)
      .addPermission(GlobalPermission.ADMINISTER);
    assertJson(execute(project.getKey())).isSimilarTo("{\"configuration\": {\"canApplyPermissionTemplate\": true}}");

    userSession.logIn()
      .addProjectPermission(UserRole.ADMIN, project);

    assertJson(execute(project.getKey())).isSimilarTo("{\"configuration\": {\"canApplyPermissionTemplate\": false}}");
  }

  @Test
  public void canUpdateProjectVisibilityToPrivate_is_true_if_logged_in_as_project_administrator() {
    db.qualityGates().createDefaultQualityGate();
    ComponentDto project = db.components().insertPublicProject();
    init(createPages());

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    assertJson(execute(project.getKey())).isSimilarTo("{\"configuration\": {\"canUpdateProjectVisibilityToPrivate\": true}}");
  }

  @Test
  public void fail_on_missing_parameters() {
    insertProject();
    init();

    TestRequest request = ws.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_on_unknown_component_key() {
    insertProject();
    init();

    assertThatThrownBy(() -> execute("unknoen"))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void throw_ForbiddenException_if_required_permission_is_not_granted() {
    ComponentDto project = insertProject();
    init();
    userSession.logIn();

    String projectDbKey = project.getKey();
    assertThatThrownBy(() -> execute(projectDbKey))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void test_example_response() {
    init(createPages());
    ComponentDto project = newPrivateProjectDto("ABCD")
      .setKey("org.codehaus.sonar:sonar")
      .setName("Sonarqube")
      .setDescription("Open source platform for continuous inspection of code quality");
    componentDbTester.insertPrivateProject(project);
    SnapshotDto analysis = newAnalysis(project)
      .setCreatedAt(parseDateTime("2016-12-06T11:44:00+0200").getTime())
      .setProjectVersion("6.3")
      .setLast(true);
    componentDbTester.insertSnapshot(analysis);
    when(resourceTypes.get(project.qualifier())).thenReturn(DefaultResourceTypes.get().getRootType());
    UserDto user = db.users().insertUser("obiwan");
    propertyDbTester.insertProperty(new PropertyDto().setKey("favourite").setComponentUuid(project.uuid()).setUserUuid(user.getUuid()),
      project.getKey(), project.name(), project.qualifier(), user.getLogin());
    addQualityProfiles(project,
      createQProfile("qp1", "Sonar Way Java", "java"),
      createQProfile("qp2", "Sonar Way Xoo", "xoo"));
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way"));
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project), qualityGateDto);
    userSession.logIn(user)
      .addProjectPermission(UserRole.USER, project)
      .addProjectPermission(UserRole.ADMIN, project);

    String result = execute(project.getKey());
    assertJson(result).ignoreFields("snapshotDate", "canBrowseAllChildProjects", "key", "qualityGate.key").isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void definition() {
    init();
    WebService.Action action = ws.getDef();

    assertThat(action.since()).isEqualTo("5.2");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.description()).isNotNull();
    assertThat(action.responseExample()).isNotNull();
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("6.4", "The 'visibility' field is added"),
      tuple("7.3", "The 'almRepoUrl' and 'almId' fields are added"),
      tuple("7.6", "The use of module keys in parameter 'component' is deprecated"),
      tuple("8.8", "Deprecated parameter 'componentKey' has been removed. Please use parameter 'component' instead"));

    WebService.Param componentId = action.param(PARAM_COMPONENT);
    assertThat(componentId.isRequired()).isFalse();
    assertThat(componentId.description()).isNotNull();
    assertThat(componentId.exampleValue()).isNotNull();
  }

  @Test
  public void fail_on_module_key_as_param() {
    ComponentDto project = insertProject();
    ComponentDto module = componentDbTester.insertComponent(newModuleDto("bcde", project).setKey("palap").setName("Palap"));
    init();

    assertThatThrownBy(() -> execute(module.getKey()))
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_on_directory_key_as_param() {
    ComponentDto project = insertProject();
    ComponentDto module = componentDbTester.insertComponent(newModuleDto("bcde", project).setKey("palap").setName("Palap"));
    ComponentDto directory = componentDbTester.insertComponent(newDirectory(module, "src/main/xoo"));
    userSession.addProjectPermission(UserRole.USER, project);
    init();
    String dirKey = directory.getKey();
    assertThatThrownBy(() -> execute(dirKey))
      .isInstanceOf(BadRequestException.class);
  }

  private ComponentDto insertProject() {
    db.qualityGates().createDefaultQualityGate();
    return db.components().insertPrivateProject("abcd", p -> p.setKey("polop")
      .setName("Polop")
      .setDescription("test project")
      .setQualifier(Qualifiers.PROJECT)
      .setScope(Scopes.PROJECT));
  }

  private void init(Page... pages) {
    PluginRepository pluginRepository = mock(PluginRepository.class);
    when(pluginRepository.hasPlugin(any())).thenReturn(true);
    when(pluginRepository.getPluginInfo(any())).thenReturn(new PluginInfo("unused").setVersion(Version.create("1.0")));
    CoreExtensionRepository coreExtensionRepository = mock(CoreExtensionRepository.class);
    when(coreExtensionRepository.isInstalled(any())).thenReturn(false);
    PageRepository pageRepository = new PageRepository(pluginRepository, coreExtensionRepository, new PageDefinition[] {context -> {
      for (Page page : pages) {
        context.addPage(page);
      }
    }});
    pageRepository.start();
    ws = new WsActionTester(
      new ComponentAction(dbClient, pageRepository, resourceTypes, userSession, new ComponentFinder(dbClient, resourceTypes),
        new QualityGateFinder(dbClient), config));
  }

  private String execute(String componentKey) {
    return ws.newRequest().setParam("component", componentKey).execute().getInput();
  }

  private void verify(String json, String jsonFile) {
    assertJson(json).isSimilarTo(getClass().getResource(ComponentActionTest.class.getSimpleName() + "/" + jsonFile));
  }

  private void executeAndVerify(String componentKey, String expectedJson) {
    verify(execute(componentKey), expectedJson);
  }

  private void addQualityProfiles(ComponentDto project, QualityProfile... qps) {
    MetricDto metric = newMetricDto().setKey(QUALITY_PROFILES_KEY);
    dbClient.metricDao().insert(db.getSession(), metric);
    dbClient.liveMeasureDao().insert(db.getSession(),
      newLiveMeasure(project, metric)
        .setData(qualityProfilesToJson(qps)));
    db.commit();
  }

  private Page[] createPages() {
    Page page1 = Page.builder("my_plugin/first_page")
      .setName("First Page")
      .setScope(COMPONENT)
      .setComponentQualifiers(Qualifier.PROJECT)
      .build();
    Page page2 = Page.builder("my_plugin/second_page")
      .setName("Second Page")
      .setScope(COMPONENT)
      .setComponentQualifiers(Qualifier.PROJECT)
      .build();
    Page adminPage = Page.builder("my_plugin/admin_page")
      .setName("Admin Page")
      .setScope(COMPONENT)
      .setComponentQualifiers(Qualifier.PROJECT)
      .setAdmin(true)
      .build();

    return new Page[] {page1, page2, adminPage};
  }

  private void verifySuccess(String componentKey) {
    String json = execute(componentKey);
    assertJson(json).isSimilarTo("{\"key\":\"" + componentKey + "\"}");
  }

  private static QualityProfile createQProfile(String qpKey, String qpName, String languageKey) {
    return new QualityProfile(qpKey, qpName, languageKey, new Date());
  }

  private static String qualityProfilesToJson(QualityProfile... qps) {
    List<QualityProfile> qualityProfiles = Arrays.asList(qps);
    return QPMeasureData.toJson(new QPMeasureData(qualityProfiles));
  }
}
