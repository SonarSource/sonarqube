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
package org.sonar.server.favorite.ws;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.favorite.ws.FavoritesWsParameters.PARAM_COMPONENT;

public class RemoveActionTest {
  private static final String PROJECT_KEY = "project-key";
  private static final String PROJECT_UUID = "project-uuid";
  private UserDto user;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final FavoriteUpdater favoriteUpdater = new FavoriteUpdater(dbClient);
  private final WsActionTester ws = new WsActionTester(new RemoveAction(userSession, dbClient, favoriteUpdater, TestComponentFinder.from(db)));

  @Before
  public void before() {
    user = db.users().insertUser();
  }

  @Test
  public void remove_a_favorite_project() {
    ComponentDto project = insertProjectAndPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    db.favorites().add(project, user.getUuid(), user.getLogin());
    db.favorites().add(file, user.getUuid(), user.getLogin());

    TestResponse result = call(PROJECT_KEY);

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    assertThat(db.favorites().hasFavorite(project, user.getUuid())).isFalse();
    assertThat(db.favorites().hasFavorite(file, user.getUuid())).isTrue();
  }

  @Test
  public void fail_if_not_already_a_favorite() {
    ComponentDto componentDto = insertProjectAndPermissions();

    assertThatThrownBy(() -> call(PROJECT_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Component '" + PROJECT_KEY + "' (uuid: "+componentDto.uuid()+") is not a favorite");
  }

  @Test
  public void fail_when_component_is_not_found() {
    userSession.logIn();

    assertThatThrownBy(() -> call("P42"))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_user_is_not_authenticated() {
    insertProject();

    assertThatThrownBy(() -> call(PROJECT_KEY))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("remove");
    assertThat(definition.isPost()).isTrue();
    WebService.Param param = definition.param("component");
    assertThat(param).isNotNull();
    assertThat(param.isRequired()).isTrue();
  }

  private ComponentDto insertProject() {
    return db.components().insertComponent(newPrivateProjectDto(PROJECT_UUID).setKey(PROJECT_KEY));
  }

  private ComponentDto insertProjectAndPermissions() {
    userSession.logIn(user);

    return insertProject();
  }

  private TestResponse call(@Nullable String componentKey) {
    TestRequest request = ws.newRequest();
    ofNullable(componentKey).ifPresent(c -> request.setParam(PARAM_COMPONENT, c));

    return request.execute();
  }
}
