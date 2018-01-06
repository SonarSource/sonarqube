/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.projectanalysis.ws;

import java.util.Date;
import java.util.List;
import java.util.function.Function;
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
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.Paging;
import org.sonarqube.ws.ProjectAnalyses.Analysis;
import org.sonarqube.ws.ProjectAnalyses.Event;
import org.sonarqube.ws.ProjectAnalyses.SearchResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.formatDate;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.event.EventTesting.newEvent;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.WsRequest.Method.POST;
import static org.sonar.server.projectanalysis.ws.EventCategory.OTHER;
import static org.sonar.server.projectanalysis.ws.EventCategory.QUALITY_GATE;
import static org.sonar.server.projectanalysis.ws.EventCategory.VERSION;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_BRANCH;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_CATEGORY;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_FROM;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_PROJECT;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_TO;

public class SearchActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();

  private WsActionTester ws = new WsActionTester(new SearchAction(dbClient, TestComponentFinder.from(db), userSession));

  @Test
  public void json_example() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey(KEY_PROJECT_EXAMPLE_001));
    userSession.addProjectPermission(UserRole.USER, project);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project).setUuid("A1").setCreatedAt(parseDateTime("2016-12-11T17:12:45+0100").getTime()));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project).setUuid("A2").setCreatedAt(parseDateTime("2016-12-12T17:12:45+0100").getTime()));
    db.events().insertEvent(newEvent(a1).setUuid("E11")
      .setName("Quality Gate is Red (was Orange)")
      .setCategory(EventCategory.QUALITY_GATE.getLabel())
      .setDescription("Coverage is < 80%"));
    db.events().insertEvent(newEvent(a1).setUuid("E12")
      .setName("6.3").setCategory(VERSION.getLabel()));
    db.events().insertEvent(newEvent(a2).setUuid("E21")
      .setName("Quality Profile changed to Sonar Way")
      .setCategory(EventCategory.QUALITY_PROFILE.getLabel()));
    db.events().insertEvent(newEvent(a2).setUuid("E22")
      .setName("6.3").setCategory(OTHER.getLabel()));

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT, KEY_PROJECT_EXAMPLE_001)
      .execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("search-example.json"));
  }

  @Test
  public void return_analyses_ordered_by_analysis_date() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    db.components().insertSnapshot(newAnalysis(project).setUuid("A1").setCreatedAt(1_000_000L));
    db.components().insertSnapshot(newAnalysis(project).setUuid("A2").setCreatedAt(2_000_000L));
    db.components().insertSnapshot(newAnalysis(project).setUuid("A3").setCreatedAt(3_000_000L));

    List<Analysis> result = call(project.getKey()).getAnalysesList();

    assertThat(result).hasSize(3);
    assertThat(result).extracting(Analysis::getKey, a -> parseDateTime(a.getDate()).getTime()).containsExactly(
      tuple("A3", 3_000_000L),
      tuple("A2", 2_000_000L),
      tuple("A1", 1_000_000L));
  }

  @Test
  public void return_only_processed_analyses() {
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey("P1"));
    userSession.addProjectPermission(UserRole.USER, project);
    db.components().insertSnapshot(newAnalysis(project).setUuid("A1"));
    db.components().insertSnapshot(newAnalysis(project).setUuid("A2").setStatus(SnapshotDto.STATUS_UNPROCESSED));

    List<Analysis> result = call("P1").getAnalysesList();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getKey()).isEqualTo("A1");
  }

  @Test
  public void return_events() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey("P1"));
    userSession.addProjectPermission(UserRole.USER, project);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project).setUuid("A1"));
    SnapshotDto a42 = db.components().insertSnapshot(newAnalysis(ComponentTesting.newPrivateProjectDto(organizationDto)).setUuid("A42"));
    EventDto e1 = db.events().insertEvent(newEvent(a1).setUuid("E1").setName("N1").setCategory(EventCategory.QUALITY_GATE.getLabel()).setDescription("D1"));
    EventDto e2 = db.events().insertEvent(newEvent(a1).setUuid("E2").setName("N2").setCategory(VERSION.getLabel()).setDescription("D2"));
    db.events().insertEvent(newEvent(a42));

    List<Analysis> result = call("P1").getAnalysesList();

    assertThat(result).hasSize(1);
    List<Event> events = result.get(0).getEventsList();
    assertThat(events).hasSize(2);
    assertThat(events).extracting(Event::getKey, wsToDbCategory(), Event::getName, Event::getDescription).containsOnly(
      tuple(e1.getUuid(), e1.getCategory(), e1.getName(), e1.getDescription()),
      tuple(e2.getUuid(), e2.getCategory(), e2.getName(), e2.getDescription()));
  }

  @Test
  public void return_analyses_of_application() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto application = db.components().insertApplication(organization);
    userSession.registerComponents(application);
    SnapshotDto firstAnalysis = db.components().insertSnapshot(newAnalysis(application).setCreatedAt(1_000_000L));
    SnapshotDto secondAnalysis = db.components().insertSnapshot(newAnalysis(application).setCreatedAt(2_000_000L));
    SnapshotDto thirdAnalysis = db.components().insertSnapshot(newAnalysis(application).setCreatedAt(3_000_000L));

    List<Analysis> result = call(application.getDbKey()).getAnalysesList();

    assertThat(result)
      .hasSize(3)
      .extracting(Analysis::getKey).containsExactly(thirdAnalysis.getUuid(), secondAnalysis.getUuid(), firstAnalysis.getUuid());
  }

  @Test
  public void return_analyses_of_portfolio() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto view = db.components().insertView(organization);
    userSession.registerComponents(view);
    SnapshotDto firstAnalysis = db.components().insertSnapshot(newAnalysis(view).setCreatedAt(1_000_000L));
    SnapshotDto secondAnalysis = db.components().insertSnapshot(newAnalysis(view).setCreatedAt(2_000_000L));
    SnapshotDto thirdAnalysis = db.components().insertSnapshot(newAnalysis(view).setCreatedAt(3_000_000L));

    List<Analysis> result = call(view.getDbKey()).getAnalysesList();

    assertThat(result)
      .hasSize(3)
      .extracting(Analysis::getKey).containsExactly(thirdAnalysis.getUuid(), secondAnalysis.getUuid(), firstAnalysis.getUuid());
  }

  @Test
  public void paginate_analyses() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    IntStream.rangeClosed(1, 9).forEach(i -> db.components().insertSnapshot(newAnalysis(project).setCreatedAt(1_000_000L * i).setUuid("A" + i)));

    SearchResponse result = call(SearchRequest.builder()
      .setProject(project.getDbKey())
      .setPage(2)
      .setPageSize(3)
      .build());

    assertThat(result.getAnalysesList()).extracting(Analysis::getKey)
      .containsExactly("A6", "A5", "A4");
  }

  @Test
  public void filter_by_category() {
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setDbKey("P1"));
    userSession.addProjectPermission(UserRole.USER, project);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project).setUuid("A1"));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project).setUuid("A2"));
    SnapshotDto a42 = db.components().insertSnapshot(newAnalysis(project).setUuid("A42"));
    db.events().insertEvent(newEvent(a1).setUuid("E11").setCategory(VERSION.getLabel()));
    db.events().insertEvent(newEvent(a1).setUuid("E12").setCategory(QUALITY_GATE.getLabel()));
    db.events().insertEvent(newEvent(a2).setUuid("E21").setCategory(QUALITY_GATE.getLabel()));
    // Analysis A42 doesn't have a quality gate event
    db.events().insertEvent(newEvent(a42).setCategory(OTHER.getLabel()));

    List<Analysis> result = call(SearchRequest.builder()
      .setProject("P1")
      .setCategory(QUALITY_GATE)
      .build()).getAnalysesList();

    assertThat(result).extracting(Analysis::getKey).containsOnly("A1", "A2");
  }

  @Test
  public void paginate_with_filter_on_category() {
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setDbKey("P1"));
    userSession.addProjectPermission(UserRole.USER, project);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project).setUuid("A1").setCreatedAt(1_000_000L));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project).setUuid("A2").setCreatedAt(2_000_000L));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(project).setUuid("A3").setCreatedAt(3_000_000L));
    SnapshotDto a42 = db.components().insertSnapshot(newAnalysis(project).setUuid("A42"));
    db.events().insertEvent(newEvent(a1).setUuid("E11").setCategory(VERSION.getLabel()));
    db.events().insertEvent(newEvent(a1).setUuid("E12").setCategory(QUALITY_GATE.getLabel()));
    db.events().insertEvent(newEvent(a2).setUuid("E21").setCategory(QUALITY_GATE.getLabel()));
    db.events().insertEvent(newEvent(a3).setUuid("E31").setCategory(QUALITY_GATE.getLabel()));
    // Analysis A42 doesn't have a quality gate event
    db.events().insertEvent(newEvent(a42).setCategory(OTHER.getLabel()));

    SearchResponse result = call(SearchRequest.builder()
      .setProject("P1")
      .setCategory(QUALITY_GATE)
      .setPage(2)
      .setPageSize(1)
      .build());

    assertThat(result.getAnalysesList()).extracting(Analysis::getKey).containsOnly("A2");
    assertThat(result.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(2, 1, 3);
  }

  @Test
  public void filter_from_date() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project).setUuid("a1").setCreatedAt(1_000_000_000L));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project).setUuid("a2").setCreatedAt(2_000_000_000L));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(project).setUuid("a3").setCreatedAt(3_000_000_000L));
    SnapshotDto a4 = db.components().insertSnapshot(newAnalysis(project).setUuid("a4").setCreatedAt(4_000_000_000L));

    SearchResponse result = call(SearchRequest.builder()
      .setProject(project.getDbKey())
      .setFrom(formatDateTime(2_000_000_000L))
      .build());

    assertThat(result.getAnalysesList())
      .extracting(Analysis::getKey)
      .containsOnly(a2.getUuid(), a3.getUuid(), a4.getUuid())
      .doesNotContain(a1.getUuid());
  }

  @Test
  public void filter_to_date() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project).setUuid("a1").setCreatedAt(1_000_000_000L));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project).setUuid("a2").setCreatedAt(2_000_000_000L));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(project).setUuid("a3").setCreatedAt(3_000_000_000L));
    SnapshotDto a4 = db.components().insertSnapshot(newAnalysis(project).setUuid("a4").setCreatedAt(4_000_000_000L));

    SearchResponse result = call(SearchRequest.builder()
      .setProject(project.getDbKey())
      .setTo(formatDateTime(2_000_000_000L))
      .build());

    assertThat(result.getAnalysesList())
      .extracting(Analysis::getKey)
      .containsOnly(a1.getUuid(), a2.getUuid())
      .doesNotContain(a3.getUuid(), a4.getUuid());
  }

  @Test
  public void filter_by_dates_using_datetime_format() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project).setUuid("a1").setCreatedAt(1_000_000_000L));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project).setUuid("a2").setCreatedAt(2_000_000_000L));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(project).setUuid("a3").setCreatedAt(3_000_000_000L));
    SnapshotDto a4 = db.components().insertSnapshot(newAnalysis(project).setUuid("a4").setCreatedAt(4_000_000_000L));

    SearchResponse result = call(SearchRequest.builder()
      .setProject(project.getDbKey())
      .setFrom(formatDateTime(2_000_000_000L))
      .setTo(formatDateTime(3_000_000_000L))
      .build());

    assertThat(result.getAnalysesList())
      .extracting(Analysis::getKey)
      .containsOnly(a2.getUuid(), a3.getUuid())
      .doesNotContain(a1.getUuid(), a4.getUuid());
  }

  @Test
  public void filter_by_dates_using_date_format() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    SnapshotDto a1 = db.components().insertSnapshot(newAnalysis(project).setUuid("a1").setCreatedAt(1_000_000_000L));
    SnapshotDto a2 = db.components().insertSnapshot(newAnalysis(project).setUuid("a2").setCreatedAt(2_000_000_000L));
    SnapshotDto a3 = db.components().insertSnapshot(newAnalysis(project).setUuid("a3").setCreatedAt(3_000_000_000L));
    SnapshotDto a4 = db.components().insertSnapshot(newAnalysis(project).setUuid("a4").setCreatedAt(4_000_000_000L));

    SearchResponse result = call(SearchRequest.builder()
      .setProject(project.getDbKey())
      .setFrom(formatDate(new Date(2_000_000_000L)))
      .setTo(formatDate(new Date(3_000_000_000L)))
      .build());

    assertThat(result.getAnalysesList())
      .extracting(Analysis::getKey)
      .containsOnly(a2.getUuid(), a3.getUuid())
      .doesNotContain(a1.getUuid(), a4.getUuid());
  }

  @Test
  public void branch() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    SnapshotDto analysis = db.components().insertSnapshot(newAnalysis(branch));
    EventDto event = db.events().insertEvent(newEvent(analysis).setCategory(EventCategory.QUALITY_GATE.getLabel()));

    List<Analysis> result = call(SearchRequest.builder()
      .setProject(project.getKey())
      .setBranch("my_branch")
      .build())
        .getAnalysesList();

    assertThat(result).extracting(Analysis::getKey).containsExactlyInAnyOrder(analysis.getUuid());
    assertThat(result.get(0).getEventsList()).extracting(Event::getKey).containsExactlyInAnyOrder(event.getUuid());

  }

  @Test
  public void empty_response() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    SearchResponse result = call(project.getDbKey());

    assertThat(result.hasPaging()).isTrue();
    assertThat(result.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsExactly(1, 100, 0);
    assertThat(result.getAnalysesCount()).isEqualTo(0);
  }

  @Test
  public void fail_if_not_enough_permissions() {
    userSession.anonymous();
    ComponentDto project = db.components().insertPrivateProject();

    expectedException.expect(ForbiddenException.class);

    call(project.getDbKey());
  }

  @Test
  public void fail_if_project_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    call("P1");
  }

  @Test
  public void fail_if_not_a_project_portfolio_or_application() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    db.components().insertSnapshot(newAnalysis(project));
    userSession.registerComponents(project, file);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A project, portfolio or application is required");

    call(file.getDbKey());
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component '%s' on branch '%s' not found", project.getKey(), "another_branch"));

    call(SearchRequest.builder()
      .setProject(project.getKey())
      .setBranch("another_branch")
      .build());
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("search");
    assertThat(definition.since()).isEqualTo("6.3");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.param("project").isRequired()).isTrue();
    assertThat(definition.param("category")).isNotNull();
    assertThat(definition.params()).hasSize(7);

    Param from = definition.param("from");
    assertThat(from.since()).isEqualTo("6.5");

    Param to = definition.param("to");
    assertThat(to.since()).isEqualTo("6.5");

    Param branch = definition.param("branch");
    assertThat(branch.since()).isEqualTo("6.6");
    assertThat(branch.isInternal()).isTrue();
    assertThat(branch.isRequired()).isFalse();
  }

  private static Function<Event, String> wsToDbCategory() {
    return e -> e == null ? null : EventCategory.valueOf(e.getCategory()).getLabel();
  }

  private SearchResponse call(@Nullable String project) {
    SearchRequest.Builder request = SearchRequest.builder();
    setNullable(project, request::setProject);
    return call(request.build());
  }

  private SearchResponse call(SearchRequest wsRequest) {
    TestRequest request = ws.newRequest()
      .setMethod(POST.name());
    setNullable(wsRequest.getProject(), project -> request.setParam(PARAM_PROJECT, project));
    setNullable(wsRequest.getBranch(), branch -> request.setParam(PARAM_BRANCH, branch));
    setNullable(wsRequest.getCategory(), category -> request.setParam(PARAM_CATEGORY, category.name()));
    setNullable(wsRequest.getPage(), page -> request.setParam(Param.PAGE, String.valueOf(page)));
    setNullable(wsRequest.getPageSize(), pageSize -> request.setParam(Param.PAGE_SIZE, String.valueOf(pageSize)));
    setNullable(wsRequest.getFrom(), from -> request.setParam(PARAM_FROM, from));
    setNullable(wsRequest.getTo(), to -> request.setParam(PARAM_TO, to));

    return request.executeProtobuf(SearchResponse.class);
  }
}
