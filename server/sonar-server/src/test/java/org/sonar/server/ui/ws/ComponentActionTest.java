/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
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
import org.sonar.db.alm.ALM;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.BillingValidations;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.ws.WsActionTester;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES_KEY;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.page.Page.Scope.COMPONENT;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.ui.ws.ComponentAction.PARAM_COMPONENT;
import static org.sonar.test.JsonAssert.assertJson;

public class ComponentActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();
  private ComponentDbTester componentDbTester = db.components();
  private PropertyDbTester propertyDbTester = new PropertyDbTester(db);
  private ResourceTypes resourceTypes = mock(ResourceTypes.class);
  private BillingValidationsProxy billingValidations = mock(BillingValidationsProxy.class);

  private WsActionTester ws;

  @Test
  public void return_info_if_user_has_browse_permission_on_project() {
    ComponentDto project = insertOrganizationAndProject();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    init();

    verifySuccess(project.getKey());
  }

  @Test
  public void return_info_if_user_has_administration_permission_on_project() {
    ComponentDto project = insertOrganizationAndProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    init();

    verifySuccess(project.getDbKey());
  }

  @Test
  public void return_info_if_user_is_system_administrator() {
    ComponentDto project = insertOrganizationAndProject();
    userSession.logIn().setSystemAdministrator();
    init();

    verifySuccess(project.getDbKey());
  }

  @Test
  public void return_component_info_when_anonymous_no_snapshot() {
    ComponentDto project = insertOrganizationAndProject();
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getDbKey(), "return_component_info_when_anonymous_no_snapshot.json");
  }

  @Test
  public void return_component_info_with_favourite() {
    ComponentDto project = insertOrganizationAndProject();
    UserDto user = db.users().insertUser("obiwan");
    propertyDbTester.insertProperty(new PropertyDto().setKey("favourite").setResourceId(project.getId()).setUserId(user.getId()));
    userSession.logIn(user).addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getDbKey(), "return_component_info_with_favourite.json");
  }

  @Test
  public void return_component_info_when_snapshot() {
    ComponentDto project = insertOrganizationAndProject();
    db.components().insertSnapshot(project, snapshot -> snapshot
      .setCreatedAt(parseDateTime("2015-04-22T11:44:00+0200").getTime())
      .setProjectVersion("3.14"));
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getDbKey(), "return_component_info_when_snapshot.json");
  }

  @Test
  public void return_component_info_when_file_on_master() {
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org2"));
    db.qualityGates().createDefaultQualityGate(organization);
    ComponentDto main = componentDbTester.insertMainBranch(organization, p -> p.setName("Sample"), p -> p.setDbKey("sample"));
    userSession.addProjectPermission(UserRole.USER, main);
    init();

    ComponentDto dirDto = componentDbTester.insertComponent(newDirectory(main, "src"));

    ComponentDto fileDto = componentDbTester.insertComponent(newFileDto(main, dirDto)
      .setUuid("abcd")
      .setName("Main.xoo")
      .setDbKey("sample:src/Main.xoo"));

    executeAndVerify(fileDto.getDbKey(), "return_component_info_when_file_on_master.json");
  }

  @Test
  public void return_component_info_when_file_on_branch() {
    OrganizationDto organization = db.organizations().insertForKey("my-org2");
    db.qualityGates().createDefaultQualityGate(organization);
    ComponentDto project = componentDbTester.insertMainBranch(organization, p -> p.setName("Sample").setDbKey("sample"));
    ComponentDto branch = componentDbTester.insertProjectBranch(project, b -> b.setKey("feature1"));
    userSession.addProjectPermission(UserRole.USER, project);
    init();
    ComponentDto dirDto = componentDbTester.insertComponent(newDirectory(branch, "src"));
    ComponentDto fileDto = componentDbTester.insertComponent(newFileDto(branch, dirDto)
      .setUuid("abcd")
      .setName("Main.xoo"));

    String json = ws.newRequest()
      .setParam("componentKey", fileDto.getKey())
      .setParam("branch", branch.getBranch())
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
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org"));
    ComponentDto project = insertProject(organization);
    QProfileDto qp1 = db.qualityProfiles().insert(organization, t -> t.setKee("qp1").setName("Sonar Way Java").setLanguage("java"));
    QProfileDto qp2 = db.qualityProfiles().insert(organization, t -> t.setKee("qp2").setName("Sonar Way Xoo").setLanguage("xoo"));
    addQualityProfiles(project,
      new QualityProfile(qp1.getKee(), qp1.getName(), qp1.getLanguage(), new Date()),
      new QualityProfile(qp2.getKee(), qp2.getName(), qp2.getLanguage(), new Date()));
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getDbKey(), "return_quality_profiles.json");

    db.getDbClient().qualityProfileDao().deleteOrgQProfilesByUuids(db.getSession(), ImmutableSet.of(qp1.getKee(), qp2.getKee()));
    db.commit();

    executeAndVerify(project.getDbKey(), "return_deleted_quality_profiles.json");
  }

  @Test
  public void return_empty_quality_profiles_when_no_measure() {
    ComponentDto project = insertOrganizationAndProject();
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getDbKey(), "return_empty_quality_profiles_when_no_measure.json");
  }

  @Test
  public void return_quality_gate_defined_on_project() {
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org"));
    db.qualityGates().createDefaultQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("Sonar way"));
    db.qualityGates().associateProjectToQualityGate(project, qualityGateDto);
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getDbKey(), "return_quality_gate.json");
  }

  @Test
  public void quality_gate_for_a_long_living_branch() {
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org"));
    db.qualityGates().createDefaultQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("Sonar way"));
    db.qualityGates().associateProjectToQualityGate(project, qualityGateDto);
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    String json = ws.newRequest()
      .setParam("componentKey", longLivingBranch.getKey())
      .setParam("branch", longLivingBranch.getBranch())
      .execute()
      .getInput();

    verify(json, "return_quality_gate.json");
  }

  @Test
  public void return_default_quality_gate() {
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org"));
    ComponentDto project = db.components().insertPrivateProject(organization);
    db.qualityGates().createDefaultQualityGate(organization, qg -> qg.setName("Sonar way"));
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(project.getDbKey(), "return_default_quality_gate.json");
  }

  @Test
  public void return_extensions() {
    ComponentDto project = insertOrganizationAndProject();
    userSession.anonymous().addProjectPermission(UserRole.USER, project);
    init(createPages());

    executeAndVerify(project.getDbKey(), "return_extensions.json");
  }

  @Test
  public void return_extensions_for_application() {
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org"));
    db.qualityGates().createDefaultQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    Page page = Page.builder("my_plugin/app_page")
      .setName("App Page")
      .setScope(COMPONENT)
      .setComponentQualifiers(Qualifier.VIEW, Qualifier.APP)
      .build();
    ComponentDto application = componentDbTester.insertPublicApplication(organization);
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("Sonar way"));
    db.qualityGates().associateProjectToQualityGate(project, qualityGateDto);
    userSession.registerComponents(application);
    init(page);

    String result = ws.newRequest()
      .setParam(PARAM_COMPONENT, application.getDbKey())
      .execute().getInput();

    assertThat(result).contains("my_plugin/app_page");
  }

  @Test
  public void return_extensions_for_admin() {
    ComponentDto project = insertOrganizationAndProject();
    userSession.anonymous()
      .addProjectPermission(UserRole.USER, project)
      .addProjectPermission(UserRole.ADMIN, project);
    init(createPages());

    executeAndVerify(project.getDbKey(), "return_extensions_for_admin.json");
  }

  @Test
  public void return_configuration_for_admin() {
    ComponentDto project = insertOrganizationAndProject();
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

    executeAndVerify(project.getDbKey(), "return_configuration_for_admin.json");
  }

  @Test
  public void return_configuration_with_all_properties() {
    ComponentDto project = insertOrganizationAndProject();
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

    executeAndVerify(project.getDbKey(), "return_configuration_with_all_properties.json");
  }

  @Test
  public void return_configuration_for_quality_profile_admin() {
    ComponentDto project = insertOrganizationAndProject();
    userSession.logIn()
      .addProjectPermission(UserRole.USER, project)
      .addPermission(ADMINISTER_QUALITY_PROFILES, project.getOrganizationUuid());
    init();

    executeAndVerify(project.getDbKey(), "return_configuration_for_quality_profile_admin.json");
  }

  @Test
  public void return_configuration_for_quality_gate_admin() {
    ComponentDto project = insertOrganizationAndProject();
    userSession.logIn()
      .addProjectPermission(UserRole.USER, project)
      .addPermission(ADMINISTER_QUALITY_GATES, project.getOrganizationUuid());
    init();

    executeAndVerify(project.getDbKey(), "return_configuration_for_quality_gate_admin.json");
  }

  @Test
  public void return_bread_crumbs_on_several_levels() {
    ComponentDto project = insertOrganizationAndProject();
    ComponentDto module = componentDbTester.insertComponent(newModuleDto("bcde", project).setDbKey("palap").setName("Palap"));
    ComponentDto directory = componentDbTester.insertComponent(newDirectory(module, "src/main/xoo"));
    ComponentDto file = componentDbTester.insertComponent(newFileDto(directory, directory, "cdef").setName("Source.xoo")
      .setDbKey("palap:src/main/xoo/Source.xoo")
      .setPath(directory.path()));
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    executeAndVerify(file.getDbKey(), "return_bread_crumbs_on_several_levels.json");
  }

  @Test
  public void project_administrator_is_allowed_to_get_information() {
    ComponentDto project = insertOrganizationAndProject();
    userSession.addProjectPermission(UserRole.ADMIN, project);
    init(createPages());

    execute(project.getDbKey());
  }

  @Test
  public void should_return_private_flag_for_project() {
    OrganizationDto org = db.organizations().insert();
    db.qualityGates().createDefaultQualityGate(org);
    ComponentDto project = db.components().insertPrivateProject(org);
    init();

    userSession.logIn()
      .addProjectPermission(UserRole.ADMIN, project)
      .addPermission(OrganizationPermission.ADMINISTER, org);
    assertJson(execute(project.getDbKey())).isSimilarTo("{\"visibility\": \"private\"}");
  }

  @Test
  public void should_return_public_flag_for_project() {
    OrganizationDto org = db.organizations().insert();
    db.qualityGates().createDefaultQualityGate(org);
    ComponentDto project = db.components().insertPublicProject(org);
    init();

    userSession.logIn()
      .addProjectPermission(UserRole.ADMIN, project)
      .addPermission(OrganizationPermission.ADMINISTER, org);
    assertJson(execute(project.getDbKey())).isSimilarTo("{\"visibility\": \"public\"}");
  }

  @Test
  public void canApplyPermissionTemplate_is_true_if_logged_in_as_organization_administrator() {
    OrganizationDto org = db.organizations().insert();
    db.qualityGates().createDefaultQualityGate(org);
    ComponentDto project = db.components().insertPrivateProject(org);
    init(createPages());

    userSession.logIn()
      .addProjectPermission(UserRole.ADMIN, project)
      .addPermission(OrganizationPermission.ADMINISTER, org);
    assertJson(execute(project.getDbKey())).isSimilarTo("{\"configuration\": {\"canApplyPermissionTemplate\": true}}");

    userSession.logIn()
      .addProjectPermission(UserRole.ADMIN, project);

    assertJson(execute(project.getDbKey())).isSimilarTo("{\"configuration\": {\"canApplyPermissionTemplate\": false}}");
  }

  @Test
  public void canUpdateProjectVisibilityToPrivate_is_true_if_logged_in_as_project_administrator_and_extension_returns_false() {
    OrganizationDto org = db.organizations().insert();
    db.qualityGates().createDefaultQualityGate(org);
    ComponentDto project = db.components().insertPublicProject(org);
    init(createPages());

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    when(billingValidations.canUpdateProjectVisibilityToPrivate(any(BillingValidations.Organization.class))).thenReturn(false);
    assertJson(execute(project.getDbKey())).isSimilarTo("{\"configuration\": {\"canUpdateProjectVisibilityToPrivate\": false}}");

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    when(billingValidations.canUpdateProjectVisibilityToPrivate(any(BillingValidations.Organization.class))).thenReturn(true);
    assertJson(execute(project.getDbKey())).isSimilarTo("{\"configuration\": {\"canUpdateProjectVisibilityToPrivate\": true}}");
  }

  @Test
  public void fail_on_missing_parameters() {
    insertOrganizationAndProject();
    init();

    expectedException.expect(IllegalArgumentException.class);
    ws.newRequest().execute();
  }

  @Test
  public void fail_on_unknown_component_key() {
    insertOrganizationAndProject();
    init();

    expectedException.expect(NotFoundException.class);
    execute("unknoen");
  }

  @Test
  public void throw_ForbiddenException_if_required_permission_is_not_granted() {
    ComponentDto project = insertOrganizationAndProject();
    init();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    execute(project.getDbKey());
  }

  @Test
  public void test_example_response() {
    init(createPages());
    OrganizationDto organizationDto = db.organizations().insertForKey("my-org-1");
    ComponentDto project = newPrivateProjectDto(organizationDto, "ABCD")
      .setDbKey("org.codehaus.sonar:sonar")
      .setName("Sonarqube")
      .setDescription("Open source platform for continuous inspection of code quality");
    componentDbTester.insertComponent(project);
    SnapshotDto analysis = newAnalysis(project)
      .setCreatedAt(parseDateTime("2016-12-06T11:44:00+0200").getTime())
      .setProjectVersion("6.3")
      .setLast(true);
    componentDbTester.insertSnapshot(analysis);
    when(resourceTypes.get(project.qualifier())).thenReturn(DefaultResourceTypes.get().getRootType());
    UserDto user = db.users().insertUser("obiwan");
    propertyDbTester.insertProperty(new PropertyDto().setKey("favourite").setResourceId(project.getId()).setUserId(user.getId()));
    addQualityProfiles(project,
      createQProfile("qp1", "Sonar Way Java", "java"),
      createQProfile("qp2", "Sonar Way Xoo", "xoo"));
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate(db.getDefaultOrganization(), qg -> qg.setName("Sonar way"));
    db.qualityGates().associateProjectToQualityGate(project, qualityGateDto);
    userSession.logIn(user)
      .addProjectPermission(UserRole.USER, project)
      .addProjectPermission(UserRole.ADMIN, project);

    String result = execute(project.getDbKey());
    assertJson(result).ignoreFields("snapshotDate", "key", "qualityGate.key").isSimilarTo(ws.getDef().responseExampleAsString());
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
      tuple("7.6", "The use of module keys in parameter 'component' is deprecated"));

    WebService.Param componentId = action.param(PARAM_COMPONENT);
    assertThat(componentId.isRequired()).isFalse();
    assertThat(componentId.description()).isNotNull();
    assertThat(componentId.exampleValue()).isNotNull();
    assertThat(componentId.deprecatedKey()).isEqualTo("componentKey");
    assertThat(componentId.deprecatedKeySince()).isEqualTo("6.4");
  }

  @Test
  public void return_alm_info_on_project() {
    ComponentDto project = insertOrganizationAndProject();
    dbClient.projectAlmBindingsDao().insertOrUpdate(db.getSession(), ALM.BITBUCKETCLOUD, "{123456789}", project.uuid(), null, "http://bitbucket.org/foo/bar");
    db.getSession().commit();
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    String json = execute(project.getKey());

    assertJson(json).isSimilarTo("{\n" +
      "  \"organization\": \"my-org\",\n" +
      "  \"key\": \"polop\",\n" +
      "  \"id\": \"abcd\",\n" +
      "  \"name\": \"Polop\",\n" +
      "  \"description\": \"test project\",\n" +
      "  \"alm\": {\n" +
      "     \"key\": \"bitbucketcloud\",\n" +
      "     \"url\": \"http://bitbucket.org/foo/bar\"\n" +
      "  }\n" +
      "}\n");
  }

  @Test(expected = BadRequestException.class)
  public void fail_on_module_key_as_param() {
    ComponentDto project = insertOrganizationAndProject();
    ComponentDto module = componentDbTester.insertComponent(newModuleDto("bcde", project).setDbKey("palap").setName("Palap"));
    init();

    execute(module.getKey());
  }

  @Test(expected = BadRequestException.class)
  public void fail_on_directory_key_as_param() {
    ComponentDto project = insertOrganizationAndProject();
    ComponentDto module = componentDbTester.insertComponent(newModuleDto("bcde", project).setDbKey("palap").setName("Palap"));
    ComponentDto directory = componentDbTester.insertComponent(newDirectory(module, "src/main/xoo"));
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    execute(directory.getDbKey());
  }

  @Test
  public void return_alm_info_on_branch() {
    ComponentDto project = insertOrganizationAndProject();
    ComponentDto branch = componentDbTester.insertProjectBranch(project, b -> b.setKey("feature1").setUuid("xyz"));
    dbClient.projectAlmBindingsDao().insertOrUpdate(db.getSession(), ALM.BITBUCKETCLOUD, "{123456789}", project.uuid(), null, "http://bitbucket.org/foo/bar");
    db.getSession().commit();
    userSession.addProjectPermission(UserRole.USER, project);
    init();

    String json = ws.newRequest()
      .setParam("componentKey", project.getKey())
      .setParam("branch", branch.getBranch())
      .execute()
      .getInput();

    assertJson(json).isSimilarTo("{\n" +
      "  \"organization\": \"my-org\",\n" +
      "  \"key\": \"polop\",\n" +
      "  \"id\": \"xyz\",\n" +
      "  \"branch\": \"feature1\"," +
      "  \"name\": \"Polop\",\n" +
      "  \"description\": \"test project\",\n" +
      "  \"alm\": {\n" +
      "     \"key\": \"bitbucketcloud\",\n" +
      "     \"url\": \"http://bitbucket.org/foo/bar\"\n" +
      "  }\n" +
      "}\n");
  }

  private ComponentDto insertOrganizationAndProject() {
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org"));
    return insertProject(organization);
  }

  private ComponentDto insertProject(OrganizationDto organization) {
    db.qualityGates().createDefaultQualityGate(organization);
    return db.components().insertPrivateProject(organization, "abcd", p -> p.setDbKey("polop").setName("Polop").setDescription("test project"));
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
        new QualityGateFinder(dbClient), billingValidations));
  }

  private String execute(String componentKey) {
    return ws.newRequest().setParam("componentKey", componentKey).execute().getInput();
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
