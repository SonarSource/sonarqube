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
package org.sonar.server.property.ws;

import com.google.common.collect.ImmutableMap;
import java.net.URL;
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
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.PropertyType.LICENSE;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.JSON;

public class IndexActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private PropertyDbTester propertyDb = new PropertyDbTester(db);
  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private PropertyDefinitions definitions = new PropertyDefinitions();

  private ComponentDto project;

  private WsActionTester ws = new WsActionTester(new IndexAction(dbClient, userSession, definitions));

  @Before
  public void setUp() throws Exception {
    project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()));
  }

  @Test
  public void return_simple_value() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("foo").build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));

    executeAndVerify(null, null, "return_simple_value.json");
  }

  @Test
  public void return_multi_values() {
    logIn();
    // Property never defined, default value is returned
    definitions.addComponent(PropertyDefinition.builder("default")
      .multiValues(true)
      .defaultValue("one,two")
      .build());
    // Property defined at global level
    definitions.addComponent(PropertyDefinition.builder("global")
      .multiValues(true)
      .build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("global").setValue("three,four"));

    executeAndVerify(null, null, "return_multi_values.json");
  }

  @Test
  public void return_multi_value_with_coma() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("global").multiValues(true).build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("global").setValue("three,four%2Cfive"));

    executeAndVerify(null, null, "return_multi_value_with_coma.json");
  }

  @Test
  public void return_property_set() {
    logIn();
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("size").name("Size").build()))
      .build());
    propertyDb.insertPropertySet("foo", null, ImmutableMap.of("key", "key1", "size", "size1"), ImmutableMap.of("key", "key2"));

    executeAndVerify(null, null, "return_property_set.json");
  }

  @Test
  public void return_default_values() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("foo").defaultValue("default").build());

    executeAndVerify(null, null, "return_default_values.json");
  }

  @Test
  public void return_global_values() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    propertyDb.insertProperties(
      // The property is overriding default value
      newGlobalPropertyDto().setKey("property").setValue("one"));

    executeAndVerify(null, null, "return_global_values.json");
  }

  @Test
  public void return_project_values() {
    logInAsProjectUser();
    definitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("property").setValue("one"),
      // The property is overriding global value
      newComponentPropertyDto(project).setKey("property").setValue("two"));

    executeAndVerify(project.getDbKey(), null, "return_project_values.json");
  }

  @Test
  public void return_global_values_when_project_does_not_exist() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("property").setValue("one"));

    executeAndVerify("unknown", null, "return_global_values.json");
  }

  @Test
  public void return_values_even_if_no_property_definition() {
    logIn();
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("globalPropertyWithoutDefinition").setValue("value"));

    executeAndVerify(null, null, "return_values_even_if_no_property_definition.json");
  }

  @Test
  public void return_empty_when_property_def_exists_but_no_value() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("foo").build());

    executeAndVerify(null, null, "empty.json");
  }

  @Test
  public void return_nothing_when_unknown_key() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("foo").defaultValue("default").build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("bar").setValue(""));

    executeAndVerify(null, "unknown", "empty.json");
  }

  @Test
  public void return_module_values() {
    logInAsProjectUser();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    definitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("property").setValue("one"),
      // The property is overriding global value
      newComponentPropertyDto(module).setKey("property").setValue("two"));

    executeAndVerify(module.getDbKey(), "property", "return_module_values.json");
  }

  @Test
  public void return_inherited_values_on_module() {
    logInAsProjectUser();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    definitions.addComponents(asList(
      PropertyDefinition.builder("defaultProperty").defaultValue("default").build(),
      PropertyDefinition.builder("globalProperty").build(),
      PropertyDefinition.builder("projectProperty").build(),
      PropertyDefinition.builder("moduleProperty").build()));
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("globalProperty").setValue("global"),
      newComponentPropertyDto(project).setKey("projectProperty").setValue("project"),
      newComponentPropertyDto(module).setKey("moduleProperty").setValue("module"));

    executeAndVerify(module.getDbKey(), null, "return_inherited_values_on_module.json");
  }

  @Test
  public void return_inherited_values_on_global_setting() {
    logIn();
    definitions.addComponents(asList(
      PropertyDefinition.builder("defaultProperty").defaultValue("default").build(),
      PropertyDefinition.builder("globalProperty").build()));
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("globalProperty").setValue("global"));

    executeAndVerify(null, null, "return_inherited_values_on_global_setting.json");
  }

  @Test
  public void does_not_return_value_of_deprecated_key() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("foo").deprecatedKey("deprecated").build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));

    executeAndVerify(null, "deprecated", "empty.json");
  }

  @Test
  public void does_not_returned_secured_settings_when_not_authenticated() {
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build()));
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("foo").setValue("one"),
      newGlobalPropertyDto().setKey("secret.secured").setValue("password"));

    executeAndVerify(null, null, "does_not_returned_secured_and_license_settings_when_not_authenticated.json");
  }

  @Test
  public void return_secured_settings_when_system_admin() {
    logInAsSystemAdministrator();
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build()));
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("foo").setValue("one"),
      newGlobalPropertyDto().setKey("secret.secured").setValue("password"));

    executeAndVerify(null, null, "return_secured_and_license_settings_when_system_admin.json");
  }

  @Test
  public void return_secured_and_license_settings_when_project_admin() {
    logInAsProjectAdmin();
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build(),
      PropertyDefinition.builder("plugin.license.secured").type(LICENSE).build()));
    propertyDb.insertProperties(
      newComponentPropertyDto(project).setKey("foo").setValue("one"),
      newComponentPropertyDto(project).setKey("secret.secured").setValue("password"),
      newComponentPropertyDto(project).setKey("plugin.license.secured").setValue("ABCD"),
      newComponentPropertyDto(project).setKey("plugin.licenseHash.secured").setValue("987654321"));

    executeAndVerify(project.getDbKey(), null, "return_secured_and_license_settings_when_project_admin.json");
  }

  @Test
  public void return_secured_settings_in_property_set_when_system_admin() {
    logInAsSystemAdministrator();
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("secret.secured").name("Secured").build()))
      .build());
    propertyDb.insertPropertySet("foo", null,
      ImmutableMap.of("key", "key1", "secret.secured", "123456"));

    executeAndVerify(null, null, "return_secured_and_license_settings_in_property_set_when_system_admin.json");
  }

  @Test
  public void return_all_settings_when_no_component_and_no_key() {
    logInAsSystemAdministrator();
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build(),
      PropertyDefinition.builder("plugin.license.secured").type(LICENSE).build()));
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("foo").setValue("one"),
      newGlobalPropertyDto().setKey("secret.secured").setValue("password"),
      newGlobalPropertyDto().setKey("plugin.license.secured").setValue("ABCD"),
      newGlobalPropertyDto().setKey("not_defined").setValue("ABCD"));

    executeAndVerify(null, null, "return_all_settings_when_no_component_and_no_key.json");
  }

  @Test
  public void return_all_project_settings_when_component_and_no_key() {
    logInAsProjectAdmin();
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build(),
      PropertyDefinition.builder("plugin.license.secured").type(LICENSE).build()));
    propertyDb.insertProperties(
      newComponentPropertyDto(project).setKey("foo").setValue("one"),
      newComponentPropertyDto(project).setKey("secret.secured").setValue("password"),
      newComponentPropertyDto(project).setKey("plugin.license.secured").setValue("ABCD"),
      newComponentPropertyDto(project).setKey("not_defined").setValue("ABCD"),
      newGlobalPropertyDto().setKey("global_not_defined").setValue("ABCD"));

    executeAndVerify(project.getDbKey(), null, "return_all_project_settings_when_component_and_no_key.json");
  }

  @Test
  public void return_only_one_setting_when_key_is_provided() {
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("bar").build()));
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("foo").setValue("one"),
      newGlobalPropertyDto().setKey("bar").setValue("two"));

    executeAndVerify(project.getDbKey(), "foo", "return_only_one_setting_when_key_is_provided.json");
    executeAndVerify(project.getDbKey(), "unknown", "empty.json");
  }

  @Test
  public void does_not_fail_when_user_has_not_project_browse_permission() {
    userSession.logIn("project-admin").addProjectPermission(CODEVIEWER, project);
    definitions.addComponent(PropertyDefinition.builder("foo").build());
    propertyDb.insertProperties(newComponentPropertyDto(project).setKey("foo").setValue("one"));

    executeAndVerify(project.getDbKey(), null, "does_not_fail_when_user_has_not_project_browse_permission.json");
  }

  @Test
  public void does_not_fail_when_format_is_set_to_json() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("foo").defaultValue("default").build());

    ws.newRequest().setParam("format", "json").execute();
  }

  @Test
  public void fail_when_format_is_set_to_xml() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("foo").defaultValue("default").build());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'format' (xml) must be one of: [json]");
    ws.newRequest().setParam("format", "xml").execute();
  }

  @Test
  public void test_example_json_response() {
    logIn();
    definitions.addComponent(PropertyDefinition
      .builder("sonar.test.jira")
      .defaultValue("abc")
      .build());
    definitions.addComponent(PropertyDefinition
      .builder("sonar.autogenerated")
      .multiValues(true)
      .build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("sonar.autogenerated").setValue("val1,val2,val3"));
    definitions.addComponent(PropertyDefinition
      .builder("sonar.demo")
      .type(PropertyType.PROPERTY_SET)
      .fields(PropertyFieldDefinition.build("text").name("Text").build(),
        PropertyFieldDefinition.build("boolean").name("Boolean").build())
      .build());
    propertyDb.insertPropertySet("sonar.demo", null, ImmutableMap.of("text", "foo", "boolean", "true"), ImmutableMap.of("text", "bar", "boolean", "false"));

    String result = ws.newRequest().setMediaType(JSON).execute().getInput();

    JsonAssert.assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(result);
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(3);
  }

  private void executeAndVerify(@Nullable String componentKey, @Nullable String key, String expectedFile) {
    TestRequest request = ws.newRequest().setMediaType(MediaTypes.JSON);
    if (key != null) {
      request.setParam("id", key);
    }
    if (componentKey != null) {
      request.setParam("resource", componentKey);
    }
    String result = request.execute().getInput();
    assertJson(result).isSimilarTo(resource(expectedFile));
  }

  private void logIn() {
    userSession.logIn();
  }

  private void logInAsProjectUser() {
    userSession.logIn().addProjectPermission(USER, project);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private void logInAsProjectAdmin() {
    userSession.logIn()
      .addProjectPermission(ADMIN, project)
      .addProjectPermission(USER, project);
  }

  protected static URL resource(String s) {
    Class<IndexActionTest> clazz = IndexActionTest.class;
    return clazz.getResource(clazz.getSimpleName() + "/" + s);
  }
}
