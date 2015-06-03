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
package org.sonar.server.computation.issue;

import java.util.Date;
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.server.computation.batch.BatchReportReader;

public class IssueLifecycle {

  private final IssueWorkflow workflow;
  private final IssueChangeContext changeContext;
  private final IssueUpdater updater;

  public IssueLifecycle(BatchReportReader reportReader, IssueWorkflow workflow, IssueUpdater updater) {
    this.workflow = workflow;
    this.updater = updater;
    this.changeContext = IssueChangeContext.createScan(new Date(reportReader.readMetadata().getAnalysisDate()));
  }

  public void initNewOpenIssue(DefaultIssue issue) {
    issue.setKey(Uuids.create());
    issue.setCreationDate(changeContext.date());
    issue.setUpdateDate(changeContext.date());
    issue.setStatus(Issue.STATUS_OPEN);
  }

  public void mergeExistingOpenIssue(DefaultIssue raw, DefaultIssue base) {
    raw.setNew(false);
    raw.setKey(base.key());
    raw.setCreationDate(base.creationDate());
    raw.setUpdateDate(base.updateDate());
    raw.setCloseDate(base.closeDate());
    raw.setActionPlanKey(base.actionPlanKey());
    raw.setResolution(base.resolution());
    raw.setStatus(base.status());
    raw.setAssignee(base.assignee());
    raw.setAuthorLogin(base.authorLogin());
    raw.setTags(base.tags());
    if (base.manualSeverity()) {
      raw.setManualSeverity(true);
      raw.setSeverity(base.severity());
    } else {
      updater.setPastSeverity(raw, base.severity(), changeContext);
    }

    // TODO attributes + changelog

    // fields coming from raw
    updater.setPastLine(raw, base.getLine());
    updater.setPastMessage(raw, base.getMessage(), changeContext);
    updater.setPastEffortToFix(raw, base.effortToFix(), changeContext);
    updater.setPastTechnicalDebt(raw, base.debt(), changeContext);
  }

  public void doAutomaticTransition(DefaultIssue issue) {
    workflow.doAutomaticTransition(issue, changeContext);
  }
}
