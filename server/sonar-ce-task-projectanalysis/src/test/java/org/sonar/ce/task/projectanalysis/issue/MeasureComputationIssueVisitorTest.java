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

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.api.issue.Issue.STATUS_IN_SANDBOX;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class MeasureComputationIssueVisitorTest {

  private final Component component = builder(Component.Type.FILE, 1).build();

  @Test
  public void onIssue_whenNonSandboxIssue_shouldCallOnNonSandboxedIssue() {
    TestMeasureComputationIssueVisitor visitor = spy(new TestMeasureComputationIssueVisitor());
    DefaultIssue openIssue = new DefaultIssue().setStatus(Issue.STATUS_OPEN);

    visitor.onIssue(component, openIssue);

    verify(visitor).onNonSandboxedIssue(component, openIssue);
  }

  @Test
  public void onIssue_whenSandboxIssue_shouldNotCallOnNonSandboxedIssue() {
    TestMeasureComputationIssueVisitor visitor = spy(new TestMeasureComputationIssueVisitor());
    DefaultIssue sandboxIssue = new DefaultIssue().setStatus(STATUS_IN_SANDBOX);

    visitor.onIssue(component, sandboxIssue);

    verify(visitor, never()).onNonSandboxedIssue(component, sandboxIssue);
  }

  private static class TestMeasureComputationIssueVisitor extends MeasureComputationIssueVisitor {
    @Override
    protected void onNonSandboxedIssue(Component component, DefaultIssue issue) {
    }
  }
}