/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import com.google.common.annotations.VisibleForTesting;
import java.util.Date;
import org.sonar.api.issue.Issue;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.Uuids;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.workflow.IssueWorkflow;

/**
 * Sets the appropriate fields when an issue is :
 * <ul>
 *   <li>newly created</li>
 *   <li>reused in incremental analysis</li>
 *   <li>merged the related base issue</li>
 *   <li>relocated (only manual issues)</li>
 * </ul>
 */
public class IssueLifecycle {

  private final IssueWorkflow workflow;
  private final IssueChangeContext changeContext;
  private final IssueFieldsSetter updater;
  private final DebtCalculator debtCalculator;

  public IssueLifecycle(AnalysisMetadataHolder analysisMetadataHolder, IssueWorkflow workflow, IssueFieldsSetter updater, DebtCalculator debtCalculator) {
    this(IssueChangeContext.createScan(new Date(analysisMetadataHolder.getAnalysisDate())), workflow, updater, debtCalculator);
  }

  @VisibleForTesting
  IssueLifecycle(IssueChangeContext changeContext, IssueWorkflow workflow, IssueFieldsSetter updater, DebtCalculator debtCalculator) {
    this.workflow = workflow;
    this.updater = updater;
    this.debtCalculator = debtCalculator;
    this.changeContext = changeContext;
  }

  public void initNewOpenIssue(DefaultIssue issue) {
    issue.setKey(Uuids.create());
    issue.setCreationDate(changeContext.date());
    issue.setUpdateDate(changeContext.date());
    issue.setStatus(Issue.STATUS_OPEN);
    issue.setEffort(debtCalculator.calculate(issue));
  }

  public void copyExistingOpenIssue(DefaultIssue raw, DefaultIssue base) {
    raw.setKey(Uuids.create());
    raw.setNew(false);
    raw.setCopied(true);
    copyFields(raw, base);

    if (base.manualSeverity()) {
      raw.setManualSeverity(true);
      raw.setSeverity(base.severity());
    }
  }

  public void mergeExistingOpenIssue(DefaultIssue raw, DefaultIssue base) {
    raw.setKey(base.key());
    raw.setNew(false);
    copyFields(raw, base);

    if (base.manualSeverity()) {
      raw.setManualSeverity(true);
      raw.setSeverity(base.severity());
    } else {
      updater.setPastSeverity(raw, base.severity(), changeContext);
    }
    // set component/module related fields from base in case current component has been moved
    // (in which case base issue belongs to original file and raw issue to component)
    raw.setComponentUuid(base.componentUuid());
    raw.setComponentKey(base.componentKey());
    raw.setModuleUuid(base.moduleUuid());
    raw.setModuleUuidPath(base.moduleUuidPath());

    // fields coming from raw
    updater.setPastLine(raw, base.getLine());
    updater.setPastLocations(raw, base.getLocations());
    updater.setPastMessage(raw, base.getMessage(), changeContext);
    updater.setPastGap(raw, base.gap(), changeContext);
    updater.setPastEffort(raw, base.effort(), changeContext);
  }

  public void doAutomaticTransition(DefaultIssue issue) {
    workflow.doAutomaticTransition(issue, changeContext);
  }

  private void copyFields(DefaultIssue toIssue, DefaultIssue fromIssue) {
    toIssue.setType(fromIssue.type());
    toIssue.setCreationDate(fromIssue.creationDate());
    toIssue.setUpdateDate(fromIssue.updateDate());
    toIssue.setCloseDate(fromIssue.closeDate());
    toIssue.setResolution(fromIssue.resolution());
    toIssue.setStatus(fromIssue.status());
    toIssue.setAssignee(fromIssue.assignee());
    toIssue.setAuthorLogin(fromIssue.authorLogin());
    toIssue.setTags(fromIssue.tags());
    toIssue.setAttributes(fromIssue.attributes());
    toIssue.setEffort(debtCalculator.calculate(toIssue));
    toIssue.setOnDisabledRule(fromIssue.isOnDisabledRule());
    toIssue.setSelectedAt(fromIssue.selectedAt());
  }
}
