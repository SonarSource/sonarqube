/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.issue.introducedissues;

import java.util.ArrayList;
import java.util.List;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.IssueVisitor;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleType;
import org.sonarsource.history.model.IntroducedIssueForHistory;

import static java.util.Collections.unmodifiableList;
import static org.sonar.ce.task.projectanalysis.issue.fixedissues.FixedIssueVisitor.effectiveImpacts;

public class IntroducedIssueVisitor extends IssueVisitor {
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder treeRootHolder;
  // shortcut: keeps one snapshot per new issue in heap, aggregate per dimension if first analyses of huge projects strain the CE heap.
  private final List<IntroducedIssueForHistory> introducedIssues = new ArrayList<>();

  public IntroducedIssueVisitor(AnalysisMetadataHolder analysisMetadataHolder, TreeRootHolder treeRootHolder) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.isNew() && issue.type() != null && issue.type() != RuleType.SECURITY_HOTSPOT && issue.severity() != null
      && analysisMetadataHolder.getBranch().isMain()) {
      introducedIssues.add(new IntroducedIssueForHistory(issue.key(), treeRootHolder.getRoot().getUuid(),
        analysisMetadataHolder.getAnalysisDate(), issue.type().name(), issue.severity(), effectiveImpacts(issue)));
    }
  }

  public List<IntroducedIssueForHistory> getIntroducedIssues() {
    return unmodifiableList(introducedIssues);
  }
}
