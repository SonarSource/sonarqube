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

import javax.annotation.Nullable;
import org.junit.Before;
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
import org.sonar.db.property.PropertyDbTester;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.DASHBOARD_SHARING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;

public class ResetActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  PropertyDbTester propertyDb = new PropertyDbTester(db);
  ComponentDbTester componentDb = new ComponentDbTester(db);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();
  ComponentFinder componentFinder = new ComponentFinder(dbClient);
  PropertyDefinitions definitions = new PropertyDefinitions();
  SettingsUpdater settingsUpdater = new SettingsUpdater(dbClient, definitions);

  ComponentDto project;

  ResetAction underTest = new ResetAction(dbClient, componentFinder, settingsUpdater, userSession, definitions);
  WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUp() throws Exception {
    project = componentDb.insertComponent(newProjectDto());
  }

  @Test
  public void remove_global_setting() throws Exception {
    setUserAsSystemAdmin();
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));

    executeRequestOnGlobalSetting("foo");
    assertGlobalPropertyDoesNotExist("foo");
  }

  @Test
  public void remove_component_setting() throws Exception {
    setUserAsProjectAdmin();
    propertyDb.insertProperties(newComponentPropertyDto(project).setKey("foo").setValue("value"));

    executeRequestOnProjectSetting("foo");
    assertProjectPropertyDoesNotExist("foo");
  }

  @Test
  public void ignore_project_setting_when_removing_global_setting() throws Exception {
    setUserAsSystemAdmin();
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));
    propertyDb.insertProperties(newComponentPropertyDto(project).setKey("foo").setValue("value"));

    executeRequestOnGlobalSetting("foo");

    assertGlobalPropertyDoesNotExist("foo");
    assertProjectPropertyExists("foo");
  }

  @Test
  public void ignore_global_setting_when_removing_project_setting() throws Exception {
    setUserAsProjectAdmin();
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));
    propertyDb.insertProperties(newComponentPropertyDto(project).setKey("foo").setValue("value"));

    executeRequestOnProjectSetting("foo");

    assertGlobalPropertyExists("foo");
    assertProjectPropertyDoesNotExist("foo");
  }

  @Test
  public void ignore_user_setting_when_removing_global_setting() throws Exception {
    setUserAsSystemAdmin();
    UserDto user = dbClient.userDao().insert(dbSession, UserTesting.newUserDto());
    propertyDb.insertProperties(newUserPropertyDto("foo", "one", user));

    executeRequestOnGlobalSetting("foo");
    assertUserPropertyExists("foo", user);
  }

  @Test
  public void ignore_user_setting_when_removing_project_setting() throws Exception {
    setUserAsProjectAdmin();
    UserDto user = dbClient.userDao().insert(dbSession, UserTesting.newUserDto());
    propertyDb.insertProperties(newUserPropertyDto("foo", "one", user));

    executeRequestOnProjectSetting("foo");
    assertUserPropertyExists("foo", user);
  }

  @Test
  public void ignore_unknown_setting_key() throws Exception {
    setUserAsSystemAdmin();

    executeRequestOnGlobalSetting("unknown");
  }

  @Test
  public void remove_setting_by_deprecated_key() throws Exception {
    setUserAsSystemAdmin();
    definitions.addComponent(PropertyDefinition.builder("foo").deprecatedKey("old").build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));

    executeRequestOnGlobalSetting("old");
    assertGlobalPropertyDoesNotExist("foo");
  }

  @Test
  public void empty_204_response() {
    setUserAsSystemAdmin();
    TestResponse result = ws.newRequest()
      .setParam("key", "my.key")
      .execute();

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    assertThat(result.getInput()).isEmpty();
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNull();
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void fail_when_not_system_admin() throws Exception {
    userSession.login("not-admin").setGlobalPermissions(DASHBOARD_SHARING);
    definitions.addComponent(PropertyDefinition.builder("foo").build());

    expectedException.expect(ForbiddenException.class);

    executeRequestOnGlobalSetting("foo");
  }

  @Test
  public void fail_when_not_project_admin() throws Exception {
    userSession.login("project-admin").addProjectUuidPermissions(USER, project.uuid());
    definitions.addComponent(PropertyDefinition.builder("foo").build());

    expectedException.expect(ForbiddenException.class);

    executeRequestOnComponentSetting("foo", project);
  }

  private void executeRequestOnGlobalSetting(String key) {
    executeRequest(key, null, null);
  }

  private void executeRequestOnProjectSetting(String key) {
    executeRequest(key, project.uuid(), null);
  }

  private void executeRequestOnComponentSetting(String key, ComponentDto componentDto) {
    executeRequest(key, componentDto.uuid(), null);
  }

  private void executeRequest(String key, @Nullable String componentId, @Nullable String componentKey) {
    TestRequest request = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("key", key);
    if (componentId != null) {
      request.setParam("componentId", componentId);
    }
    if (componentKey != null) {
      request.setParam("componentKey", componentKey);
    }
    request.execute();
  }

  private void setUserAsSystemAdmin() {
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

  private void setUserAsProjectAdmin() {
    userSession.login("project-admin").addProjectUuidPermissions(ADMIN, project.uuid());
  }

  private void assertGlobalPropertyDoesNotExist(String key) {
    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, key)).isNull();
  }

  private void assertGlobalPropertyExists(String key) {
    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, key)).isNotNull();
  }

  private void assertProjectPropertyDoesNotExist(String key) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(project.getId()).setKey(key).build(), dbSession)).isEmpty();
  }

  private void assertProjectPropertyExists(String key) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(project.getId()).setKey(key).build(), dbSession)).isNotEmpty();
  }

  private void assertUserPropertyExists(String key, UserDto user) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey(key)
      .setUserId(user.getId().intValue())
      .build(),
      dbSession)).isNotEmpty();
  }

}
