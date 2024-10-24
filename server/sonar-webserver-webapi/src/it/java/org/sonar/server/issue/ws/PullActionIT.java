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
package org.sonar.server.issue.ws;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.ws.pull.PullActionProtobufObjectGenerator;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Issues;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class PullActionIT {

  private static final long NOW = 10_000_000_000L;
  private static final long PAST = 1_000_000_000L;

  private static final String DEFAULT_BRANCH = DEFAULT_MAIN_BRANCH_NAME;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final System2 system2 = mock(System2.class);
  private final TaintChecker taintChecker = mock(TaintChecker.class);
  private final PullActionProtobufObjectGenerator pullActionProtobufObjectGenerator = new PullActionProtobufObjectGenerator();

  private final ComponentTypesRule resourceTypes = new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT);
  private final ComponentFinder componentFinder = new ComponentFinder(db.getDbClient(), resourceTypes);

  private final IssueDbTester issueDbTester = new IssueDbTester(db);

  private final PullAction underTest = new PullAction(system2, componentFinder, db.getDbClient(), userSession,
    pullActionProtobufObjectGenerator, taintChecker);
  private final WsActionTester tester = new WsActionTester(underTest);

  private RuleDto correctRule, incorrectRule;
  private ComponentDto correctMainBranch, incorrectMainBranch;
  private ProjectDto project;
  private ComponentDto correctFile, incorrectFile;

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(NOW);
    correctRule = db.rules().insertIssueRule();
    ProjectData projectData = db.components().insertPrivateProject();
    correctMainBranch = projectData.getMainBranchComponent();
    project = projectData.getProjectDto();
    correctFile = db.components().insertComponent(newFileDto(correctMainBranch));

    incorrectRule = db.rules().insertIssueRule();
    incorrectMainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    incorrectFile = db.components().insertComponent(newFileDto(incorrectMainBranch));

    when(taintChecker.getTaintRepositories()).thenReturn(List.of("roslyn.sonaranalyzer.security.cs",
      "javasecurity", "jssecurity", "tssecurity", "phpsecurity", "pythonsecurity"));
  }

  @Test
  public void givenMissingParams_expectIllegalArgumentException() {
    TestRequest request = tester.newRequest();

    assertThatThrownBy(() -> request.executeProtobuf(Issues.IssuesPullQueryTimestamp.class))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void givenNotExistingProjectKey_throwException() {
    TestRequest request = tester.newRequest()
      .setParam("projectKey", "projectKey")
      .setParam("branchName", DEFAULT_BRANCH);

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project 'projectKey' not found");
  }

  @Test
  public void givenValidProjectKeyWithoutPermissionsTo_throwException() {
    userSession.logIn();

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void givenNotExistingBranchKey_throwException() {
    DbCommons.TextRange textRange = DbCommons.TextRange.newBuilder()
      .setStartLine(1)
      .setEndLine(2)
      .setStartOffset(3)
      .setEndOffset(4)
      .build();
    DbIssues.Locations.Builder mainLocation = DbIssues.Locations.newBuilder()
      .setChecksum("hash")
      .setTextRange(textRange);

    RuleDto rule = db.rules().insertIssueRule(r -> r.setRepositoryKey("java").setRuleKey("S1000"));
    IssueDto issueDto = issueDbTester.insertIssue(rule, p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("message")
      .setCreatedAt(NOW)
      .setStatus(Issue.STATUS_RESOLVED)
      .setLocations(mainLocation.build())
      .setType(Common.RuleType.BUG.getNumber()));
    loginWithBrowsePermission(issueDto);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", issueDto.getProjectKey())
      .setParam("branchName", "non-existent-branch");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Branch 'non-existent-branch' in project '%s' not found", issueDto.getProjectKey()));
  }

  @Test
  public void givenTaintRuleRepository_throwException() {
    TestRequest request = tester.newRequest()
      .setParam("projectKey", "project-key")
      .setParam("branchName", "branch-name")
      .setParam("ruleRepositories", "javasecurity");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Incorrect rule repositories list: it should only include repositories that define Issues, and no Taint Vulnerabilities");
  }

  @Test
  public void givenValidProjectKeyAndOneIssueOnBranch_returnOneIssue() throws IOException {
    DbCommons.TextRange textRange = DbCommons.TextRange.newBuilder()
      .setStartLine(1)
      .setEndLine(2)
      .setStartOffset(3)
      .setEndOffset(4)
      .build();
    DbIssues.Locations.Builder mainLocation = DbIssues.Locations.newBuilder()
      .setChecksum("hash")
      .setTextRange(textRange);

    RuleDto rule = db.rules().insertIssueRule(r -> r.setRepositoryKey("java").setRuleKey("S1000"));
    IssueDto issueDto = issueDbTester.insertIssue(rule, p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("message")
      .setIssueCreationTime(NOW)
      .setStatus(Issue.STATUS_RESOLVED)
      .setLocations(mainLocation.build())
      .setType(Common.RuleType.BUG.getNumber()));
    loginWithBrowsePermission(issueDto);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", issueDto.getProjectKey())
      .setParam("branchName", DEFAULT_BRANCH);

    TestResponse response = request.execute();
    List<Issues.IssueLite> issues = readAllIssues(response);

    assertThat(issues).hasSize(1);

    Issues.IssueLite issueLite = issues.get(0);
    assertThat(issueLite.getKey()).isEqualTo(issueDto.getKey());
    assertThat(issueLite.getUserSeverity()).isEqualTo(Common.Severity.MINOR);
    assertThat(issueLite.getCreationDate()).isEqualTo(NOW);
    assertThat(issueLite.getResolved()).isTrue();
    assertThat(issueLite.getRuleKey()).isEqualTo("java:S1000");
    assertThat(issueLite.getType()).isEqualTo(Common.RuleType.forNumber(issueDto.getType()));

    Issues.Location location = issueLite.getMainLocation();
    assertThat(location.getMessage()).isEqualTo(issueDto.getMessage());

    Issues.TextRange locationTextRange = location.getTextRange();
    assertThat(locationTextRange.getStartLine()).isEqualTo(1);
    assertThat(locationTextRange.getEndLine()).isEqualTo(2);
    assertThat(locationTextRange.getStartLineOffset()).isEqualTo(3);
    assertThat(locationTextRange.getEndLineOffset()).isEqualTo(4);
    assertThat(locationTextRange.getHash()).isEqualTo("hash");
  }

  @Test
  public void givenValidProjectKeyAndOneTaintVulnerabilityOnBranch_returnNoIssues() throws IOException {
    DbCommons.TextRange textRange = DbCommons.TextRange.newBuilder()
      .setStartLine(1)
      .setEndLine(2)
      .setStartOffset(3)
      .setEndOffset(4)
      .build();
    DbIssues.Locations.Builder mainLocation = DbIssues.Locations.newBuilder()
      .setChecksum("hash")
      .setTextRange(textRange);

    RuleDto rule = db.rules().insertIssueRule(r -> r.setRepositoryKey("javasecurity").setRuleKey("S1000"));
    IssueDto issueDto = issueDbTester.insertIssue(rule, p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("message")
      .setCreatedAt(NOW)
      .setStatus(Issue.STATUS_RESOLVED)
      .setLocations(mainLocation.build())
      .setType(Common.RuleType.BUG.getNumber()));
    loginWithBrowsePermission(issueDto);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", issueDto.getProjectKey())
      .setParam("branchName", DEFAULT_BRANCH);

    TestResponse response = request.execute();
    List<Issues.IssueLite> issues = readAllIssues(response);

    assertThat(issues).isEmpty();
  }

  @Test
  public void givenIssueOnAnotherBranch_returnOneIssue() throws IOException {
    ComponentDto developBranch = db.components().insertPrivateProjectWithCustomBranch("develop").getMainBranchComponent();
    ComponentDto developFile = db.components().insertComponent(newFileDto(developBranch));
    generateIssues(correctRule, developBranch, developFile, 1);
    loginWithBrowsePermission(developBranch.branchUuid());

    TestRequest request = tester.newRequest()
      .setParam("projectKey", developBranch.getKey())
      .setParam("branchName", "develop");

    TestResponse response = request.execute();
    List<Issues.IssueLite> issues = readAllIssues(response);

    assertThat(issues).hasSize(1);
  }

  @Test
  public void inIncrementalModeReturnClosedIssues() throws IOException {
    IssueDto openIssue = issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setStatus(Issue.STATUS_OPEN)
      .setType(Common.RuleType.BUG.getNumber()));

    issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setMessage("closedIssue")
      .setCreatedAt(NOW)
      .setStatus(Issue.STATUS_CLOSED)
      .setType(Common.RuleType.BUG.getNumber())
      .setComponentUuid(openIssue.getComponentUuid())
      .setProjectUuid(openIssue.getProjectUuid())
      .setIssueUpdateTime(PAST)
      .setIssueCreationTime(PAST));

    loginWithBrowsePermission(openIssue);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", openIssue.getProjectKey())
      .setParam("branchName", DEFAULT_BRANCH)
      .setParam("changedSince", PAST + "");

    TestResponse response = request.execute();
    List<Issues.IssueLite> issues = readAllIssues(response);

    assertThat(issues).hasSize(2);
  }

  @Test
  public void given15IssuesInTheTable_returnOnly10ThatBelongToProject() throws IOException {
    loginWithBrowsePermission(project);
    generateIssues(correctRule, correctMainBranch, correctFile, 10);
    generateIssues(incorrectRule, incorrectMainBranch, incorrectFile, 5);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH);

    TestResponse response = request.execute();
    List<Issues.IssueLite> issues = readAllIssues(response);

    assertThat(issues).hasSize(10);
  }

  @Test
  public void givenNoIssuesBelongToTheProject_return0Issues() throws IOException {
    loginWithBrowsePermission(project);
    generateIssues(incorrectRule, incorrectMainBranch, incorrectFile, 5);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH);

    TestResponse response = request.execute();
    List<Issues.IssueLite> issues = readAllIssues(response);

    assertThat(issues).isEmpty();
  }

  @Test
  public void testLanguagesParam_return1Issue() throws IOException {
    loginWithBrowsePermission(project);
    RuleDto javaRule = db.rules().insert(r -> r.setLanguage("java"));

    IssueDto javaIssue = issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setRule(javaRule)
      .setRuleUuid(javaRule.getUuid())
      .setStatus(Issue.STATUS_OPEN)
      .setLanguage("java")
      .setProject(correctMainBranch)
      .setComponent(correctFile)
      .setType(Common.RuleType.BUG.getNumber()));

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH)
      .setParam("languages", "java");

    TestResponse response = request.execute();
    List<Issues.IssueLite> issues = readAllIssues(response);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getKey()).isEqualTo(javaIssue.getKey());
  }

  @Test
  public void testLanguagesParam_givenWrongLanguage_return0Issues() throws IOException {
    loginWithBrowsePermission(project);
    RuleDto javascriptRule = db.rules().insert(r -> r.setLanguage("javascript"));

    issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setRule(javascriptRule)
      .setRuleUuid(javascriptRule.getUuid())
      .setStatus(Issue.STATUS_OPEN)
      .setProject(correctMainBranch)
      .setComponent(correctFile)
      .setType(2));

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH)
      .setParam("languages", "java");

    TestResponse response = request.execute();
    List<Issues.IssueLite> issues = readAllIssues(response);

    assertThat(issues).isEmpty();
  }

  @Test
  public void testRuleRepositoriesParam_return1IssueForGivenRepository() throws IOException {
    loginWithBrowsePermission(project);
    RuleDto javaRule = db.rules().insert(r -> r.setRepositoryKey("java"));
    RuleDto javaScriptRule = db.rules().insert(r -> r.setRepositoryKey("javascript"));

    IssueDto issueDto = issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setRule(javaRule)
      .setStatus(Issue.STATUS_OPEN)
      .setProject(correctMainBranch)
      .setComponent(correctFile)
      .setType(2));

    //this one should not be returned - it is a different rule repository
    issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setRule(javaScriptRule)
      .setStatus(Issue.STATUS_OPEN)
      .setProject(correctMainBranch)
      .setComponent(correctFile)
      .setType(Common.RuleType.BUG.getNumber()));

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH)
      .setParam("ruleRepositories", "java");

    TestResponse response = request.execute();
    List<Issues.IssueLite> issues = readAllIssues(response);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getKey()).isEqualTo(issueDto.getKey());
  }

  private void generateIssues(RuleDto rule, ComponentDto project, ComponentDto file, int numberOfIssues) {
    for (int j = 0; j < numberOfIssues; j++) {
      issueDbTester.insert(i -> i.setProject(project)
        .setComponentUuid(file.uuid())
        .setRuleUuid(rule.getUuid())
        .setStatus(Issue.STATUS_OPEN)
        .setType(2));
    }
  }

  private List<Issues.IssueLite> readAllIssues(TestResponse response) throws IOException {
    List<Issues.IssueLite> issues = new ArrayList<>();
    InputStream inputStream = response.getInputStream();
    Issues.IssuesPullQueryTimestamp.parseDelimitedFrom(inputStream);

    while (inputStream.available() > 0) {
      issues.add(Issues.IssueLite.parseDelimitedFrom(inputStream));
    }

    return issues;
  }

  private void loginWithBrowsePermission(IssueDto issueDto) {
    loginWithBrowsePermission(issueDto.getProjectUuid());
  }

  private void loginWithBrowsePermission(String branchUuid) {
    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectByBranchUuid(db.getSession(), branchUuid);
    loginWithBrowsePermission(projectDto.get());
  }

  private void loginWithBrowsePermission(ProjectDto projectDto) {
    UserDto user = db.users().insertUser("john");
    userSession.logIn(user)
      .addProjectPermission(USER, projectDto);
  }

}
