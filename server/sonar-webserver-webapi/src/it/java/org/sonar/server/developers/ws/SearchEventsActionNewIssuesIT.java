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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.rules.RuleType;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Developers.SearchEventsWsResponse;
import org.sonarqube.ws.Developers.SearchEventsWsResponse.Event;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.server.developers.ws.SearchEventsAction.PARAM_FROM;
import static org.sonar.server.developers.ws.SearchEventsAction.PARAM_PROJECTS;

public class SearchEventsActionNewIssuesIT {

  private static final RuleType[] RULE_TYPES_EXCEPT_HOTSPOT = Stream.of(RuleType.values())
    .filter(r -> r != RuleType.SECURITY_HOTSPOT)
    .toArray(RuleType[]::new);

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private Server server = mock(Server.class);

  private IssueIndex issueIndex = new IssueIndex(es.client(), null, null, null);
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()), null);
  private IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private WsActionTester ws = new WsActionTester(new SearchEventsAction(db.getDbClient(), userSession, server, issueIndex,
    issueIndexSyncProgressChecker));
  private final Random random = new SecureRandom();

  @Test
  public void issue_event() {
    userSession.logIn();
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project));
    SnapshotDto analysis = insertAnalysis(project, 1_500_000_000_000L);
    insertIssue(project, analysis);
    insertIssue(project, analysis);
    // will be ignored
    insertSecurityHotspot(project, analysis);
    issueIndexer.indexAllIssues();

    long from = analysis.getCreatedAt() - 1_000_000L;
    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(from))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getCategory, Event::getProject, Event::getMessage, Event::getLink, Event::getDate)
      .containsOnly(
        tuple("NEW_ISSUES", project.getKey(), format("You have 2 new issues on project '%s'", project.name()),
          format("https://sonarcloud.io/project/issues?id=%s&createdAfter=%s&assignees=%s&resolved=false", project.getKey(), encode(formatDateTime(from + 1_000L)),
            userSession.getLogin()),
          formatDateTime(analysis.getCreatedAt())));
  }

  @Test
  public void many_issues_events() {
    userSession.logIn();
    long from = 1_500_000_000_000L;
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setName("SonarQube"));
    ComponentDto mainBranchComponent = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto());
    SnapshotDto analysis = insertAnalysis(mainBranchComponent, from);
    insertIssue(mainBranchComponent, analysis);
    insertIssue(mainBranchComponent, analysis);
    issueIndexer.indexAllIssues();
    String fromDate = formatDateTime(from - 1_000L);

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, mainBranchComponent.getKey())
      .setParam(PARAM_FROM, fromDate)
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).extracting(Event::getCategory, Event::getMessage, Event::getProject, Event::getDate)
      .containsExactly(tuple("NEW_ISSUES", "You have 2 new issues on project 'SonarQube'", mainBranchComponent.getKey(),
        formatDateTime(from)));
  }

  @Test
  public void does_not_return_old_issue() {
    userSession.logIn();
    ProjectData project = db.components().insertPrivateProject();
    userSession.addProjectPermission(USER, project.getProjectDto());
    SnapshotDto analysis = insertAnalysis(project.getMainBranchComponent(), 1_500_000_000_000L);
    db.issues().insert(db.rules().insert(), project.getMainBranchComponent(), project.getMainBranchComponent(),
      i -> i.setIssueCreationDate(new Date(analysis.getCreatedAt() - 10_000L)));
    issueIndexer.indexAllIssues();

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.projectKey())
      .setParam(PARAM_FROM, formatDateTime(analysis.getCreatedAt() - 1_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).isEmpty();
  }

  @Test
  public void return_link_to_issue_search_for_new_issues_event() {
    userSession.logIn("my_login");
    ComponentDto project = db.components().insertPrivateProject(p -> p.setKey("my_project")).getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project));
    SnapshotDto analysis = insertAnalysis(project, 1_400_000_000_000L);
    insertIssue(project, analysis);
    issueIndexer.indexAllIssues();
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");

    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(analysis.getCreatedAt() - 1_000L))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).extracting(Event::getLink)
      .containsExactly("https://sonarcloud.io/project/issues?id=my_project&createdAfter=" + encode(formatDateTime(analysis.getCreatedAt())) + "&assignees=my_login&resolved=false");
  }

  @Test
  public void branch_issues_events() {
    userSession.logIn().setSystemAdministrator();
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project));
    String branchName1 = "branch1";
    ComponentDto branch1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH).setKey(branchName1));
    SnapshotDto branch1Analysis = insertAnalysis(branch1, project.uuid(), 1_500_000_000_000L);
    insertIssue(branch1, branch1Analysis);
    insertIssue(branch1, branch1Analysis);
    String branchName2 = "branch2";
    ComponentDto branch2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setKey(branchName2));
    SnapshotDto branch2Analysis = insertAnalysis(branch2, project.uuid(), 1_300_000_000_000L);
    insertIssue(branch2, branch2Analysis);
    issueIndexer.indexAllIssues();

    long from = 1_000_000_000_000L;
    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(from))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getCategory, Event::getProject, Event::getMessage, Event::getLink, Event::getDate)
      .containsOnly(
        tuple("NEW_ISSUES", project.getKey(), format("You have 2 new issues on project '%s' on branch '%s'", project.name(), branchName1),
          format("https://sonarcloud.io/project/issues?id=%s&createdAfter=%s&assignees=%s&resolved=false&branch=%s", branch1.getKey(), encode(formatDateTime(from + 1_000L)),
            userSession.getLogin(), branchName1),
          formatDateTime(branch1Analysis.getCreatedAt())),
        tuple("NEW_ISSUES", project.getKey(), format("You have 1 new issue on project '%s' on branch '%s'", project.name(), branchName2),
          format("https://sonarcloud.io/project/issues?id=%s&createdAfter=%s&assignees=%s&resolved=false&branch=%s", branch2.getKey(), encode(formatDateTime(from + 1_000L)),
            userSession.getLogin(), branchName2),
          formatDateTime(branch2Analysis.getCreatedAt())));
  }

  @Test
  public void pull_request_issues_events() {
    userSession.logIn().setSystemAdministrator();
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project));
    String nonMainBranchName = "nonMain";
    ComponentDto nonMainBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH).setKey(nonMainBranchName));
    SnapshotDto nonMainBranchAnalysis = insertAnalysis(nonMainBranch, project.uuid(), 1_500_000_000_000L);
    insertIssue(nonMainBranch, nonMainBranchAnalysis);
    insertIssue(nonMainBranch, nonMainBranchAnalysis);
    String pullRequestKey = "42";
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setKey(pullRequestKey));
    SnapshotDto pullRequestAnalysis = insertAnalysis(pullRequest, project.uuid(), 1_300_000_000_000L);
    insertIssue(pullRequest, pullRequestAnalysis);
    issueIndexer.indexAllIssues();

    long from = 1_000_000_000_000L;
    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, formatDateTime(from))
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList())
      .extracting(Event::getCategory, Event::getProject, Event::getMessage, Event::getLink, Event::getDate)
      .containsOnly(
        tuple("NEW_ISSUES", project.getKey(), format("You have 2 new issues on project '%s' on branch '%s'", project.name(), nonMainBranchName),
          format("https://sonarcloud.io/project/issues?id=%s&createdAfter=%s&assignees=%s&resolved=false&branch=%s", nonMainBranch.getKey(), encode(formatDateTime(from + 1_000L)),
            userSession.getLogin(), nonMainBranchName),
          formatDateTime(nonMainBranchAnalysis.getCreatedAt())),
        tuple("NEW_ISSUES", project.getKey(), format("You have 1 new issue on project '%s' on pull request '%s'", project.name(), pullRequestKey),
          format("https://sonarcloud.io/project/issues?id=%s&createdAfter=%s&assignees=%s&resolved=false&pullRequest=%s", pullRequest.getKey(),
            encode(formatDateTime(from + 1_000L)),
            userSession.getLogin(), pullRequestKey),
          formatDateTime(pullRequestAnalysis.getCreatedAt())));
  }

  @Test
  public void encode_link() {
    userSession.logIn("rågnar").setSystemAdministrator();
    long from = 1_500_000_000_000L;
    ComponentDto project = db.components().insertPrivateProject(p -> p.setKey("M&M's")).getMainBranchComponent();
    userSession.addProjectPermission(USER, db.components().getProjectDtoByMainBranch(project));
    SnapshotDto analysis = insertAnalysis(project, from);
    insertIssue(project, analysis);
    issueIndexer.indexAllIssues();
    when(server.getPublicRootUrl()).thenReturn("http://sonarcloud.io");

    String fromDate = formatDateTime(from - 1_000L);
    SearchEventsWsResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_FROM, fromDate)
      .executeProtobuf(SearchEventsWsResponse.class);

    assertThat(result.getEventsList()).extracting(Event::getLink)
      .containsExactly("http://sonarcloud.io/project/issues?id=M%26M%27s&createdAfter=" + encode(formatDateTime(from)) + "&assignees=r%C3%A5gnar&resolved=false");
  }

  private String encode(String text) {
    try {
      return URLEncoder.encode(text, UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(format("Cannot encode %s", text), e);
    }
  }

  private void insertIssue(ComponentDto component, SnapshotDto analysis) {
    RuleDto rule = db.rules().insert(r -> r.setType(randomRuleTypeExceptHotspot()));
    db.issues().insert(rule, component, component,
      i -> i.setIssueCreationDate(new Date(analysis.getCreatedAt()))
        .setAssigneeUuid(userSession.getUuid())
        .setType(randomRuleTypeExceptHotspot()));
  }

  private void insertSecurityHotspot(ComponentDto component, SnapshotDto analysis) {
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.SECURITY_HOTSPOT));
    db.issues().insert(rule, component, component,
      i -> i.setIssueCreationDate(new Date(analysis.getCreatedAt()))
        .setAssigneeUuid(userSession.getUuid())
        .setType(RuleType.SECURITY_HOTSPOT));
  }


  private SnapshotDto insertAnalysis(ComponentDto project, long analysisDate) {
    SnapshotDto analysis = db.components().insertSnapshot(project, s -> s.setCreatedAt(analysisDate));
    insertActivity(project.uuid(), analysis, CeActivityDto.Status.SUCCESS);
    return analysis;
  }

  private SnapshotDto insertAnalysis(ComponentDto branch, String mainBranchUuid, long analysisDate) {
    SnapshotDto analysis = db.components().insertSnapshot(branch, s -> s.setCreatedAt(analysisDate));
    insertActivity(mainBranchUuid, analysis, CeActivityDto.Status.SUCCESS);
    return analysis;
  }

  private CeActivityDto insertActivity(String mainBranchUuid, SnapshotDto analysis, CeActivityDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(mainBranchUuid);
    queueDto.setUuid(secure().nextAlphanumeric(40));
    queueDto.setCreatedAt(random.nextLong(Long.MAX_VALUE));
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(random.nextLong(Long.MAX_VALUE));
    activityDto.setExecutedAt(random.nextLong(Long.MAX_VALUE));
    activityDto.setAnalysisUuid(analysis.getUuid());
    db.getDbClient().ceActivityDao().insert(db.getSession(), activityDto);
    db.commit();
    return activityDto;
  }

  private RuleType randomRuleTypeExceptHotspot() {
    return RULE_TYPES_EXCEPT_HOTSPOT[random.nextInt(RULE_TYPES_EXCEPT_HOTSPOT.length)];
  }
}
