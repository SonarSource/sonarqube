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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
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
import org.sonar.db.property.PropertyDbTester;
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
  PropertyDbTester propertyDb = new PropertyDbTester(db);
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
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));

    ValuesWsResponse result = executeRequestForGlobalProperties("foo");

    assertThat(result.getSettingsList()).hasSize(1);
    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo("foo");
    assertThat(value.getValue()).isEqualTo("one");
    assertThat(value.hasValues()).isFalse();
    assertThat(value.hasFieldValues()).isFalse();
    assertThat(value.getInherited()).isFalse();
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
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("global").setValue("three,four"));

    ValuesWsResponse result = executeRequestForGlobalProperties("default", "global");
    assertThat(result.getSettingsList()).hasSize(2);

    Settings.Setting foo = result.getSettings(0);
    assertThat(foo.getKey()).isEqualTo("default");
    assertThat(foo.hasValue()).isFalse();
    assertThat(foo.getValues().getValuesList()).containsOnly("one", "two");
    assertThat(foo.hasFieldValues()).isFalse();

    Settings.Setting bar = result.getSettings(1);
    assertThat(bar.getKey()).isEqualTo("global");
    assertThat(bar.hasValue()).isFalse();
    assertThat(bar.getValues().getValuesList()).containsOnly("three", "four");
    assertThat(bar.hasFieldValues()).isFalse();
  }

  @Test
  public void return_multi_value_with_coma() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("global").multiValues(true).build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("global").setValue("three,four%2Cfive"));

    ValuesWsResponse result = executeRequestForGlobalProperties("global");

    assertThat(result.getSettingsList()).hasSize(1);
    Settings.Setting setting = result.getSettings(0);
    assertThat(setting.getKey()).isEqualTo("global");
    assertThat(setting.getValues().getValuesList()).containsOnly("three", "four,five");
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
    propertyDb.insertPropertySet("foo", null, ImmutableMap.of("key", "key1", "size", "size1"), ImmutableMap.of("key", "key2"));

    ValuesWsResponse result = executeRequestForGlobalProperties("foo");

    assertThat(result.getSettingsList()).hasSize(1);
    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo("foo");
    assertThat(value.hasValue()).isFalse();
    assertThat(value.hasValues()).isFalse();
    assertFieldValues(value, ImmutableMap.of("key", "key1", "size", "size1"), ImmutableMap.of("key", "key2"));
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
    propertyDb.insertPropertySet("foo", project, ImmutableMap.of("key", "key1", "size", "size1"), ImmutableMap.of("key", "key2"));

    ValuesWsResponse result = executeRequestForProjectProperties("foo");

    assertThat(result.getSettingsList()).hasSize(1);
    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo("foo");
    assertThat(value.hasValue()).isFalse();
    assertThat(value.hasValues()).isFalse();
    assertFieldValues(value, ImmutableMap.of("key", "key1", "size", "size1"), ImmutableMap.of("key", "key2"));
  }

  @Test
  public void return_default_values() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .defaultValue("default")
      .build());

    ValuesWsResponse result = executeRequestForGlobalProperties("foo");

    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "foo", "default", true);
  }

  @Test
  public void return_global_values() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    propertyDb.insertProperties(
      // The property is overriding default value
      newGlobalPropertyDto().setKey("property").setValue("one"));

    ValuesWsResponse result = executeRequestForGlobalProperties("property");

    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "property", "one", false);
  }

  @Test
  public void return_project_values() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("property").setValue("one"),
      // The property is overriding global value
      newComponentPropertyDto(project).setKey("property").setValue("two"));

    ValuesWsResponse result = executeRequestForProjectProperties("property");

    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "property", "two", false);
  }

  @Test
  public void return_is_inherited_to_true_when_property_is_defined_only_at_global_level() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    // The property is not defined on project
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("property").setValue("one"));

    ValuesWsResponse result = executeRequestForProjectProperties("property");

    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "property", "one", true);
  }

  @Test
  public void return_values_even_if_no_property_definition() throws Exception {
    setUserAsSystemAdmin();
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("globalPropertyWithoutDefinition").setValue("value"));

    ValuesWsResponse result = executeRequestForGlobalProperties("globalPropertyWithoutDefinition");

    Settings.Setting globalPropertyWithoutDefinitionValue = result.getSettings(0);
    assertThat(globalPropertyWithoutDefinitionValue.getKey()).isEqualTo("globalPropertyWithoutDefinition");
    assertThat(globalPropertyWithoutDefinitionValue.getValue()).isEqualTo("value");
    assertThat(globalPropertyWithoutDefinitionValue.getInherited()).isFalse();
  }

  @Test
  public void return_empty_when_property_def_exists_but_no_value() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .build());

    ValuesWsResponse result = executeRequestForGlobalProperties("foo");

    assertThat(result.getSettingsList()).isEmpty();
  }

  @Test
  public void return_nothing_when_unknown_keys() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .defaultValue("default")
      .build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("bar").setValue(""));

    ValuesWsResponse result = executeRequestForGlobalProperties("unknown");

    assertThat(result.getSettingsList()).isEmpty();
  }

  @Test
  public void return_module_values() throws Exception {
    setUserAsSystemAdmin();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    propertyDefinitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("property").setValue("one"),
      // The property is overriding global value
      newComponentPropertyDto(module).setKey("property").setValue("two"));

    ValuesWsResponse result = executeRequestForComponentProperties(module, "property");

    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "property", "two", false);
  }

  @Test
  public void return_inherited_values_on_module() throws Exception {
    setUserAsSystemAdmin();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("defaultProperty").defaultValue("default").build(),
      PropertyDefinition.builder("globalProperty").build(),
      PropertyDefinition.builder("projectProperty").build(),
      PropertyDefinition.builder("moduleProperty").build()));
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("globalProperty").setValue("global"),
      newComponentPropertyDto(project).setKey("projectProperty").setValue("project"),
      newComponentPropertyDto(module).setKey("moduleProperty").setValue("module"));

    ValuesWsResponse result = executeRequestForComponentProperties(module, "defaultProperty", "globalProperty", "projectProperty", "moduleProperty");

    assertThat(result.getSettingsList()).hasSize(4);
    assertSetting(result.getSettings(0), "defaultProperty", "default", true);
    assertSetting(result.getSettings(1), "globalProperty", "global", true);
    assertSetting(result.getSettings(2), "projectProperty", "project", true);
    assertSetting(result.getSettings(3), "moduleProperty", "module", false);
  }

  @Test
  public void return_inherited_values_on_global_setting() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("defaultProperty").defaultValue("default").build(),
      PropertyDefinition.builder("globalProperty").build()));
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("globalProperty").setValue("global"));

    ValuesWsResponse result = executeRequestForGlobalProperties("defaultProperty", "globalProperty");

    assertThat(result.getSettingsList()).hasSize(2);
    assertSetting(result.getSettings(0), "defaultProperty", "default", true);
    assertSetting(result.getSettings(1), "globalProperty", "global", false);
  }

  @Test
  public void return_parent_value() throws Exception {
    setUserAsSystemAdmin();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    ComponentDto subModule = componentDb.insertComponent(newModuleDto(module));
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("foo").defaultValue("default").build()));
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("foo").setValue("global"),
      newComponentPropertyDto(project).setKey("foo").setValue("project"),
      newComponentPropertyDto(module).setKey("foo").setValue("module"));

    assertParentValue(executeRequestForComponentProperties(subModule, "foo").getSettings(0), "module");
    assertParentValue(executeRequestForComponentProperties(module, "foo").getSettings(0), "project");
    assertParentValue(executeRequestForComponentProperties(project, "foo").getSettings(0), "global");
    assertParentValue(executeRequestForGlobalProperties("foo").getSettings(0), "default");
  }

  @Test
  public void return_parent_values() throws Exception {
    setUserAsSystemAdmin();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    ComponentDto subModule = componentDb.insertComponent(newModuleDto(module));
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("foo").defaultValue("default1,default2").multiValues(true).build()));
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("foo").setValue("global1,global2"),
      newComponentPropertyDto(project).setKey("foo").setValue("project1,project2"),
      newComponentPropertyDto(module).setKey("foo").setValue("module1,module2"));

    assertParentValues(executeRequestForComponentProperties(subModule, "foo").getSettings(0), "module1", "module2");
    assertParentValues(executeRequestForComponentProperties(module, "foo").getSettings(0), "project1", "project2");
    assertParentValues(executeRequestForComponentProperties(project, "foo").getSettings(0), "global1", "global2");
    assertParentValues(executeRequestForGlobalProperties("foo").getSettings(0), "default1", "default2");
  }

  @Test
  public void return_parent_field_values() throws Exception {
    setUserAsSystemAdmin();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    ComponentDto subModule = componentDb.insertComponent(newModuleDto(module));
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("size").name("Size").build()))
      .build());
    propertyDb.insertPropertySet("foo", null, ImmutableMap.of("key", "keyG1", "size", "sizeG1"));
    propertyDb.insertPropertySet("foo", project, ImmutableMap.of("key", "keyP1", "size", "sizeP1"));
    propertyDb.insertPropertySet("foo", module, ImmutableMap.of("key", "keyM1", "size", "sizeM1"));

    assertParentFieldValues(executeRequestForComponentProperties(subModule, "foo").getSettings(0), ImmutableMap.of("key", "keyM1", "size", "sizeM1"));
    assertParentFieldValues(executeRequestForComponentProperties(module, "foo").getSettings(0), ImmutableMap.of("key", "keyP1", "size", "sizeP1"));
    assertParentFieldValues(executeRequestForComponentProperties(project, "foo").getSettings(0), ImmutableMap.of("key", "keyG1", "size", "sizeG1"));
    assertParentFieldValues(executeRequestForGlobalProperties("foo").getSettings(0));
  }

  @Test
  public void return_no_parent_value() throws Exception {
    setUserAsSystemAdmin();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    ComponentDto subModule = componentDb.insertComponent(newModuleDto(module));
    propertyDefinitions.addComponents(asList(
      PropertyDefinition.builder("simple").build(),
      PropertyDefinition.builder("multi").multiValues(true).build(),
      PropertyDefinition.builder("set")
        .type(PropertyType.PROPERTY_SET)
        .fields(asList(
          PropertyFieldDefinition.build("key").name("Key").build(),
          PropertyFieldDefinition.build("size").name("Size").build()))
        .build()));
    propertyDb.insertProperties(
      newComponentPropertyDto(module).setKey("simple").setValue("module"),
      newComponentPropertyDto(module).setKey("multi").setValue("module1,module2"));
    propertyDb.insertPropertySet("set", module, ImmutableMap.of("key", "keyM1", "size", "sizeM1"));

    assertParentValue(executeRequestForComponentProperties(subModule, "simple").getSettings(0), null);
    assertParentValues(executeRequestForComponentProperties(subModule, "multi").getSettings(0));
    assertParentFieldValues(executeRequestForComponentProperties(subModule, "set").getSettings(0));
  }

  @Test
  public void return_parent_value_when_no_definition() throws Exception {
    setUserAsSystemAdmin();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    propertyDb.insertProperties(
      newGlobalPropertyDto().setKey("foo").setValue("global"),
      newComponentPropertyDto(project).setKey("foo").setValue("project"));

    assertParentValue(executeRequestForComponentProperties(module, "foo").getSettings(0), "project");
    assertParentValue(executeRequestForComponentProperties(project, "foo").getSettings(0), "global");
    assertParentValue(executeRequestForGlobalProperties("foo").getSettings(0), null);
  }

  @Test
  public void return_value_of_deprecated_key() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .deprecatedKey("deprecated")
      .build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));

    ValuesWsResponse result = executeRequestForGlobalProperties("deprecated");

    assertThat(result.getSettingsList()).hasSize(1);
    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo("deprecated");
    assertThat(value.getValue()).isEqualTo("one");
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
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("sonar.autogenerated").setValue("val1,val2,val3"));
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("sonar.demo")
      .type(PropertyType.PROPERTY_SET)
      .fields(PropertyFieldDefinition.build("text").name("Text").build(),
        PropertyFieldDefinition.build("boolean").name("Boolean").build())
      .build());
    propertyDb.insertPropertySet("sonar.demo", null, ImmutableMap.of("text", "foo", "boolean", "true"), ImmutableMap.of("text", "bar", "boolean", "false"));

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

    executeRequest(project.uuid(), project.key());
  }

  @Test
  public void fail_when_not_system_admin() throws Exception {
    userSession.login("not-admin").setGlobalPermissions(DASHBOARD_SHARING);
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build());

    expectedException.expect(ForbiddenException.class);

    executeRequestForGlobalProperties("foo");
  }

  @Test
  public void fail_when_not_project_admin() throws Exception {
    userSession.login("project-admin").addProjectUuidPermissions(USER, project.uuid());
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build());

    expectedException.expect(ForbiddenException.class);

    executeRequest(project.uuid(), null, "foo");
  }

  @Test
  public void fail_when_deprecated_key_and_new_key_are_used() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .deprecatedKey("deprecated")
      .build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'foo' and 'deprecated' cannot be used at the same time as they refer to the same setting");

    executeRequestForGlobalProperties("foo", "deprecated");
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(3);
  }

  private ValuesWsResponse executeRequestForComponentProperties(ComponentDto componentDto, String... keys) {
    return executeRequest(componentDto.uuid(), null, keys);
  }

  private ValuesWsResponse executeRequestForProjectProperties(String... keys) {
    return executeRequest(project.uuid(), null, keys);
  }

  private ValuesWsResponse executeRequestForGlobalProperties(String... keys) {
    return executeRequest(null, null, keys);
  }

  private ValuesWsResponse executeRequest(@Nullable String id, @Nullable String key, String... keys) {
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

  private void assertSetting(Settings.Setting setting, String expectedKey, String expectedValue, boolean expectedInherited) {
    assertThat(setting.getKey()).isEqualTo(expectedKey);
    assertThat(setting.getValue()).isEqualTo(expectedValue);
    assertThat(setting.getInherited()).isEqualTo(expectedInherited);
  }

  private void assertFieldValues(Settings.Setting setting, Map<String, String>... fieldsValues) {
    assertThat(setting.getFieldValues().getFieldValuesList()).hasSize(fieldsValues.length);
    int index = 0;
    for (Settings.FieldValues.Value fieldsValue : setting.getFieldValues().getFieldValuesList()) {
      assertThat(fieldsValue.getValue()).isEqualTo(fieldsValues[index]);
      index++;
    }
  }

  private void assertParentValue(Settings.Setting setting, @Nullable String parentValue) {
    if (parentValue == null) {
      assertThat(setting.hasParentValue()).isFalse();
    } else {
      assertThat(setting.getParentValue()).isEqualTo(parentValue);
    }
  }

  private void assertParentValues(Settings.Setting setting, String... parentValues) {
    if (parentValues.length == 0) {
      assertThat(setting.hasParentValues()).isFalse();
    } else {
      assertThat(setting.getParentValues().getValuesList()).containsOnly(parentValues);
    }
  }

  private void assertParentFieldValues(Settings.Setting setting, Map<String, String>... fieldsValues) {
    if (fieldsValues.length == 0) {
      assertThat(setting.hasParentFieldValues()).isFalse();
    } else {
      assertThat(setting.getParentFieldValues().getFieldValuesList()).hasSize(fieldsValues.length);
      int index = 0;
      for (Settings.FieldValues.Value fieldsValue : setting.getParentFieldValues().getFieldValuesList()) {
        assertThat(fieldsValue.getValue()).isEqualTo(fieldsValues[index]);
        index++;
      }
    }
  }

}
