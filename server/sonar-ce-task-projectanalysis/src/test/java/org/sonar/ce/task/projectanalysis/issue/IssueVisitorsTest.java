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

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sonar.api.issue.Issue.STATUS_IN_SANDBOX;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class IssueVisitorsTest {

  private final Component component = builder(Component.Type.FILE, 1).build();

  @Test
  public void onIssue_whenNonSandboxIssue_shouldCallAllVisitors() {
    IssueVisitor visitor1 = mock(IssueVisitor.class);
    MeasureComputationIssueVisitor visitor2 = mock(MeasureComputationIssueVisitor.class);
    IssueVisitors underTest = new IssueVisitors(new IssueVisitor[]{visitor1, visitor2});

    DefaultIssue openIssue = new DefaultIssue().setStatus(Issue.STATUS_OPEN);

    underTest.onIssue(component, openIssue);

    verify(visitor1).onIssue(component, openIssue);
    verify(visitor2).onIssue(component, openIssue);
  }

  @Test
  public void onIssue_whenSandboxIssue_shouldSkipMeasureComputationVisitors() {
    IssueVisitor visitor1 = mock(IssueVisitor.class);
    MeasureComputationIssueVisitor visitor2 = mock(MeasureComputationIssueVisitor.class);
    IssueVisitors underTest = new IssueVisitors(new IssueVisitor[]{visitor1, visitor2});

    DefaultIssue sandboxIssue = new DefaultIssue().setStatus(STATUS_IN_SANDBOX);

    underTest.onIssue(component, sandboxIssue);

    verify(visitor1).onIssue(component, sandboxIssue);
    verify(visitor2, never()).onIssue(component, sandboxIssue);
  }

  @Test
  public void onIssue_whenSandboxIssueAndRegularVisitors_shouldCallAllVisitors() {
    IssueVisitor visitor1 = mock(IssueVisitor.class);
    IssueVisitor visitor2 = mock(IssueVisitor.class);
    IssueVisitors underTest = new IssueVisitors(new IssueVisitor[]{visitor1, visitor2});

    DefaultIssue sandboxIssue = new DefaultIssue().setStatus(STATUS_IN_SANDBOX);

    underTest.onIssue(component, sandboxIssue);

    verify(visitor1).onIssue(component, sandboxIssue);
    verify(visitor2).onIssue(component, sandboxIssue);
  }

  @Test
  public void onIssue_whenMixedVisitorTypes_shouldSkipOnlyMeasureComputationVisitors() {
    IssueVisitor regularVisitor = mock(IssueVisitor.class);
    MeasureComputationIssueVisitor measureVisitor1 = mock(MeasureComputationIssueVisitor.class);
    MeasureComputationIssueVisitor measureVisitor2 = mock(MeasureComputationIssueVisitor.class);
    IssueVisitors underTest = new IssueVisitors(new IssueVisitor[]{regularVisitor, measureVisitor1, measureVisitor2});

    DefaultIssue sandboxIssue = new DefaultIssue().setStatus(STATUS_IN_SANDBOX);

    underTest.onIssue(component, sandboxIssue);

    verify(regularVisitor).onIssue(component, sandboxIssue);
    verify(measureVisitor1, never()).onIssue(component, sandboxIssue);
    verify(measureVisitor2, never()).onIssue(component, sandboxIssue);
  }

  @Test
  public void constructor_shouldSortVisitorsByPriority() {
    List<String> executionOrder = new ArrayList<>();
    
    TestVisitor normalVisitor = new TestVisitor(PriorityIssueVisitor.NORMAL_PRIORITY, "Normal", executionOrder);
    TestVisitor highPriorityVisitor = new TestVisitor(PriorityIssueVisitor.HIGH_PRIORITY, "High", executionOrder);
    TestVisitor noPriorityVisitor = new TestVisitor("NoPriority", executionOrder);

    IssueVisitors underTest = new IssueVisitors(new IssueVisitor[]{
      normalVisitor, highPriorityVisitor, noPriorityVisitor
    });

    DefaultIssue issue = new DefaultIssue().setStatus(Issue.STATUS_OPEN);

    underTest.onIssue(component, issue);

    assertThat(executionOrder).containsExactly("High", "Normal", "NoPriority");
  }

  private static class TestVisitor extends IssueVisitor implements PriorityIssueVisitor {
    private final int priority;
    private final String name;
    private final List<String> executionOrder;

    TestVisitor(int priority, String name, List<String> executionOrder) {
      this.priority = priority;
      this.name = name;
      this.executionOrder = executionOrder;
    }

    TestVisitor(String name, List<String> executionOrder) {
      this.priority = NORMAL_PRIORITY;
      this.name = name;
      this.executionOrder = executionOrder;
    }

    @Override
    public int getPriority() {
      return priority;
    }

    @Override
    public void onIssue(Component component, DefaultIssue issue) {
      executionOrder.add(name);
    }
  }
}