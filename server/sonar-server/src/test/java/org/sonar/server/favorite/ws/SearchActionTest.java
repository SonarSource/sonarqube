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
package org.sonar.server.favorite.ws;

import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.WsRequest.Method.POST;

public class SearchActionTest {
  private static final int USER_ID = 123;
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setUserId(USER_ID);
  @Rule
  public DbTester db = DbTester.create();
  private DbClient dbClient = db.getDbClient();

  private FavoriteFinder favoriteFinder = new FavoriteFinder(dbClient, userSession);

  private WsActionTester ws = new WsActionTester(new SearchAction(favoriteFinder, dbClient, userSession));

  @Test
  public void return_favorites() {
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization(), "P1").setDbKey("K1").setName("N1");
    addComponent(project);
    addComponent(newFileDto(project).setDbKey("K11").setName("N11"));
    addComponent(newPrivateProjectDto(db.getDefaultOrganization(), "P2").setDbKey("K2").setName("N2"));

    SearchResponse result = call();

    assertThat(result.getPaging())
      .extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(1, 100, 3);
    assertThat(result.getFavoritesList())
      .extracting(Favorite::getKey, Favorite::getName, Favorite::getQualifier)
      .containsOnly(
        tuple("K1", "N1", PROJECT),
        tuple("K11", "N11", FILE),
        tuple("K2", "N2", PROJECT));
  }

  @Test
  public void empty_list() {
    SearchResponse result = call();

    assertThat(result.getFavoritesCount()).isEqualTo(0);
    assertThat(result.getFavoritesList()).isEmpty();
  }

  @Test
  public void filter_authorized_components() {
    OrganizationDto organizationDto = db.organizations().insert();
    addComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey("K1"));
    ComponentDto unauthorizedProject = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto));
    db.favorites().add(unauthorizedProject, USER_ID);

    SearchResponse result = call();

    assertThat(result.getFavoritesCount()).isEqualTo(1);
    assertThat(result.getFavorites(0).getKey()).isEqualTo("K1");
  }

  @Test
  public void paginate_results() {
    IntStream.rangeClosed(1, 9)
      .forEach(i -> addComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey("K" + i).setName("N" + i)));
    ComponentDto unauthorizedProject = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()));
    db.favorites().add(unauthorizedProject, USER_ID);

    SearchResponse result = call(2, 3);

    assertThat(result.getFavoritesCount()).isEqualTo(3);
    assertThat(result.getFavoritesList())
      .extracting(Favorite::getKey)
      .containsExactly("K4", "K5", "K6");

  }

  @Test
  public void return_only_users_favorite() {
    OrganizationDto organizationDto = db.organizations().insert();
    addComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey("K1"));
    ComponentDto otherUserFavorite = ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey("K42");
    db.components().insertComponent(otherUserFavorite);
    db.favorites().add(otherUserFavorite, 42);
    db.commit();

    SearchResponse result = call();

    assertThat(result.getFavoritesList()).extracting(Favorite::getKey).containsExactly("K1");
  }

  @Test
  public void favorites_ordered_by_name() {
    OrganizationDto organizationDto = db.organizations().insert();
    addComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setName("N2"));
    addComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setName("N3"));
    addComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setName("N1"));

    SearchResponse result = call();

    assertThat(result.getFavoritesList()).extracting(Favorite::getName)
      .containsExactly("N1", "N2", "N3");
  }

  @Test
  public void json_example() {
    OrganizationDto organization1 = db.organizations().insertForKey("my-org");
    OrganizationDto organization2 = db.organizations().insertForKey("openjdk");
    addComponent(ComponentTesting.newPrivateProjectDto(organization1).setDbKey("K1").setName("Samba"));
    addComponent(ComponentTesting.newPrivateProjectDto(organization1).setDbKey("K2").setName("Apache HBase"));
    addComponent(ComponentTesting.newPrivateProjectDto(organization2).setDbKey("K3").setName("JDK9"));

    String result = ws.newRequest().execute().getInput();

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

    expectedException.expect(UnauthorizedException.class);

    call();
  }

  private void addComponent(ComponentDto component) {
    db.components().insertComponent(component);
    db.favorites().add(component, USER_ID);
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
