/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.issue.workflow;

import java.util.List;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.rule.RuleType;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflow;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflow;

/**
 * Common entry point for both issues and security hotspots, because some features are not making the difference.
 * For example some web APIs allow to act on issues or hotspots.
 */
@ServerSide
@ComputeEngineSide
public class IssueWorkflow {

  private final CodeQualityIssueWorkflow codeQualityIssueWorkflow;
  private final SecurityHotspotWorkflow securityHotspotWorkflow;

  public IssueWorkflow(CodeQualityIssueWorkflow codeQualityIssueWorkflow, SecurityHotspotWorkflow securityHotspotWorkflow) {
    this.codeQualityIssueWorkflow = codeQualityIssueWorkflow;
    this.securityHotspotWorkflow = securityHotspotWorkflow;
  }

  public boolean doManualTransition(DefaultIssue issue, WorkflowTransition transition, IssueChangeContext issueChangeContext) {
    return doManualTransition(issue, transition.getKey(), issueChangeContext);
  }

  public boolean doManualTransition(DefaultIssue issue, String transitionKey, IssueChangeContext issueChangeContext) {
    if (isSecurityHotspot(issue)) {
      return securityHotspotWorkflow.doManualTransition(issue, transitionKey, issueChangeContext);
    } else {
      return codeQualityIssueWorkflow.doManualTransition(issue, transitionKey, issueChangeContext);
    }
  }

  public List<String> outTransitionsKeys(DefaultIssue issue) {
    if (isSecurityHotspot(issue)) {
      return securityHotspotWorkflow.outTransitionsKeys(issue);
    } else {
      return codeQualityIssueWorkflow.outTransitionsKeys(issue);
    }
  }

  public void doAutomaticTransition(DefaultIssue issue, IssueChangeContext issueChangeContext) {
    if (isSecurityHotspot(issue)) {
      securityHotspotWorkflow.doAutomaticTransition(issue, issueChangeContext);
    } else {
      codeQualityIssueWorkflow.doAutomaticTransition(issue, issueChangeContext);
    }
  }

  private static boolean isSecurityHotspot(DefaultIssue issue) {
    return issue.type() == RuleType.SECURITY_HOTSPOT;
  }

}
