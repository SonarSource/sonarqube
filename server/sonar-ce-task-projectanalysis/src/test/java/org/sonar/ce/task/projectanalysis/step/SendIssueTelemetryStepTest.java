/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.IssueCountsByRuleHolderImpl;
import org.sonar.ce.task.projectanalysis.issue.TargetBranchComponentUuids;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.component.BranchType;
import org.sonar.server.project.Project;
import org.sonar.telemetry.core.event.AnalyticsEventPublisher;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent.RuleCounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE;

class SendIssueTelemetryStepTest {

  private static final RuleKey RULE_1 = RuleKey.of("java", "S1234");
  private static final RuleKey RULE_2 = RuleKey.of("xoo", "x1");

  private final Configuration configuration = mock();
  private final AnalysisMetadataHolder analysisMetadataHolder = mock();
  private final TreeRootHolder treeRootHolder = mock();
  private final IssueCountsByRuleHolderImpl issueCountsByRuleHolder = new IssueCountsByRuleHolderImpl();
  private final TargetBranchComponentUuids targetBranchComponentUuids = mock();
  private final AnalyticsEventPublisher analyticsEventPublisher = mock();
  private final ConfigurationRepository configurationRepository = mock();
  private final Configuration projectConfiguration = mock();
  private final Branch branch = mock();
  private final Component root = mock();
  private final ComputationStep.Context context = mock();

  private final SendIssueTelemetryStep underTest = new SendIssueTelemetryStep(configuration, analysisMetadataHolder,
    treeRootHolder, issueCountsByRuleHolder, targetBranchComponentUuids, analyticsEventPublisher, configurationRepository);

  {
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(true));
    when(analysisMetadataHolder.getProject()).thenReturn(new Project("project-uuid", "key", "name", null, Collections.emptyList()));
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
    when(analysisMetadataHolder.getAnalysisDate()).thenReturn(1_700_000_000_000L);
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    when(branch.isMain()).thenReturn(false);
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    when(branch.getReferenceBranchUuid()).thenReturn("reference-branch-uuid");
    when(treeRootHolder.getRoot()).thenReturn(root);
    when(root.getUuid()).thenReturn("branch-uuid");
    when(configurationRepository.getConfiguration()).thenReturn(projectConfiguration);
    when(projectConfiguration.getStringArray(BRANCHES_TO_KEEP_WHEN_INACTIVE)).thenReturn(new String[0]);
  }

  @Test
  void execute_whenTelemetryDisabled_doesNotPublish() {
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(false));
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);

    underTest.execute(context);

    verifyNoInteractions(analyticsEventPublisher);
  }

  @Test
  void execute_whenNoIssueCounted_doesNotPublish() {
    when(branch.isMain()).thenReturn(true);

    underTest.execute(context);

    verifyNoInteractions(analyticsEventPublisher);
  }

  @Test
  void execute_whenPurgableRegularBranch_doesNotPublish() {
    when(branch.getName()).thenReturn("feature-branch");
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);

    underTest.execute(context);

    verifyNoInteractions(analyticsEventPublisher);
  }

  @Test
  void execute_whenCountsPresent_publishesOneEventWithAllRules() {
    when(branch.getName()).thenReturn("main-line");
    when(projectConfiguration.getStringArray(BRANCHES_TO_KEEP_WHEN_INACTIVE)).thenReturn(new String[] {"main-line"});
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.ACCEPTED);

    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.CONFIRMED);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.CONFIRMED);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.FALSE_POSITIVE);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.IN_SANDBOX);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.IN_SANDBOX);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.IN_SANDBOX);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.IN_SANDBOX);

    underTest.execute(context);

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueBacklogTelemetryEvent.TYPE), eventCaptor.capture());
    IssueBacklogTelemetryEvent event = eventCaptor.getValue();

    assertThat(event.projectUuid()).isEqualTo("project-uuid");
    assertThat(event.branchUuid()).isEqualTo("branch-uuid");
    assertThat(event.branchType()).isEqualTo("BRANCH");
    assertThat(event.mergeBranchUuid()).isEqualTo("reference-branch-uuid");
    assertThat(event.analysisDate()).isEqualTo(1_700_000_000_000L);
    assertThat(event.rules()).containsExactlyInAnyOrder(
      new RuleCounts("java:S1234", 3, 0, 0, 1, 0),
      new RuleCounts("xoo:x1", 0, 2, 1, 0, 4));
    verifyNoInteractions(targetBranchComponentUuids);
  }

  @Test
  void execute_whenPullRequestAnalysis_reflectsBranchTypeAndTargetBranchUuidInEvent() {
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(analysisMetadataHolder.isPullRequest()).thenReturn(true);
    when(targetBranchComponentUuids.getTargetBranchUuid()).thenReturn("target-branch-uuid");
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);

    underTest.execute(context);

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueBacklogTelemetryEvent.TYPE), eventCaptor.capture());
    IssueBacklogTelemetryEvent event = eventCaptor.getValue();
    assertThat(event.branchType()).isEqualTo("PULL_REQUEST");
    assertThat(event.mergeBranchUuid()).isEqualTo("target-branch-uuid");
    verify(branch, never()).getReferenceBranchUuid();
  }

  @Test
  void execute_whenMainBranch_mergeBranchUuidIsNullAndReferenceBranchUuidIsNeverCalled() {
    when(branch.isMain()).thenReturn(true);
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);

    underTest.execute(context);

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueBacklogTelemetryEvent.TYPE), eventCaptor.capture());
    assertThat(eventCaptor.getValue().mergeBranchUuid()).isNull();
    verify(branch, never()).getReferenceBranchUuid();
    verifyNoInteractions(targetBranchComponentUuids);
  }
}
