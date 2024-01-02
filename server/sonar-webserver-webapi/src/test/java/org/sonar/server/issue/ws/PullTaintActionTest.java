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
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.ws.pull.PullTaintActionProtobufObjectGenerator;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.MessageFormattingUtils;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Issues;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.protobuf.DbIssues.MessageFormattingType.CODE;

public class PullTaintActionTest {
  private static final long NOW = 10_000_000_000L;
  private static final long PAST = 1_000_000_000L;

  private static final String DEFAULT_BRANCH = DEFAULT_MAIN_BRANCH_NAME;
  public static final DbIssues.MessageFormatting MESSAGE_FORMATTING = DbIssues.MessageFormatting.newBuilder().setStart(0).setEnd(4).setType(CODE).build();

  @Rule
  public DbTester dbTester = DbTester.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final System2 system2 = mock(System2.class);
  private final TaintChecker taintChecker = mock(TaintChecker.class);
  private final ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private final ComponentFinder componentFinder = new ComponentFinder(db.getDbClient(), resourceTypes);
  private final IssueDbTester issueDbTester = new IssueDbTester(db);
  private final ComponentDbTester componentDbTester = new ComponentDbTester(db);

  private PullTaintActionProtobufObjectGenerator objectGenerator = new PullTaintActionProtobufObjectGenerator(db.getDbClient(), userSession);
  private PullTaintAction underTest = new PullTaintAction(system2, componentFinder, db.getDbClient(), userSession,
    objectGenerator, taintChecker);
  private WsActionTester tester = new WsActionTester(underTest);

  private RuleDto correctRule, incorrectRule;
  private ComponentDto correctProject, incorrectProject;
  private ComponentDto correctFile, incorrectFile;

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(NOW);
    RuleRepositoryDto repository = new RuleRepositoryDto("javasecurity", "java", "Security SonarAnalyzer");
    db.getDbClient().ruleRepositoryDao().insert(db.getSession(), List.of(repository));
    correctRule = db.rules().insertIssueRule(r -> r.setRepositoryKey("javasecurity").setRuleKey("S1000").setSeverity(3));

    correctProject = db.components().insertPrivateProject();
    correctFile = db.components().insertComponent(newFileDto(correctProject));

    incorrectRule = db.rules().insertIssueRule();
    incorrectProject = db.components().insertPrivateProject();
    incorrectFile = db.components().insertComponent(newFileDto(incorrectProject));

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
      .setParam("projectKey", correctProject.getKey())
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
  public void givenValidProjectKeyAndOneNormalIssueOnBranch_returnNoTaintVulnerabilities() throws IOException {
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
      .setParam("branchName", DEFAULT_BRANCH);

    TestResponse response = request.execute();
    List<Issues.TaintVulnerabilityLite> taints = readAllTaint(response);

    assertThat(taints).isEmpty();
  }

  @Test
  public void givenValidProjectKeyAndOneTaintOnBranch_returnOneTaint_WithMetadataSeverity() throws IOException {
    loginWithBrowsePermission(correctProject.branchUuid(), correctFile.uuid());
    DbCommons.TextRange textRange = DbCommons.TextRange.newBuilder()
      .setStartLine(1)
      .setEndLine(2)
      .setStartOffset(3)
      .setEndOffset(4)
      .build();
    DbIssues.Locations.Builder mainLocation = DbIssues.Locations.newBuilder()
      .setChecksum("hash")
      .setTextRange(textRange);

    IssueDto issueDto = issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setRule(correctRule)
      .setProject(correctProject)
      .setComponent(correctFile)
      .setAssigneeUuid(userSession.getUuid())
      .setManualSeverity(true)
      .setMessage("message")
      .setMessageFormattings(DbIssues.MessageFormattings.newBuilder().addMessageFormatting(MESSAGE_FORMATTING).build())
      .setCreatedAt(NOW)
      .setStatus(Issue.STATUS_OPEN)
      .setLocations(mainLocation.build())
      .setType(Common.RuleType.VULNERABILITY.getNumber()));

    TestRequest request = tester.newRequest()
      .setParam("projectKey", issueDto.getProjectKey())
      .setParam("branchName", DEFAULT_BRANCH);

    TestResponse response = request.execute();
    List<Issues.TaintVulnerabilityLite> taints = readAllTaint(response);
    Issues.TaintVulnerabilityLite taintLite = taints.get(0);

    assertThat(taints).hasSize(1);

    assertThat(taintLite.getKey()).isEqualTo(issueDto.getKey());
    assertThat(taintLite.getSeverity()).isEqualTo(Common.Severity.MINOR);
    assertThat(taintLite.getCreationDate()).isEqualTo(NOW);
    assertThat(taintLite.getResolved()).isFalse();
    assertThat(taintLite.getRuleKey()).isEqualTo("javasecurity:S1000");
    assertThat(taintLite.getType()).isEqualTo(Common.RuleType.forNumber(issueDto.getType()));
    assertThat(taintLite.getAssignedToSubscribedUser()).isTrue();

    Issues.Location location = taintLite.getMainLocation();
    assertThat(location.getMessage()).isEqualTo(issueDto.getMessage());
    assertThat(location.getMessageFormattingsList()).isEqualTo(MessageFormattingUtils.dbMessageFormattingListToWs(List.of(MESSAGE_FORMATTING)));

    Issues.TextRange locationTextRange = location.getTextRange();
    assertThat(locationTextRange.getStartLine()).isEqualTo(1);
    assertThat(locationTextRange.getEndLine()).isEqualTo(2);
    assertThat(locationTextRange.getStartLineOffset()).isEqualTo(3);
    assertThat(locationTextRange.getEndLineOffset()).isEqualTo(4);
    assertThat(locationTextRange.getHash()).isEqualTo("hash");
  }

  @Test
  public void givenTaintOnAnotherBranch_returnOneTaint() throws IOException {
    ComponentDto developBranch = componentDbTester.insertPrivateProjectWithCustomBranch("develop");
    ComponentDto developFile = db.components().insertComponent(newFileDto(developBranch));
    generateTaints(correctRule, developBranch, developFile, 1);
    loginWithBrowsePermission(developBranch.uuid(), developFile.uuid());

    TestRequest request = tester.newRequest()
      .setParam("projectKey", developBranch.getKey())
      .setParam("branchName", "develop");

    TestResponse response = request.execute();
    List<Issues.TaintVulnerabilityLite> taints = readAllTaint(response);

    assertThat(taints).hasSize(1);
  }

  @Test
  public void given15TaintsInTheTable_returnOnly10ThatBelongToProject() throws IOException {
    loginWithBrowsePermission(correctProject.uuid(), correctFile.uuid());
    generateTaints(correctRule, correctProject, correctFile, 10);
    generateTaints(incorrectRule, incorrectProject, incorrectFile, 5);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctProject.getKey())
      .setParam("branchName", DEFAULT_BRANCH);

    TestResponse response = request.execute();
    List<Issues.TaintVulnerabilityLite> taints = readAllTaint(response);

    assertThat(taints).hasSize(10);
  }

  @Test
  public void givenNoTaintsBelongToTheProject_return0Taints() throws IOException {
    loginWithBrowsePermission(correctProject.uuid(), correctFile.uuid());
    generateTaints(incorrectRule, incorrectProject, incorrectFile, 5);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctProject.getKey())
      .setParam("branchName", DEFAULT_BRANCH);

    TestResponse response = request.execute();
    List<Issues.TaintVulnerabilityLite> taints = readAllTaint(response);

    assertThat(taints).isEmpty();
  }

  @Test
  public void testLanguagesParam_return1Taint() throws IOException {
    loginWithBrowsePermission(correctProject.uuid(), correctFile.uuid());
    RuleDto javaRule = db.rules().insert(r -> r.setLanguage("java").setRepositoryKey("javasecurity"));
    RuleDto javascriptRule = db.rules().insert(r -> r.setLanguage("javascript").setRepositoryKey("javasecurity"));

    IssueDto javaIssue = issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setRule(javaRule)
      .setRuleUuid(javaRule.getUuid())
      .setStatus(Issue.STATUS_OPEN)
      .setLanguage("java")
      .setProject(correctProject)
      .setComponent(correctFile)
      .setType(Common.RuleType.VULNERABILITY.getNumber()));

    issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setRule(javascriptRule)
      .setRuleUuid(javascriptRule.getUuid())
      .setStatus(Issue.STATUS_OPEN)
      .setLanguage("java")
      .setProject(correctProject)
      .setComponent(correctFile)
      .setType(Common.RuleType.VULNERABILITY.getNumber()));

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctProject.getKey())
      .setParam("branchName", DEFAULT_BRANCH)
      .setParam("languages", "java");

    TestResponse response = request.execute();
    List<Issues.TaintVulnerabilityLite> taints = readAllTaint(response);

    assertThat(taints).hasSize(1);
    assertThat(taints.get(0).getKey()).isEqualTo(javaIssue.getKey());
  }

  @Test
  public void testLanguagesParam_givenWrongLanguage_return0Taints() throws IOException {
    loginWithBrowsePermission(correctProject.uuid(), correctFile.uuid());
    RuleDto javascriptRule = db.rules().insert(r -> r.setLanguage("jssecurity"));

    issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setRule(javascriptRule)
      .setRuleUuid(javascriptRule.getUuid())
      .setStatus(Issue.STATUS_OPEN)
      .setProject(correctProject)
      .setComponent(correctFile)
      .setType(2));

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctProject.getKey())
      .setParam("branchName", DEFAULT_BRANCH)
      .setParam("languages", "java");

    TestResponse response = request.execute();
    List<Issues.TaintVulnerabilityLite> taints = readAllTaint(response);

    assertThat(taints).isEmpty();
  }

  @Test
  public void given1TaintAnd1NormalIssue_return1Taint() throws IOException {
    loginWithBrowsePermission(correctProject.uuid(), correctFile.uuid());
    RuleDto javaRule = db.rules().insert(r -> r.setRepositoryKey("javasecurity"));
    RuleDto javaScriptRule = db.rules().insert(r -> r.setRepositoryKey("javascript"));

    String ruledescriptionContextKey = randomAlphabetic(6);
    IssueDto issueDto = issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setRule(javaRule)
      .setStatus(Issue.STATUS_OPEN)
      .setProject(correctProject)
      .setComponent(correctFile)
      .setType(2)
      .setRuleDescriptionContextKey(ruledescriptionContextKey));

    //this one should not be returned - it is a normal issue, no taint
    issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setRule(javaScriptRule)
      .setStatus(Issue.STATUS_OPEN)
      .setProject(correctProject)
      .setComponent(correctFile)
      .setType(Common.RuleType.VULNERABILITY.getNumber()));

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctProject.getKey())
      .setParam("branchName", DEFAULT_BRANCH);

    TestResponse response = request.execute();
    List<Issues.TaintVulnerabilityLite> taints = readAllTaint(response);

    assertThat(taints).hasSize(1);
    Issues.TaintVulnerabilityLite taintVulnerabilityLite = taints.get(0);
    assertThat(taintVulnerabilityLite.getKey()).isEqualTo(issueDto.getKey());
    assertThat(taintVulnerabilityLite.getRuleDescriptionContextKey()).isEqualTo(ruledescriptionContextKey);
  }

  @Test
  public void inIncrementalModeReturnClosedIssues() throws IOException {
    IssueDto openIssue = issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setRule(correctRule)
      .setManualSeverity(true)
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setStatus(Issue.STATUS_OPEN)
      .setType(Common.RuleType.BUG.getNumber()));

    issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setRule(correctRule)
      .setMessage("closedIssue")
      .setCreatedAt(NOW)
      .setStatus(Issue.STATUS_CLOSED)
      .setType(Common.RuleType.BUG.getNumber())
      .setComponentUuid(openIssue.getComponentUuid())
      .setProjectUuid(openIssue.getProjectUuid())
      .setIssueUpdateTime(PAST)
      .setIssueCreationTime(PAST));

    issueDbTester.insertIssue(p -> p.setSeverity("MINOR")
      .setRule(incorrectRule)
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
    List<Issues.TaintVulnerabilityLite> taints = readAllTaint(response);

    assertThat(taints).hasSize(2);
  }

  private void generateTaints(RuleDto rule, ComponentDto project, ComponentDto file, int numberOfIssues) {
    for (int j = 0; j < numberOfIssues; j++) {
      issueDbTester.insert(i -> i.setProject(project)
        .setRule(rule)
        .setComponent(file)
        .setStatus(Issue.STATUS_OPEN)
        .setType(3));
    }
  }

  private List<Issues.TaintVulnerabilityLite> readAllTaint(TestResponse response) throws IOException {
    List<Issues.TaintVulnerabilityLite> taints = new ArrayList<>();
    InputStream inputStream = response.getInputStream();
    Issues.TaintVulnerabilityPullQueryTimestamp.parseDelimitedFrom(inputStream);

    while (inputStream.available() > 0) {
      taints.add(Issues.TaintVulnerabilityLite.parseDelimitedFrom(inputStream));
    }

    return taints;
  }

  private void loginWithBrowsePermission(IssueDto issueDto) {
    loginWithBrowsePermission(issueDto.getProjectUuid(), issueDto.getComponentUuid());
  }

  private void loginWithBrowsePermission(String projectUuid, String componentUuid) {
    UserDto user = dbTester.users().insertUser("john");
    userSession.logIn(user)
      .addProjectPermission(USER,
        db.getDbClient().componentDao().selectByUuid(dbTester.getSession(), projectUuid).get(),
        db.getDbClient().componentDao().selectByUuid(dbTester.getSession(), componentUuid).get());
  }

}
