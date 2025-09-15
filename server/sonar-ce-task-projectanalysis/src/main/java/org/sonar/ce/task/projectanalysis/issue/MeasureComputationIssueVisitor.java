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

import org.sonar.api.issue.IssueStatus;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;

/**
 * Base class for issue visitors that compute measures and should exclude issues in sandbox status.
 */
public abstract class MeasureComputationIssueVisitor extends IssueVisitor {

  @Override
  public final void onIssue(Component component, DefaultIssue issue) {
    if (IssueStatus.IN_SANDBOX.equals(IssueStatus.of(issue.getStatus(), issue.resolution()))) {
      return;
    }
    onNonSandboxedIssue(component, issue);
  }

  protected abstract void onNonSandboxedIssue(Component component, DefaultIssue issue);
}
