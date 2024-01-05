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
package org.sonar.ce.task.projectanalysis.step;

import org.sonar.ce.task.projectanalysis.issue.ChangedIssuesRepository;
import org.sonar.ce.task.projectanalysis.issue.ProtoIssueCache;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

public class LoadChangedIssuesStep implements ComputationStep {
  private final PeriodHolder periodHolder;
  private final ProtoIssueCache protoIssueCache;
  private final ChangedIssuesRepository changedIssuesRepository;

  public LoadChangedIssuesStep(PeriodHolder periodHolder, ProtoIssueCache protoIssueCache, ChangedIssuesRepository changedIssuesRepository) {
    this.periodHolder = periodHolder;
    this.protoIssueCache = protoIssueCache;
    this.changedIssuesRepository = changedIssuesRepository;
  }

  @Override
  public void execute(Context context) {
    try (CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse()) {
      while (issues.hasNext()) {
        DefaultIssue issue = issues.next();
        if (shouldUpdateIndexForIssue(issue)) {
          changedIssuesRepository.addIssueKey(issue.key());
        }
      }
    }
  }

  private boolean shouldUpdateIndexForIssue(DefaultIssue issue) {
    return issue.isNew() || issue.isCopied() || issue.isChanged()
      || (isOnBranchUsingReferenceBranch() && (issue.isNoLongerNewCodeReferenceIssue() || issue.isToBeMigratedAsNewCodeReferenceIssue()));
  }

  private boolean isOnBranchUsingReferenceBranch() {
    if (periodHolder.hasPeriod()) {
      return NewCodePeriodType.REFERENCE_BRANCH.name().equals(periodHolder.getPeriod().getMode());
    }
    return false;
  }

  @Override
  public String getDescription() {
    return "Load changed issues for indexation";
  }

}
