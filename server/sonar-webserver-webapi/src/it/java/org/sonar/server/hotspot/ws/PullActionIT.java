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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.ResourceTypesRule;
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
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots;
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
  private final PullHotspotsActionProtobufObjectGenerator pullActionProtobufObjectGenerator = new PullHotspotsActionProtobufObjectGenerator();

  private final ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private final ComponentFinder componentFinder = new ComponentFinder(db.getDbClient(), resourceTypes);

  private final IssueDbTester issueDbTester = new IssueDbTester(db);

  private final PullAction underTest = new PullAction(system2, componentFinder, db.getDbClient(), userSession,
    pullActionProtobufObjectGenerator);
  private final WsActionTester tester = new WsActionTester(underTest);

  private ProjectDto project;
  private ComponentDto correctMainBranch, incorrectMainBranch;
  private ComponentDto correctFile, incorrectFile;

  @Before
  public void before() {
    when(system2.now()).thenReturn(NOW);
    ProjectData projectData = db.components().insertPrivateProject();
    correctMainBranch = projectData.getMainBranchComponent();
    project = projectData.getProjectDto();

    correctFile = db.components().insertComponent(newFileDto(correctMainBranch));

    ProjectData incorrectProjectData = db.components().insertPrivateProject();
    incorrectMainBranch = incorrectProjectData.getMainBranchComponent();
    incorrectFile = db.components().insertComponent(newFileDto(incorrectMainBranch));
  }

  @Test
  public void wsExecution_whenMissingParams_shouldThrowIllegalArgumentException() {
    TestRequest request = tester.newRequest();

    assertThatThrownBy(() -> request.executeProtobuf(Issues.IssuesPullQueryTimestamp.class))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void wsExecution_whenNotExistingProjectKey_shouldThrowException() {
    TestRequest request = tester.newRequest()
      .setParam("projectKey", "projectKey")
      .setParam("branchName", DEFAULT_BRANCH);

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project 'projectKey' not found");
  }

  @Test
  public void wsExecution_whenValidProjectKeyWithoutPermissionsTo_shouldThrowException() {
    userSession.logIn();

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void wsExecution_whenNotExistingBranchKey_shouldThrowException() {
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
  public void wsExecution_whenValidProjectKeyAndOneHotspotOnBranch_shouldReturnOneHotspot() throws IOException {
    DbCommons.TextRange textRange = DbCommons.TextRange.newBuilder()
      .setStartLine(1)
      .setEndLine(2)
      .setStartOffset(3)
      .setEndOffset(4)
      .build();
    DbIssues.Locations.Builder mainLocation = DbIssues.Locations.newBuilder()
      .setChecksum("hash")
      .setTextRange(textRange);

    UserDto assignee = db.users().insertUser();
    IssueDto issueDto = issueDbTester.insertHotspot(p -> p.setSeverity("MINOR")
      .setMessage("message")
      .setAssigneeUuid(assignee.getUuid())
      .setIssueCreationTime(NOW)
      .setStatus(Issue.STATUS_TO_REVIEW)
      .setLocations(mainLocation.build()));

    loginWithBrowsePermission(issueDto);

    TestResponse response = tester.newRequest()
      .setParam("projectKey", issueDto.getProjectKey())
      .setParam("branchName", DEFAULT_BRANCH)
      .execute();

    List<Hotspots.HotspotLite> issues = readAllIssues(response);

    assertThat(issues).hasSize(1);

    Hotspots.TextRange expectedTextRange = Hotspots.TextRange.newBuilder()
      .setStartLine(1)
      .setEndLine(2)
      .setStartLineOffset(3)
      .setEndLineOffset(4)
      .setHash("hash")
      .build();
    Hotspots.HotspotLite expectedHotspotLite = Hotspots.HotspotLite.newBuilder()
      .setKey(issueDto.getKey())
      .setFilePath(issueDto.getFilePath())
      .setVulnerabilityProbability("LOW")
      .setStatus(Issue.STATUS_TO_REVIEW)
      .setMessage("message")
      .setCreationDate(NOW)
      .setTextRange(expectedTextRange)
      .setRuleKey(issueDto.getRuleKey().toString())
      .setAssignee(assignee.getLogin())
      .build();
    Hotspots.HotspotLite issueLite = issues.get(0);
    assertThat(issueLite).isEqualTo(expectedHotspotLite);
  }

  @Test
  public void wsExecution_whenHotspotOnAnotherBranchThanMain_shouldReturnOneIssue() throws IOException {
    ProjectData projectData = db.components().insertPrivateProjectWithCustomBranch("develop");
    ProjectDto project = projectData.getProjectDto();
    ComponentDto developBranch = projectData.getMainBranchComponent();
    ComponentDto developFile = db.components().insertComponent(newFileDto(developBranch));
    List<String> hotspotKeys = generateHotspots(developBranch, developFile, 1);
    loginWithBrowsePermission(project);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", developBranch.getKey())
      .setParam("branchName", "develop");

    TestResponse response = request.execute();
    List<Hotspots.HotspotLite> issues = readAllIssues(response);

    assertThat(issues).hasSize(1)
      .extracting(Hotspots.HotspotLite::getKey)
      .containsExactlyInAnyOrderElementsOf(hotspotKeys);
  }

  @Test
  public void wsExecution_whenIncrementalModeThen_shouldReturnClosedIssues() throws IOException {
    IssueDto toReviewHotspot = issueDbTester.insertHotspot(p -> p.setSeverity("MINOR")
      .setMessage("toReviewHotspot")
      .setCreatedAt(NOW)
      .setStatus(Issue.STATUS_TO_REVIEW));

    issueDbTester.insertHotspot(p -> p.setSeverity("MINOR")
      .setMessage("closedIssue")
      .setCreatedAt(NOW)
      .setStatus(Issue.STATUS_CLOSED)
      .setComponentUuid(toReviewHotspot.getComponentUuid())
      .setProjectUuid(toReviewHotspot.getProjectUuid())
      .setIssueUpdateTime(PAST)
      .setIssueCreationTime(PAST));

    loginWithBrowsePermission(toReviewHotspot);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", toReviewHotspot.getProjectKey())
      .setParam("branchName", DEFAULT_BRANCH)
      .setParam("changedSince", PAST + "");

    TestResponse response = request.execute();
    List<Hotspots.HotspotLite> issues = readAllIssues(response);

    assertThat(issues).hasSize(2);
  }

  @Test
  public void wsExecution_whenDifferentHotspotsInTheTable_shouldReturnOnlyThatBelongToSelectedProject() throws IOException {
    loginWithBrowsePermission(project);
    List<String> correctIssueKeys = generateHotspots(correctMainBranch, correctFile, 10);
    List<String> incorrectIssueKeys = generateHotspots(incorrectMainBranch, incorrectFile, 5);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH);

    TestResponse response = request.execute();
    List<Hotspots.HotspotLite> issues = readAllIssues(response);

    assertThat(issues)
      .hasSize(10)
      .extracting(Hotspots.HotspotLite::getKey)
      .containsExactlyInAnyOrderElementsOf(correctIssueKeys)
      .doesNotContainAnyElementsOf(incorrectIssueKeys);
  }

  @Test
  public void wsExecution_whenNoIssuesBelongToTheProject_shouldReturnZeroIssues() throws IOException {
    loginWithBrowsePermission(project);
    generateHotspots(incorrectMainBranch, incorrectFile, 5);

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH);

    TestResponse response = request.execute();
    List<Hotspots.HotspotLite> issues = readAllIssues(response);

    assertThat(issues).isEmpty();
  }

  @Test
  public void wsExecution_whenLanguagesParam_shouldReturnOneIssue() throws IOException {
    loginWithBrowsePermission(project);
    RuleDto javaRule = db.rules().insert(r -> r.setLanguage("java"));

    IssueDto javaIssue = issueDbTester.insertHotspot(p -> p.setSeverity("MINOR")
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setRule(javaRule)
      .setRuleUuid(javaRule.getUuid())
      .setStatus(Issue.STATUS_TO_REVIEW)
      .setLanguage("java")
      .setProject(correctMainBranch)
      .setComponent(correctFile));

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH)
      .setParam("languages", "java");

    TestResponse response = request.execute();
    List<Hotspots.HotspotLite> issues = readAllIssues(response);

    assertThat(issues).hasSize(1)
      .extracting(Hotspots.HotspotLite::getKey)
      .containsExactly(javaIssue.getKey());
  }

  @Test
  public void wsExecution_whenChangedSinceParam_shouldReturnMatchingIssue() throws IOException {
    loginWithBrowsePermission(project);
    RuleDto javaRule = db.rules().insert(r -> r.setLanguage("java"));

    IssueDto issueBefore = issueDbTester.insertHotspot(p -> p.setSeverity("MINOR")
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setIssueUpdateDate(new Date(NOW))
      .setRule(javaRule)
      .setRuleUuid(javaRule.getUuid())
      .setStatus(Issue.STATUS_TO_REVIEW)
      .setLanguage("java")
      .setProject(correctMainBranch)
      .setComponent(correctFile));

    IssueDto issueAfter = issueDbTester.insertHotspot(p -> p.setSeverity("MINOR")
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setIssueUpdateDate(new Date(NOW + 1L))
      .setRule(javaRule)
      .setRuleUuid(javaRule.getUuid())
      .setStatus(Issue.STATUS_TO_REVIEW)
      .setLanguage("java")
      .setProject(correctMainBranch)
      .setComponent(correctFile));

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH)
      .setParam("languages", "java")
      .setParam("changedSince", String.valueOf(issueBefore.getIssueUpdateTime() + 1L));

    TestResponse response = request.execute();
    List<Hotspots.HotspotLite> issues = readAllIssues(response);

    assertThat(issues).extracting(Hotspots.HotspotLite::getKey)
      .doesNotContain(issueBefore.getKey())
      .containsExactly(issueAfter.getKey());
  }

  @Test
  public void wsExecution_whenWrongLanguageSet_shouldReturnZeroIssues() throws IOException {
    loginWithBrowsePermission(project);
    RuleDto javascriptRule = db.rules().insert(r -> r.setLanguage("javascript"));

    issueDbTester.insertHotspot(p -> p.setSeverity("MINOR")
      .setMessage("openIssue")
      .setCreatedAt(NOW)
      .setRule(javascriptRule)
      .setRuleUuid(javascriptRule.getUuid())
      .setStatus(Issue.STATUS_TO_REVIEW)
      .setProject(correctMainBranch)
      .setComponent(correctFile));

    TestRequest request = tester.newRequest()
      .setParam("projectKey", correctMainBranch.getKey())
      .setParam("branchName", DEFAULT_BRANCH)
      .setParam("languages", "java");

    TestResponse response = request.execute();
    List<Hotspots.HotspotLite> issues = readAllIssues(response);

    assertThat(issues).isEmpty();
  }

  private List<String> generateHotspots(ComponentDto project, ComponentDto file, int numberOfIssues) {
    Consumer<IssueDto> consumer = i -> i.setProject(project)
      .setComponentUuid(file.uuid())
      .setStatus(Issue.STATUS_TO_REVIEW);
    return Stream.generate(() -> issueDbTester.insertHotspot(consumer))
      .limit(numberOfIssues)
      .map(IssueDto::getKey)
      .toList();
  }

  private List<Hotspots.HotspotLite> readAllIssues(TestResponse response) throws IOException {
    List<Hotspots.HotspotLite> issues = new ArrayList<>();
    InputStream inputStream = response.getInputStream();
    Hotspots.HotspotPullQueryTimestamp hotspotPullQueryTimestamp = Hotspots.HotspotPullQueryTimestamp.parseDelimitedFrom(inputStream);
    assertThat(hotspotPullQueryTimestamp).isNotNull();
    assertThat(hotspotPullQueryTimestamp.getQueryTimestamp()).isEqualTo(NOW);
    while (inputStream.available() > 0) {
      issues.add(Hotspots.HotspotLite.parseDelimitedFrom(inputStream));
    }

    return issues;
  }

  private void loginWithBrowsePermission(IssueDto issueDto) {
    BranchDto branchDto = db.getDbClient().branchDao().selectByUuid(db.getSession(), issueDto.getProjectUuid()).get();
    ProjectDto projectDto = db.getDbClient().projectDao().selectByUuid(db.getSession(), branchDto.getProjectUuid()).get();
    loginWithBrowsePermission(projectDto);
  }

  private void loginWithBrowsePermission(ProjectDto project) {
    UserDto user = db.users().insertUser("john");
    userSession.logIn(user).addProjectPermission(USER, project);
  }
}
