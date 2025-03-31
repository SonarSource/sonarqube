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
package org.sonar.server.issue.workflow.securityhotspot;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.workflow.issue.DefaultIssueWorkflowActions;

@ServerSide
@ComputeEngineSide
public class SecurityHotspotWorkflowActionsFactory {

  private final IssueFieldsSetter updater;

  public SecurityHotspotWorkflowActionsFactory(IssueFieldsSetter updater) {
    this.updater = updater;
  }

  SecurityHotspotWorkflowActions provideContextualActions(DefaultIssue issue, IssueChangeContext issueChangeContext) {
    return new DefaultIssueWorkflowActions(updater, issue, issueChangeContext);
  }

}
