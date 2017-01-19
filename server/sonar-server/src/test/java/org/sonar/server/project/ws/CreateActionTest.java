/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.project.ws;

import com.google.common.base.Throwables;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsProjects.CreateWsResponse;
import org.sonarqube.ws.client.project.CreateRequest;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_COMPONENTS;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURE;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.WsRequest.Method.POST;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NAME;

public class CreateActionTest {

  private static final String DEFAULT_PROJECT_KEY = "project-key";
  private static final String DEFAULT_PROJECT_NAME = "project-name";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = new EsTester(new ComponentIndexDefinition(new MapSettings()), new ProjectMeasuresIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public I18nRule i18n = new I18nRule().put("qualifier.TRK", "Project");

  private Settings settings = new MapSettings();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private PermissionTemplateDto permissionTemplateDto;

  private WsActionTester ws = new WsActionTester(
    new CreateAction(
      db.getDbClient(), userSession,
      new ComponentService(db.getDbClient(), i18n, userSession, system2,
        new ProjectMeasuresIndexer(system2, db.getDbClient(), es.client()),
        new ComponentIndexer(db.getDbClient(), es.client())),
      new PermissionTemplateService(db.getDbClient(), settings, new PermissionIndexer(db.getDbClient(), es.client()), userSession),
      new FavoriteUpdater(db.getDbClient(), userSession),
      defaultOrganizationProvider));

  @Before
  public void setUp() throws Exception {
    permissionTemplateDto = db.permissionTemplates().insertTemplate();
    setTemplateAsDefault(permissionTemplateDto);
  }

  @Test
  public void create_project() throws Exception {
    userSession.setGlobalPermissions(PROVISIONING);

    CreateWsResponse response = call(CreateRequest.builder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build());

    assertThat(response.getProject().getKey()).isEqualTo(DEFAULT_PROJECT_KEY);
    assertThat(response.getProject().getName()).isEqualTo(DEFAULT_PROJECT_NAME);
    assertThat(response.getProject().getQualifier()).isEqualTo("TRK");
    ComponentDto project = db.getDbClient().componentDao().selectOrFailByKey(db.getSession(), DEFAULT_PROJECT_KEY);
    assertThat(project.getKey()).isEqualTo(DEFAULT_PROJECT_KEY);
    assertThat(project.name()).isEqualTo(DEFAULT_PROJECT_NAME);
    assertThat(project.qualifier()).isEqualTo("TRK");
  }

  @Test
  public void create_project_with_branch() throws Exception {
    userSession.setGlobalPermissions(PROVISIONING);

    CreateWsResponse response = call(CreateRequest.builder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setBranch("origin/master")
      .build());

    assertThat(response.getProject().getKey()).isEqualTo("project-key:origin/master");
  }

  @Test
  public void verify_permission_template_is_applied() throws Exception {
    UserDto userDto = db.users().insertUser();
    userSession.login(userDto).setGlobalPermissions(PROVISIONING);
    db.permissionTemplates().addUserToTemplate(permissionTemplateDto.getId(), userDto.getId(), USER);

    call(CreateRequest.builder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build());

    ComponentDto project = db.getDbClient().componentDao().selectOrFailByKey(db.getSession(), DEFAULT_PROJECT_KEY);
    assertThat(db.users().selectProjectPermissionsOfUser(userDto, project)).containsOnly(USER);
  }

  @Test
  public void add_project_to_favorite_when_logged() throws Exception {
    UserDto userDto = db.users().insertUser();
    userSession.login(userDto).setGlobalPermissions(PROVISIONING);
    db.permissionTemplates().addProjectCreatorToTemplate(permissionTemplateDto.getId(), USER);

    call(CreateRequest.builder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build());

    ComponentDto project = db.getDbClient().componentDao().selectOrFailByKey(db.getSession(), DEFAULT_PROJECT_KEY);
    assertThat(db.favorites().hasFavorite(project, userDto.getId())).isTrue();
  }

  @Test
  public void does_not_add_project_to_favorite_when_not_logged() throws Exception {
    userSession.setGlobalPermissions(PROVISIONING);
    db.permissionTemplates().addProjectCreatorToTemplate(permissionTemplateDto.getId(), USER);

    call(CreateRequest.builder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build());

    ComponentDto project = db.getDbClient().componentDao().selectOrFailByKey(db.getSession(), DEFAULT_PROJECT_KEY);
    assertThat(db.favorites().hasNoFavorite(project)).isTrue();
  }

  @Test
  public void does_not_add_project_to_favorite_when_project_create_has_no_permission_on_template() throws Exception {
    UserDto userDto = db.users().insertUser();
    userSession.login(userDto).setGlobalPermissions(PROVISIONING);

    call(CreateRequest.builder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build());

    ComponentDto project = db.getDbClient().componentDao().selectOrFailByKey(db.getSession(), DEFAULT_PROJECT_KEY);
    assertThat(db.favorites().hasNoFavorite(project)).isTrue();
  }

  @Test
  public void verify_project_exists_in_es_indexes() throws Exception {
    userSession.setGlobalPermissions(PROVISIONING);

    call(CreateRequest.builder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build());

    ComponentDto project = db.getDbClient().componentDao().selectOrFailByKey(db.getSession(), DEFAULT_PROJECT_KEY);
    assertThat(es.getIds(INDEX_COMPONENTS, TYPE_COMPONENT)).containsOnly(project.uuid());
    assertThat(es.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).containsOnly(project.uuid());
  }

  @Test
  public void create_project_with_deprecated_parameter() throws Exception {
    userSession.setGlobalPermissions(PROVISIONING);

    ws.newRequest()
      .setMethod(POST.name())
      .setParam("key", DEFAULT_PROJECT_KEY)
      .setParam(PARAM_NAME, DEFAULT_PROJECT_NAME)
      .execute();

    assertThat(db.getDbClient().componentDao().selectByKey(db.getSession(), DEFAULT_PROJECT_KEY).isPresent()).isTrue();
  }

  @Test
  public void fail_when_project_already_exists() throws Exception {
    userSession.setGlobalPermissions(PROVISIONING);
    db.components().insertComponent(ComponentTesting.newProjectDto(db.getDefaultOrganization()).setKey(DEFAULT_PROJECT_KEY));
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Could not create Project, key already exists: project-key");

    call(CreateRequest.builder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build());
  }

  @Test
  public void fail_when_missing_project_parameter() throws Exception {
    userSession.setGlobalPermissions(PROVISIONING);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'project' parameter is missing");

    call(CreateRequest.builder().setName(DEFAULT_PROJECT_NAME).build());
  }

  @Test
  public void fail_when_missing_name_parameter() throws Exception {
    userSession.setGlobalPermissions(PROVISIONING);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    call(CreateRequest.builder().setKey(DEFAULT_PROJECT_KEY).build());
  }

  @Test
  public void fail_when_key_has_bad_format() throws Exception {
    userSession.setGlobalPermissions(PROVISIONING);
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Malformed key for Project: 1234");

    call(CreateRequest.builder().setKey("1234").setName(DEFAULT_PROJECT_NAME).build());
  }

  @Test
  public void fail_when_missing_create_project_permission() throws Exception {
    userSession.setGlobalPermissions(QUALITY_GATE_ADMIN);
    expectedException.expect(ForbiddenException.class);

    call(CreateRequest.builder().setKey(DEFAULT_PROJECT_KEY).setName(DEFAULT_PROJECT_NAME).build());
  }

  @Test
  public void test_example() {
    userSession.setGlobalPermissions(PROVISIONING);

    String result = ws.newRequest()
      .setParam("key", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("create-example.json"));
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    Assertions.assertThat(definition.key()).isEqualTo("create");
    Assertions.assertThat(definition.since()).isEqualTo("4.0");
    Assertions.assertThat(definition.isInternal()).isFalse();
    Assertions.assertThat(definition.responseExampleAsString()).isNotEmpty();
    Assertions.assertThat(definition.params()).hasSize(3);
  }

  private CreateWsResponse call(CreateRequest request) {
    TestRequest httpRequest = ws.newRequest()
      .setMethod(POST.name())
      .setMediaType(MediaTypes.PROTOBUF);
    setNullable(request.getKey(), e -> httpRequest.setParam("project", e));
    setNullable(request.getName(), e -> httpRequest.setParam("name", e));
    setNullable(request.getBranch(), e -> httpRequest.setParam("branch", e));
    try {
      return CreateWsResponse.parseFrom(httpRequest.execute().getInputStream());
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private void setTemplateAsDefault(PermissionTemplateDto permissionTemplateDto) {
    settings.appendProperty("sonar.permission.template.default", permissionTemplateDto.getUuid());
  }

}
