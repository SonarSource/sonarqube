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
package org.sonar.server.pushapi.issues;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.pushevent.PushEventDto;
import org.sonar.db.rule.RuleDto;
import org.sonarqube.ws.Common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.issue.DefaultTransitions.CONFIRM;
import static org.sonar.api.issue.DefaultTransitions.FALSE_POSITIVE;
import static org.sonar.api.issue.DefaultTransitions.REOPEN;
import static org.sonar.api.issue.DefaultTransitions.RESOLVE;
import static org.sonar.api.issue.DefaultTransitions.UNCONFIRM;
import static org.sonar.api.issue.DefaultTransitions.WONT_FIX;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonarqube.ws.Common.Severity.BLOCKER;
import static org.sonarqube.ws.Common.Severity.CRITICAL;
import static org.sonarqube.ws.Common.Severity.MAJOR;

public class IssueChangeEventServiceImplTest {

  @Rule
  public DbTester db = DbTester.create();

  public final IssueChangeEventServiceImpl underTest = new IssueChangeEventServiceImpl(db.getDbClient());

  @Test
  public void distributeIssueChangeEvent_singleIssueChange_severityChange() {
    ComponentDto componentDto = db.components().insertPublicProject();
    ProjectDto project = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto.uuid()).get();
    BranchDto branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.getUuid()).get();
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, componentDto, i -> i.setSeverity(MAJOR.name()));

    assertPushEventIsPersisted(project, branch, issue, BLOCKER.name(), null, null, null, 1);
  }

  @Test
  public void distributeIssueChangeEvent_singleIssueChange_typeChange() {
    ComponentDto componentDto = db.components().insertPublicProject();
    ProjectDto project = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto.uuid()).get();
    BranchDto branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.getUuid()).get();
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, componentDto, i -> i.setSeverity(MAJOR.name()));

    assertPushEventIsPersisted(project, branch, issue, null, Common.RuleType.BUG.name(), null, null, 1);
  }

  @Test
  public void distributeIssueChangeEvent_singleIssueChange_transitionChanges() {
    ComponentDto componentDto = db.components().insertPublicProject();
    ProjectDto project = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto.uuid()).get();
    BranchDto branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.getUuid()).get();
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, componentDto, i -> i.setSeverity(MAJOR.name()));

    assertPushEventIsPersisted(project, branch, issue, null, null, WONT_FIX, true, 1);
    assertPushEventIsPersisted(project, branch, issue, null, null, REOPEN, false, 2);
    assertPushEventIsPersisted(project, branch, issue, null, null, FALSE_POSITIVE, true, 3);
    assertPushEventIsPersisted(project, branch, issue, null, null, REOPEN, false, 4);
    assertPushEventIsPersisted(project, branch, issue, null, null, RESOLVE, false, 5);
    assertPushEventIsPersisted(project, branch, issue, null, null, REOPEN, false, 6);
    assertNoIssueDistribution(project, branch, issue, null, null, CONFIRM, 7);
    assertNoIssueDistribution(project, branch, issue, null, null, UNCONFIRM, 8);
  }

  @Test
  public void distributeIssueChangeEvent_singleIssueChange_severalChanges() {
    ComponentDto componentDto = db.components().insertPublicProject();
    ProjectDto project = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto.uuid()).get();
    BranchDto branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.getUuid()).get();
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, componentDto, i -> i.setSeverity(MAJOR.name()));

    assertPushEventIsPersisted(project, branch, issue, BLOCKER.name(), Common.RuleType.BUG.name(), WONT_FIX, true, 1);
  }

  @Test
  public void distributeIssueChangeEvent_bulkIssueChange() {
    RuleDto rule = db.rules().insert();

    ComponentDto componentDto1 = db.components().insertPublicProject();
    ProjectDto project1 = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto1.uuid()).get();
    BranchDto branch1 = db.getDbClient().branchDao().selectByUuid(db.getSession(), project1.getUuid()).get();
    IssueDto issue1 = db.issues().insert(rule, project1, componentDto1, i -> i.setSeverity(MAJOR.name()).setType(RuleType.BUG));

    ComponentDto componentDto2 = db.components().insertPublicProject();
    ProjectDto project2 = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto2.uuid()).get();
    BranchDto branch2 = db.getDbClient().branchDao().selectByUuid(db.getSession(), project2.getUuid()).get();
    IssueDto issue2 = db.issues().insert(rule, project2, componentDto2, i -> i.setSeverity(MAJOR.name()).setType(RuleType.BUG));

    ComponentDto componentDto3 = db.components().insertPublicProject();
    ProjectDto project3 = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto3.uuid()).get();
    BranchDto branch3 = db.getDbClient().branchDao().selectByUuid(db.getSession(), project3.getUuid()).get();
    IssueDto issue3 = db.issues().insert(rule, project3, componentDto3, i -> i.setSeverity(MAJOR.name()).setType(RuleType.BUG));

    DefaultIssue defaultIssue1 = issue1.toDefaultIssue().setCurrentChangeWithoutAddChange(new FieldDiffs()
      .setDiff("resolution", null, null)
      .setDiff("severity", MAJOR.name(), CRITICAL.name())
      .setDiff("type", RuleType.BUG.name(), CODE_SMELL.name()));
    DefaultIssue defaultIssue2 = issue2.toDefaultIssue().setCurrentChangeWithoutAddChange(new FieldDiffs()
      .setDiff("resolution", "OPEN", "FALSE-POSITIVE")
      .setDiff("severity", MAJOR.name(), CRITICAL.name())
      .setDiff("type", RuleType.BUG.name(), CODE_SMELL.name()));

    Set<DefaultIssue> issues = Set.of(defaultIssue1, defaultIssue2, issue3.toDefaultIssue());
    Map<String, ComponentDto> projectsByUuid = new HashMap<>();
    projectsByUuid.put(componentDto1.branchUuid(), componentDto1);
    projectsByUuid.put(componentDto2.branchUuid(), componentDto2);
    projectsByUuid.put(componentDto3.branchUuid(), componentDto3);
    Map<String, BranchDto> branchesByProjectUuid = new HashMap<>();
    branchesByProjectUuid.put(componentDto1.branchUuid(), branch1);
    branchesByProjectUuid.put(componentDto2.branchUuid(), branch2);
    branchesByProjectUuid.put(componentDto3.branchUuid(), branch3);

    underTest.distributeIssueChangeEvent(issues, projectsByUuid, branchesByProjectUuid);

    Deque<PushEventDto> issueChangedEvents = db.getDbClient().pushEventDao()
      .selectChunkByProjectUuids(db.getSession(), Set.of(project1.getUuid(), project2.getUuid()),
        1l, null, 3);

    assertThat(issueChangedEvents).hasSize(2);

    assertThat(issueChangedEvents)
      .extracting(PushEventDto::getName, PushEventDto::getProjectUuid)
      .containsExactlyInAnyOrder(
        tuple("IssueChanged", project1.getUuid()),
        tuple("IssueChanged", project2.getUuid()));

    Optional<PushEventDto> project1Event = issueChangedEvents.stream().filter(e -> e.getProjectUuid().equals(project1.getUuid())).findFirst();
    Optional<PushEventDto> project2Event = issueChangedEvents.stream().filter(e -> e.getProjectUuid().equals(project2.getUuid())).findFirst();

    assertThat(project1Event).isPresent();
    assertThat(project2Event).isPresent();

    String firstPayload = new String(project1Event.get().getPayload(), StandardCharsets.UTF_8);
    assertThat(firstPayload)
      .contains("\"userSeverity\":\"" + CRITICAL.name() + "\"",
        "\"userType\":\"" + CODE_SMELL.name() + "\"",
        "\"resolved\":" + false);

    String secondPayload = new String(project2Event.get().getPayload(), StandardCharsets.UTF_8);
    assertThat(secondPayload)
      .contains("\"userSeverity\":\"" + CRITICAL.name() + "\"",
        "\"userType\":\"" + CODE_SMELL.name() + "\"",
        "\"resolved\":" + true);
  }

  @Test
  public void doNotDistributeIssueChangeEvent_forPullRequestIssues() {
    RuleDto rule = db.rules().insert();

    ComponentDto project = db.components().insertPublicProject();
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setKey("myBranch1")
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(project.uuid()));
    BranchDto branch1 = db.getDbClient().branchDao().selectByUuid(db.getSession(), pullRequest.uuid()).get();
    ComponentDto file = db.components().insertComponent(newFileDto(pullRequest));
    IssueDto issue1 = db.issues().insert(rule, pullRequest, file, i -> i.setSeverity(MAJOR.name()).setType(RuleType.BUG));

    DefaultIssue defaultIssue1 = issue1.toDefaultIssue().setCurrentChangeWithoutAddChange(new FieldDiffs()
      .setDiff("resolution", null, null)
      .setDiff("severity", MAJOR.name(), CRITICAL.name())
      .setDiff("type", RuleType.BUG.name(), CODE_SMELL.name()));

    Set<DefaultIssue> issues = Set.of(defaultIssue1);
    Map<String, ComponentDto> projectsByUuid = new HashMap<>();
    projectsByUuid.put(project.branchUuid(), project);
    Map<String, BranchDto> branchesByProjectUuid = new HashMap<>();
    branchesByProjectUuid.put(project.branchUuid(), branch1);

    underTest.distributeIssueChangeEvent(issues, projectsByUuid, branchesByProjectUuid);

    Deque<PushEventDto> events = db.getDbClient().pushEventDao()
      .selectChunkByProjectUuids(db.getSession(), Set.of(project.uuid()), 1l, null, 20);
    assertThat(events).isEmpty();
  }

  private void assertNoIssueDistribution(ProjectDto project, BranchDto branch, IssueDto issue, @Nullable String severity,
    @Nullable String type, @Nullable String transition, int page) {
    underTest.distributeIssueChangeEvent(issue.toDefaultIssue(), severity, type, transition, branch, project.getKey());

    Deque<PushEventDto> events = db.getDbClient().pushEventDao()
      .selectChunkByProjectUuids(db.getSession(), Set.of(project.getUuid()), 1l, null, page);
    assertThat(events).hasSizeLessThan(page);
  }

  private void assertPushEventIsPersisted(ProjectDto project, BranchDto branch, IssueDto issue, @Nullable String severity,
    @Nullable String type, @Nullable String transition, Boolean resolved, int page) {
    underTest.distributeIssueChangeEvent(issue.toDefaultIssue(), severity, type, transition, branch, project.getKey());

    Deque<PushEventDto> events = db.getDbClient().pushEventDao()
      .selectChunkByProjectUuids(db.getSession(), Set.of(project.getUuid()), 1l, null, page);
    assertThat(events).isNotEmpty();
    assertThat(events).extracting(PushEventDto::getName, PushEventDto::getProjectUuid)
      .contains(tuple("IssueChanged", project.getUuid()));

    String payload = new String(events.getLast().getPayload(), StandardCharsets.UTF_8);
    if (severity != null) {
      assertThat(payload).contains("\"userSeverity\":\"" + severity + "\"");
    }

    if (type != null) {
      assertThat(payload).contains("\"userType\":\"" + type + "\"");
    }

    if (resolved != null) {
      assertThat(payload).contains("\"resolved\":" + resolved);
    }

  }

}
