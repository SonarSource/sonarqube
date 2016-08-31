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

package org.sonar.server.setting.ws;

import java.io.IOException;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.Settings.ListDefinitionsWsResponse;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.Qualifiers.MODULE;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.DASHBOARD_SHARING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonarqube.ws.MediaTypes.JSON;
import static org.sonarqube.ws.Settings.Type.BOOLEAN;
import static org.sonarqube.ws.Settings.Type.PROPERTY_SET;
import static org.sonarqube.ws.Settings.Type.SINGLE_SELECT_LIST;
import static org.sonarqube.ws.Settings.Type.STRING;
import static org.sonarqube.ws.Settings.Type.TEXT;

public class ListDefinitionsActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();
  ComponentDbTester componentDb = new ComponentDbTester(db);

  ComponentDto project;

  PropertyDefinitions propertyDefinitions = new PropertyDefinitions();

  WsActionTester ws = new WsActionTester(new ListDefinitionsAction(dbClient, new ComponentFinder(dbClient), userSession, propertyDefinitions));

  @Before
  public void setUp() throws Exception {
    project = componentDb.insertComponent(newProjectDto());
  }

  @Test
  public void return_settings_definitions() {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .name("Foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.TEXT)
      .defaultValue("default")
      .multiValues(true)
      .build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    Settings.Definition definition = result.getDefinitions(0);
    assertThat(definition.getKey()).isEqualTo("foo");
    assertThat(definition.getName()).isEqualTo("Foo");
    assertThat(definition.getDescription()).isEqualTo("desc");
    assertThat(definition.getCategory()).isEqualTo("cat");
    assertThat(definition.getSubCategory()).isEqualTo("subCat");
    assertThat(definition.getType()).isEqualTo(TEXT);
    assertThat(definition.getDefaultValue()).isEqualTo("default");
    assertThat(definition.getMultiValues()).isTrue();
  }

  @Test
  public void return_settings_definitions_with_minimum_fields() {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    Settings.Definition definition = result.getDefinitions(0);
    assertThat(definition.getKey()).isEqualTo("foo");
    assertThat(definition.getType()).isEqualTo(STRING);
    assertThat(definition.hasName()).isFalse();
    assertThat(definition.hasCategory()).isFalse();
    assertThat(definition.hasSubCategory()).isFalse();
    assertThat(definition.hasDefaultValue()).isFalse();
    assertThat(definition.getMultiValues()).isFalse();
    assertThat(definition.getOptionsCount()).isZero();
    assertThat(definition.getFieldsCount()).isZero();
    assertThat(definition.hasDeprecatedKey()).isFalse();
  }

  @Test
  public void return_settings_definitions_with_deprecated_key() {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .name("Foo")
      .deprecatedKey("deprecated")
      .build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    Settings.Definition definition = result.getDefinitions(0);
    assertThat(definition.getKey()).isEqualTo("foo");
    assertThat(definition.getName()).isEqualTo("Foo");
    assertThat(definition.getDeprecatedKey()).isEqualTo("deprecated");
  }

  @Test
  public void return_default_category() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build(), "default");
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").category("").build(), "default");

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    assertThat(result.getDefinitions(0).getCategory()).isEqualTo("default");
    assertThat(result.getDefinitions(0).getSubCategory()).isEqualTo("default");
  }

  @Test
  public void return_single_select_list_property() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.SINGLE_SELECT_LIST)
      .options("one", "two")
      .build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    Settings.Definition definition = result.getDefinitions(0);
    assertThat(definition.getType()).isEqualTo(SINGLE_SELECT_LIST);
    assertThat(definition.getOptionsList()).containsExactly("one", "two");
  }

  @Test
  public void return_property_set() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(
        PropertyFieldDefinition.build("boolean").name("Boolean").description("boolean desc").type(PropertyType.BOOLEAN).build(),
        PropertyFieldDefinition.build("list").name("List").description("list desc").type(PropertyType.SINGLE_SELECT_LIST).options("one", "two").build())
      .build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    Settings.Definition definition = result.getDefinitions(0);
    assertThat(definition.getType()).isEqualTo(PROPERTY_SET);
    assertThat(definition.getFieldsList()).hasSize(2);

    assertThat(definition.getFields(0).getKey()).isEqualTo("boolean");
    assertThat(definition.getFields(0).getName()).isEqualTo("Boolean");
    assertThat(definition.getFields(0).getDescription()).isEqualTo("boolean desc");
    assertThat(definition.getFields(0).getType()).isEqualTo(BOOLEAN);
    assertThat(definition.getFields(0).getOptionsCount()).isZero();

    assertThat(definition.getFields(1).getKey()).isEqualTo("list");
    assertThat(definition.getFields(1).getName()).isEqualTo("List");
    assertThat(definition.getFields(1).getDescription()).isEqualTo("list desc");
    assertThat(definition.getFields(1).getType()).isEqualTo(SINGLE_SELECT_LIST);
    assertThat(definition.getFields(1).getOptionsList()).containsExactly("one", "two");
  }

  @Test
  public void does_not_return_license_type_property_set() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(PropertyFieldDefinition.build("license").name("License").type(PropertyType.LICENSE).build())
      .build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    assertThat(result.getDefinitions(0).getFieldsList()).isEmpty();
  }

  @Test
  public void return_global_settings_definitions() {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
  }

  @Test
  public void return_project_settings_def_by_project_key() {
    setUserAsProjectAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .onQualifiers(PROJECT)
      .build());

    ListDefinitionsWsResponse result = executeRequest(null, project.key());

    assertThat(result.getDefinitionsList()).hasSize(1);
  }

  @Test
  public void return_project_settings_def_by_project_id() {
    setUserAsProjectAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .onQualifiers(PROJECT)
      .build());

    ListDefinitionsWsResponse result = executeRequest(project.uuid(), null);

    assertThat(result.getDefinitionsList()).hasSize(1);
  }

  @Test
  public void return_only_global_properties_when_no_component_parameter() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("global").build(),
      PropertyDefinition.builder("global-and-project").onQualifiers(PROJECT).build(),
      PropertyDefinition.builder("only-on-project").onlyOnQualifiers(PROJECT).build(),
      PropertyDefinition.builder("only-on-module").onlyOnQualifiers(MODULE).build()));

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).extracting("key").containsOnly("global", "global-and-project");
  }

  @Test
  public void return_only_properties_available_for_component_qualifier() throws Exception {
    setUserAsProjectAdmin();
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("global").build(),
      PropertyDefinition.builder("global-and-project").onQualifiers(PROJECT).build(),
      PropertyDefinition.builder("only-on-project").onlyOnQualifiers(PROJECT).build(),
      PropertyDefinition.builder("only-on-module").onlyOnQualifiers(MODULE).build()));

    ListDefinitionsWsResponse result = executeRequest(project.uuid(), null);

    assertThat(result.getDefinitionsList()).extracting("key").containsOnly("global-and-project", "only-on-project");
  }

  @Test
  public void does_not_return_hidden_properties() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").hidden().build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).isEmpty();
  }

  @Test
  public void does_not_return_license_type() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("license").type(PropertyType.LICENSE).build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).isEmpty();
  }

  @Test
  public void fail_when_id_and_key_are_set() throws Exception {
    setUserAsProjectAdmin();

    expectedException.expect(IllegalArgumentException.class);

    executeRequest(project.uuid(), project.key());
  }

  @Test
  public void fail_when_not_system_admin() throws Exception {
    userSession.login("not-admin").setGlobalPermissions(DASHBOARD_SHARING);
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build());

    expectedException.expect(ForbiddenException.class);

    executeRequest();
  }

  @Test
  public void fail_when_not_project_admin() throws Exception {
    userSession.login("project-admin").addProjectUuidPermissions(USER, project.uuid());
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build());

    expectedException.expect(ForbiddenException.class);

    executeRequest(project.uuid(), null);
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void test_example_json_response() {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("sonar.string")
        .name("String")
        .description("String property")
        .type(PropertyType.STRING)
        .category("general")
        .subCategory("test")
        .defaultValue("123")
        .build(),
      PropertyDefinition.builder("sonar.list")
        .name("List")
        .description("List property")
        .type(PropertyType.SINGLE_SELECT_LIST)
        .category("general")
        .options("a", "b")
        .build(),
      PropertyDefinition.builder("sonar.multiValues")
        .name("Multi values")
        .description("Multi values property")
        .type(PropertyType.STRING)
        .category("general")
        .multiValues(true)
        .build(),
      PropertyDefinition.builder("sonar.propertySet")
        .name("Property Set")
        .description("Property Set property")
        .type(PropertyType.PROPERTY_SET)
        .category("property")
        .subCategory("set")
        .fields(
          PropertyFieldDefinition.build("text")
            .name("Text")
            .description("Text field description")
            .type(PropertyType.TEXT)
            .build(),
          PropertyFieldDefinition.build("list")
            .name("List")
            .description("List field description")
            .type(PropertyType.SINGLE_SELECT_LIST)
            .options("value1", "value2")
            .build())
        .build()));

    String result = ws.newRequest().setMediaType(JSON).execute().getInput();

    JsonAssert.assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(result);
  }

  private ListDefinitionsWsResponse executeRequest() {
    return executeRequest(null, null);
  }

  private ListDefinitionsWsResponse executeRequest(@Nullable String id, @Nullable String key) {
    TestRequest request = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF);
    if (id != null) {
      request.setParam("componentId", id);
    }
    if (key != null) {
      request.setParam("componentKey", key);
    }
    try {
      return ListDefinitionsWsResponse.parseFrom(request.execute().getInputStream());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void setUserAsSystemAdmin() {
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

  private void setUserAsProjectAdmin() {
    userSession.login("project-admin").addProjectUuidPermissions(ADMIN, project.uuid());
  }

}
