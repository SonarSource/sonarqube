/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.issue.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.time.Clock;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.Durations;
import org.sonar.core.rule.RuleType;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.NewCodePeriodResolver;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.workflow.CodeQualityIssueWorkflow;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.issue.workflow.SecurityHostpotWorkflow;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.MessageFormattingUtils;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Common.Severity;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Issue;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.measures.CoreMetrics.ANALYSIS_FROM_SONARQUBE_9_4_KEY;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.db.protobuf.DbIssues.MessageFormattingType.CODE;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;
import static org.sonar.db.rule.RuleTesting.XOO_X1;
import static org.sonar.db.rule.RuleTesting.XOO_X2;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonarqube.ws.Common.Impact.newBuilder;

@RunWith(DataProviderRunner.class)
public class ListActionIT {

  public static final DbIssues.MessageFormatting MESSAGE_FORMATTING = DbIssues.MessageFormatting.newBuilder()
    .setStart(0).setEnd(11).setType(CODE).build();
  private final UuidFactoryFast uuidFactory = UuidFactoryFast.getInstance();
  @Rule
  public UserSessionRule userSession = standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private final IssueWorkflow issueWorkflow = new IssueWorkflow(new FunctionExecutor(issueFieldsSetter), issueFieldsSetter, new CodeQualityIssueWorkflow(mock(TaintChecker.class)),
    new SecurityHostpotWorkflow());
  private final SearchResponseLoader searchResponseLoader = new SearchResponseLoader(userSession, dbClient, new TransitionService(userSession, issueWorkflow));
  private final Languages languages = new Languages();
  private final UserResponseFormatter userFormatter = new UserResponseFormatter(new AvatarResolverImpl());
  private final SearchResponseFormat searchResponseFormat = new SearchResponseFormat(new Durations(), languages, new TextRangeResponseFormatter(), userFormatter);
  private final ComponentFinder componentFinder = TestComponentFinder.from(db);
  private final WsActionTester ws = new WsActionTester(
    new ListAction(userSession, dbClient, new NewCodePeriodResolver(dbClient, Clock.systemUTC()), searchResponseLoader, searchResponseFormat, componentFinder));

  @Test
  public void whenNoComponentOrProjectProvided_shouldFailWithMessage() {
    TestRequest request = ws.newRequest();
    assertThatThrownBy(() -> request.executeProtobuf(Issues.ListWsResponse.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Either 'project' or 'component' parameter must be provided");
  }

  @Test
  public void whenBranchAndPullRequestProvided_shouldFailWithMessage() {
    TestRequest request = ws.newRequest()
      .setParam("project", "some-project")
      .setParam("branch", "some-branch")
      .setParam("pullRequest", "some-pr");
    assertThatThrownBy(() -> request.executeProtobuf(Issues.ListWsResponse.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Only one of parameters 'branch' and 'pullRequest' can be provided");
  }

  @Test
  public void whenAnonymousUser_shouldFailIfInsufficientPrivileges() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();
    db.issues().insertIssue(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_RESOLVED)
      .setResolution(RESOLUTION_FIXED)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    TestRequest request = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .setParam("branch", projectData.getMainBranchDto().getKey());
    assertThatThrownBy(() -> request.executeProtobuf(Issues.ListWsResponse.class))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void whenNoProjectOrComponent_shouldFail() {
    TestRequest request = ws.newRequest()
      .setParam("branch", "test-branch");
    assertThatThrownBy(() -> request.executeProtobuf(Issues.ListWsResponse.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Either 'project' or 'component' parameter must be provided");
  }

  @Test
  public void whenListIssuesByProjectAndBranch_shouldReturnAllFields() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("BLOCKER")
      .replaceAllImpacts(List.of(new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(org.sonar.api.issue.impact.Severity.BLOCKER)))
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Issues.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(
        Issue::getKey, Issue::getRule, Issue::getSeverity, Issue::getComponent, Issue::getResolution, Issue::getStatus, Issue::getMessage, Issue::getMessageFormattingsList,
        Issue::getEffort, Issue::getAssignee, Issue::getAuthor, Issue::getLine, Issue::getHash, Issue::getTagsList, Issue::getCreationDate, Issue::getUpdateDate,
        Issue::getQuickFixAvailable, Issue::getCodeVariantsList, Issue::getImpactsList)
      .containsExactlyInAnyOrder(
        tuple(issue.getKey(), rule.getKey().toString(), Severity.BLOCKER, file.getKey(), "", STATUS_OPEN, "the message",
          MessageFormattingUtils.dbMessageFormattingListToWs(List.of(MESSAGE_FORMATTING)), "10min",
          simon.getLogin(), "John", 42, "a227e508d6646b55a086ee11d63b21e9", asList("bug", "owasp"), formatDateTime(issue.getIssueCreationDate()),
          formatDateTime(issue.getIssueUpdateDate()), false, List.of("variant1", "variant2"),
          List.of(newBuilder().setSoftwareQuality(Common.SoftwareQuality.MAINTAINABILITY).setSeverity(Common.ImpactSeverity.ImpactSeverity_BLOCKER).build())));

    assertThat(response.getComponentsList())
      .extracting(
        Issues.Component::getKey, Issues.Component::getName, Issues.Component::getQualifier, Issues.Component::getLongName, Issues.Component::getPath)
      .containsExactlyInAnyOrder(
        tuple(project.getKey(), project.name(), project.qualifier(), project.longName(), ""),
        tuple(file.getKey(), file.name(), file.qualifier(), file.longName(), file.path()));
  }

  @Test
  public void whenListIssuesByProject_shouldReturnIssuesFromMainBranch() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    ComponentDto anotherBranch = db.components().insertProjectBranch(project, b -> b.setKey("branch1"));
    ComponentDto fileFromAnotherBranch = db.components().insertComponent(newFileDto(anotherBranch));
    IssueDto issueFromAnotherBranch = db.issues().insertIssue(rule, anotherBranch, fileFromAnotherBranch, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Issues.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue.getKey())
      .doesNotContain(issueFromAnotherBranch.getKey());
  }

  @Test
  public void whenListIssuesByProjectAndPullRequest_shouldIssuesForPullRequestOnly() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    String pullRequestId = "42";
    ComponentDto pullRequest = db.components().insertProjectBranch(project, branchDto -> branchDto.setKey(pullRequestId).setBranchType(BranchType.PULL_REQUEST));
    ComponentDto file = db.components().insertComponent(newFileDto(pullRequest));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, pullRequest, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Issues.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .setParam("pullRequest", pullRequestId)
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue.getKey());
  }

  @Test
  public void whenListIssuesByProjectOnly_shouldReturnIssuesForMainBranchOnly() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Issues.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue.getKey());
  }

  @Test
  public void whenListIssuesByComponent_shouldReturnIssues() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Issues.ListWsResponse response = ws.newRequest()
      .setParam("component", file.getKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue.getKey());
  }

  @Test
  public void whenListIssuesByTypes_shouldReturnIssuesWithSpecifiedTypes() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i
      .setType(RuleType.CODE_SMELL)
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    RuleDto bugRule = newIssueRule(XOO_X2, RuleType.BUG);
    IssueDto bugIssue = db.issues().insertIssue(bugRule, project, file, i -> i
      .setType(RuleType.BUG)
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Issues.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.getProjectDto().getKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .setParam("types", RuleType.BUG.name())
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(bugIssue.getKey())
      .doesNotContain(issue.getKey());
  }

  @Test
  public void whenListIssuesByResolved_shouldReturnResolvedIssues() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i
      .setType(RuleType.CODE_SMELL)
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_CLOSED)
      .setResolution(RESOLUTION_FIXED)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    RuleDto bugRule = newIssueRule(XOO_X2, RuleType.BUG);
    IssueDto bugIssue = db.issues().insertIssue(bugRule, project, file, i -> i
      .setType(RuleType.BUG)
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_RESOLVED)
      .setResolution(RESOLUTION_WONT_FIX)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    IssueDto vulnerabilityIssue = db.issues().insertIssue(rule, project, file, i -> i
      .setType(RuleType.VULNERABILITY)
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2")));

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Issues.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.getProjectDto().getKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .setParam("resolved", "true")
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue.getKey(), bugIssue.getKey())
      .doesNotContain(vulnerabilityIssue.getKey());

    response = ws.newRequest()
      .setParam("project", projectData.getProjectDto().getKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .setParam("resolved", "false")
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(vulnerabilityIssue.getKey())
      .doesNotContain(issue.getKey(), bugIssue.getKey());

    response = ws.newRequest()
      .setParam("project", projectData.getProjectDto().getKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrder(vulnerabilityIssue.getKey(), issue.getKey(), bugIssue.getKey());
  }

  @Test
  public void whenListIssuesByNewCodePeriodDate_shouldReturnIssues() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();

    db.components().insertSnapshot(project, s -> s.setLast(true).setPeriodDate(parseDateTime("2014-09-05T00:00:00+0100").getTime()));

    List<String> beforeNewCodePeriod = IntStream.range(0, 10).mapToObj(number -> db.issues().insertIssue(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2"))))
      .map(IssueDto::getKey)
      .toList();

    List<String> afterNewCodePeriod = IntStream.range(0, 5).mapToObj(number -> db.issues().insertIssue(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2015-01-02"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2"))))
      .map(IssueDto::getKey)
      .toList();

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Issues.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .setParam("inNewCodePeriod", "true")
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrderElementsOf(afterNewCodePeriod)
      .doesNotContainAnyElementsOf(beforeNewCodePeriod);
  }

  @Test
  public void whenListIssuesByNewCodePeriodReferenceBranch_shouldReturnIssues() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();

    db.components().insertSnapshot(project, s -> s.setLast(true).setPeriodMode(REFERENCE_BRANCH.name()));
    db.measures().insertMeasure(project, m -> m.addValue(ANALYSIS_FROM_SONARQUBE_9_4_KEY, 1.0D));

    List<String> beforeNewCodePeriod = IntStream.range(0, 10).mapToObj(number -> db.issues().insertIssue(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2"))))
      .map(IssueDto::getKey)
      .toList();

    List<String> afterNewCodePeriod = IntStream.range(0, 5).mapToObj(number -> db.issues().insertIssue(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2015-01-02"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2"))))
      .peek(issueDto -> db.issues().insertNewCodeReferenceIssue(issueDto))
      .map(IssueDto::getKey)
      .toList();

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Issues.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .setParam("inNewCodePeriod", "true")
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey)
      .containsExactlyInAnyOrderElementsOf(afterNewCodePeriod)
      .doesNotContainAnyElementsOf(beforeNewCodePeriod);
  }

  @Test
  public void whenListIssuesWithoutTypesParam_shouldNotReturnSecurityHotspots() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    RuleDto rule = db.rules().insertHotspotRule();
    db.issues().insertHotspot(rule, project, file, t -> t.setStatus(STATUS_CONFIRMED));

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Issues.ListWsResponse response = ws.newRequest()
      .setParam("component", file.getKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .executeProtobuf(Issues.ListWsResponse.class);
    assertThat(response.getIssuesList()).isEmpty();
  }

  @Test
  @UseDataProvider("pages")
  public void whenUsingPagination_shouldReturnPaginatedResults(String page, int expectedNumberOfIssues) {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newIssueRule();
    IntStream.range(0, 10).forEach(number -> db.issues().insertIssue(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_OPEN)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04"))
      .setCodeVariants(List.of("variant1", "variant2"))));

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Issues.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .setParam("p", page)
      .setParam("ps", "3")
      .executeProtobuf(Issues.ListWsResponse.class);

    assertThat(response.getIssuesList()).hasSize(expectedNumberOfIssues);
    assertThat(response.getPaging())
      .extracting(Common.Paging::getPageIndex, Common.Paging::getPageSize, Common.Paging::getTotal)
      .containsExactly(Integer.parseInt(page), expectedNumberOfIssues, 0);
  }

  private RuleDto newIssueRule() {
    return newIssueRule(XOO_X1, RuleType.CODE_SMELL);
  }

  private RuleDto newIssueRule(RuleKey ruleKey, RuleType ruleType) {
    RuleDto rule = newRule(ruleKey, createDefaultRuleDescriptionSection(uuidFactory.create(), "Rule desc"))
      .setLanguage("xoo")
      .setName("Rule name")
      .setType(ruleType)
      .setStatus(RuleStatus.READY);
    db.rules().insert(rule);
    return rule;
  }

  @DataProvider
  public static Object[][] pages() {
    return new Object[][] {
      {"1", 3},
      {"2", 3},
      {"3", 3},
      {"4", 1},
    };
  }
}
