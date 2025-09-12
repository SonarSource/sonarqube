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

import java.util.Arrays;
import java.util.Comparator;
import javax.annotation.Nullable;

import org.sonar.api.issue.IssueStatus;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;

public class IssueVisitors {

  private final IssueVisitor[] visitors;

  public IssueVisitors(IssueVisitor[] visitors) {
    this.visitors = sortVisitorsByPriority(visitors);
  }

  private static IssueVisitor[] sortVisitorsByPriority(IssueVisitor[] visitors) {
    return Arrays.stream(visitors)
      .sorted(Comparator.comparingInt(IssueVisitors::getPriority).reversed())
      .toArray(IssueVisitor[]::new);
  }

  private static int getPriority(IssueVisitor visitor) {
    return visitor instanceof PriorityIssueVisitor priorityVisitor ? priorityVisitor.getPriority() : PriorityIssueVisitor.NORMAL_PRIORITY;
  }

  public void beforeComponent(Component component) {
    for (IssueVisitor visitor : visitors) {
      visitor.beforeComponent(component);
    }
  }

  public void onIssue(Component component, DefaultIssue issue) {
    for (IssueVisitor visitor : visitors) {
      if (visitor instanceof MeasureComputationIssueVisitor &&
        IssueStatus.IN_SANDBOX.equals(IssueStatus.of(issue.getStatus(), issue.resolution()))) {
        continue;
      }
      
      visitor.onIssue(component, issue);
    }
  }

  public void afterComponent(Component component) {
    for (IssueVisitor visitor : visitors) {
      visitor.afterComponent(component);
    }
  }

  public void beforeCaching(Component component) {
    for (IssueVisitor visitor : visitors) {
      visitor.beforeCaching(component);
    }
  }

  public void onRawIssues(Component component, Input<DefaultIssue> rawIssues, @Nullable Input<DefaultIssue> targetIssues) {
    for (IssueVisitor visitor : visitors) {
      visitor.onRawIssues(component, rawIssues, targetIssues);
    }
  }

}
