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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.DASHBOARD_SHARING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
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
  SettingsWsComponentParameters settingsWsComponentParameters = new SettingsWsComponentParameters(new ComponentFinder(dbClient), userSession);
  PropertyDefinitions propertyDefinitions = new PropertyDefinitions();

  ComponentDto project;

  WsActionTester ws = new WsActionTester(new ValuesAction(dbClient, settingsWsComponentParameters, propertyDefinitions));

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
    assertThat(result.getValuesList()).hasSize(1);

    Settings.Value value = result.getValues(0);
    assertThat(value.getKey()).isEqualTo("foo");
    assertThat(value.getValue()).isEqualTo("one");
    assertThat(value.getValuesCount()).isZero();
    assertThat(value.getSetValues()).isEmpty();
    assertThat(value.getIsDefault()).isFalse();
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
    assertThat(result.getValuesList()).hasSize(2);

    Settings.Value foo = result.getValues(0);
    assertThat(foo.getKey()).isEqualTo("default");
    assertThat(foo.hasValue()).isFalse();
    assertThat(foo.getValuesList()).containsOnly("one", "two");
    assertThat(foo.getSetValues()).isEmpty();

    Settings.Value bar = result.getValues(1);
    assertThat(bar.getKey()).isEqualTo("global");
    assertThat(bar.hasValue()).isFalse();
    assertThat(bar.getValuesList()).containsOnly("three", "four");
    assertThat(bar.getSetValues()).isEmpty();
  }

  @Test
  public void return_default_values() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .defaultValue("default")
      .build());

    ValuesWsResponse result = newRequestForGlobalProperties("foo");
    assertThat(result.getValuesList()).hasSize(1);

    Settings.Value value = result.getValues(0);
    assertThat(value.getKey()).isEqualTo("foo");
    assertThat(value.getValue()).isEqualTo("default");
    assertThat(value.getIsDefault()).isTrue();
    assertThat(value.getIsInherited()).isFalse();
  }

  @Test
  public void return_global_values() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    insertProperties(
      // The property is overriding default value
      newGlobalPropertyDto().setKey("property").setValue("one"));

    ValuesWsResponse result = newRequestForGlobalProperties("property");
    assertThat(result.getValuesList()).hasSize(1);

    Settings.Value globalPropertyValue = result.getValues(0);
    assertThat(globalPropertyValue.getKey()).isEqualTo("property");
    assertThat(globalPropertyValue.getValue()).isEqualTo("one");
    assertThat(globalPropertyValue.getIsDefault()).isFalse();
    assertThat(globalPropertyValue.getIsInherited()).isFalse();
  }

  @Test
  public void return_component_values() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    insertProperties(
      newGlobalPropertyDto().setKey("property").setValue("one"),
      // The property is overriding global value
      newComponentPropertyDto(project).setKey("property").setValue("two"));

    ValuesWsResponse result = newRequestForProjectProperties("property");
    assertThat(result.getValuesList()).hasSize(1);

    Settings.Value globalPropertyValue = result.getValues(0);
    assertThat(globalPropertyValue.getKey()).isEqualTo("property");
    assertThat(globalPropertyValue.getValue()).isEqualTo("two");
    assertThat(globalPropertyValue.getIsDefault()).isFalse();
    assertThat(globalPropertyValue.getIsInherited()).isFalse();
  }

  @Test
  public void return_is_inherited_to_true_when_property_is_defined_only_at_global_level() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition.builder("property").defaultValue("default").build());
    // The property is not defined on project
    insertProperties(newGlobalPropertyDto().setKey("property").setValue("one"));

    ValuesWsResponse result = newRequestForProjectProperties("property");
    assertThat(result.getValuesList()).hasSize(1);

    Settings.Value globalPropertyValue = result.getValues(0);
    assertThat(globalPropertyValue.getKey()).isEqualTo("property");
    assertThat(globalPropertyValue.getValue()).isEqualTo("one");
    assertThat(globalPropertyValue.getIsDefault()).isFalse();
     assertThat(globalPropertyValue.getIsInherited()).isTrue();
  }

  @Test
  public void return_values_even_if_no_property_definition() throws Exception {
    setUserAsSystemAdmin();
    insertProperties(newGlobalPropertyDto().setKey("globalPropertyWithoutDefinition").setValue("value"));

    ValuesWsResponse result = newRequestForGlobalProperties("globalPropertyWithoutDefinition");
    Settings.Value globalPropertyWithoutDefinitionValue = result.getValues(0);
    assertThat(globalPropertyWithoutDefinitionValue.getKey()).isEqualTo("globalPropertyWithoutDefinition");
    assertThat(globalPropertyWithoutDefinitionValue.getValue()).isEqualTo("value");
    assertThat(globalPropertyWithoutDefinitionValue.getIsDefault()).isFalse();
  }

  @Test
  public void return_empty_when_property_def_exists_but_no_value() throws Exception {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("foo")
      .build());
    insertProperties(newGlobalPropertyDto().setKey("bar").setValue(""));

    ValuesWsResponse result = newRequestForGlobalProperties("foo", "bar");
    assertThat(result.getValuesList()).isEmpty();
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
    assertThat(result.getValuesList()).isEmpty();
  }

  @Test
  @Ignore
  public void test_example_json_response() {
    setUserAsSystemAdmin();
    propertyDefinitions.addComponent(PropertyDefinition
      .builder("sonar.test.jira")
      .defaultValue("abc")
      .build());

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
    newRequestForGlobalProperties();
  }

  @Test
  public void fail_when_not_project_admin() throws Exception {
    userSession.login("project-admin").addProjectUuidPermissions(USER, project.uuid());
    propertyDefinitions.addComponent(PropertyDefinition.builder("foo").build());

    expectedException.expect(ForbiddenException.class);
    newRequest(project.uuid(), null);
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

}
