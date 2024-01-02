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

import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.process.ProcessProperties;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;

public class ResetActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private I18nRule i18n = new I18nRule();
  private PropertyDbTester propertyDb = new PropertyDbTester(db);
  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentFinder componentFinder = TestComponentFinder.from(db);
  private PropertyDefinitions definitions = new PropertyDefinitions(System2.INSTANCE);
  private SettingsUpdater settingsUpdater = new SettingsUpdater(dbClient, definitions);
  private SettingValidations settingValidations = new SettingValidations(definitions, dbClient, i18n);
  private ComponentDto project;
  private ResetAction underTest = new ResetAction(dbClient, componentFinder, settingsUpdater, userSession, definitions, settingValidations);
  private WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUp() {
    project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto());
  }

  @Test
  public void remove_global_setting() {
    logInAsSystemAdministrator();
    definitions.addComponent(PropertyDefinition.builder("foo").build());
    propertyDb.insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("foo").setValue("one"));

    executeRequestOnGlobalSetting("foo");
    assertGlobalPropertyDoesNotExist("foo");
  }

  @Test
  public void remove_global_setting_even_if_not_defined() {
    logInAsSystemAdministrator();
    propertyDb.insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("foo").setValue("one"));

    executeRequestOnGlobalSetting("foo");
    assertGlobalPropertyDoesNotExist("foo");
  }

  @Test
  public void remove_component_setting() {
    logInAsProjectAdmin();
    definitions.addComponent(PropertyDefinition.builder("foo").onQualifiers(PROJECT).build());
    propertyDb.insertProperties(null, null, null, null, newComponentPropertyDto(project).setKey("foo").setValue("value"));

    executeRequestOnProjectSetting("foo");
    assertProjectPropertyDoesNotExist("foo");
  }

  @Test
  public void remove_component_setting_even_if_not_defined() {
    logInAsProjectAdmin();
    propertyDb.insertProperties(null, null, null, null, newComponentPropertyDto(project).setKey("foo").setValue("value"));

    executeRequestOnProjectSetting("foo");
    assertProjectPropertyDoesNotExist("foo");
  }

  @Test
  public void remove_hidden_setting() {
    logInAsSystemAdministrator();
    definitions.addComponent(PropertyDefinition.builder("foo").hidden().build());
    propertyDb.insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("foo").setValue("one"));

    executeRequestOnGlobalSetting("foo");
    assertGlobalPropertyDoesNotExist("foo");
  }

  @Test
  public void ignore_project_setting_when_removing_global_setting() {
    logInAsSystemAdministrator();
    propertyDb.insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("foo").setValue("one"));
    propertyDb.insertProperties(null, project.getKey(), project.name(), project.qualifier(),
      newComponentPropertyDto(project).setKey("foo").setValue("value"));

    executeRequestOnGlobalSetting("foo");

    assertGlobalPropertyDoesNotExist("foo");
    assertProjectPropertyExists("foo");
  }

  @Test
  public void ignore_global_setting_when_removing_project_setting() {
    logInAsProjectAdmin();
    propertyDb.insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("foo").setValue("one"));
    propertyDb.insertProperties(null, project.getKey(), project.name(), project.qualifier(),
      newComponentPropertyDto(project).setKey("foo").setValue("value"));

    executeRequestOnProjectSetting("foo");

    assertGlobalPropertyExists("foo");
    assertProjectPropertyDoesNotExist("foo");
  }

  @Test
  public void ignore_user_setting_when_removing_global_setting() {
    logInAsSystemAdministrator();
    UserDto user = dbClient.userDao().insert(dbSession, UserTesting.newUserDto());
    propertyDb.insertProperties(user.getLogin(), null, null, null, newUserPropertyDto("foo", "one", user));

    executeRequestOnGlobalSetting("foo");
    assertUserPropertyExists("foo", user);
  }

  @Test
  public void ignore_user_setting_when_removing_project_setting() {
    logInAsProjectAdmin();
    UserDto user = dbClient.userDao().insert(dbSession, UserTesting.newUserDto());
    propertyDb.insertProperties(user.getLogin(), null, null, null, newUserPropertyDto("foo", "one", user));

    executeRequestOnProjectSetting("foo");
    assertUserPropertyExists("foo", user);
  }

  @Test
  public void ignore_unknown_setting_key() {
    logInAsSystemAdministrator();

    executeRequestOnGlobalSetting("unknown");
  }

  @Test
  public void remove_setting_by_deprecated_key() {
    logInAsSystemAdministrator();
    definitions.addComponent(PropertyDefinition.builder("foo").deprecatedKey("old").build());
    propertyDb.insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("foo").setValue("one"));

    executeRequestOnGlobalSetting("old");
    assertGlobalPropertyDoesNotExist("foo");
  }

  @Test
  public void remove_setting_on_branch() {
    ComponentDto project = db.components().insertPublicProject();
    String branchName = randomAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchName));
    definitions.addComponent(PropertyDefinition.builder("foo").onQualifiers(PROJECT).build());
    propertyDb.insertProperties(null, branch.name(), null, null, newComponentPropertyDto(branch).setKey("foo").setValue("value"));
    userSession.logIn().addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("keys", "foo")
      .setParam("component", branch.getKey())
      .setParam("branch", branchName)
      .execute();

    assertProjectPropertyDoesNotExist(branch, "foo");
  }

  @Test
  public void empty_204_response() {
    logInAsSystemAdministrator();
    TestResponse result = ws.newRequest()
      .setParam("keys", "my.key")
      .execute();

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    assertThat(result.getInput()).isEmpty();
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNull();
    assertThat(action.params()).extracting(Param::key).containsExactlyInAnyOrder("keys", "component", "branch", "pullRequest");
  }

  @Test
  public void throw_ForbiddenException_if_global_setting_and_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();
    definitions.addComponent(PropertyDefinition.builder("foo").build());

    assertThatThrownBy(() -> executeRequestOnGlobalSetting("foo"))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_ForbiddenException_if_project_setting_and_not_project_administrator() {
    userSession.logIn().addProjectPermission(USER, project);
    definitions.addComponent(PropertyDefinition.builder("foo").build());

    assertThatThrownBy(() -> {
      executeRequestOnComponentSetting("foo", project);
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_ForbiddenException_if_project_setting_and_system_administrator() {
    logInAsSystemAdministrator();
    definitions.addComponent(PropertyDefinition.builder("foo").build());

    assertThatThrownBy(() -> {
      executeRequestOnComponentSetting("foo", project);
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_when_not_global_and_no_component() {
    logInAsSystemAdministrator();
    definitions.addComponent(PropertyDefinition.builder("foo")
      .onlyOnQualifiers(VIEW)
      .build());

    assertThatThrownBy(() -> {
      executeRequestOnGlobalSetting("foo");
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Setting 'foo' cannot be global");
  }

  @Test
  public void fail_when_qualifier_not_included() {
    userSession.logIn().addProjectPermission(ADMIN, project);
    definitions.addComponent(PropertyDefinition.builder("foo")
      .onQualifiers(VIEW)
      .build());
    i18n.put("qualifier." + PROJECT, "project");

    assertThatThrownBy(() -> {
      executeRequestOnComponentSetting("foo", project);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Setting 'foo' cannot be set on a project");
  }

  @Test
  public void fail_to_reset_setting_component_when_setting_is_global() {
    userSession.logIn().addProjectPermission(ADMIN, project);

    definitions.addComponent(PropertyDefinition.builder("foo").build());
    i18n.put("qualifier." + PROJECT, "project");

    assertThatThrownBy(() -> {
      executeRequestOnComponentSetting("foo", project);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Setting 'foo' cannot be set on a project");
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
    ComponentDto view = db.components().insertPublicPortfolio();
    succeedForPropertyWithoutDefinitionAndValidComponent(view, view);
  }

  @Test
  public void succeed_for_property_without_definition_when_set_on_subview_component() {
    ComponentDto view = db.components().insertPublicPortfolio();
    ComponentDto subview = db.components().insertComponent(ComponentTesting.newSubPortfolio(view));
    succeedForPropertyWithoutDefinitionAndValidComponent(view, subview);
  }

  @Test
  public void fail_for_property_without_definition_when_set_on_projectCopy_component() {
    ComponentDto view = db.components().insertPublicPortfolio();
    ComponentDto projectCopy = db.components().insertComponent(ComponentTesting.newProjectCopy("a", db.components().insertPrivateProject(), view));

    failForPropertyWithoutDefinitionOnUnsupportedComponent(view, projectCopy);
  }

  @Test
  public void fail_when_component_not_found() {
    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("keys", "foo")
        .setParam("component", "unknown")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'unknown' not found");
  }

  @Test
  public void fail_when_branch_not_found() {
    ComponentDto project = db.components().insertPublicProject();
    logInAsProjectAdmin(project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    String settingKey = "not_allowed_on_branch";

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("keys", settingKey)
        .setParam("component", branch.getKey())
        .setParam("branch", "unknown")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component '%s' on branch 'unknown' not found", branch.getKey()));
  }

  @Test
  public void fail_when_setting_key_is_defined_in_sonar_properties() {
    ComponentDto project = db.components().insertPrivateProject();
    logInAsProjectAdmin(project);
    String settingKey = ProcessProperties.Property.JDBC_URL.getKey();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("keys", settingKey)
        .setParam("component", project.getKey())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Setting '%s' can only be used in sonar.properties", settingKey));
  }

  private void succeedForPropertyWithoutDefinitionAndValidComponent(ComponentDto root, ComponentDto module) {
    logInAsProjectAdmin(root);

    executeRequestOnComponentSetting("foo", module);
  }

  private void failForPropertyWithoutDefinitionOnUnsupportedComponent(ComponentDto root, ComponentDto component) {
    i18n.put("qualifier." + component.qualifier(), "QualifierLabel");
    logInAsProjectAdmin(root);

    assertThatThrownBy(() -> {
      executeRequestOnComponentSetting("foo", component);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Setting 'foo' cannot be set on a QualifierLabel");
  }

  private void executeRequestOnGlobalSetting(String key) {
    executeRequest(key, null);
  }

  private void executeRequestOnProjectSetting(String key) {
    executeRequest(key, project.getKey());
  }

  private void executeRequestOnComponentSetting(String key, ComponentDto componentDto) {
    executeRequest(key, componentDto.getKey());
  }

  private void executeRequest(String key, @Nullable String componentKey) {
    TestRequest request = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("keys", key);
    if (componentKey != null) {
      request.setParam("component", componentKey);
    }
    request.execute();
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private void logInAsProjectAdmin() {
    userSession.logIn().addProjectPermission(ADMIN, project);
  }

  private void logInAsProjectAdmin(ComponentDto root) {
    userSession.logIn().addProjectPermission(ADMIN, root);
  }

  private void assertGlobalPropertyDoesNotExist(String key) {
    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, key)).isNull();
  }

  private void assertGlobalPropertyExists(String key) {
    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, key)).isNotNull();
  }

  private void assertProjectPropertyDoesNotExist(ComponentDto component, String key) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentUuid(component.uuid()).setKey(key).build(), dbSession)).isEmpty();
  }

  private void assertProjectPropertyDoesNotExist(String key) {
    assertProjectPropertyDoesNotExist(project, key);
  }

  private void assertProjectPropertyExists(String key) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentUuid(project.uuid()).setKey(key).build(), dbSession)).isNotEmpty();
  }

  private void assertUserPropertyExists(String key, UserDto user) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
        .setKey(key)
        .setUserUuid(user.getUuid())
        .build(),
      dbSession)).isNotEmpty();
  }

  private ComponentDto randomPublicOrPrivateProject() {
    return new Random().nextBoolean() ? db.components().insertPrivateProject() : db.components().insertPublicProject();
  }

}
