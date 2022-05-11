/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.util.issue.IssueChangedEvent;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonarqube.ws.Common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

  IssueChangeEventsDistributor eventsDistributor = mock(IssueChangeEventsDistributor.class);

  public final IssueChangeEventServiceImpl underTest = new IssueChangeEventServiceImpl(eventsDistributor);

  @Test
  public void distributeIssueChangeEvent_singleIssueChange_severityChange() {
    ComponentDto componentDto = db.components().insertPublicProject();
    ProjectDto project = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto.uuid()).get();
    BranchDto branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.getUuid()).get();
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, componentDto, i-> i.setSeverity(MAJOR.name()));

    assertIssueDistribution(project, branch, issue, BLOCKER.name(), null, null, null, 1);
  }

  @Test
  public void distributeIssueChangeEvent_singleIssueChange_typeChange() {
    ComponentDto componentDto = db.components().insertPublicProject();
    ProjectDto project = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto.uuid()).get();
    BranchDto branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.getUuid()).get();
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, componentDto, i-> i.setSeverity(MAJOR.name()));

    assertIssueDistribution(project, branch, issue, null, Common.RuleType.BUG.name(), null, null, 1);
  }

  @Test
  public void distributeIssueChangeEvent_singleIssueChange_transitionChanges() {
    ComponentDto componentDto = db.components().insertPublicProject();
    ProjectDto project = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto.uuid()).get();
    BranchDto branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.getUuid()).get();
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, componentDto, i-> i.setSeverity(MAJOR.name()));

    assertIssueDistribution(project, branch, issue, null, null, WONT_FIX, true, 1);
    assertIssueDistribution(project, branch, issue, null, null, REOPEN, false, 2);
    assertIssueDistribution(project, branch, issue, null, null, FALSE_POSITIVE, true, 3);
    assertIssueDistribution(project, branch, issue, null, null, REOPEN, false, 4);
    assertIssueDistribution(project, branch, issue, null, null, RESOLVE, false, 5);
    assertIssueDistribution(project, branch, issue, null, null, REOPEN, false, 6);
    assertNoIssueDistribution(project, branch, issue, null, null, CONFIRM);
    assertNoIssueDistribution(project, branch, issue, null, null, UNCONFIRM);
  }

  @Test
  public void distributeIssueChangeEvent_singleIssueChange_severalChanges() {
    ComponentDto componentDto = db.components().insertPublicProject();
    ProjectDto project = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto.uuid()).get();
    BranchDto branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.getUuid()).get();
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, componentDto, i-> i.setSeverity(MAJOR.name()));

    assertIssueDistribution(project, branch, issue, BLOCKER.name(), Common.RuleType.BUG.name(), WONT_FIX, true, 1);
  }

  @Test
  public void distributeIssueChangeEvent_bulkIssueChange() {
    RuleDto rule = db.rules().insert();

    ComponentDto componentDto1 = db.components().insertPublicProject();
    ProjectDto project1 = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto1.uuid()).get();
    BranchDto branch1 = db.getDbClient().branchDao().selectByUuid(db.getSession(), project1.getUuid()).get();
    IssueDto issue1 = db.issues().insert(rule, project1, componentDto1, i-> i.setSeverity(MAJOR.name()).setType(RuleType.BUG));

    ComponentDto componentDto2 = db.components().insertPublicProject();
    ProjectDto project2 = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto2.uuid()).get();
    BranchDto branch2 = db.getDbClient().branchDao().selectByUuid(db.getSession(), project2.getUuid()).get();
    IssueDto issue2 = db.issues().insert(rule, project2, componentDto2, i-> i.setSeverity(MAJOR.name()).setType(RuleType.BUG));

    ComponentDto componentDto3 = db.components().insertPublicProject();
    ProjectDto project3 = db.getDbClient().projectDao().selectByUuid(db.getSession(), componentDto3.uuid()).get();
    BranchDto branch3 = db.getDbClient().branchDao().selectByUuid(db.getSession(), project3.getUuid()).get();
    IssueDto issue3 = db.issues().insert(rule, project3, componentDto3, i-> i.setSeverity(MAJOR.name()).setType(RuleType.BUG));

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
    projectsByUuid.put(componentDto1.projectUuid(), componentDto1);
    projectsByUuid.put(componentDto2.projectUuid(), componentDto2);
    projectsByUuid.put(componentDto3.projectUuid(), componentDto3);
    Map<String, BranchDto> branchesByProjectUuid = new HashMap<>();
    branchesByProjectUuid.put(componentDto1.projectUuid(), branch1);
    branchesByProjectUuid.put(componentDto2.projectUuid(), branch2);
    branchesByProjectUuid.put(componentDto3.projectUuid(), branch3);

    underTest.distributeIssueChangeEvent(issues, projectsByUuid, branchesByProjectUuid);

    ArgumentCaptor<IssueChangedEvent> eventCaptor = ArgumentCaptor.forClass(IssueChangedEvent.class);
    verify(eventsDistributor, times(2)).pushEvent(eventCaptor.capture());

    List<IssueChangedEvent> issueChangedEvents = eventCaptor.getAllValues();
    assertThat(issueChangedEvents).hasSize(2);

    assertThat(issueChangedEvents)
      .extracting(IssueChangedEvent::getEvent, IssueChangedEvent::getProjectKey,
        IssueChangedEvent::getUserSeverity, IssueChangedEvent::getUserType, IssueChangedEvent::getResolved)
      .containsExactlyInAnyOrder(
        tuple("IssueChangedEvent", project1.getKey(), CRITICAL.name(), CODE_SMELL.name(), false),
        tuple("IssueChangedEvent", project2.getKey(), CRITICAL.name(), CODE_SMELL.name(), true));
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
    IssueDto issue1 = db.issues().insert(rule, pullRequest, file, i-> i.setSeverity(MAJOR.name()).setType(RuleType.BUG));

    DefaultIssue defaultIssue1 = issue1.toDefaultIssue().setCurrentChangeWithoutAddChange(new FieldDiffs()
      .setDiff("resolution", null, null)
      .setDiff("severity", MAJOR.name(), CRITICAL.name())
      .setDiff("type", RuleType.BUG.name(), CODE_SMELL.name()));

    Set<DefaultIssue> issues = Set.of(defaultIssue1);
    Map<String, ComponentDto> projectsByUuid = new HashMap<>();
    projectsByUuid.put(project.projectUuid(), project);
    Map<String, BranchDto> branchesByProjectUuid = new HashMap<>();
    branchesByProjectUuid.put(project.projectUuid(), branch1);

    underTest.distributeIssueChangeEvent(issues, projectsByUuid, branchesByProjectUuid);

    verifyNoInteractions(eventsDistributor);
  }

  private void assertIssueDistribution(ProjectDto project, BranchDto branch, IssueDto issue, @Nullable String severity,
    @Nullable String type, @Nullable String transition, Boolean resolved, int times) {
    underTest.distributeIssueChangeEvent(issue.toDefaultIssue(), severity, type, transition, branch, project.getKey());

    ArgumentCaptor<IssueChangedEvent> eventCaptor = ArgumentCaptor.forClass(IssueChangedEvent.class);
    verify(eventsDistributor, times(times)).pushEvent(eventCaptor.capture());

    IssueChangedEvent issueChangedEvent = eventCaptor.getValue();
    assertThat(issueChangedEvent).isNotNull();
    assertThat(issueChangedEvent).extracting(IssueChangedEvent::getEvent, IssueChangedEvent::getProjectKey,
        IssueChangedEvent::getUserSeverity, IssueChangedEvent::getUserType, IssueChangedEvent::getResolved)
      .containsExactly("IssueChangedEvent", project.getKey(), severity, type, resolved);
  }

  private void assertNoIssueDistribution(ProjectDto project, BranchDto branch, IssueDto issue, @Nullable String severity,
    @Nullable String type, @Nullable String transition) {
    underTest.distributeIssueChangeEvent(issue.toDefaultIssue(), severity, type, transition, branch, project.getKey());

    ArgumentCaptor<IssueChangedEvent> eventCaptor = ArgumentCaptor.forClass(IssueChangedEvent.class);
    verifyNoMoreInteractions(eventsDistributor);
  }

}
