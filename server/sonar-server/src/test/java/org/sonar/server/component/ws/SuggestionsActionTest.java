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
package org.sonar.server.component.ws;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.favorite.FavoriteFinder;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Project;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Suggestion;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.component.index.ComponentIndexQuery.DEFAULT_LIMIT;
import static org.sonar.server.component.ws.SuggestionsAction.EXTENDED_LIMIT;
import static org.sonar.server.component.ws.SuggestionsAction.PARAM_MORE;
import static org.sonar.server.component.ws.SuggestionsAction.PARAM_QUERY;
import static org.sonar.server.component.ws.SuggestionsAction.PARAM_RECENTLY_BROWSED;
import static org.sonar.server.component.ws.SuggestionsAction.SHORT_INPUT_WARNING;
import static org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Category;
import static org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Organization;

public class SuggestionsActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = new EsTester(new ComponentIndexDefinition(new MapSettings()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private ComponentIndexer componentIndexer = new ComponentIndexer(db.getDbClient(), es.client());
  private FavoriteFinder favoriteFinder = mock(FavoriteFinder.class);
  private ComponentIndex index = new ComponentIndex(es.client(), new AuthorizationTypeSupport(userSessionRule));
  private SuggestionsAction underTest = new SuggestionsAction(db.getDbClient(), index, favoriteFinder);
  private OrganizationDto organization;
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, componentIndexer);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    organization = db.organizations().insert();
  }

  @Test
  public void define_suggestions_action() {
    WebService.Action action = actionTester.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder(
      PARAM_MORE,
      PARAM_QUERY,
      PARAM_RECENTLY_BROWSED);

    WebService.Param recentlyBrowsed = action.param(PARAM_RECENTLY_BROWSED);
    assertThat(recentlyBrowsed.since()).isEqualTo("6.4");
    assertThat(recentlyBrowsed.exampleValue()).isNotEmpty();
    assertThat(recentlyBrowsed.description()).isNotEmpty();
    assertThat(recentlyBrowsed.isRequired()).isFalse();
  }

  @Test
  public void exact_match_in_one_qualifier() throws Exception {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organization));

    componentIndexer.indexOnStartup(null);
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = actionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, project.getKey())
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(Category::getQ)
      .containsExactly(Qualifiers.PROJECT);

    // assert correct id to be found
    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getKey, Suggestion::getOrganization)
      .containsExactly(tuple(project.getKey(), organization.getKey()));
  }

  @Test
  public void must_not_search_if_no_valid_tokens_are_provided() throws Exception {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organization).setName("SonarQube"));

    componentIndexer.indexOnStartup(null);
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = actionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "S o")
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList()).filteredOn(q -> q.getItemsCount() > 0).isEmpty();
    assertThat(response.getWarning()).contains(SHORT_INPUT_WARNING);
  }

  @Test
  public void should_warn_about_short_inputs() throws Exception {
    SuggestionsWsResponse response = actionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "validLongToken x")
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getWarning()).contains(SHORT_INPUT_WARNING);
  }

  @Test
  public void should_contain_organization_names() throws Exception {
    OrganizationDto organization1 = db.organizations().insert(o -> o.setKey("org-1").setName("Organization One"));
    OrganizationDto organization2 = db.organizations().insert(o -> o.setKey("org-2").setName("Organization Two"));

    ComponentDto project1 = db.components().insertComponent(newPrivateProjectDto(organization1).setName("Project1"));
    componentIndexer.indexProject(project1.projectUuid(), ProjectIndexer.Cause.PROJECT_CREATION);
    authorizationIndexerTester.allowOnlyAnyone(project1);

    ComponentDto project2 = db.components().insertComponent(newPrivateProjectDto(organization2).setName("Project2"));
    componentIndexer.indexProject(project2.projectUuid(), ProjectIndexer.Cause.PROJECT_CREATION);
    authorizationIndexerTester.allowOnlyAnyone(project2);

    SuggestionsWsResponse response = actionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "Project")
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getOrganizationsList())
      .extracting(Organization::getKey, Organization::getName)
      .containsExactlyInAnyOrder(
        of(organization1, organization2)
          .map(o -> tuple(o.getKey(), o.getName())).toArray(Tuple[]::new));
  }

  @Test
  public void should_contain_project_names() throws Exception {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organization));
    db.components().insertComponent(newModuleDto(project).setName("Module1"));
    db.components().insertComponent(newModuleDto(project).setName("Module2"));
    componentIndexer.indexProject(project.projectUuid(), ProjectIndexer.Cause.PROJECT_CREATION);
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = actionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "Module")
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getProject)
      .containsOnly(project.key());

    assertThat(response.getProjectsList())
      .extracting(Project::getKey, Project::getName)
      .containsExactlyInAnyOrder(
        tuple(project.key(), project.longName()));
  }

  @Test
  public void should_mark_recently_browsed_items() throws Exception {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organization));
    ComponentDto module1 = newModuleDto(project).setName("Module1");
    db.components().insertComponent(module1);
    ComponentDto module2 = newModuleDto(project).setName("Module2");
    db.components().insertComponent(module2);
    componentIndexer.indexProject(project.projectUuid(), ProjectIndexer.Cause.PROJECT_CREATION);
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = actionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "Module")
      .setParam(PARAM_RECENTLY_BROWSED, Stream.of(module1.getKey()).collect(joining(",")))
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getIsRecentlyBrowsed)
      .containsExactly(true, false);
  }

  @Test
  public void should_mark_favorite_items() throws Exception {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organization));
    ComponentDto favorite = newModuleDto(project).setName("Module1");
    db.components().insertComponent(favorite);
    doReturn(singletonList(favorite)).when(favoriteFinder).list();

    ComponentDto nonFavorite = newModuleDto(project).setName("Module2");
    db.components().insertComponent(nonFavorite);
    componentIndexer.indexProject(project.projectUuid(), ProjectIndexer.Cause.PROJECT_CREATION);
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = actionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "Module")
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getKey, Suggestion::getIsFavorite)
      .containsExactly(tuple(favorite.getKey(), true), tuple(nonFavorite.getKey(), false));
  }

  @Test
  public void should_propose_to_show_more_results_if_7_projects_are_found() throws Exception {
    check_proposal_to_show_more_results(7, DEFAULT_LIMIT, 1L, null);
  }

  @Test
  public void should_not_propose_to_show_more_results_if_6_projects_are_found() throws Exception {
    check_proposal_to_show_more_results(6, DEFAULT_LIMIT, 0L, null);
  }

  @Test
  public void should_not_propose_to_show_more_results_if_5_projects_are_found() throws Exception {
    check_proposal_to_show_more_results(5, DEFAULT_LIMIT, 0L, null);
  }

  @Test
  public void show_show_more_results_if_requested() throws Exception {
    check_proposal_to_show_more_results(21, EXTENDED_LIMIT, 1L, SuggestionCategory.PROJECT);
  }

  private void check_proposal_to_show_more_results(int numberOfProjects, int results, long numberOfMoreResults, @Nullable SuggestionCategory more) throws Exception {
    String namePrefix = "MyProject";

    List<ComponentDto> projects = range(0, numberOfProjects)
      .mapToObj(i -> db.components().insertComponent(newPrivateProjectDto(organization).setName(namePrefix + i)))
      .collect(Collectors.toList());

    componentIndexer.indexOnStartup(null);
    projects.forEach(authorizationIndexerTester::allowOnlyAnyone);

    TestRequest request = actionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, namePrefix);
    ofNullable(more).ifPresent(c -> request.setParam(PARAM_MORE, c.getName()));
    SuggestionsWsResponse response = request
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(Category::getQ)
      .containsExactly(Qualifiers.PROJECT);

    // include limited number of results in the response
    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .hasSize(Math.min(results, numberOfProjects));

    // indicate, that there are more results
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(Category::getMore)
      .containsExactly(numberOfMoreResults);
  }
}
