/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.ComputeComponentsRefCache;
import org.sonar.server.computation.issue.IssueComputation;

import java.util.List;

public class ParseReportStep implements ComputationStep {

  private final IssueComputation issueComputation;
  private final ComputeComponentsRefCache computeComponentsRefCache;

  public ParseReportStep(IssueComputation issueComputation, ComputeComponentsRefCache computeComponentsRefCache) {
    this.issueComputation = issueComputation;
    this.computeComponentsRefCache = computeComponentsRefCache;
  }

  @Override
  public void execute(ComputationContext context) {
    int rootComponentRef = context.getReportMetadata().getRootComponentRef();
    recursivelyProcessComponent(context, rootComponentRef);
    processDeletedComponents(context);
    issueComputation.afterReportProcessing();
  }

  private void recursivelyProcessComponent(ComputationContext context, int componentRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    List<BatchReport.Issue> issues = reportReader.readComponentIssues(componentRef);
    ComputeComponentsRefCache.ComputeComponent computeProject = computeComponentsRefCache.getByRef(context.getReportMetadata().getRootComponentRef());
    issueComputation.processComponentIssues(context, issues, computeComponentsRefCache.getByRef(componentRef).getUuid(), componentRef, computeProject.getKey(),
      computeProject.getUuid());
    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(context, childRef);
    }
  }

  private void processDeletedComponents(ComputationContext context) {
    int deletedComponentsCount = context.getReportMetadata().getDeletedComponentsCount();
    ComputeComponentsRefCache.ComputeComponent computeProject = computeComponentsRefCache.getByRef(context.getReportMetadata().getRootComponentRef());
    for (int componentRef = 1; componentRef <= deletedComponentsCount; componentRef++) {
      BatchReport.Issues issues = context.getReportReader().readDeletedComponentIssues(componentRef);
      issueComputation.processComponentIssues(context, issues.getIssueList(), issues.getComponentUuid(), null, computeProject.getKey(), computeProject.getUuid());
    }
  }

  @Override
  public String getDescription() {
    return "Digest analysis report";
  }
}
