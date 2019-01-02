/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;

public class IssueRelocationToRoot {
  private final BatchReportReader reader;
  private final List<ScannerReport.Issue> movedIssues = new ArrayList<>();

  public IssueRelocationToRoot(BatchReportReader reader) {
    this.reader = reader;
  }

  public List<ScannerReport.Issue> getMovedIssues() {
    return Collections.unmodifiableList(movedIssues);
  }

  public void relocate(ScannerReport.Component root, ScannerReport.Component component) {
    CloseableIterator<ScannerReport.Issue> issueIt = reader.readComponentIssues(component.getRef());
    while (issueIt.hasNext()) {
      ScannerReport.Issue issue = issueIt.next();
      movedIssues.add(moveIssueToRoot(root, component, issue));
    }
  }

  private static ScannerReport.Issue moveIssueToRoot(ScannerReport.Component root, ScannerReport.Component component, ScannerReport.Issue issue) {
    return ScannerReport.Issue.newBuilder(issue)
      .clearFlow()
      .clearTextRange()
      .addAllFlow(issue.getFlowList().stream()
        .map(flow -> moveFlow(root, component, flow))
        .collect(Collectors.toList()))
      .build();
  }

  private static ScannerReport.IssueLocation moveLocation(ScannerReport.Component root, ScannerReport.Component component, ScannerReport.IssueLocation location) {
    String msg = "[" + component.getProjectRelativePath() + "] " + location.getMsg();
    return ScannerReport.IssueLocation.newBuilder()
      .setComponentRef(root.getRef())
      .setMsg(msg)
      .build();
  }

  private static ScannerReport.Flow moveFlow(ScannerReport.Component root, ScannerReport.Component component, ScannerReport.Flow flow) {
    return ScannerReport.Flow.newBuilder()
      .addAllLocation(flow.getLocationList().stream()
        .map(location -> moveLocation(root, component, location))
        .collect(Collectors.toList()))
      .build();
  }
}
