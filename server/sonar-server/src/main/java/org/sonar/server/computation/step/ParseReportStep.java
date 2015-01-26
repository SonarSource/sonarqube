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

import org.sonar.batch.protocol.output.BatchOutputReader;
import org.sonar.batch.protocol.output.BatchOutput;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.issue.IssueComputation;

public class ParseReportStep implements ComputationStep {

  private final IssueComputation issueComputation;

  public ParseReportStep(IssueComputation issueComputation) {
    this.issueComputation = issueComputation;
  }

  @Override
  public void execute(ComputationContext context) {
    int rootComponentRef = context.getReportReader().readMetadata().getRootComponentRef();
    processComponent(context, rootComponentRef);
    issueComputation.afterReportProcessing();
  }

  private void processComponent(ComputationContext context, int componentRef) {
    BatchOutputReader reader = context.getReportReader();
    BatchOutput.ReportComponent component = reader.readComponent(componentRef);
    issueComputation.processComponentIssues(context, component.getUuid(), reader.readComponentIssues(componentRef));

    for (Integer childRef : component.getChildRefsList()) {
      processComponent(context, childRef);
    }
  }

  @Override
  public String getDescription() {
    return "Digest analysis report";
  }
}
