/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Optional;
import java.util.Set;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

public class NewIssueClassifier {
  private final NewLinesRepository newLinesRepository;
  private final PeriodHolder periodHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public NewIssueClassifier(NewLinesRepository newLinesRepository, PeriodHolder periodHolder, AnalysisMetadataHolder analysisMetadataHolder) {
    this.newLinesRepository = newLinesRepository;
    this.periodHolder = periodHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  public boolean isEnabled() {
    return analysisMetadataHolder.isPullRequest() || periodHolder.hasPeriodDate() ||
      (periodHolder.hasPeriod() && isOnBranchUsingReferenceBranch());
  }

  public boolean isNew(Component component, DefaultIssue issue) {
    if (analysisMetadataHolder.isPullRequest()) {
      return true;
    }

    if (periodHolder.hasPeriod()) {
      if (periodHolder.hasPeriodDate()) {
        return periodHolder.getPeriod().isOnPeriod(issue.creationDate());
      }

      if (isOnBranchUsingReferenceBranch()) {
        return hasAtLeastOneLocationOnChangedLines(component, issue);
      }
    }
    return false;
  }

  public boolean isOnBranchUsingReferenceBranch() {
    if (periodHolder.hasPeriod()) {
      return periodHolder.getPeriod().getMode().equals(NewCodePeriodType.REFERENCE_BRANCH.name());
    }
    return false;
  }

  public boolean hasAtLeastOneLocationOnChangedLines(Component component, DefaultIssue issue) {
    if (component.getType() != Component.Type.FILE) {
      return false;
    }
    final Optional<Set<Integer>> newLinesOpt = newLinesRepository.getNewLines(component);
    if (newLinesOpt.isEmpty()) {
      return false;
    }
    Set<Integer> newLines = newLinesOpt.get();
    return IssueLocations.allLinesFor(issue, component.getUuid()).anyMatch(newLines::contains);
  }

}
