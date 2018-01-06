/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Optional;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.Uuids;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.workflow.IssueWorkflow;

/**
 * Sets the appropriate fields when an issue is :
 * <ul>
 *   <li>newly created</li>
 *   <li>merged the related base issue</li>
 *   <li>relocated (only manual issues)</li>
 * </ul>
 */
public class IssueLifecycle {

  private final IssueWorkflow workflow;
  private final IssueChangeContext changeContext;
  private final IssueFieldsSetter updater;
  private final DebtCalculator debtCalculator;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public IssueLifecycle(AnalysisMetadataHolder analysisMetadataHolder, IssueWorkflow workflow, IssueFieldsSetter updater, DebtCalculator debtCalculator) {
    this(analysisMetadataHolder, IssueChangeContext.createScan(new Date(analysisMetadataHolder.getAnalysisDate())), workflow, updater, debtCalculator);
  }

  @VisibleForTesting
  IssueLifecycle(AnalysisMetadataHolder analysisMetadataHolder, IssueChangeContext changeContext, IssueWorkflow workflow, IssueFieldsSetter updater,
    DebtCalculator debtCalculator) {
    this.analysisMetadataHolder = analysisMetadataHolder;
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

  public void copyExistingOpenIssueFromLongLivingBranch(DefaultIssue raw, DefaultIssue base, String fromLongBranchName) {
    raw.setKey(Uuids.create());
    raw.setNew(false);
    copyIssueAttributes(raw, base);
    raw.setFieldChange(changeContext, IssueFieldsSetter.FROM_LONG_BRANCH, fromLongBranchName, analysisMetadataHolder.getBranch().getName());
  }

  public void mergeConfirmedOrResolvedFromShortLivingBranch(DefaultIssue raw, DefaultIssue base, String fromShortBranchName) {
    copyIssueAttributes(raw, base);
    raw.setFieldChange(changeContext, IssueFieldsSetter.FROM_SHORT_BRANCH, fromShortBranchName, analysisMetadataHolder.getBranch().getName());
  }

  private void copyIssueAttributes(DefaultIssue to, DefaultIssue from) {
    to.setCopied(true);
    copyFields(to, from);
    if (from.manualSeverity()) {
      to.setManualSeverity(true);
      to.setSeverity(from.severity());
    }
    copyChanges(to, from);
  }

  private static void copyChanges(DefaultIssue raw, DefaultIssue base) {
    base.comments().forEach(c -> raw.addComment(copy(raw.key(), c)));
    base.changes().forEach(c -> copy(raw.key(), c).ifPresent(raw::addChange));
  }

  /**
   * Copy a comment from another issue
   */
  private static DefaultIssueComment copy(String issueKey, IssueComment c) {
    DefaultIssueComment comment = new DefaultIssueComment();
    comment.setIssueKey(issueKey);
    comment.setKey(Uuids.create());
    comment.setUserLogin(c.userLogin());
    comment.setMarkdownText(c.markdownText());
    comment.setCreatedAt(c.createdAt()).setUpdatedAt(c.updatedAt());
    comment.setNew(true);
    return comment;
  }

  /**
   * Copy a diff from another issue
   */
  private static Optional<FieldDiffs> copy(String issueKey, FieldDiffs c) {
    FieldDiffs result = new FieldDiffs();
    result.setIssueKey(issueKey);
    result.setUserLogin(c.userLogin());
    result.setCreationDate(c.creationDate());
    // Don't copy "file" changelogs as they refer to file uuids that might later be purged
    c.diffs().entrySet().stream().filter(e -> !e.getKey().equals(IssueFieldsSetter.FILE))
      .forEach(e -> result.setDiff(e.getKey(), e.getValue().oldValue(), e.getValue().newValue()));
    if (result.diffs().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(result);
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
