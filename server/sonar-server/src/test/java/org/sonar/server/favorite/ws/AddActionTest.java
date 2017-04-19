/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.favorite.ws;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonarqube.ws.client.favorite.FavoritesWsParameters.PARAM_COMPONENT;

public class AddActionTest {
  private static final String PROJECT_KEY = "project-key";
  private static final String PROJECT_UUID = "project-uuid";
  private static final int USER_ID = 123;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private FavoriteUpdater favoriteUpdater = new FavoriteUpdater(dbClient);
  private WsActionTester ws = new WsActionTester(new AddAction(userSession, dbClient, favoriteUpdater, new ComponentFinder(dbClient)));

  @Test
  public void add_a_project() {
    ComponentDto project = insertProjectAndPermissions();

    TestResponse result = call(PROJECT_KEY);

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    List<PropertyDto> favorites = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserId(USER_ID)
      .setKey("favourite")
      .build(), dbSession);
    assertThat(favorites).hasSize(1);
    PropertyDto favorite = favorites.get(0);
    assertThat(favorite)
      .extracting(PropertyDto::getResourceId, PropertyDto::getUserId, PropertyDto::getKey)
      .containsOnly(project.getId(), USER_ID, "favourite");
  }

  @Test
  public void add_a_file() {
    ComponentDto project = insertProjectAndPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    userSession.addProjectPermission(UserRole.USER, project, file);

    call(file.key());

    List<PropertyDto> favorites = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserId(USER_ID)
      .setKey("favourite")
      .build(), dbSession);
    assertThat(favorites).hasSize(1);
    PropertyDto favorite = favorites.get(0);
    assertThat(favorite)
      .extracting(PropertyDto::getResourceId, PropertyDto::getUserId, PropertyDto::getKey)
      .containsOnly(file.getId(), USER_ID, "favourite");
  }

  @Test
  public void fail_when_no_browse_permission_on_the_project() {
    ComponentDto project = insertProject();
    userSession.logIn();
    userSession.addProjectPermission(UserRole.ADMIN, project);

    expectedException.expect(ForbiddenException.class);

    call(PROJECT_KEY);
  }

  @Test
  public void fail_when_component_is_not_found() {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);

    call("P42");
  }

  @Test
  public void fail_when_user_is_not_authenticated() {
    insertProject();

    expectedException.expect(UnauthorizedException.class);

    call(PROJECT_KEY);
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("add");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.param("component").isRequired()).isTrue();
  }

  private ComponentDto insertProject() {
    return db.components().insertComponent(newPrivateProjectDto(db.organizations().insert(), PROJECT_UUID).setKey(PROJECT_KEY));
  }

  private ComponentDto insertProjectAndPermissions() {
    ComponentDto project = insertProject();
    userSession
      .logIn()
      .setUserId(USER_ID)
      .addProjectPermission(UserRole.USER, project);

    return project;
  }

  private TestResponse call(@Nullable String componentKey) {
    TestRequest request = ws.newRequest();
    setNullable(componentKey, c -> request.setParam(PARAM_COMPONENT, c));

    return request.execute();
  }
}
