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
package org.sonar.server.setting.ws;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.Settings.Definition;
import org.sonarqube.ws.Settings.ListDefinitionsWsResponse;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonarqube.ws.MediaTypes.JSON;
import static org.sonarqube.ws.Settings.Definition.CategoryOneOfCase.CATEGORYONEOF_NOT_SET;
import static org.sonarqube.ws.Settings.Definition.DefaultValueOneOfCase.DEFAULTVALUEONEOF_NOT_SET;
import static org.sonarqube.ws.Settings.Definition.DeprecatedKeyOneOfCase.DEPRECATEDKEYONEOF_NOT_SET;
import static org.sonarqube.ws.Settings.Definition.NameOneOfCase.NAMEONEOF_NOT_SET;
import static org.sonarqube.ws.Settings.Definition.SubCategoryOneOfCase.SUBCATEGORYONEOF_NOT_SET;
import static org.sonarqube.ws.Settings.Type.BOOLEAN;
import static org.sonarqube.ws.Settings.Type.LICENSE;
import static org.sonarqube.ws.Settings.Type.PROPERTY_SET;
import static org.sonarqube.ws.Settings.Type.SINGLE_SELECT_LIST;
import static org.sonarqube.ws.Settings.Type.STRING;
import static org.sonarqube.ws.Settings.Type.TEXT;

public class ListDefinitionsActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private ProjectDto project;
  private final PropertyDefinitions propertyDefinitions = new PropertyDefinitions(System2.INSTANCE);
  private final SettingsWsSupport support = new SettingsWsSupport(userSession);
  private final WsActionTester ws = new WsActionTester(new ListDefinitionsAction(dbClient, userSession, propertyDefinitions, support));

  @Before
  public void setUp() {
    project = db.components().insertPrivateProject().getProjectDto();
  }

  @Test
  public void return_settings_definitions() {
    logIn();
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
    Definition definition = result.getDefinitions(0);
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
    logIn();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    Definition definition = result.getDefinitions(0);
    assertThat(definition.getKey()).isEqualTo("foo");
    assertThat(definition.getType()).isEqualTo(STRING);
    assertThat(definition.getNameOneOfCase()).isEqualTo(NAMEONEOF_NOT_SET);
    assertThat(definition.getCategoryOneOfCase()).isEqualTo(CATEGORYONEOF_NOT_SET);
    assertThat(definition.getSubCategoryOneOfCase()).isEqualTo(SUBCATEGORYONEOF_NOT_SET);
    assertThat(definition.getDefaultValueOneOfCase()).isEqualTo(DEFAULTVALUEONEOF_NOT_SET);
    assertThat(definition.getMultiValues()).isFalse();
    assertThat(definition.getOptionsCount()).isZero();
    assertThat(definition.getFieldsCount()).isZero();
    assertThat(definition.getDeprecatedKeyOneOfCase()).isEqualTo(DEPRECATEDKEYONEOF_NOT_SET);
  }

  @Test
  public void return_settings_definitions_with_deprecated_key() {
    logIn();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .name("Foo")
      .deprecatedKey("deprecated")
      .build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    Definition definition = result.getDefinitions(0);
    assertThat(definition.getKey()).isEqualTo("foo");
    assertThat(definition.getName()).isEqualTo("Foo");
    assertThat(definition.getDeprecatedKey()).isEqualTo("deprecated");
  }

  @Test
  public void return_default_category() {
    logIn();
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build(), "default");
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").category("").build(), "default");

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    assertThat(result.getDefinitions(0).getCategory()).isEqualTo("default");
    assertThat(result.getDefinitions(0).getSubCategory()).isEqualTo("default");
  }

  @Test
  public void return_single_select_list_property() {
    logIn();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.SINGLE_SELECT_LIST)
      .options("one", "two")
      .build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    Definition definition = result.getDefinitions(0);
    assertThat(definition.getType()).isEqualTo(SINGLE_SELECT_LIST);
    assertThat(definition.getOptionsList()).containsExactly("one", "two");
  }

  @Test
  public void return_JSON_property() {
    logIn();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.JSON)
      .build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    Definition definition = result.getDefinitions(0);
    assertThat(definition.getType()).isEqualTo(Settings.Type.JSON);
  }

  @Test
  public void return_property_set() {
    logIn();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(
        PropertyFieldDefinition.build("boolean").name("Boolean").description("boolean desc").type(PropertyType.BOOLEAN).build(),
        PropertyFieldDefinition.build("list").name("List").description("list desc").type(PropertyType.SINGLE_SELECT_LIST).options("one", "two").build())
      .build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
    Definition definition = result.getDefinitions(0);
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
  public void return_global_settings_definitions() {
    logIn();
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).hasSize(1);
  }

  @Test
  public void definitions_are_ordered_by_category_then_index_then_name_case_insensitive() {
    logIn();
    propertyDefinitions.addComponent(PropertyDefinition.builder("sonar.prop.11").category("cat-1").index(1).name("prop 1").build());
    propertyDefinitions.addComponent(PropertyDefinition.builder("sonar.prop.12").category("cat-1").index(2).name("prop 2").build());
    propertyDefinitions.addComponent(PropertyDefinition.builder("sonar.prop.13").category("CAT-1").index(1).name("prop 3").build());
    propertyDefinitions.addComponent(PropertyDefinition.builder("sonar.prop.41").category("cat-0").index(25).name("prop 1").build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).extracting(Definition::getKey)
      .containsExactly("sonar.prop.41", "sonar.prop.11", "sonar.prop.13", "sonar.prop.12");
  }

  @Test
  public void return_project_settings_def_by_project_key() {
    logInAsProjectUser();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .onQualifiers(PROJECT)
      .build());

    ListDefinitionsWsResponse result = executeRequest(project.getKey());

    assertThat(result.getDefinitionsList()).hasSize(1);
  }

  @Test
  public void return_only_global_properties_when_no_component_parameter() {
    logInAsProjectUser();
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("global").build(),
      PropertyDefinition.builder("global-and-project").onQualifiers(PROJECT).build(),
      PropertyDefinition.builder("only-on-project").onlyOnQualifiers(PROJECT).build()));

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).extracting("key").containsOnly("global", "global-and-project");
  }

  @Test
  public void return_only_properties_available_for_component_qualifier() {
    logInAsProjectUser();
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("global").build(),
      PropertyDefinition.builder("global-and-project").onQualifiers(PROJECT).build(),
      PropertyDefinition.builder("only-on-project").onlyOnQualifiers(PROJECT).build()));

    ListDefinitionsWsResponse result = executeRequest(project.getKey());

    assertThat(result.getDefinitionsList()).extracting("key").containsOnly("global-and-project", "only-on-project");
  }

  @Test
  public void does_not_return_hidden_properties() {
    logInAsAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").hidden().build());

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).isEmpty();
  }

  @Test
  public void does_not_returned_secured_and_license_settings_when_not_authenticated() {
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build()));

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).extracting(Definition::getKey).containsOnly("foo");
  }

  @Test
  public void return_secured_settings_when_not_authenticated_but_with_scan_permission() {
    userSession.anonymous().addPermission(GlobalPermission.SCAN);
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build()));

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).extracting(Definition::getKey).containsOnly("foo", "secret.secured");
  }

  @Test
  public void return_secured_settings_when_system_admin() {
    logInAsAdmin();
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build()));

    ListDefinitionsWsResponse result = executeRequest();

    assertThat(result.getDefinitionsList()).extracting(Definition::getKey).containsOnly("foo", "secret.secured");
  }

  @Test
  public void return_secured_settings_when_project_admin() {
    logInAsProjectAdmin();
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("foo").onQualifiers(PROJECT).build(),
      PropertyDefinition.builder("secret.secured").onQualifiers(PROJECT).build()));

    ListDefinitionsWsResponse result = executeRequest(project.getKey());

    assertThat(result.getDefinitionsList()).extracting(Definition::getKey).containsOnly("foo", "secret.secured");
  }

  @Test
  public void fail_when_user_has_not_project_browse_permission() {
    userSession.logIn("project-admin").addProjectPermission(UserRole.CODEVIEWER, project);
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build());

    assertThatThrownBy(() -> executeRequest(project.getKey()))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_component_not_found() {
    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("component", "unknown")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'unknown' not found");
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).extracting(Param::key).containsExactlyInAnyOrder("component");
  }

  @Test
  public void test_example_json_response() {
    logInAsProjectAdmin();
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
    return executeRequest(null);
  }

  private ListDefinitionsWsResponse executeRequest(@Nullable String key) {
    TestRequest request = ws.newRequest();
    if (key != null) {
      request.setParam("component", key);
    }
    return request.executeProtobuf(ListDefinitionsWsResponse.class);
  }

  private void logIn() {
    userSession.logIn();
  }

  private void logInAsProjectUser() {
    userSession.logIn().addProjectPermission(UserRole.USER, project);
  }

  private void logInAsAdmin() {
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
  }

  private void logInAsProjectAdmin() {
    userSession.logIn()
      .addProjectPermission(UserRole.ADMIN, project)
      .addProjectPermission(UserRole.USER, project);
  }

}
