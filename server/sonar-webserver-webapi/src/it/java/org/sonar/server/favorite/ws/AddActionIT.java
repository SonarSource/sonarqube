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

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.server.favorite.ws.FavoritesWsParameters.PARAM_COMPONENT;

public class AddActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final FavoriteUpdater favoriteUpdater = new FavoriteUpdater(dbClient);
  private final WsActionTester ws = new WsActionTester(new AddAction(userSession, dbClient, favoriteUpdater));

  @Test
  public void add_a_project() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(USER, project);

    TestResponse result = call(project.getKey());

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    List<PropertyDto> favorites = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserUuid(user.getUuid())
      .setKey("favourite")
      .build(), dbSession);
    assertThat(favorites).hasSize(1);
    PropertyDto favorite = favorites.get(0);
    assertThat(favorite)
      .extracting(PropertyDto::getEntityUuid, PropertyDto::getUserUuid, PropertyDto::getKey)
      .containsOnly(project.getUuid(), user.getUuid(), "favourite");
  }

  @Test
  public void fail_when_no_browse_permission_on_the_project() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    assertThatThrownBy(() -> call(project.getKey()))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_component_is_not_found() {
    userSession.logIn();

    assertThatThrownBy(() ->  call("P42"))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_user_is_not_authenticated() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    assertThatThrownBy(() -> call(project.getKey()))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_on_file() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(USER, project.getProjectDto());

    assertThatThrownBy(() -> call(file.getKey()))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity with key '" + file.getKey() + "' not found");
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("add");
    assertThat(definition.isPost()).isTrue();
    WebService.Param param = definition.param("component");
    assertThat(param).isNotNull();
    assertThat(param.isRequired()).isTrue();
  }

  private TestResponse call(@Nullable String componentKey) {
    TestRequest request = ws.newRequest();
    ofNullable(componentKey).ifPresent(c -> request.setParam(PARAM_COMPONENT, c));

    return request.execute();
  }
}
