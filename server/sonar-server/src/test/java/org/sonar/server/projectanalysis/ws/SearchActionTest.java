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
package org.sonar.server.projectanalysis.ws;

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
import org.sonarqube.ws.client.projectanalysis.EventCategory;
import org.sonarqube.ws.client.projectanalysis.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.event.EventTesting.newEvent;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.WsRequest.Method.POST;
import static org.sonarqube.ws.client.projectanalysis.EventCategory.OTHER;
import static org.sonarqube.ws.client.projectanalysis.EventCategory.QUALITY_GATE;
import static org.sonarqube.ws.client.projectanalysis.EventCategory.VERSION;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_CATEGORY;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_PROJECT;

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
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setKey(KEY_PROJECT_EXAMPLE_001));
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
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setKey("P1"));
    userSession.addProjectPermission(UserRole.USER, project);
    db.components().insertSnapshot(newAnalysis(project).setUuid("A1").setCreatedAt(1_000_000L));
    db.components().insertSnapshot(newAnalysis(project).setUuid("A2").setCreatedAt(2_000_000L));
    db.components().insertSnapshot(newAnalysis(project).setUuid("A3").setCreatedAt(3_000_000L));

    List<Analysis> result = call("P1").getAnalysesList();

    assertThat(result).hasSize(3);
    assertThat(result).extracting(Analysis::getKey, a -> parseDateTime(a.getDate()).getTime()).containsExactly(
      tuple("A3", 3_000_000L),
      tuple("A2", 2_000_000L),
      tuple("A1", 1_000_000L));
  }

  @Test
  public void return_only_processed_analyses() {
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey("P1"));
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
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setKey("P1"));
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
  public void paginate_analyses() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    IntStream.rangeClosed(1, 9).forEach(i -> db.components().insertSnapshot(newAnalysis(project).setCreatedAt(1_000_000L * i).setUuid("A" + i)));

    SearchResponse result = call(SearchRequest.builder()
      .setProject(project.key())
      .setPage(2)
      .setPageSize(3)
      .build());

    assertThat(result.getAnalysesList()).extracting(Analysis::getKey)
      .containsExactly("A6", "A5", "A4");
  }

  @Test
  public void filter_by_category() {
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setKey("P1"));
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
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setKey("P1"));
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
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("search");
    assertThat(definition.since()).isEqualTo("6.3");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.param(PARAM_PROJECT).isRequired()).isTrue();
    assertThat(definition.param(PARAM_CATEGORY)).isNotNull();
  }

  @Test
  public void fail_if_not_enough_permissions() {
    userSession.anonymous();
    ComponentDto project = db.components().insertPrivateProject();

    expectedException.expect(ForbiddenException.class);

    call(project.key());
  }

  @Test
  public void fail_if_project_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    call("P1");
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
    setNullable(wsRequest.getCategory(), category -> request.setParam(PARAM_CATEGORY, category.name()));
    setNullable(wsRequest.getPage(), page -> request.setParam(Param.PAGE, String.valueOf(page)));
    setNullable(wsRequest.getPageSize(), pageSize -> request.setParam(Param.PAGE_SIZE, String.valueOf(pageSize)));

    return request.executeProtobuf(SearchResponse.class);
  }
}
