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
package org.sonar.server.hotspot.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.time.Clock;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.NewCodePeriodResolver;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.MessageFormattingUtils;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.api.issue.Issue.RESOLUTION_ACKNOWLEDGED;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
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
  private final TextRangeResponseFormatter textRangeResponseFormatter = new TextRangeResponseFormatter();
  private final HotspotWsResponseFormatter hotspotWsResponseFormatter = new HotspotWsResponseFormatter(textRangeResponseFormatter);
  private final ComponentFinder componentFinder = TestComponentFinder.from(db);
  private final WsActionTester ws = new WsActionTester(
    new ListAction(dbClient, userSession, hotspotWsResponseFormatter, new NewCodePeriodResolver(dbClient, Clock.systemUTC()), componentFinder));

  @Test
  public void whenNoProjectProvided_shouldFailWithMessage() {
    TestRequest request = ws.newRequest();
    assertThatThrownBy(() -> request.executeProtobuf(Hotspots.ListWsResponse.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'project' parameter is missing");
  }

  @Test
  public void whenBranchAndPullRequestProvided_shouldFailWithMessage() {
    TestRequest request = ws.newRequest()
      .setParam("project", "some-project")
      .setParam("branch", "some-branch")
      .setParam("pullRequest", "some-pr");
    assertThatThrownBy(() -> request.executeProtobuf(Hotspots.ListWsResponse.class))
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
    RuleDto rule = newHotspotRule();
    db.issues().insertHotspot(rule, project, file, i -> i
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
    assertThatThrownBy(() -> request.executeProtobuf(Hotspots.ListWsResponse.class))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void whenListHotspotsByProject_shouldReturnAllFields() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newHotspotRule();
    IssueDto hotspot = db.issues().insertHotspot(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_TO_REVIEW)
      .setResolution(null)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04")));

    ComponentDto anotherBranch = db.components().insertProjectBranch(project, b -> b.setKey("branch1"));

    ComponentDto fileFromAnotherBranch = db.components().insertComponent(newFileDto(anotherBranch));
    db.issues().insertHotspot(rule, anotherBranch, fileFromAnotherBranch, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_REVIEWED)
      .setResolution(RESOLUTION_FIXED)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04")));

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Hotspots.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .executeProtobuf(Hotspots.ListWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(
        Hotspots.SearchWsResponse.Hotspot::getKey, Hotspots.SearchWsResponse.Hotspot::getRuleKey, Hotspots.SearchWsResponse.Hotspot::getSecurityCategory,
        Hotspots.SearchWsResponse.Hotspot::getComponent, Hotspots.SearchWsResponse.Hotspot::getResolution, Hotspots.SearchWsResponse.Hotspot::getStatus,
        Hotspots.SearchWsResponse.Hotspot::getMessage, Hotspots.SearchWsResponse.Hotspot::getMessageFormattingsList,
        Hotspots.SearchWsResponse.Hotspot::getAssignee, Hotspots.SearchWsResponse.Hotspot::getAuthor, Hotspots.SearchWsResponse.Hotspot::getLine,
        Hotspots.SearchWsResponse.Hotspot::getCreationDate, Hotspots.SearchWsResponse.Hotspot::getUpdateDate)
      .containsExactlyInAnyOrder(
        tuple(hotspot.getKey(), rule.getKey().toString(), "others", file.getKey(), "", STATUS_TO_REVIEW, "the message",
          MessageFormattingUtils.dbMessageFormattingListToWs(List.of(MESSAGE_FORMATTING)), simon.getUuid(), "John", 42,
          formatDateTime(hotspot.getIssueCreationDate()), formatDateTime(hotspot.getIssueUpdateDate())));
  }

  @Test
  public void whenListHotspotsByResolution_shouldReturnValidHotspots() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newHotspotRule();
    IssueDto hotspot1 = db.issues().insertHotspot(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_REVIEWED)
      .setResolution(RESOLUTION_FIXED)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04")));

    IssueDto hotspot2 = db.issues().insertHotspot(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_REVIEWED)
      .setResolution(RESOLUTION_FIXED)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04")));

    IssueDto hotspot3 = db.issues().insertHotspot(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_REVIEWED)
      .setResolution(RESOLUTION_SAFE)
      .setSeverity("MAJOR")
      .setAuthorLogin("John")
      .setAssigneeUuid(simon.getUuid())
      .setTags(asList("bug", "owasp"))
      .setIssueCreationDate(parseDate("2014-09-03"))
      .setIssueUpdateDate(parseDate("2017-12-04")));

    RuleDto vulnerabilityRule = newIssueRule(XOO_X2, RuleType.VULNERABILITY);
    IssueDto vulnerabilityIssue = db.issues().insertIssue(vulnerabilityRule, project, file, i -> i
      .setType(RuleType.VULNERABILITY)
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

    userSession
      .logIn(user)
      .registerProjects(projectData.getProjectDto());

    Hotspots.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.getProjectDto().getKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .setParam("resolution", RESOLUTION_FIXED)
      .executeProtobuf(Hotspots.ListWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(Hotspots.SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(hotspot1.getKey(), hotspot2.getKey())
      .doesNotContain(hotspot3.getKey(), vulnerabilityIssue.getKey());

    response = ws.newRequest()
      .setParam("project", projectData.getProjectDto().getKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .setParam("resolution", RESOLUTION_SAFE)
      .executeProtobuf(Hotspots.ListWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(Hotspots.SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrder(hotspot3.getKey())
      .doesNotContain(hotspot1.getKey(), hotspot2.getKey(), vulnerabilityIssue.getKey());

    response = ws.newRequest()
      .setParam("project", projectData.getProjectDto().getKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .setParam("resolution", RESOLUTION_ACKNOWLEDGED)
      .executeProtobuf(Hotspots.ListWsResponse.class);

    assertThat(response.getHotspotsList()).isEmpty();
  }

  @Test
  public void whenListHotspotsByNewCodePeriodDate_shouldReturnHotspots() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newHotspotRule();

    db.components().insertSnapshot(project, s -> s.setLast(true).setPeriodDate(parseDateTime("2014-09-05T00:00:00+0100").getTime()));

    List<String> beforeNewCodePeriod = IntStream.range(0, 10).mapToObj(number -> db.issues().insertHotspot(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_TO_REVIEW)
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

    List<String> afterNewCodePeriod = IntStream.range(0, 5).mapToObj(number -> db.issues().insertHotspot(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_TO_REVIEW)
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

    Hotspots.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .setParam("inNewCodePeriod", "true")
      .executeProtobuf(Hotspots.ListWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(Hotspots.SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrderElementsOf(afterNewCodePeriod)
      .doesNotContainAnyElementsOf(beforeNewCodePeriod);
  }

  @Test
  public void whenListHotspotsByNewCodePeriodReferenceBranch_shouldReturnHotspots() {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newHotspotRule();

    db.components().insertSnapshot(project, s -> s.setLast(true).setPeriodMode(REFERENCE_BRANCH.name()));
    db.measures().insertMeasure(project, m -> m.addValue(ANALYSIS_FROM_SONARQUBE_9_4_KEY, 1.0D));

    List<String> beforeNewCodePeriod = IntStream.range(0, 10).mapToObj(number -> db.issues().insertHotspot(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_TO_REVIEW)
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

    List<String> afterNewCodePeriod = IntStream.range(0, 5).mapToObj(number -> db.issues().insertHotspot(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_TO_REVIEW)
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

    Hotspots.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .setParam("inNewCodePeriod", "true")
      .executeProtobuf(Hotspots.ListWsResponse.class);

    assertThat(response.getHotspotsList())
      .extracting(Hotspots.SearchWsResponse.Hotspot::getKey)
      .containsExactlyInAnyOrderElementsOf(afterNewCodePeriod)
      .doesNotContainAnyElementsOf(beforeNewCodePeriod);
  }

  @Test
  @UseDataProvider("pages")
  public void whenUsingPagination_shouldReturnPaginatedResults(String page, int expectedNumberOfIssues) {
    UserDto user = db.users().insertUser();

    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    UserDto simon = db.users().insertUser();
    RuleDto rule = newHotspotRule();
    IntStream.range(0, 10).forEach(number -> db.issues().insertHotspot(rule, project, file, i -> i
      .setEffort(10L)
      .setLine(42)
      .setChecksum("a227e508d6646b55a086ee11d63b21e9")
      .setMessage("the message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setStatus(STATUS_TO_REVIEW)
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

    Hotspots.ListWsResponse response = ws.newRequest()
      .setParam("project", projectData.projectKey())
      .setParam("branch", projectData.getMainBranchDto().getKey())
      .setParam("p", page)
      .setParam("ps", "3")
      .executeProtobuf(Hotspots.ListWsResponse.class);

    assertThat(response.getHotspotsList()).hasSize(expectedNumberOfIssues);
    assertThat(response.getPaging())
      .extracting(Common.Paging::getPageIndex, Common.Paging::getPageSize, Common.Paging::getTotal)
      .containsExactly(Integer.parseInt(page), expectedNumberOfIssues, 0);
  }

  private RuleDto newHotspotRule() {
    return newIssueRule(XOO_X1, RuleType.SECURITY_HOTSPOT);
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
