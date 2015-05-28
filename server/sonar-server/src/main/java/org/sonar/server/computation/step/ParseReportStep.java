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

import java.util.List;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.issue.IssueComputation;

public class ParseReportStep implements ComputationStep {

  private final IssueComputation issueComputation;
  private final BatchReportReader reportReader;

  public ParseReportStep(IssueComputation issueComputation, BatchReportReader reportReader) {
    this.issueComputation = issueComputation;
    this.reportReader = reportReader;
  }

  @Override
  public void execute(ComputationContext context) {
    IssueDepthTraversalTypeAwareVisitor visitor = new IssueDepthTraversalTypeAwareVisitor();
    visitor.visit(context.getRoot());
    processDeletedComponents(visitor);
    issueComputation.afterReportProcessing();
  }

  private void processDeletedComponents(IssueDepthTraversalTypeAwareVisitor visitor) {
    int deletedComponentsCount = reportReader.readMetadata().getDeletedComponentsCount();
    for (int componentRef = 1; componentRef <= deletedComponentsCount; componentRef++) {
      BatchReport.Issues issues = reportReader.readDeletedComponentIssues(componentRef);
      issueComputation.processComponentIssues(issues.getIssueList(), issues.getComponentUuid(), null, visitor.projectKey, visitor.projectUuid);
    }
  }

  @Override
  public String getDescription() {
    return "Digest analysis report";
  }

  private class IssueDepthTraversalTypeAwareVisitor extends DepthTraversalTypeAwareVisitor {
    private String projectKey;
    private String projectUuid;

    public IssueDepthTraversalTypeAwareVisitor() {
      super(Component.Type.FILE, Order.PRE_ORDER);
    }

    @Override
    public void visitProject(Component tree) {
      projectKey = tree.getKey();
      projectUuid = tree.getUuid();
      executeForComponent(tree);
    }

    @Override
    public void visitModule(Component module) {
      executeForComponent(module);
    }

    @Override
    public void visitDirectory(Component directory) {
      executeForComponent(directory);
    }

    @Override
    public void visitFile(Component file) {
      executeForComponent(file);
    }

    private void executeForComponent(Component component) {
      int componentRef = component.getRef();
      List<BatchReport.Issue> issues = reportReader.readComponentIssues(componentRef);
      issueComputation.processComponentIssues(issues, component.getUuid(), componentRef, projectKey, projectUuid);
    }
  }
}
