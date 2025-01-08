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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.component.BranchType;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class NewIssueClassifierTest {
  @Rule
  public PeriodHolderRule periodHolder = new PeriodHolderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  private final NewLinesRepository newLinesRepository = mock(NewLinesRepository.class);
  private final NewIssueClassifier newIssueClassifier = new NewIssueClassifier(newLinesRepository, periodHolder, analysisMetadataHolder);

  @Test
  public void isEnabled_returns_false() {
    periodHolder.setPeriod(null);
    assertThat(newIssueClassifier.isEnabled()).isFalse();
  }

  @Test
  public void isEnabled_returns_true_when_pull_request() {
    periodHolder.setPeriod(null);
    analysisMetadataHolder.setBranch(newPr());
    assertThat(newIssueClassifier.isEnabled()).isTrue();
  }

  @Test
  public void isEnabled_returns_true_when_periodDate_present() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "10", 1000L));
    assertThat(newIssueClassifier.isEnabled()).isTrue();
  }

  @Test
  public void isEnabled_returns_true_when_reference_period_present() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    assertThat(newIssueClassifier.isEnabled()).isTrue();
  }

  @Test
  public void isNew_returns_true_for_any_issue_if_pull_request() {
    periodHolder.setPeriod(null);
    analysisMetadataHolder.setBranch(newPr());
    assertThat(newIssueClassifier.isNew(mock(Component.class), mock(DefaultIssue.class))).isTrue();
  }

  @Test
  public void isNew_returns_true_if_issue_is_on_period() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "10", 1000L));
    DefaultIssue issue = mock(DefaultIssue.class);
    when(issue.creationDate()).thenReturn(new Date(2000L));
    assertThat(newIssueClassifier.isNew(mock(Component.class), issue)).isTrue();
    verify(issue).creationDate();
    verifyNoMoreInteractions(issue);
  }

  @Test
  public void isNew_returns_true_for_issue_located_on_changed_lines() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    Component file = mock(Component.class);
    DefaultIssue issue = mock(DefaultIssue.class);
    when(file.getType()).thenReturn(Component.Type.FILE);
    when(file.getUuid()).thenReturn("fileUuid");
    when(newLinesRepository.getNewLines(file)).thenReturn(Optional.of(Set.of(2, 3)));
    when(issue.getLocations()).thenReturn(DbIssues.Locations.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder()
        .setStartLine(2)
        .setStartOffset(1)
        .setEndLine(2)
        .setEndOffset(2)
        .build())
      .build());
    assertThat(newIssueClassifier.isNew(file, issue)).isTrue();
    assertThat(newIssueClassifier.isOnBranchUsingReferenceBranch()).isTrue();
    assertThat(newIssueClassifier.hasAtLeastOneLocationOnChangedLines(file, issue)).isTrue();
  }

  @Test
  public void isNew_returns_false_for_issue_not_located_on_changed_lines() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    Component file = mock(Component.class);
    DefaultIssue issue = mock(DefaultIssue.class);
    when(file.getType()).thenReturn(Component.Type.FILE);
    when(file.getUuid()).thenReturn("fileUuid");
    when(newLinesRepository.getNewLines(file)).thenReturn(Optional.of(Set.of(2, 3)));
    when(issue.getLocations()).thenReturn(DbIssues.Locations.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder()
        .setStartLine(10)
        .setStartOffset(1)
        .setEndLine(10)
        .setEndOffset(2)
        .build())
      .build());
    assertThat(newIssueClassifier.isNew(file, issue)).isFalse();
    assertThat(newIssueClassifier.isOnBranchUsingReferenceBranch()).isTrue();
    assertThat(newIssueClassifier.hasAtLeastOneLocationOnChangedLines(file, issue)).isFalse();
  }

  @Test
  public void isNew_returns_false_for_issue_which_was_new_but_it_is_not_located_on_changed_lines_anymore() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    Component file = mock(Component.class);
    DefaultIssue issue = mock(DefaultIssue.class);
    when(file.getType()).thenReturn(Component.Type.FILE);
    when(file.getUuid()).thenReturn("fileUuid");
    when(newLinesRepository.getNewLines(file)).thenReturn(Optional.of(Set.of(2, 3)));
    when(issue.getLocations()).thenReturn(DbIssues.Locations.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder()
        .setStartLine(10)
        .setStartOffset(1)
        .setEndLine(10)
        .setEndOffset(2)
        .build())
      .build());
    when(issue.isNewCodeReferenceIssue()).thenReturn(true);
    assertThat(newIssueClassifier.isNew(file, issue)).isFalse();
    assertThat(newIssueClassifier.isOnBranchUsingReferenceBranch()).isTrue();
    assertThat(newIssueClassifier.hasAtLeastOneLocationOnChangedLines(file, issue)).isFalse();
  }

  @Test
  public void isNew_returns_true_for_issue_which_was_new_and_is_still_located_on_changed_lines() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "master", null));
    Component file = mock(Component.class);
    DefaultIssue issue = mock(DefaultIssue.class);
    when(file.getType()).thenReturn(Component.Type.FILE);
    when(file.getUuid()).thenReturn("fileUuid");
    when(newLinesRepository.getNewLines(file)).thenReturn(Optional.of(Set.of(2, 3)));
    when(issue.getLocations()).thenReturn(DbIssues.Locations.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder()
        .setStartLine(2)
        .setStartOffset(1)
        .setEndLine(2)
        .setEndOffset(2)
        .build())
      .build());
    when(issue.isNewCodeReferenceIssue()).thenReturn(true);
    assertThat(newIssueClassifier.isNew(file, issue)).isTrue();
    assertThat(newIssueClassifier.isOnBranchUsingReferenceBranch()).isTrue();
    assertThat(newIssueClassifier.hasAtLeastOneLocationOnChangedLines(file, issue)).isTrue();
  }

  @Test
  public void isNew_returns_false_if_issue_is_not_on_period() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "10", 1000L));
    DefaultIssue issue = mock(DefaultIssue.class);
    when(issue.creationDate()).thenReturn(new Date(500L));
    assertThat(newIssueClassifier.isNew(mock(Component.class), issue)).isFalse();
    verify(issue).creationDate();
    verifyNoMoreInteractions(issue);
  }

  @Test
  public void isNew_returns_false_if_period_without_date() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "10", null));
    assertThat(newIssueClassifier.isNew(mock(Component.class), mock(DefaultIssue.class))).isFalse();
  }

  private Branch newPr() {
    Branch nonMainBranch = mock(Branch.class);
    when(nonMainBranch.isMain()).thenReturn(false);
    when(nonMainBranch.getType()).thenReturn(BranchType.PULL_REQUEST);
    return nonMainBranch;
  }
}
