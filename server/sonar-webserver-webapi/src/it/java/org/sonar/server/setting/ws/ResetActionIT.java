/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.process.ProcessProperties;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.ProjectPermission.ADMIN;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentQualifiers.VIEW;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;

public class ResetActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final I18nRule i18n = new I18nRule();
  private final PropertyDbTester propertyDb = new PropertyDbTester(db);
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final PropertyDefinitions definitions = new PropertyDefinitions(System2.INSTANCE);
  private final SettingsUpdater settingsUpdater = new SettingsUpdater(dbClient, definitions);
  private final SettingValidations settingValidations = new SettingValidations(definitions, dbClient, i18n);
  private ProjectDto project;
  private final ResetAction underTest = new ResetAction(dbClient, settingsUpdater, userSession, definitions, settingValidations);
  private final WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUp() {
    project = db.components().insertPrivateProject().getProjectDto();
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
    propertyDb.insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("foo").setValue("value"));

    executeRequestOnGlobalSetting("foo");

    assertGlobalPropertyDoesNotExist("foo");
    assertProjectPropertyExists("foo");
  }

  @Test
  public void ignore_global_setting_when_removing_project_setting() {
    logInAsProjectAdmin();
    propertyDb.insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("foo").setValue("one"));
    propertyDb.insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
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
    assertThat(action.params()).extracting(Param::key).containsExactlyInAnyOrder("keys", "component");
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

    assertThatThrownBy(() -> executeRequestOnComponentSetting("foo", project))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_ForbiddenException_if_project_setting_and_system_administrator() {
    logInAsSystemAdministrator();
    definitions.addComponent(PropertyDefinition.builder("foo").build());

    assertThatThrownBy(() -> executeRequestOnComponentSetting("foo", project))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_when_not_global_and_no_component() {
    logInAsSystemAdministrator();
    definitions.addComponent(PropertyDefinition.builder("foo")
      .onlyOnQualifiers(VIEW)
      .build());

    assertThatThrownBy(() -> executeRequestOnGlobalSetting("foo"))
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

    assertThatThrownBy(() -> executeRequestOnComponentSetting("foo", project))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Setting 'foo' cannot be set on a project");
  }

  @Test
  public void fail_to_reset_setting_component_when_setting_is_global() {
    userSession.logIn().addProjectPermission(ADMIN, project);

    definitions.addComponent(PropertyDefinition.builder("foo").build());
    i18n.put("qualifier." + PROJECT, "project");

    assertThatThrownBy(() -> executeRequestOnComponentSetting("foo", project))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Setting 'foo' cannot be set on a project");
  }

  @Test
  public void succeed_for_property_without_definition_when_set_on_project_component() {
    ProjectDto project = randomPublicOrPrivateProject();
    succeedForPropertyWithoutDefinitionAndValidComponent(project);
  }

  @Test
  public void succeed_for_property_without_definition_when_set_on_view_component() {
    PortfolioDto view = db.components().insertPublicPortfolioDto();
    succeedForPropertyWithoutDefinitionAndValidComponent(view);
  }

  @Test
  public void fail_when_component_not_found() {
    TestRequest request = ws.newRequest()
      .setParam("keys", "foo")
      .setParam("component", "unknown");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'unknown' not found");
  }

  @Test
  public void fail_when_setting_key_is_defined_in_sonar_properties() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    logInAsProjectAdmin(project);
    String settingKey = ProcessProperties.Property.JDBC_URL.getKey();

    TestRequest request = ws.newRequest()
      .setParam("keys", settingKey)
      .setParam("component", project.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Setting '%s' can only be used in sonar.properties", settingKey));
  }

  private void succeedForPropertyWithoutDefinitionAndValidComponent(ProjectDto project) {
    logInAsProjectAdmin(project);
    executeRequestOnComponentSetting("foo", project);
  }

  private void succeedForPropertyWithoutDefinitionAndValidComponent(PortfolioDto project) {
    logInAsProjectAdmin(project);
    executeRequestOnComponentSetting("foo", project);
  }

  private void executeRequestOnGlobalSetting(String key) {
    executeRequest(key, null);
  }

  private void executeRequestOnProjectSetting(String key) {
    executeRequest(key, project.getKey());
  }

  private void executeRequestOnComponentSetting(String key, EntityDto entity) {
    executeRequest(key, entity.getKey());
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

  private void logInAsProjectAdmin(ProjectDto root) {
    userSession.logIn().addProjectPermission(ADMIN, root);
  }

  private void logInAsProjectAdmin(PortfolioDto root) {
    userSession.logIn().addPortfolioPermission(ADMIN, root);
  }

  private void assertGlobalPropertyDoesNotExist(String key) {
    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, key)).isNull();
  }

  private void assertGlobalPropertyExists(String key) {
    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, key)).isNotNull();
  }

  private void assertProjectPropertyDoesNotExist(EntityDto entity, String key) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setEntityUuid(entity.getUuid()).setKey(key).build(), dbSession)).isEmpty();
  }

  private void assertProjectPropertyDoesNotExist(String key) {
    assertProjectPropertyDoesNotExist(project, key);
  }

  private void assertProjectPropertyExists(String key) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setEntityUuid(project.getUuid()).setKey(key).build(), dbSession)).isNotEmpty();
  }

  private void assertUserPropertyExists(String key, UserDto user) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey(key)
      .setUserUuid(user.getUuid())
      .build(),
      dbSession)).isNotEmpty();
  }

  private ProjectDto randomPublicOrPrivateProject() {
    return new Random().nextBoolean() ? db.components().insertPrivateProject().getProjectDto() : db.components().insertPublicProject().getProjectDto();
  }

}
