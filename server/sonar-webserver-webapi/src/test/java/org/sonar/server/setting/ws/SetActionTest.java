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
package org.sonar.server.setting.ws;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.process.ProcessProperties;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.setting.SettingsChangeNotifier;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonar.db.user.UserTesting.newUserDto;

public class SetActionTest {

  private static final Gson GSON = GsonHelper.create();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private PropertyDbTester propertyDb = new PropertyDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentFinder componentFinder = TestComponentFinder.from(db);

  private I18nRule i18n = new I18nRule();
  private PropertyDefinitions definitions = new PropertyDefinitions();
  private FakeSettingsNotifier settingsChangeNotifier = new FakeSettingsNotifier(dbClient);
  private SettingsUpdater settingsUpdater = new SettingsUpdater(dbClient, definitions);
  private SettingValidations validations = new SettingValidations(definitions, dbClient, i18n);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private SetAction underTest = new SetAction(definitions, dbClient, componentFinder, userSession, settingsUpdater,
    settingsChangeNotifier, validations, new SettingsWsSupport(defaultOrganizationProvider, userSession));

  private WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUp() {
    // by default test doesn't care about permissions
    userSession.logIn().setSystemAdministrator();
  }

  @Test
  public void empty_204_response() {
    TestResponse result = ws.newRequest()
      .setParam("key", "my.key")
      .setParam("value", "my value")
      .execute();

    assertThat(result.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    assertThat(result.getInput()).isEmpty();
  }

  @Test
  public void persist_new_global_setting() {
    callForGlobalSetting("my.key", "my,value");

    assertGlobalSetting("my.key", "my,value");
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void update_existing_global_setting() {
    propertyDb.insertProperty(newGlobalPropertyDto("my.key", "my value"));
    assertGlobalSetting("my.key", "my value");

    callForGlobalSetting("my.key", "my new value");

    assertGlobalSetting("my.key", "my new value");
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void persist_new_project_setting() {
    propertyDb.insertProperty(newGlobalPropertyDto("my.key", "my global value"));
    ComponentDto project = db.components().insertPrivateProject();
    logInAsProjectAdministrator(project);

    callForProjectSettingByKey("my.key", "my project value", project.getDbKey());

    assertGlobalSetting("my.key", "my global value");
    assertComponentSetting("my.key", "my project value", project.getId());
    assertThat(settingsChangeNotifier.wasCalled).isFalse();
  }

  @Test
  public void persist_project_property_with_project_admin_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    logInAsProjectAdministrator(project);

    callForProjectSettingByKey("my.key", "my value", project.getDbKey());

    assertComponentSetting("my.key", "my value", project.getId());
  }

  @Test
  public void update_existing_project_setting() {
    propertyDb.insertProperty(newGlobalPropertyDto("my.key", "my global value"));
    ComponentDto project = db.components().insertPrivateProject();
    propertyDb.insertProperty(newComponentPropertyDto("my.key", "my project value", project));
    assertComponentSetting("my.key", "my project value", project.getId());
    logInAsProjectAdministrator(project);

    callForProjectSettingByKey("my.key", "my new project value", project.getDbKey());

    assertComponentSetting("my.key", "my new project value", project.getId());
  }

  @Test
  public void persist_several_multi_value_setting() {
    callForMultiValueGlobalSetting("my.key", newArrayList("first,Value", "second,Value", "third,Value"));

    String expectedValue = "first%2CValue,second%2CValue,third%2CValue";
    assertGlobalSetting("my.key", expectedValue);
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void persist_one_multi_value_setting() {
    callForMultiValueGlobalSetting("my.key", newArrayList("first,Value"));

    assertGlobalSetting("my.key", "first%2CValue");
  }

  @Test
  public void persist_property_set_setting() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(newArrayList(
        PropertyFieldDefinition.build("firstField")
          .name("First Field")
          .type(PropertyType.STRING)
          .build(),
        PropertyFieldDefinition.build("secondField")
          .name("Second Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    callForGlobalPropertySet("my.key", newArrayList(
      GSON.toJson(ImmutableMap.of("firstField", "firstValue", "secondField", "secondValue")),
      GSON.toJson(ImmutableMap.of("firstField", "anotherFirstValue", "secondField", "anotherSecondValue")),
      GSON.toJson(ImmutableMap.of("firstField", "yetFirstValue", "secondField", "yetSecondValue"))));

    assertThat(dbClient.propertiesDao().selectGlobalProperties(dbSession)).hasSize(7);
    assertGlobalSetting("my.key", "1,2,3");
    assertGlobalSetting("my.key.1.firstField", "firstValue");
    assertGlobalSetting("my.key.1.secondField", "secondValue");
    assertGlobalSetting("my.key.2.firstField", "anotherFirstValue");
    assertGlobalSetting("my.key.2.secondField", "anotherSecondValue");
    assertGlobalSetting("my.key.3.firstField", "yetFirstValue");
    assertGlobalSetting("my.key.3.secondField", "yetSecondValue");
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void update_property_set_setting() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(newArrayList(
        PropertyFieldDefinition.build("firstField")
          .name("First Field")
          .type(PropertyType.STRING)
          .build(),
        PropertyFieldDefinition.build("secondField")
          .name("Second Field")
          .type(PropertyType.STRING)
          .build()))
      .build());
    propertyDb.insertProperties(
      newGlobalPropertyDto("my.key", "1,2,3,4"),
      newGlobalPropertyDto("my.key.1.firstField", "oldFirstValue"),
      newGlobalPropertyDto("my.key.1.secondField", "oldSecondValue"),
      newGlobalPropertyDto("my.key.2.firstField", "anotherOldFirstValue"),
      newGlobalPropertyDto("my.key.2.secondField", "anotherOldSecondValue"),
      newGlobalPropertyDto("my.key.3.firstField", "oldFirstValue"),
      newGlobalPropertyDto("my.key.3.secondField", "oldSecondValue"),
      newGlobalPropertyDto("my.key.4.firstField", "anotherOldFirstValue"),
      newGlobalPropertyDto("my.key.4.secondField", "anotherOldSecondValue"));

    callForGlobalPropertySet("my.key", newArrayList(
      GSON.toJson(ImmutableMap.of("firstField", "firstValue", "secondField", "secondValue")),
      GSON.toJson(ImmutableMap.of("firstField", "anotherFirstValue", "secondField", "anotherSecondValue")),
      GSON.toJson(ImmutableMap.of("firstField", "yetFirstValue", "secondField", "yetSecondValue"))));

    assertThat(dbClient.propertiesDao().selectGlobalProperties(dbSession)).hasSize(7);
    assertGlobalSetting("my.key", "1,2,3");
    assertGlobalSetting("my.key.1.firstField", "firstValue");
    assertGlobalSetting("my.key.1.secondField", "secondValue");
    assertGlobalSetting("my.key.2.firstField", "anotherFirstValue");
    assertGlobalSetting("my.key.2.secondField", "anotherSecondValue");
    assertGlobalSetting("my.key.3.firstField", "yetFirstValue");
    assertGlobalSetting("my.key.3.secondField", "yetSecondValue");
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void update_property_set_on_component_setting() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .onQualifiers(Qualifiers.PROJECT)
      .fields(newArrayList(
        PropertyFieldDefinition.build("firstField")
          .name("First Field")
          .type(PropertyType.STRING)
          .build(),
        PropertyFieldDefinition.build("secondField")
          .name("Second Field")
          .type(PropertyType.STRING)
          .build()))
      .build());
    ComponentDto project = db.components().insertPrivateProject();
    propertyDb.insertProperties(
      newGlobalPropertyDto("my.key", "1"),
      newGlobalPropertyDto("my.key.1.firstField", "oldFirstValue"),
      newGlobalPropertyDto("my.key.1.secondField", "oldSecondValue"),
      newComponentPropertyDto("my.key", "1", project),
      newComponentPropertyDto("my.key.1.firstField", "componentFirstValue", project),
      newComponentPropertyDto("my.key.1.firstField", "componentSecondValue", project));
    logInAsProjectAdministrator(project);

    callForComponentPropertySet("my.key", newArrayList(
      GSON.toJson(ImmutableMap.of("firstField", "firstValue", "secondField", "secondValue")),
      GSON.toJson(ImmutableMap.of("firstField", "anotherFirstValue", "secondField", "anotherSecondValue"))),
      project.getDbKey());

    assertThat(dbClient.propertiesDao().selectGlobalProperties(dbSession)).hasSize(3);
    assertThat(dbClient.propertiesDao().selectProjectProperties(dbSession, project.getDbKey())).hasSize(5);
    assertGlobalSetting("my.key", "1");
    assertGlobalSetting("my.key.1.firstField", "oldFirstValue");
    assertGlobalSetting("my.key.1.secondField", "oldSecondValue");
    Long projectId = project.getId();
    assertComponentSetting("my.key", "1,2", projectId);
    assertComponentSetting("my.key.1.firstField", "firstValue", projectId);
    assertComponentSetting("my.key.1.secondField", "secondValue", projectId);
    assertComponentSetting("my.key.2.firstField", "anotherFirstValue", projectId);
    assertComponentSetting("my.key.2.secondField", "anotherSecondValue", projectId);
    assertThat(settingsChangeNotifier.wasCalled).isFalse();
  }

  @Test
  public void persist_multi_value_with_type_metric() {
    definitions.addComponent(PropertyDefinition
      .builder("my_key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.METRIC)
      .defaultValue("default")
      .multiValues(true)
      .build());
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("metric_key_1"));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("metric_key_2"));
    dbSession.commit();

    callForMultiValueGlobalSetting("my_key", newArrayList("metric_key_1", "metric_key_2"));

    assertGlobalSetting("my_key", "metric_key_1,metric_key_2");
  }

  @Test
  public void persist_multi_value_with_type_logIn() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.USER_LOGIN)
      .defaultValue("default")
      .multiValues(true)
      .build());
    db.users().insertUser(newUserDto().setLogin("login.1"));
    db.users().insertUser(newUserDto().setLogin("login.2"));

    callForMultiValueGlobalSetting("my.key", newArrayList("login.1", "login.2"));

    assertGlobalSetting("my.key", "login.1,login.2");
  }

  @Test
  public void user_setting_is_not_updated() {
    propertyDb.insertProperty(newGlobalPropertyDto("my.key", "my user value").setUserId(42));
    propertyDb.insertProperty(newGlobalPropertyDto("my.key", "my global value"));

    callForGlobalSetting("my.key", "my new global value");

    assertGlobalSetting("my.key", "my new global value");
    assertUserSetting("my.key", "my user value", 42);
  }

  @Test
  public void persist_global_property_with_deprecated_key() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .deprecatedKey("my.old.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.STRING)
      .defaultValue("default")
      .build());

    callForGlobalSetting("my.old.key", "My Value");

    assertGlobalSetting("my.key", "My Value");
  }

  @Test
  public void persist_global_setting_with_non_ascii_characters() {
    callForGlobalSetting("my.key", "ﬁ±∞…");

    assertGlobalSetting("my.key", "ﬁ±∞…");
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void set_leak_on_branch() {
    ComponentDto project = db.components().insertMainBranch();
    logInAsProjectAdministrator(project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    String leakKey = "sonar.leak.period";
    definitions.addComponent(PropertyDefinition.builder(leakKey)
      .name("Leak")
      .description("desc")
      .onQualifiers(Qualifiers.PROJECT)
      .build());
    propertyDb.insertProperties(newComponentPropertyDto(leakKey, "1", branch));

    ws.newRequest()
      .setParam("key", leakKey)
      .setParam("value", "2")
      .setParam("component", branch.getKey())
      .setParam("branch", branch.getBranch())
      .execute();

    assertComponentSetting(leakKey, "2", branch.getId());
  }

  @Test
  public void fail_when_no_key() {
    expectedException.expect(IllegalArgumentException.class);

    callForGlobalSetting(null, "my value");
  }

  @Test
  public void fail_when_empty_key_value() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'key' parameter is missing");

    callForGlobalSetting("  ", "my value");
  }

  @Test
  public void fail_when_no_value() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Either 'value', 'values' or 'fieldValues' must be provided");

    callForGlobalSetting("my.key", null);
  }

  @Test
  public void fail_when_empty_value() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A non empty value must be provided");

    callForGlobalSetting("my.key", "");
  }

  @Test
  public void fail_when_one_empty_value_on_multi_value() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A non empty value must be provided");

    callForMultiValueGlobalSetting("my.key", newArrayList("oneValue", "  ", "anotherValue"));

  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    callForGlobalSetting("my.key", "my value");
  }

  @Test
  public void fail_when_data_and_type_do_not_match() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.INTEGER)
      .defaultValue("default")
      .build());
    i18n.put("property.error.notInteger", "Not an integer error message");
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Not an integer error message");

    callForGlobalSetting("my.key", "My Value");
  }

  @Test
  public void fail_when_data_and_metric_type_with_invalid_key() {
    definitions.addComponent(PropertyDefinition
      .builder("my_key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.METRIC)
      .defaultValue("default")
      .multiValues(true)
      .build());
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("metric_key"));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("metric_disabled_key").setEnabled(false));
    dbSession.commit();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Error when validating metric setting with key 'my_key' and values [metric_key, metric_disabled_key]. A value is not a valid metric key.");

    callForMultiValueGlobalSetting("my_key", newArrayList("metric_key", "metric_disabled_key"));
  }

  @Test
  public void fail_when_data_and_login_type_with_invalid_logIn() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.USER_LOGIN)
      .defaultValue("default")
      .multiValues(true)
      .build());
    db.users().insertUser(newUserDto().setLogin("login.1"));
    db.users().insertUser(newUserDto().setLogin("login.2").setActive(false));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Error when validating login setting with key 'my.key' and values [login.1, login.2]. A value is not a valid login.");

    callForMultiValueGlobalSetting("my.key", newArrayList("login.1", "login.2"));
  }

  @Test
  public void fail_when_data_and_type_do_not_match_with_unknown_error_key() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.INTEGER)
      .defaultValue("default")
      .build());
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Error when validating setting with key 'my.key' and value [My Value, My Other Value]");

    callForMultiValueGlobalSetting("my.key", newArrayList("My Value", "My Other Value"));
  }

  @Test
  public void fail_when_global_with_property_only_on_projects() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.INTEGER)
      .defaultValue("default")
      .onlyOnQualifiers(Qualifiers.PROJECT)
      .build());
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Setting 'my.key' cannot be global");

    callForGlobalSetting("my.key", "42");
  }

  @Test
  public void fail_when_view_property_when_on_projects_only() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.STRING)
      .defaultValue("default")
      .onQualifiers(Qualifiers.PROJECT)
      .build());
    ComponentDto view = db.components().insertComponent(newView(db.getDefaultOrganization(), "view-uuid"));
    i18n.put("qualifier." + Qualifiers.VIEW, "View");
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Setting 'my.key' cannot be set on a View");
    logInAsProjectAdministrator(view);

    callForProjectSettingByKey("my.key", "My Value", view.getDbKey());
  }

  @Test
  public void fail_when_property_with_definition_when_component_qualifier_does_not_match() {
    ComponentDto project = randomPublicOrPrivateProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.STRING)
      .defaultValue("default")
      .onQualifiers(Qualifiers.PROJECT)
      .build());
    i18n.put("qualifier." + file.qualifier(), "CptLabel");
    logInAsProjectAdministrator(project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Setting 'my.key' cannot be set on a CptLabel");

    callForProjectSettingByKey("my.key", "My Value", file.getDbKey());
  }

  @Test
  public void succeed_for_property_without_definition_when_set_on_project_component() {
    ComponentDto project = randomPublicOrPrivateProject();
    succeedForPropertyWithoutDefinitionAndValidComponent(project, project);
  }

  @Test
  public void succeed_for_property_without_definition_when_set_on_module_component() {
    ComponentDto project = randomPublicOrPrivateProject();
    ComponentDto module = db.components().insertComponent(ComponentTesting.newModuleDto(project));
    succeedForPropertyWithoutDefinitionAndValidComponent(project, module);
  }

  @Test
  public void fail_for_property_without_definition_when_set_on_directory_component() {
    ComponentDto project = randomPublicOrPrivateProject();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(project, "A/B"));
    failForPropertyWithoutDefinitionOnUnsupportedComponent(project, directory);
  }

  @Test
  public void fail_for_property_without_definition_when_set_on_file_component() {
    ComponentDto project = randomPublicOrPrivateProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));
    failForPropertyWithoutDefinitionOnUnsupportedComponent(project, file);
  }

  @Test
  public void succeed_for_property_without_definition_when_set_on_view_component() {
    ComponentDto view = db.components().insertView();
    succeedForPropertyWithoutDefinitionAndValidComponent(view, view);
  }

  @Test
  public void succeed_for_property_without_definition_when_set_on_subview_component() {
    ComponentDto view = db.components().insertView();
    ComponentDto subview = db.components().insertComponent(ComponentTesting.newSubView(view));
    succeedForPropertyWithoutDefinitionAndValidComponent(view, subview);
  }

  @Test
  public void fail_for_property_without_definition_when_set_on_projectCopy_component() {
    ComponentDto view = db.components().insertView();
    ComponentDto projectCopy = db.components().insertComponent(ComponentTesting.newProjectCopy("a", db.components().insertPrivateProject(), view));

    failForPropertyWithoutDefinitionOnUnsupportedComponent(view, projectCopy);
  }

  private void succeedForPropertyWithoutDefinitionAndValidComponent(ComponentDto project, ComponentDto module) {
    logInAsProjectAdministrator(project);

    callForProjectSettingByKey("my.key", "My Value", module.getDbKey());

    assertComponentSetting("my.key", "My Value", module.getId());
  }

  private void failForPropertyWithoutDefinitionOnUnsupportedComponent(ComponentDto root, ComponentDto component) {
    i18n.put("qualifier." + component.qualifier(), "QualifierLabel");
    logInAsProjectAdministrator(root);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Setting 'my.key' cannot be set on a QualifierLabel");

    callForProjectSettingByKey("my.key", "My Value", component.getDbKey());
  }

  @Test
  public void fail_when_single_and_multi_value_provided() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Either 'value', 'values' or 'fieldValues' must be provided");

    call("my.key", "My Value", newArrayList("Another Value"), null, null);
  }

  @Test
  public void fail_when_multi_definition_and_single_value_provided() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .type(PropertyType.STRING)
      .multiValues(true)
      .build());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Parameter 'value' must be used for single value setting. Parameter 'values' must be used for multi value setting.");

    callForGlobalSetting("my.key", "My Value");
  }

  @Test
  public void fail_when_single_definition_and_multi_value_provided() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .type(PropertyType.STRING)
      .multiValues(false)
      .build());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Parameter 'value' must be used for single value setting. Parameter 'values' must be used for multi value setting.");

    callForMultiValueGlobalSetting("my.key", newArrayList("My Value"));
  }

  @Test
  public void fail_when_empty_values_on_one_property_set() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(newArrayList(
        PropertyFieldDefinition.build("firstField")
          .name("First Field")
          .type(PropertyType.STRING)
          .build(),
        PropertyFieldDefinition.build("secondField")
          .name("Second Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A non empty value must be provided");

    callForGlobalPropertySet("my.key", newArrayList(
      GSON.toJson(ImmutableMap.of("firstField", "firstValue", "secondField", "secondValue")),
      GSON.toJson(ImmutableMap.of("firstField", "", "secondField", "")),
      GSON.toJson(ImmutableMap.of("firstField", "yetFirstValue", "secondField", "yetSecondValue"))));
  }

  @Test
  public void do_not_fail_when_only_one_empty_value_on_one_property_set() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(newArrayList(
        PropertyFieldDefinition.build("firstField")
          .name("First Field")
          .type(PropertyType.STRING)
          .build(),
        PropertyFieldDefinition.build("secondField")
          .name("Second Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    callForGlobalPropertySet("my.key", newArrayList(
      GSON.toJson(ImmutableMap.of("firstField", "firstValue", "secondField", "secondValue")),
      GSON.toJson(ImmutableMap.of("firstField", "anotherFirstValue", "secondField", "")),
      GSON.toJson(ImmutableMap.of("firstField", "yetFirstValue", "secondField", "yetSecondValue"))));

    assertGlobalSetting("my.key", "1,2,3");
  }

  @Test
  public void fail_when_property_set_setting_is_not_defined() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Setting 'my.key' is undefined");

    callForGlobalPropertySet("my.key", singletonList("{\"field\":\"value\"}"));
  }

  @Test
  public void fail_when_property_set_with_unknown_field() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(newArrayList(
        PropertyFieldDefinition.build("field")
          .name("Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Unknown field key 'unknownField' for setting 'my.key'");

    callForGlobalPropertySet("my.key", newArrayList(GSON.toJson(ImmutableMap.of("field", "value", "unknownField", "anotherValue"))));
  }

  @Test
  public void fail_when_property_set_has_field_with_incorrect_type() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(newArrayList(
        PropertyFieldDefinition.build("field")
          .name("Field")
          .type(PropertyType.INTEGER)
          .build()))
      .build());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Error when validating setting with key 'my.key'. Field 'field' has incorrect value 'notAnInt'.");

    callForGlobalPropertySet("my.key", newArrayList(GSON.toJson(ImmutableMap.of("field", "notAnInt"))));
  }

  @Test
  public void fail_when_property_set_has_a_null_field_value() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(newArrayList(
        PropertyFieldDefinition.build("field")
          .name("Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A non empty value must be provided");

    callForGlobalPropertySet("my.key", newArrayList("{\"field\": null}"));
  }

  @Test
  public void fail_when_property_set_with_invalid_json() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(newArrayList(
        PropertyFieldDefinition.build("field")
          .name("Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("JSON 'incorrectJson:incorrectJson' does not respect expected format for setting 'my.key'. " +
      "Ex: {\"field1\":\"value1\", \"field2\":\"value2\"}");

    callForGlobalPropertySet("my.key", newArrayList("incorrectJson:incorrectJson"));
  }

  @Test
  public void fail_when_property_set_with_json_of_the_wrong_format() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(newArrayList(
        PropertyFieldDefinition.build("field")
          .name("Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("JSON '[{\"field\":\"v1\"}, {\"field\":\"v2\"}]' does not respect expected format for setting 'my.key'. " +
      "Ex: {\"field1\":\"value1\", \"field2\":\"value2\"}");

    callForGlobalPropertySet("my.key", newArrayList("[{\"field\":\"v1\"}, {\"field\":\"v2\"}]"));
  }

  @Test
  public void fail_when_property_set_on_component_of_global_setting() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(newArrayList(PropertyFieldDefinition.build("firstField").name("First Field").type(PropertyType.STRING).build()))
      .build());
    i18n.put("qualifier." + Qualifiers.PROJECT, "Project");
    ComponentDto project = db.components().insertPrivateProject();
    logInAsProjectAdministrator(project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Setting 'my.key' cannot be set on a Project");

    callForComponentPropertySet("my.key", newArrayList(
      GSON.toJson(ImmutableMap.of("firstField", "firstValue"))), project.getDbKey());
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    callForProjectSettingByKey("my.key", "My Value", branch.getDbKey());
  }

  @Test
  public void fail_when_component_not_found() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'unknown' not found");

    ws.newRequest()
      .setParam("key", "foo")
      .setParam("value", "2")
      .setParam("component", "unknown")
      .execute();
  }

  @Test
  public void fail_when_branch_not_found() {
    ComponentDto project = db.components().insertMainBranch();
    logInAsProjectAdministrator(project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    String settingKey = "not_allowed_on_branch";

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component '%s' on branch 'unknown' not found", branch.getKey()));

    ws.newRequest()
      .setParam("key", settingKey)
      .setParam("value", "2")
      .setParam("component", branch.getKey())
      .setParam("branch", "unknown")
      .execute();
  }

  @Test
  public void fail_when_setting_not_allowed_setting_on_branch() {
    ComponentDto project = db.components().insertMainBranch();
    logInAsProjectAdministrator(project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    String settingKey = "not_allowed_on_branch";

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Setting '%s' cannot be set on a branch", settingKey));

    ws.newRequest()
      .setParam("key", settingKey)
      .setParam("value", "2")
      .setParam("component", branch.getKey())
      .setParam("branch", branch.getBranch())
      .execute();
  }

  @Test
  public void fail_when_setting_key_is_defined_in_sonar_properties() {
    ComponentDto project = db.components().insertPrivateProject();
    logInAsProjectAdministrator(project);
    String settingKey = ProcessProperties.Property.JDBC_URL.getKey();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Setting '%s' can only be used in sonar.properties", settingKey));

    ws.newRequest()
      .setParam("key", settingKey)
      .setParam("value", "any value")
      .setParam("component", project.getKey())
      .execute();
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("set");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.since()).isEqualTo("6.1");
    assertThat(definition.params()).extracting(Param::key)
      .containsOnly("key", "value", "values", "fieldValues", "component", "branch", "pullRequest");

    Param branch = definition.param("branch");
    assertThat(branch.isInternal()).isTrue();
    assertThat(branch.since()).isEqualTo("6.6");
    assertThat(branch.description()).isEqualTo("Branch key. Only available on following settings : sonar.leak.period");
  }

  private void assertGlobalSetting(String key, String value) {
    PropertyDto result = dbClient.propertiesDao().selectGlobalProperty(key);

    assertThat(result)
      .extracting(PropertyDto::getKey, PropertyDto::getValue, PropertyDto::getResourceId)
      .containsExactly(key, value, null);
  }

  private void assertUserSetting(String key, String value, int userId) {
    List<PropertyDto> result = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setKey(key).setUserId(userId).build(), dbSession);

    assertThat(result).hasSize(1)
      .extracting(PropertyDto::getKey, PropertyDto::getValue, PropertyDto::getUserId)
      .containsExactly(tuple(key, value, userId));
  }

  private void assertComponentSetting(String key, String value, long componentId) {
    PropertyDto result = dbClient.propertiesDao().selectProjectProperty(componentId, key);

    assertThat(result)
      .isNotNull()
      .extracting(PropertyDto::getKey, PropertyDto::getValue, PropertyDto::getResourceId)
      .containsExactly(key, value, componentId);
  }

  private void callForGlobalSetting(@Nullable String key, @Nullable String value) {
    call(key, value, null, null, null);
  }

  private void callForMultiValueGlobalSetting(@Nullable String key, @Nullable List<String> values) {
    call(key, null, values, null, null);
  }

  private void callForGlobalPropertySet(@Nullable String key, @Nullable List<String> fieldValues) {
    call(key, null, null, fieldValues, null);
  }

  private void callForComponentPropertySet(@Nullable String key, @Nullable List<String> fieldValues, @Nullable String componentKey) {
    call(key, null, null, fieldValues, componentKey);
  }

  private void callForProjectSettingByKey(@Nullable String key, @Nullable String value, @Nullable String componentKey) {
    call(key, value, null, null, componentKey);
  }

  private void call(@Nullable String key, @Nullable String value, @Nullable List<String> values, @Nullable List<String> fieldValues, @Nullable String componentKey) {
    TestRequest request = ws.newRequest();
    if (key != null) {
      request.setParam("key", key);
    }
    if (value != null) {
      request.setParam("value", value);
    }
    if (values != null) {
      request.setMultiParam("values", values);
    }
    if (fieldValues != null) {
      request.setMultiParam("fieldValues", fieldValues);
    }
    if (componentKey != null) {
      request.setParam("component", componentKey);
    }
    request.execute();
  }

  private static class FakeSettingsNotifier extends SettingsChangeNotifier {

    private final DbClient dbClient;
    private boolean wasCalled = false;

    private FakeSettingsNotifier(DbClient dbClient) {
      this.dbClient = dbClient;
    }

    @Override
    public void onGlobalPropertyChange(String key, @Nullable String value) {
      wasCalled = true;
      PropertyDto property = dbClient.propertiesDao().selectGlobalProperty(key);

      assertThat(property.getValue()).isEqualTo(value);
    }

  }

  private void logInAsProjectAdministrator(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }

  private ComponentDto randomPublicOrPrivateProject() {
    return new Random().nextBoolean() ? db.components().insertPrivateProject() : db.components().insertPublicProject();
  }
}
