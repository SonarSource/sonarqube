/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.process.ProcessProperties;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.Settings.ValuesWsResponse;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.permission.ProjectPermission.ADMIN;
import static org.sonar.db.permission.ProjectPermission.CODEVIEWER;
import static org.sonar.db.permission.ProjectPermission.SCAN;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonarqube.ws.MediaTypes.JSON;
import static org.sonarqube.ws.Settings.Setting.ParentValueOneOfCase.PARENTVALUEONEOF_NOT_SET;

public class ValuesActionIT {

  private static final Joiner COMMA_JOINER = Joiner.on(",");

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final PropertyDefinitions definitions = new PropertyDefinitions(System2.INSTANCE);
  private final SettingsWsSupport support = new SettingsWsSupport(userSession);
  private final WsActionTester wsActionTester = new WsActionTester(new ValuesAction(dbClient, userSession, definitions, support));
  private ProjectDto project;

  @Before
  public void setUp() {
    ProjectData projectData = db.components().insertPrivateProject();
    project = projectData.getProjectDto();
  }

  @Test
  public void return_simple_value() {
    logIn();
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .build());
    db.properties().insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("foo").setValue("one"));

    ValuesWsResponse result = executeRequestForGlobalProperties("foo");

    assertThat(result.getSettingsList()).hasSize(1);
    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo("foo");
    assertThat(value.getValue()).isEqualTo("one");
    assertThat(value.getInherited()).isFalse();
  }

  @Test
  public void return_formatted_values() {
    logIn();
    String propertyKey = "sonar.login.message";
    definitions.addComponent(PropertyDefinition
      .builder(propertyKey)
        .type(PropertyType.FORMATTED_TEXT)
      .build());
    db.properties().insertProperties(null, null, null, null, newGlobalPropertyDto().setKey(propertyKey).setValue("[link](https://link.com)"));

    ValuesWsResponse result = executeRequestForGlobalProperties(propertyKey);

    assertThat(result.getSettingsList()).hasSize(1);
    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo(propertyKey);
    assertThat(value.getValues().getValuesList())
      .hasSize(2)
      .containsExactly("[link](https://link.com)", "<a href=\"https://link.com\" target=\"_blank\" rel=\"noopener noreferrer\">link</a>");
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
    db.properties().insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("global").setValue("three,four"));

    ValuesWsResponse result = executeRequestForGlobalProperties("default", "global");
    assertThat(result.getSettingsList()).hasSize(2);

    Settings.Setting foo = result.getSettings(0);
    assertThat(foo.getKey()).isEqualTo("default");
    assertThat(foo.getValues().getValuesList()).containsOnly("one", "two");

    Settings.Setting bar = result.getSettings(1);
    assertThat(bar.getKey()).isEqualTo("global");
    assertThat(bar.getValues().getValuesList()).containsOnly("three", "four");
  }

  @Test
  public void return_multi_value_with_coma() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("global").multiValues(true).build());
    db.properties().insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("global").setValue("three,four%2Cfive"));

    ValuesWsResponse result = executeRequestForGlobalProperties("global");

    assertThat(result.getSettingsList()).hasSize(1);
    Settings.Setting setting = result.getSettings(0);
    assertThat(setting.getKey()).isEqualTo("global");
    assertThat(setting.getValues().getValuesList()).containsOnly("three", "four,five");
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
    db.properties().insertPropertySet("foo", null, ImmutableMap.of("key", "key1", "size", "size1"), ImmutableMap.of("key", "key2"));

    ValuesWsResponse result = executeRequestForGlobalProperties("foo");

    assertThat(result.getSettingsList()).hasSize(1);
    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo("foo");
    assertFieldValues(value, ImmutableMap.of("key", "key1", "size", "size1"), ImmutableMap.of("key", "key2"));
  }

  @Test
  public void return_property_set_for_component() {
    logInAsProjectUser();
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .onQualifiers(PROJECT)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("size").name("Size").build()))
      .build());
    db.properties().insertPropertySet("foo", project, ImmutableMap.of("key", "key1", "size", "size1"), ImmutableMap.of("key", "key2"));

    ValuesWsResponse result = executeRequestForProjectProperties("foo");

    assertThat(result.getSettingsList()).hasSize(1);
    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo("foo");
    assertFieldValues(value, ImmutableMap.of("key", "key1", "size", "size1"), ImmutableMap.of("key", "key2"));
  }

  @Test
  public void return_default_values() {
    logIn();
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .defaultValue("default")
      .build());

    ValuesWsResponse result = executeRequestForGlobalProperties("foo");

    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "foo", "default", true);
  }

  @Test
  public void return_global_values() {
    logIn();
    definitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    db.properties().insertProperties(null, null, null,
      // The property is overriding default value
      null, newGlobalPropertyDto().setKey("property").setValue("one"));

    ValuesWsResponse result = executeRequestForGlobalProperties("property");

    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "property", "one", false);
  }

  @Test
  public void return_project_values() {
    logInAsProjectUser();
    definitions.addComponent(
      PropertyDefinition.builder("property").defaultValue("default").onQualifiers(PROJECT).build());
    db.properties().insertProperties(null, null, null,
      null, newGlobalPropertyDto().setKey("property").setValue("one"));
    db.properties().insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      // The property is overriding global value
      newComponentPropertyDto(project).setKey("property").setValue("two"));

    ValuesWsResponse result = executeRequestForProjectProperties("property");

    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "property", "two", false);
  }

  @Test
  public void return_settings_defined_only_at_global_level_when_loading_project_settings() {
    logInAsProjectUser();
    definitions.addComponents(asList(
      PropertyDefinition.builder("global").build(),
      PropertyDefinition.builder("global.default").defaultValue("default").build(),
      PropertyDefinition.builder("project").onQualifiers(PROJECT).build()));
    db.properties().insertProperties(null, null, null,
      null, newGlobalPropertyDto().setKey("global").setValue("one"));
    db.properties().insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("project").setValue("two"));

    ValuesWsResponse result = executeRequestForProjectProperties();

    assertThat(result.getSettingsList()).extracting(Settings.Setting::getKey, Settings.Setting::getValue)
      .containsOnly(tuple("project", "two"), tuple("global.default", "default"), tuple("global", "one"));
  }

  @Test
  public void return_is_inherited_to_true_when_property_is_defined_only_at_global_level() {
    logInAsProjectUser();
    definitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").onQualifiers(PROJECT).build());
    // The property is not defined on project
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("property").setValue("one"));

    ValuesWsResponse result = executeRequestForProjectProperties("property");

    assertThat(result.getSettingsList()).hasSize(1);
    assertSetting(result.getSettings(0), "property", "one", true);
  }

  @Test
  public void return_values_even_if_no_property_definition() {
    logIn();
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("globalPropertyWithoutDefinition").setValue("value"));

    ValuesWsResponse result = executeRequestForGlobalProperties("globalPropertyWithoutDefinition");

    Settings.Setting globalPropertyWithoutDefinitionValue = result.getSettings(0);
    assertThat(globalPropertyWithoutDefinitionValue.getKey()).isEqualTo("globalPropertyWithoutDefinition");
    assertThat(globalPropertyWithoutDefinitionValue.getValue()).isEqualTo("value");
    assertThat(globalPropertyWithoutDefinitionValue.getInherited()).isFalse();
  }

  @Test
  public void return_values_of_component_even_if_no_property_definition() {
    logInAsProjectUser();
    db.properties().insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("property").setValue("foo"));

    ValuesWsResponse response = executeRequestForComponentProperties(project, "property");
    assertThat(response.getSettingsCount()).isOne();
    assertSetting(response.getSettings(0), "property", "foo", false);
  }

  @Test
  public void return_empty_when_property_def_exists_but_no_value() {
    logIn();
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .build());

    ValuesWsResponse result = executeRequestForGlobalProperties("foo");

    assertThat(result.getSettingsList()).isEmpty();
  }

  @Test
  public void return_nothing_when_unknown_keys() {
    logIn();
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .defaultValue("default")
      .build());
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("bar").setValue(""));

    ValuesWsResponse result = executeRequestForGlobalProperties("unknown");

    assertThat(result.getSettingsList()).isEmpty();
  }

  @Test
  public void return_inherited_values_on_global_setting() {
    logIn();
    definitions.addComponents(asList(
      PropertyDefinition.builder("defaultProperty").defaultValue("default").build(),
      PropertyDefinition.builder("globalProperty").build()));
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("globalProperty").setValue("global"));

    ValuesWsResponse result = executeRequestForGlobalProperties("defaultProperty", "globalProperty");

    assertThat(result.getSettingsList()).hasSize(2);
    assertSetting(result.getSettings(0), "defaultProperty", "default", true);
    assertSetting(result.getSettings(1), "globalProperty", "global", false);
  }

  @Test
  public void return_value_of_deprecated_key() {
    logIn();
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .deprecatedKey("deprecated")
      .build());
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("foo").setValue("one"));

    ValuesWsResponse result = executeRequestForGlobalProperties("deprecated");

    assertThat(result.getSettingsList()).hasSize(1);
    Settings.Setting value = result.getSettings(0);
    assertThat(value.getKey()).isEqualTo("deprecated");
    assertThat(value.getValue()).isEqualTo("one");
  }

  @Test
  public void do_not_return_secured_settings_when_not_authenticated() {
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build()));
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("foo").setValue("one"),
      newGlobalPropertyDto().setKey("secret.secured").setValue("password"));

    ValuesWsResponse result = executeRequestForGlobalProperties();

    assertThat(result.getSettingsList()).extracting(Settings.Setting::getKey).containsOnly("foo");
  }

  @Test
  public void do_not_return_secured_settings_in_property_set_when_not_authenticated() {
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("secret.secured").name("Secured").build()))
      .build());
    db.properties().insertPropertySet("foo", null, ImmutableMap.of("key", "key1", "secret.secured", "123456"));

    ValuesWsResponse result = executeRequestForGlobalProperties();

    assertFieldValues(result.getSettings(0), ImmutableMap.of("key", "key1"));
  }

  @Test
  public void return_global_secured_settings_when_not_authenticated_but_with_scan_permission() {
    userSession.anonymous().addPermission(GlobalPermission.SCAN);
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build()));
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("foo").setValue("one"),
      newGlobalPropertyDto().setKey("secret.secured").setValue("password"));

    ValuesWsResponse result = executeRequestForGlobalProperties();

    assertThat(result.getSettingsList()).extracting(Settings.Setting::getKey).containsOnly("foo");
    assertThat(result.getSetSecuredSettingsList()).containsOnly("secret.secured");
  }

  @Test
  public void return_component_secured_settings_when_not_authenticated_but_with_project_scan_permission() {
    userSession
      .addProjectPermission(USER, project)
      .addProjectPermission(ProjectPermission.SCAN, project);
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").onQualifiers(PROJECT).build(),
      PropertyDefinition.builder("global.secret.secured").build(),
      PropertyDefinition.builder("secret.secured").onQualifiers(PROJECT).build()));
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("global.secret.secured").setValue("very secret"));
    db.properties().insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("foo").setValue("one"),
      newComponentPropertyDto(project).setKey("secret.secured").setValue("password"));

    ValuesWsResponse result = executeRequestForProjectProperties();

    assertThat(result.getSettingsList()).extracting(Settings.Setting::getKey).containsOnly("foo");
    assertThat(result.getSetSecuredSettingsList()).contains("global.secret.secured", "secret.secured");
  }

  @Test
  public void return_component_secured_settings_even_if_not_defined_when_not_authenticated_but_with_scan_permission() {
    userSession
      .addProjectPermission(USER, project)
      .addProjectPermission(ProjectPermission.SCAN, project);
    db.properties().insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("not-defined.secured").setValue("123"));

    ValuesWsResponse result = executeRequestForProjectProperties("not-defined.secured");

    assertThat(result.getSetSecuredSettingsList()).containsOnly("not-defined.secured");
  }

  @Test
  public void return_secured_settings_when_system_admin() {
    logInAsAdmin();
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build()));
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("foo").setValue("one"),
      newGlobalPropertyDto().setKey("secret.secured").setValue("password"));

    ValuesWsResponse result = executeRequestForGlobalProperties();

    assertThat(result.getSettingsList()).extracting(Settings.Setting::getKey).containsOnly("foo");
    assertThat(result.getSetSecuredSettingsList()).containsOnly("secret.secured");
  }

  @Test
  public void return_secured_settings_when_project_admin() {
    logInAsProjectAdmin();
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").onQualifiers(PROJECT).build(),
      PropertyDefinition.builder("global.secret.secured").build(),
      PropertyDefinition.builder("secret.secured").onQualifiers(PROJECT).build()));
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("global.secret.secured").setValue("very secret"));
    db.properties().insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("foo").setValue("one"),
      newComponentPropertyDto(project).setKey("secret.secured").setValue("password"));

    ValuesWsResponse result = executeRequestForProjectProperties();

    List<Settings.Setting> settingsList = result.getSettingsList().stream().sorted(comparing(Settings.Setting::getKey)).toList();
    assertThat(settingsList).extracting(Settings.Setting::getKey).containsExactly("foo");
    assertThat(settingsList).extracting(Settings.Setting::hasValue).containsExactly(true);
    assertThat(result.getSetSecuredSettingsList()).containsOnly("global.secret.secured", "secret.secured");
  }

  @Test
  public void return_secured_settings_even_if_not_defined_when_project_admin() {
    logInAsProjectAdmin();
    db.properties().insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("not-defined.secured").setValue("123"));

    ValuesWsResponse result = executeRequestForProjectProperties("not-defined.secured");

    assertThat(result.getSettingsList()).extracting(Settings.Setting::getKey).isEmpty();
    assertThat(result.getSetSecuredSettingsList()).containsOnly("not-defined.secured");
  }

  @Test
  public void return_secured_settings_in_property_set_when_system_admin() {
    logInAsAdmin();
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("secret.secured").name("Secured").build()))
      .build());
    db.properties().insertPropertySet("foo", null, ImmutableMap.of("key", "key1", "secret.secured", "123456"));

    ValuesWsResponse result = executeRequestForGlobalProperties();

    assertFieldValues(result.getSettings(0), ImmutableMap.of("key", "key1", "secret.secured", "123456"));
  }

  @Test
  public void return_admin_only_settings_in_property_set_when_system_admin() {
    String anyAdminOnlySettingKey = SettingsWsSupport.ADMIN_ONLY_SETTINGS.iterator().next();
    logInAsAdmin();
    definitions.addComponent(PropertyDefinition
      .builder(anyAdminOnlySettingKey)
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build(anyAdminOnlySettingKey).name("Key admnin only").build(),
        PropertyFieldDefinition.build(anyAdminOnlySettingKey).name("Value admin only").build()))
      .build());
    ImmutableMap<String, String> keyValuePairs = ImmutableMap.of(anyAdminOnlySettingKey, "test_val");
    db.properties().insertPropertySet(anyAdminOnlySettingKey, null, keyValuePairs);

    ValuesWsResponse result = executeRequestForGlobalProperties();

    assertFieldValues(result.getSettings(0), keyValuePairs);
  }

  @Test
  public void return_admin_only_settings_in_property_not_set_when_simple_user() {
    String anyAdminOnlySettingKey = SettingsWsSupport.ADMIN_ONLY_SETTINGS.iterator().next();
    logIn();
    definitions.addComponent(PropertyDefinition
      .builder(anyAdminOnlySettingKey)
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build(anyAdminOnlySettingKey).name("Key admnin only").build(),
        PropertyFieldDefinition.build(anyAdminOnlySettingKey).name("Value admin only").build()))
      .build());
    ImmutableMap<String, String> keyValuePairs = ImmutableMap.of(anyAdminOnlySettingKey, "test_val");
    db.properties().insertPropertySet(anyAdminOnlySettingKey, null, keyValuePairs);

    ValuesWsResponse result = executeRequestForGlobalProperties();

    assertThat(result.getSettingsList()).isEmpty();
  }

  @Test
  public void return_global_settings_from_definitions_when_no_component_and_no_keys() {
    logInAsAdmin();
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("secret.secured").build()));
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("foo").setValue("one"),
      newGlobalPropertyDto().setKey("secret.secured").setValue("password"));

    ValuesWsResponse result = executeRequestForGlobalProperties();

    assertThat(result.getSettingsList()).extracting(Settings.Setting::getKey).containsOnly("foo");
    assertThat(result.getSetSecuredSettingsList()).containsOnly("secret.secured");
  }

  @Test
  public void return_project_settings_from_definitions_when_component_and_no_keys() {
    logInAsProjectAdmin();
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").onQualifiers(PROJECT).build(),
      PropertyDefinition.builder("secret.secured").onQualifiers(PROJECT).build()));
    db.properties().insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("foo").setValue("one"),
      newComponentPropertyDto(project).setKey("secret.secured").setValue("password"));

    ValuesWsResponse result = executeRequestForProjectProperties();

    assertThat(result.getSettingsList()).extracting(Settings.Setting::getKey).containsOnly("foo");
    assertThat(result.getSetSecuredSettingsList()).containsOnly("secret.secured");
  }

  @Test
  public void return_additional_settings_specific_for_scanner_when_no_keys() {
    logInAsAdmin();
    definitions.addComponent(PropertyDefinition.builder("secret.secured").build());
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("sonar.core.id").setValue("ID"),
      newGlobalPropertyDto().setKey("sonar.core.startTime").setValue("2017-01-01"));

    ValuesWsResponse result = executeRequestForGlobalProperties();

    assertThat(result.getSettingsList()).extracting(Settings.Setting::getKey).containsOnly("sonar.core.id", "sonar.core.startTime");
  }

  @Test
  public void return_simple_value_with_non_ascii_characters() {
    logIn();
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .build());
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("foo").setValue("ﬁ±∞…"));

    ValuesWsResponse result = executeRequestForGlobalProperties("foo");

    assertThat(result.getSettings(0).getValue()).isEqualTo("ﬁ±∞…");
  }

  @Test
  public void fail_when_user_has_not_project_browse_permission() {
    userSession.logIn("project-admin").addProjectPermission(CODEVIEWER, project);
    definitions.addComponent(PropertyDefinition.builder("foo").build());

    assertThatThrownBy(() -> {
      executeRequest(project.getKey(), "foo");
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_deprecated_key_and_new_key_are_used() {
    logIn();
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .deprecatedKey("deprecated")
      .build());
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("foo").setValue("one"));

    assertThatThrownBy(() -> {
      executeRequestForGlobalProperties("foo", "deprecated");
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'foo' and 'deprecated' cannot be used at the same time as they refer to the same setting");
  }

  @Test
  public void fail_when_component_not_found() {
    assertThatThrownBy(() -> {
      wsActionTester.newRequest()
        .setParam("keys", "foo")
        .setParam("component", "unknown")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'unknown' not found");
  }

  @Test
  public void test_example_json_response() {
    logInAsAdmin();
    definitions.addComponent(PropertyDefinition
      .builder("sonar.test.jira")
      .defaultValue("abc")
      .build());
    definitions.addComponent(PropertyDefinition
      .builder("sonar.autogenerated")
      .multiValues(true)
      .build());
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("sonar.autogenerated").setValue("val1,val2,val3"));
    definitions.addComponent(PropertyDefinition
      .builder("sonar.demo")
      .type(PropertyType.PROPERTY_SET)
      .fields(PropertyFieldDefinition.build("text").name("Text").build(),
        PropertyFieldDefinition.build("boolean").name("Boolean").build())
      .build());
    db.properties().insertPropertySet("sonar.demo", null, ImmutableMap.of("text", "foo", "boolean", "true"), ImmutableMap.of("text", "bar", "boolean", "false"));

    definitions.addComponent(PropertyDefinition
      .builder("email.smtp_port.secured")
      .defaultValue("25")
      .build());
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("email.smtp_port.secured").setValue("25"));

    String result = wsActionTester.newRequest()
      .setParam("keys", "sonar.test.jira,sonar.autogenerated,sonar.demo,email.smtp_port.secured")
      .setMediaType(JSON)
      .execute()
      .getInput();

    JsonAssert.assertJson(wsActionTester.getDef().responseExampleAsString()).isSimilarTo(result);
  }

  @Test
  public void fail_when_setting_key_is_defined_in_sonar_properties() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(ProjectPermission.USER, project);
    String settingKey = ProcessProperties.Property.JDBC_URL.getKey();

    assertThatThrownBy(() -> {
      wsActionTester.newRequest()
        .setParam("keys", settingKey)
        .setParam("component", project.getKey())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Setting '%s' can only be used in sonar.properties", settingKey));
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = wsActionTester.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("keys", "component");
  }

  @Test
  public void global_secured_properties_require_system_admin_permission() {
    PropertyDefinition securedDef = PropertyDefinition.builder("my.password.secured").build();
    PropertyDefinition standardDef = PropertyDefinition.builder("my.property").build();
    definitions.addComponents(asList(securedDef, standardDef));
    db.properties().insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey(securedDef.key()).setValue("securedValue"),
      newGlobalPropertyDto().setKey(standardDef.key()).setValue("standardValue"));

    // anonymous
    ValuesWsResponse response = executeRequest(null, securedDef.key(), standardDef.key());
    assertThat(response.getSettingsList()).extracting(Settings.Setting::getKey).containsExactly("my.property");

    // only scan global permission
    userSession.logIn()
      .addPermission(GlobalPermission.SCAN);
    response = executeRequest(null, securedDef.key(), standardDef.key());
    assertThat(response.getSetSecuredSettingsList()).contains("my.password.secured");

    // global administrator
    userSession.logIn()
      .addPermission(GlobalPermission.ADMINISTER);
    response = executeRequest(null, securedDef.key(), standardDef.key());
    assertThat(response.getSetSecuredSettingsList()).contains("my.password.secured");

    // system administrator
    userSession.logIn().setSystemAdministrator();
    response = executeRequest(null, securedDef.key(), standardDef.key());
    assertThat(response.getSetSecuredSettingsList()).contains("my.password.secured");
  }

  private ValuesWsResponse executeRequestForComponentProperties(EntityDto entity, String... keys) {
    return executeRequest(entity.getKey(), keys);
  }

  private ValuesWsResponse executeRequestForProjectProperties(String... keys) {
    return executeRequest(project.getKey(), keys);
  }

  private ValuesWsResponse executeRequestForGlobalProperties(String... keys) {
    return executeRequest(null, keys);
  }

  private ValuesWsResponse executeRequest(@Nullable String componentKey, String... keys) {
    TestRequest request = wsActionTester.newRequest();
    if (keys.length > 0) {
      request.setParam("keys", COMMA_JOINER.join(keys));
    }
    if (componentKey != null) {
      request.setParam("component", componentKey);
    }
    return request.executeProtobuf(ValuesWsResponse.class);
  }

  private void logIn() {
    userSession.logIn();
  }

  private void logInAsProjectUser() {
    userSession.logIn().addProjectPermission(USER, project);
  }

  private void logInAsAdmin() {
    userSession.logIn().setSystemAdministrator();
  }

  private void logInAsProjectAdmin() {
    userSession.logIn()
      .addProjectPermission(ADMIN, project)
      .addProjectPermission(USER, project);
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
      assertThat(setting.getParentValueOneOfCase()).isEqualTo(PARENTVALUEONEOF_NOT_SET);
    } else {
      assertThat(setting.getParentValue()).isEqualTo(parentValue);
    }
  }

  private void assertParentValues(Settings.Setting setting, String... parentValues) {
    if (parentValues.length == 0) {
      assertThat(setting.getParentValueOneOfCase()).isEqualTo(PARENTVALUEONEOF_NOT_SET);
    } else {
      assertThat(setting.getParentValues().getValuesList()).containsOnly(parentValues);
    }
  }

  private void assertParentFieldValues(Settings.Setting setting, Map<String, String>... fieldsValues) {
    if (fieldsValues.length == 0) {
      assertThat(setting.getParentValueOneOfCase()).isEqualTo(PARENTVALUEONEOF_NOT_SET);
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
