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
import javax.annotation.Nullable;
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
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse;

import static java.util.Optional.ofNullable;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.server.component.ws.SuggestionsAction.DEFAULT_LIMIT;
import static org.sonar.server.component.ws.SuggestionsAction.EXTENDED_LIMIT;
import static org.sonar.server.component.ws.SuggestionsAction.SHORT_INPUT_WARNING;
import static org.sonar.server.component.ws.SuggestionsAction.PARAM_MORE;
import static org.sonar.server.component.ws.SuggestionsAction.PARAM_QUERY;

public class SuggestionsActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = new EsTester(new ComponentIndexDefinition(new MapSettings()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private ComponentIndexer componentIndexer = new ComponentIndexer(db.getDbClient(), es.client());
  private ComponentIndex index = new ComponentIndex(es.client(), new AuthorizationTypeSupport(userSessionRule));
  private SuggestionsAction underTest = new SuggestionsAction(db.getDbClient(), index);
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
      PARAM_QUERY);
  }

  @Test
  public void exact_match_in_one_qualifier() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto(organization));

    componentIndexer.indexOnStartup(null);
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = actionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, project.getKey())
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(SuggestionsWsResponse.Qualifier::getQ)
      .containsExactly(Qualifiers.PROJECT);

    // assert correct id to be found
    assertThat(response.getResultsList())
      .flatExtracting(SuggestionsWsResponse.Qualifier::getItemsList)
      .extracting(WsComponents.Component::getKey, WsComponents.Component::getOrganization)
      .containsExactly(tuple(project.getKey(), organization.getKey()));
  }

  @Test
  public void must_not_search_if_no_valid_tokens_are_provided() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto(organization).setName("SonarQube"));

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
    check_proposal_to_show_more_results(21, EXTENDED_LIMIT, 1L, Qualifiers.PROJECT);
  }

  private void check_proposal_to_show_more_results(int numberOfProjects, int results, long numberOfMoreResults, @Nullable String moreQualifier) throws Exception {
    String namePrefix = "MyProject";

    List<ComponentDto> projects = range(0, numberOfProjects)
      .mapToObj(i -> db.components().insertComponent(newProjectDto(organization).setName(namePrefix + i)))
      .collect(Collectors.toList());

    componentIndexer.indexOnStartup(null);
    projects.forEach(authorizationIndexerTester::allowOnlyAnyone);

    TestRequest request = actionTester.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, namePrefix);
    ofNullable(moreQualifier).ifPresent(q -> request.setParam(PARAM_MORE, q));
    SuggestionsWsResponse response = request
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(SuggestionsWsResponse.Qualifier::getQ)
      .containsExactly(Qualifiers.PROJECT);

    // include limited number of results in the response
    assertThat(response.getResultsList())
      .flatExtracting(SuggestionsWsResponse.Qualifier::getItemsList)
      .hasSize(Math.min(results, numberOfProjects));

    // indicate, that there are more results
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(SuggestionsWsResponse.Qualifier::getMore)
      .containsExactly(numberOfMoreResults);
  }
}
