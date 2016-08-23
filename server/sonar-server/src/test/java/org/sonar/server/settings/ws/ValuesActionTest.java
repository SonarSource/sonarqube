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

package org.sonar.server.settings.ws;

import com.google.common.base.Joiner;
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
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.Settings.ValuesWsResponse;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.entry;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.DASHBOARD_SHARING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonarqube.ws.MediaTypes.JSON;

public class ValuesActionTest {

  static Joiner COMMA_JOINER = Joiner.on(",");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();
  ComponentDbTester componentDb = new ComponentDbTester(db);
  PropertyDefinitions propertyDefinitions = new PropertyDefinitions();
  SettingsFinder settingsFinder = new SettingsFinder(dbClient, propertyDefinitions);

  ComponentDto project;

  WsActionTester ws = new WsActionTester(new ValuesAction(dbClient, new ComponentFinder(dbClient), userSession, propertyDefinitions, settingsFinder));

  @Before
  public void setUp() throws Exception {
    project = componentDb.insertComponent(newProjectDto());
  }

  @Test
  public void return_simple_value() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .build());
    insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));

    ValuesWsResponse result = newRequestForGlobalProperties("foo");
    assertThat(result.getSettingsList()).hasSize(1);

    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo("foo");
    assertThat(value.getValue()).isEqualTo("one");
    assertThat(value.hasValues()).isFalse();
    assertThat(value.hasFieldsValues()).isFalse();
    assertThat(value.getDefault()).isFalse();
  }

  @Test
  public void return_multi_values() throws Exception {
    setUserAsSystemAdmin();

    // Property never defined, default value is returned
    propertyDefinitions.addComponent(PropertyDefinition.builder("default")
      .multiValues(true)
      .defaultValue("one,two")
      .build());

    // Property defined at global level
    propertyDefinitions.addComponent(PropertyDefinition.builder("global")
      .multiValues(true)
      .build());
    insertProperties(newGlobalPropertyDto().setKey("global").setValue("three,four"));

    ValuesWsResponse result = newRequestForGlobalProperties("default", "global");
    assertThat(result.getSettingsList()).hasSize(2);

    Settings.Setting foo = result.getSettings(0);
    assertThat(foo.getKey()).isEqualTo("default");
    assertThat(foo.hasValue()).isFalse();
    assertThat(foo.getValues().getValuesList()).containsOnly("one", "two");
    assertThat(foo.hasFieldsValues()).isFalse();

    Settings.Setting bar = result.getSettings(1);
    assertThat(bar.getKey()).isEqualTo("global");
    assertThat(bar.hasValue()).isFalse();
    assertThat(bar.getValues().getValuesList()).containsOnly("three", "four");
    assertThat(bar.hasFieldsValues()).isFalse();
  }

  @Test
  public void return_property_set() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("size").name("Size").build()))
      .build());
    insertProperties(
      newGlobalPropertyDto().setKey("foo").setValue("1,2"),
      newGlobalPropertyDto().setKey("foo.1.key").setValue("key1"),
      newGlobalPropertyDto().setKey("foo.1.size").setValue("size1"),
      newGlobalPropertyDto().setKey("foo.2.key").setValue("key2"));

    ValuesWsResponse result = newRequestForGlobalProperties("foo");
    assertThat(result.getSettingsList()).hasSize(1);

    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo("foo");
    assertThat(value.hasValue()).isFalse();
    assertThat(value.hasValues()).isFalse();

    assertThat(value.getFieldsValues().getFieldsValuesList()).hasSize(2);
    assertThat(value.getFieldsValues().getFieldsValuesList().get(0).getValue()).containsOnly(entry("key", "key1"), entry("size", "size1"));
    assertThat(value.getFieldsValues().getFieldsValuesList().get(1).getValue()).containsOnly(entry("key", "key2"));
  }

  @Test
  public void return_property_set_for_component() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("size").name("Size").build()))
      .build());
    insertProperties(
      newComponentPropertyDto(project).setKey("foo").setValue("1,2"),
      newComponentPropertyDto(project).setKey("foo.1.key").setValue("key1"),
      newComponentPropertyDto(project).setKey("foo.1.size").setValue("size1"),
      newComponentPropertyDto(project).setKey("foo.2.key").setValue("key2"));

    ValuesWsResponse result = newRequestForProjectProperties("foo");
    assertThat(result.getSettingsList()).hasSize(1);

    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo("foo");
    assertThat(value.hasValue()).isFalse();
    assertThat(value.hasValues()).isFalse();

    assertThat(value.getFieldsValues().getFieldsValuesList()).hasSize(2);
    assertThat(value.getFieldsValues().getFieldsValuesList().get(0).getValue()).containsOnly(entry("key", "key1"), entry("size", "size1"));
    assertThat(value.getFieldsValues().getFieldsValuesList().get(1).getValue()).containsOnly(entry("key", "key2"));
  }

  @Test
  public void return_default_values() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .defaultValue("default")
      .build());

    ValuesWsResponse result = newRequestForGlobalProperties("foo");
    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "foo", "default", true, false);
  }

  @Test
  public void return_global_values() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    insertProperties(
      // The property is overriding default value
      newGlobalPropertyDto().setKey("property").setValue("one"));

    ValuesWsResponse result = newRequestForGlobalProperties("property");
    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "property", "one", false, false);
  }

  @Test
  public void return_project_values() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    insertProperties(
      newGlobalPropertyDto().setKey("property").setValue("one"),
      // The property is overriding global value
      newComponentPropertyDto(project).setKey("property").setValue("two"));

    ValuesWsResponse result = newRequestForProjectProperties("property");
    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "property", "two", false, false);
  }

  @Test
  public void return_is_inherited_to_true_when_property_is_defined_only_at_global_level() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    // The property is not defined on project
    insertProperties(newGlobalPropertyDto().setKey("property").setValue("one"));

    ValuesWsResponse result = newRequestForProjectProperties("property");
    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "property", "one", false, true);
  }

  @Test
  public void return_values_even_if_no_property_definition() throws Exception {
    setUserAsSystemAdmin();
    insertProperties(newGlobalPropertyDto().setKey("globalPropertyWithoutDefinition").setValue("value"));

    ValuesWsResponse result = newRequestForGlobalProperties("globalPropertyWithoutDefinition");
    Settings.Setting globalPropertyWithoutDefinitionValue = result.getSettings(0);
    assertThat(globalPropertyWithoutDefinitionValue.getKey()).isEqualTo("globalPropertyWithoutDefinition");
    assertThat(globalPropertyWithoutDefinitionValue.getValue()).isEqualTo("value");
    assertThat(globalPropertyWithoutDefinitionValue.getDefault()).isFalse();
  }

  @Test
  public void return_empty_when_property_def_exists_but_no_value() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .build());

    ValuesWsResponse result = newRequestForGlobalProperties("foo");
    assertThat(result.getSettingsList()).isEmpty();
  }

  @Test
  public void does_return_nothing_when_unknown_keys() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .defaultValue("default")
      .build());
    insertProperties(newGlobalPropertyDto().setKey("bar").setValue(""));

    ValuesWsResponse result = newRequestForGlobalProperties("unknown");
    assertThat(result.getSettingsList()).isEmpty();
  }

  @Test
  public void return_module_values() throws Exception {
    setUserAsSystemAdmin();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));

    propertyDefinitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    insertProperties(
      newGlobalPropertyDto().setKey("property").setValue("one"),
      // The property is overriding global value
      newComponentPropertyDto(module).setKey("property").setValue("two"));

    ValuesWsResponse result = newRequestForComponentProperties(module, "property");
    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "property", "two", false, false);
  }

  @Test
  public void return_inherited_values_on_sub_module() throws Exception {
    setUserAsSystemAdmin();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    ComponentDto subModule = componentDb.insertComponent(newModuleDto(module));

    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("defaultProperty").defaultValue("default").build(),
      PropertyDefinition.builder("globalProperty").build(),
      PropertyDefinition.builder("projectProperty").build(),
      PropertyDefinition.builder("moduleProperty").build()));
    insertProperties(
      newGlobalPropertyDto().setKey("globalProperty").setValue("global"),
      newComponentPropertyDto(project).setKey("projectProperty").setValue("project"),
      newComponentPropertyDto(module).setKey("moduleProperty").setValue("module"));

    ValuesWsResponse result = newRequestForComponentProperties(subModule, "defaultProperty", "globalProperty", "projectProperty", "moduleProperty");
    assertThat(result.getSettingsList()).hasSize(4);
    assertSetting(result.getSettings(0), "defaultProperty", "default", true, false);
    assertSetting(result.getSettings(1), "globalProperty", "global", false, true);
    assertSetting(result.getSettings(2), "projectProperty", "project", false, true);
    assertSetting(result.getSettings(3), "moduleProperty", "module", false, true);
  }

  @Test
  public void test_example_json_response() {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("sonar.test.jira")
      .defaultValue("abc")
      .build());
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("sonar.autogenerated")
      .multiValues(true)
      .build());
    insertProperties(newGlobalPropertyDto().setKey("sonar.autogenerated").setValue("val1,val2,val3"));

    propertyDefinitions.addComponent(PropertyDefinition
      .builder("sonar.demo")
      .type(PropertyType.PROPERTY_SET)
      .fields(PropertyFieldDefinition.build("text").name("Text").build(),
        PropertyFieldDefinition.build("boolean").name("Boolean").build())
      .build());
    insertProperties(
      newGlobalPropertyDto().setKey("sonar.demo").setValue("1,2"),
      newGlobalPropertyDto().setKey("sonar.demo.1.text").setValue("foo"),
      newGlobalPropertyDto().setKey("sonar.demo.1.boolean").setValue("true"),
      newGlobalPropertyDto().setKey("sonar.demo.2.text").setValue("bar"),
      newGlobalPropertyDto().setKey("sonar.demo.2.boolean").setValue("false"));

    String result = ws.newRequest()
      .setParam("keys", "sonar.test.jira,sonar.autogenerated,sonar.demo")
      .setMediaType(JSON)
      .execute()
      .getInput();
    JsonAssert.assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(result);
  }

  @Test
  public void fail_when_id_and_key_are_set() throws Exception {
    setUserAsProjectAdmin();

    expectedException.expect(IllegalArgumentException.class);
    newRequest(project.uuid(), project.key());
  }

  @Test
  public void fail_when_not_system_admin() throws Exception {
    userSession.login("not-admin").setGlobalPermissions(DASHBOARD_SHARING);
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build());

    expectedException.expect(ForbiddenException.class);
    newRequestForGlobalProperties("foo");
  }

  @Test
  public void fail_when_not_project_admin() throws Exception {
    userSession.login("project-admin").addProjectUuidPermissions(USER, project.uuid());
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build());

    expectedException.expect(ForbiddenException.class);
    newRequest(project.uuid(), null, "foo");
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

  private ValuesWsResponse newRequestForComponentProperties(ComponentDto componentDto, String... keys) {
    return newRequest(componentDto.uuid(), null, keys);
  }

  private ValuesWsResponse newRequestForProjectProperties(String... keys) {
    return newRequest(project.uuid(), null, keys);
  }

  private ValuesWsResponse newRequestForGlobalProperties(String... keys) {
    return newRequest(null, null, keys);
  }

  private ValuesWsResponse newRequest(@Nullable String id, @Nullable String key, String... keys) {
    TestRequest request = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("keys", COMMA_JOINER.join(keys));
    if (id != null) {
      request.setParam("componentId", id);
    }
    if (key != null) {
      request.setParam("componentKey", key);
    }
    try {
      return ValuesWsResponse.parseFrom(request.execute().getInputStream());
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

  private void insertProperties(PropertyDto... properties) {
    for (PropertyDto propertyDto : properties) {
      dbClient.propertiesDao().insertProperty(dbSession, propertyDto);
    }
    dbSession.commit();
  }

  private void assertSetting(Settings.Setting setting, String expectedKey, String expectedValue, boolean expectedDefault, boolean expectedInherited) {
    assertThat(setting.getKey()).isEqualTo(expectedKey);
    assertThat(setting.getValue()).isEqualTo(expectedValue);
    assertThat(setting.getDefault()).isEqualTo(expectedDefault);
    assertThat(setting.getInherited()).isEqualTo(expectedInherited);
  }

}
