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

import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.favorite.FavoriteFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.Paging;
import org.sonarqube.ws.Favorites.Favorite;
import org.sonarqube.ws.Favorites.SearchResponse;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.WsRequest.Method.POST;

public class SearchActionIT {
  private String userUuid;
  private String userLogin;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  private final DbClient dbClient = db.getDbClient();

  private final FavoriteFinder favoriteFinder = new FavoriteFinder(dbClient, userSession);

  private final WsActionTester ws = new WsActionTester(new SearchAction(favoriteFinder, userSession));

  @Before
  public void before() {
    UserDto userDto = db.users().insertUser();
    userSession.logIn(userDto);
    userUuid = userDto.getUuid();
    userLogin = userDto.getLogin();
  }

  @Test
  public void return_favorites() {
    addPermissionAndFavorite(db.components().insertPrivateProject("P1", c -> c.setKey("K1").setName("N1")).getProjectDto());
    addPermissionAndFavorite(db.components().insertPrivateProject("P2", c -> c.setKey("K2").setName("N2")).getProjectDto());

    SearchResponse result = call();

    assertThat(result.getPaging())
      .extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(1, 100, 2);
    assertThat(result.getFavoritesList())
      .extracting(Favorite::getKey, Favorite::getName, Favorite::getQualifier)
      .containsOnly(
        tuple("K1", "N1", PROJECT),
        tuple("K2", "N2", PROJECT));
  }

  @Test
  public void empty_list() {
    SearchResponse result = call();

    assertThat(result.getFavoritesCount()).isZero();
    assertThat(result.getFavoritesList()).isEmpty();
  }

  @Test
  public void filter_authorized_components() {
    addPermissionAndFavorite(db.components().insertPrivateProject(c -> c.setKey("K1")).getProjectDto());
    ProjectDto unauthorizedProject = db.components().insertPrivateProject().getProjectDto();
    db.favorites().add(unauthorizedProject, userUuid, userLogin);

    SearchResponse result = call();

    assertThat(result.getFavoritesCount()).isOne();
    assertThat(result.getFavorites(0).getKey()).isEqualTo("K1");
  }

  @Test
  public void paginate_results() {
    IntStream.rangeClosed(1, 9)
      .forEach(i -> addPermissionAndFavorite(db.components().insertPrivateProject(c -> c.setKey("K" + i).setName("N" + i)).getProjectDto()));
    ProjectDto unauthorizedProject = db.components().insertPrivateProject().getProjectDto();
    db.favorites().add(unauthorizedProject, userUuid, userLogin);

    SearchResponse result = call(2, 3);

    assertThat(result.getFavoritesCount()).isEqualTo(3);
    assertThat(result.getFavoritesList())
      .extracting(Favorite::getKey)
      .containsExactly("K4", "K5", "K6");
  }

  @Test
  public void return_only_users_favorite() {
    addPermissionAndFavorite(db.components().insertPrivateProject(c -> c.setKey("K1")).getProjectDto());
    ProjectDto otherUserFavorite = db.components().insertPrivateProject(c -> c.setKey("K42")).getProjectDto();
    db.favorites().add(otherUserFavorite, "42", userLogin);
    db.commit();

    SearchResponse result = call();

    assertThat(result.getFavoritesList()).extracting(Favorite::getKey).containsExactly("K1");
  }

  @Test
  public void favorites_ordered_by_name() {
    addPermissionAndFavorite(db.components().insertPrivateProject(c -> c.setName("N2")).getProjectDto());
    addPermissionAndFavorite(db.components().insertPrivateProject(c -> c.setName("N3")).getProjectDto());
    addPermissionAndFavorite(db.components().insertPrivateProject(c -> c.setName("N1")).getProjectDto());

    SearchResponse result = call();

    assertThat(result.getFavoritesList()).extracting(Favorite::getName)
      .containsExactly("N1", "N2", "N3");
  }

  @Test
  public void json_example() {
    addPermissionAndFavorite(db.components().insertPrivateProject(c -> c.setKey("K1").setName("Samba")).getProjectDto());
    addPermissionAndFavorite(db.components().insertPrivateProject(c -> c.setKey("K2").setName("Apache HBase")).getProjectDto());
    addPermissionAndFavorite(db.components().insertPrivateProject(c -> c.setKey("K3").setName("JDK9")).getProjectDto());

    String result = ws.newRequest().execute().getInput();

    assertThat(ws.getDef().responseExampleAsString()).isNotNull();
    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("search");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void fail_if_not_authenticated() {
    userSession.anonymous();

    assertThatThrownBy(this::call)
      .isInstanceOf(UnauthorizedException.class);
  }

  private void addPermissionAndFavorite(ProjectDto component) {
    db.favorites().add(component, userUuid, userLogin);
    db.commit();
    userSession.addProjectPermission(UserRole.USER, component);
  }

  private SearchResponse call(@Nullable Integer page, @Nullable Integer pageSize) {
    TestRequest request = ws.newRequest()
      .setMethod(POST.name());
    ofNullable(page).ifPresent(p -> request.setParam(Param.PAGE, p.toString()));
    ofNullable(pageSize).ifPresent(ps -> request.setParam(Param.PAGE_SIZE, ps.toString()));

    return request.executeProtobuf(SearchResponse.class);
  }

  private SearchResponse call() {
    return call(null, null);
  }
}
