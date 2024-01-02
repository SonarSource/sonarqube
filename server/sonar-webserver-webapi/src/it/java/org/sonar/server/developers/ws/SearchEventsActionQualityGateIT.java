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
package org.sonar.server.developers.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.projectanalysis.ws.EventCategory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Developers.SearchEventsWsResponse;
import org.sonarqube.ws.Developers.SearchEventsWsResponse.Event;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.event.EventTesting.newEvent;
import static org.sonar.server.developers.ws.SearchEventsAction.PARAM_FROM;
import static org.sonar.server.developers.ws.SearchEventsAction.PARAM_PROJECTS;

public class SearchEventsActionQualityGateIT {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn();

  private static final long ANY_TIMESTAMP = 1666666666L;

  private final Server server = mock(Server.class);
  private final IssueIndex issueIndex = new IssueIndex(es.client(), null, userSession, null);
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private final WsActionTester ws = new WsActionTester(new SearchEventsAction(db.getDbClient(), userSession, server, issueIndex,
    issueIndexSyncProgressChecker));

  @Test
  public void quality_gate_events() {
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project));
    SnapshotDto projectAnalysis = insertSuccessfulActivity(project, 1_500_000_000_000L);
    db.events().insertEvent(newQualityGateEvent(projectAnalysis).setDate(projectAnalysis.getCreatedAt()).setName("Failed"));

    long from = 1_000_000_000_000L;
    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(from))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getCategory, Event::getProject, Event::getMessage, Event::getLink, Event::getDate)
      .containsOnly(
        tuple("QUALITY_GATE", project.getKey(),
          format("Quality Gate status of project '%s' changed to 'Failed'", project.name()),
          format("https://sonarcloud.io/dashboard?id=%s", project.getKey()),
          formatDateTime(projectAnalysis.getCreatedAt()))
      );
  }

  @Test
  public void branch_quality_gate_events() {
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project));
    String branchName = randomAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH).setKey(branchName));
    insertSuccessfulActivity(project, 1_500_000_000_000L);
    SnapshotDto branchAnalysis = insertSuccessfulActivity(branch, 1_500_000_000_000L);
    insertActivity(project.uuid(), branchAnalysis, CeActivityDto.Status.SUCCESS);
    db.events().insertEvent(newQualityGateEvent(branchAnalysis).setDate(branchAnalysis.getCreatedAt()).setName("Failed"));

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(branchAnalysis.getCreatedAt() - 1_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getCategory, Event::getProject, Event::getMessage, Event::getLink)
      .containsOnly(
        tuple("QUALITY_GATE", project.getKey(),
          format("Quality Gate status of project '%s' on branch '%s' changed to 'Failed'", project.name(), branchName),
          format("https://sonarcloud.io/dashboard?id=%s&branch=%s", project.getKey(), branchName))
      );
  }

  @Test
  public void does_not_return_quality_gate_events_on_pull_request() {
    userSession.logIn().setSystemAdministrator();
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto pr = db.components().insertProjectBranch(project, b -> b.setBranchType(PULL_REQUEST));
    SnapshotDto prAnalysis = insertSuccessfulActivity(pr, 1_500_000_000_000L);
    insertActivity(project.uuid(), prAnalysis, CeActivityDto.Status.SUCCESS);
    db.events().insertEvent(newQualityGateEvent(prAnalysis).setDate(prAnalysis.getCreatedAt()).setName("Failed"));

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(prAnalysis.getCreatedAt() - 1_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).isEmpty();
  }

  @Test
  public void return_only_latest_quality_gate_event() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setName("My Project")).getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project));
    SnapshotDto a1 = insertSuccessfulActivity(project, 1_500_000_000_000L);
    EventDto e1 = db.events().insertEvent(newQualityGateEvent(a1).setName("Failed").setDate(a1.getCreatedAt()));
    SnapshotDto a2 = insertSuccessfulActivity(project, 1_500_000_000_001L);
    EventDto e2 = db.events().insertEvent(newQualityGateEvent(a2).setName("Passed").setDate(a2.getCreatedAt() + 1L));

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(a1.getCreatedAt() - 1_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).extracting(Event::getMessage)
      .containsExactly("Quality Gate status of project 'My Project' changed to 'Passed'");
  }

  @Test
  public void return_link_to_dashboard_for_quality_gate_event() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project));
    SnapshotDto analysis = insertSuccessfulActivity(project, 1_500_000_000_000L);
    EventDto e1 = db.events().insertEvent(newQualityGateEvent(analysis).setName("Failed").setDate(analysis.getCreatedAt()));
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(analysis.getCreatedAt() - 1_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).extracting(Event::getLink)
      .containsExactly("https://sonarcloud.io/dashboard?id=" + project.getKey());
  }

  @Test
  public void encode_link() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setKey("M&M's")).getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project));
    SnapshotDto analysis = insertSuccessfulActivity(project, 1_500_000_000_000L);
    EventDto event = db.events().insertEvent(newQualityGateEvent(analysis).setName("Failed").setDate(analysis.getCreatedAt()));
    when(server.getPublicRootUrl()).thenReturn("http://sonarcloud.io");

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(analysis.getCreatedAt() - 1_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).extracting(Event::getLink)
      .containsExactly("http://sonarcloud.io/dashboard?id=M%26M%27s");
  }

  @Test
  public void filter_quality_gate_event() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project));
    SnapshotDto analysis = insertSuccessfulActivity(project, 1_500_000_000_000L);
    EventDto qualityGateEvent = db.events().insertEvent(newQualityGateEvent(analysis).setDate(analysis.getCreatedAt()));
    EventDto versionEvent = db.events().insertEvent(newEvent(analysis).setCategory(EventCategory.VERSION.getLabel()).setDate(analysis.getCreatedAt()));
    EventDto qualityProfileEvent = db.events().insertEvent(newEvent(analysis).setCategory(EventCategory.QUALITY_PROFILE.getLabel()).setDate(analysis.getCreatedAt()));

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(analysis.getCreatedAt() - 1_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).extracting(Event::getCategory)
      .containsExactly("QUALITY_GATE");
  }

  @Test
  public void filter_by_from_date_inclusive() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project1));
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project2));
    ComponentDto project3 = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project3));
    long from1 = 1_500_000_000_000L;
    long from2 = 1_400_000_000_000L;
    long from3 = 1_300_000_000_000L;
    SnapshotDto a1 = insertSuccessfulActivity(project1, from1 - 1L);
    db.events().insertEvent(newQualityGateEvent(a1).setDate(a1.getCreatedAt()));
    SnapshotDto a2 = insertSuccessfulActivity(project2, from2);
    db.events().insertEvent(newQualityGateEvent(a2).setDate(from2));
    SnapshotDto a3 = insertSuccessfulActivity(project3, from3 + 1L);
    db.events().insertEvent(newQualityGateEvent(a3).setDate(from3 + 1L));

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, join(",", project1.getKey(), project2.getKey(), project3.getKey()))
      .setParam(PARAM_FROM, join(",", formatDateTime(from1 - 1_000L), formatDateTime(from2 - 1_000L), formatDateTime(from3 - 1_000L)))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getProject)
      .containsExactlyInAnyOrder(project2.getKey(), project3.getKey());
  }

  @Test
  public void return_one_quality_gate_change_per_project() {
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setName("p1")).getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project1));
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setName("p2")).getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project2));
    long from = 1_500_000_000_000L;
    SnapshotDto a11 = insertSuccessfulActivity(project1, from);
    SnapshotDto a12 = insertSuccessfulActivity(project1, from + 1L);
    SnapshotDto a21 = insertSuccessfulActivity(project2, from);
    SnapshotDto a22 = insertSuccessfulActivity(project2, from + 1L);
    EventDto e11 = db.events().insertEvent(newQualityGateEvent(a11).setName("e11").setDate(from));
    EventDto e12 = db.events().insertEvent(newQualityGateEvent(a12).setName("e12").setDate(from + 1L));
    EventDto e21 = db.events().insertEvent(newQualityGateEvent(a21).setName("e21").setDate(from));
    EventDto e22 = db.events().insertEvent(newQualityGateEvent(a22).setName("e22").setDate(from + 1L));
    String fromDate = formatDateTime(from - 1_000L);

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, join(",", project1.getKey(), project2.getKey()))
      .setParam(PARAM_FROM, join(",", fromDate, fromDate))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getProject, Event::getMessage)
      .containsExactlyInAnyOrder(
        tuple(project1.getKey(), "Quality Gate status of project 'p1' changed to 'e12'"),
        tuple(project2.getKey(), "Quality Gate status of project 'p2' changed to 'e22'"));
  }

  private static EventDto newQualityGateEvent(SnapshotDto analysis) {
    return newEvent(analysis).setCategory(EventCategory.QUALITY_GATE.getLabel());
  }

  private SnapshotDto insertSuccessfulActivity(ComponentDto project, long analysisDate) {
    SnapshotDto analysis = db.components().insertSnapshot(project, s -> s.setCreatedAt(analysisDate));
    insertActivity(project.uuid(), analysis, CeActivityDto.Status.SUCCESS);
    return analysis;
  }

  private CeActivityDto insertActivity(String mainBranchUuid, SnapshotDto analysis, CeActivityDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(mainBranchUuid);
    queueDto.setUuid(randomAlphanumeric(40));
    queueDto.setCreatedAt(ANY_TIMESTAMP);
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(1000L);
    activityDto.setExecutedAt(ANY_TIMESTAMP);
    activityDto.setAnalysisUuid(analysis.getUuid());
    db.getDbClient().ceActivityDao().insert(db.getSession(), activityDto);
    db.commit();
    return activityDto;
  }
}
