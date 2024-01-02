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
package org.sonar.ce.task.projectanalysis.issue;

import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class IssuesOnReferenceBranchVisitorTest {

  private final NewIssueClassifier newIssueClassifier = mock(NewIssueClassifier.class);
  private final Component component = mock(Component.class);
  private final DefaultIssue issue = mock(DefaultIssue.class);

  private final IssueOnReferenceBranchVisitor underTest = new IssueOnReferenceBranchVisitor(newIssueClassifier);

  @Test
  public void issue_is_not_changed_when_newIssueClassifier_is_not_enabled() {
    when(newIssueClassifier.isEnabled()).thenReturn(false);

    underTest.onIssue(component, issue);
    verifyNoInteractions(issue);
  }

  @Test
  public void handles_issue_not_on_branch_using_reference_branch() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isOnBranchUsingReferenceBranch()).thenReturn(false);

    underTest.onIssue(component, issue);
    verifyNoMoreInteractions(issue);
  }

  @Test
  public void handles_overall_code_issue_on_branch_using_reference_branch() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isOnBranchUsingReferenceBranch()).thenReturn(true);
    when(newIssueClassifier.hasAtLeastOneLocationOnChangedLines(component, issue)).thenReturn(true);
    when(issue.isNewCodeReferenceIssue()).thenReturn(false);

    underTest.onIssue(component, issue);
    verify(issue).setIsOnChangedLine(true);
    verify(issue).isNewCodeReferenceIssue();
    verifyNoMoreInteractions(issue);
  }

  @Test
  public void handles_new_code_issue_on_branch_using_reference_branch_which_is_still_new() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isOnBranchUsingReferenceBranch()).thenReturn(true);
    when(newIssueClassifier.hasAtLeastOneLocationOnChangedLines(component, issue)).thenReturn(true);
    when(issue.isNewCodeReferenceIssue()).thenReturn(true);
    when(issue.isOnChangedLine()).thenReturn(true);

    underTest.onIssue(component, issue);
    verify(issue).setIsOnChangedLine(true);
    verify(issue).isNewCodeReferenceIssue();
    verify(issue).isOnChangedLine();
    verifyNoMoreInteractions(issue);
  }

  @Test
  public void handles_new_code_issue_on_branch_using_reference_branch_which_is_no_longer_new() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isOnBranchUsingReferenceBranch()).thenReturn(true);
    when(newIssueClassifier.hasAtLeastOneLocationOnChangedLines(component, issue)).thenReturn(false);
    when(issue.isNewCodeReferenceIssue()).thenReturn(true);
    when(issue.isOnChangedLine()).thenReturn(false);

    underTest.onIssue(component, issue);
    verify(issue).setIsOnChangedLine(false);
    verify(issue).isNewCodeReferenceIssue();
    verify(issue).setIsNoLongerNewCodeReferenceIssue(true);
    verify(issue).setIsNewCodeReferenceIssue(false);
  }
}
