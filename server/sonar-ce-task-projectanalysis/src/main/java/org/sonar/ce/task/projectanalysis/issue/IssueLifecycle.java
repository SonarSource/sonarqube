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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.Date;
import java.util.Optional;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.KeyType;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.workflow.IssueWorkflow;

import static java.util.Objects.requireNonNull;

/**
 * Sets the appropriate fields when an issue is :
 * <ul>
 * <li>newly created</li>
 * <li>merged the related base issue</li>
 * <li>relocated (only manual issues)</li>
 * </ul>
 */
public class IssueLifecycle {

  private final IssueWorkflow workflow;
  private final IssueChangeContext changeContext;
  private final RuleRepository ruleRepository;
  private final IssueFieldsSetter updater;
  private final DebtCalculator debtCalculator;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public IssueLifecycle(AnalysisMetadataHolder analysisMetadataHolder, IssueWorkflow workflow, IssueFieldsSetter updater, DebtCalculator debtCalculator,
    RuleRepository ruleRepository) {
    this(analysisMetadataHolder, IssueChangeContext.createScan(new Date(analysisMetadataHolder.getAnalysisDate())), workflow, updater, debtCalculator, ruleRepository);
  }

  @VisibleForTesting
  IssueLifecycle(AnalysisMetadataHolder analysisMetadataHolder, IssueChangeContext changeContext, IssueWorkflow workflow, IssueFieldsSetter updater,
    DebtCalculator debtCalculator, RuleRepository ruleRepository) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.workflow = workflow;
    this.updater = updater;
    this.debtCalculator = debtCalculator;
    this.changeContext = changeContext;
    this.ruleRepository = ruleRepository;
  }

  public void initNewOpenIssue(DefaultIssue issue) {
    Preconditions.checkArgument(issue.isFromExternalRuleEngine() != (issue.type() == null), "At this stage issue type should be set for and only for external issues");
    Rule rule = ruleRepository.getByKey(issue.ruleKey());
    issue.setKey(Uuids.create());
    issue.setCreationDate(changeContext.date());
    issue.setUpdateDate(changeContext.date());
    issue.setEffort(debtCalculator.calculate(issue));
    issue.setIsFromHotspot(rule.getType() == RuleType.SECURITY_HOTSPOT);
    setType(issue, rule);
    setStatus(issue, rule);
  }

  private void setType(DefaultIssue issue, Rule rule) {
    if (issue.isFromExternalRuleEngine()) {
      return;
    }
    issue.setType(requireNonNull(rule.getType(), "No rule type"));
  }

  private void setStatus(DefaultIssue issue, Rule rule) {
    if (issue.isFromExternalRuleEngine() || rule.getType() != RuleType.SECURITY_HOTSPOT) {
      issue.setStatus(Issue.STATUS_OPEN);
    } else {
      issue.setStatus(Issue.STATUS_TO_REVIEW);
    }
  }

  public void copyExistingOpenIssueFromLongLivingBranch(DefaultIssue raw, DefaultIssue base, String fromLongBranchName) {
    raw.setKey(Uuids.create());
    raw.setNew(false);
    copyAttributesOfIssueFromOtherBranch(raw, base);
    raw.setFieldChange(changeContext, IssueFieldsSetter.FROM_LONG_BRANCH, fromLongBranchName, analysisMetadataHolder.getBranch().getName());
  }

  public void mergeConfirmedOrResolvedFromShortLivingBranchOrPr(DefaultIssue raw, DefaultIssue base, KeyType branchType, String fromShortBranchNameOrPR) {
    copyAttributesOfIssueFromOtherBranch(raw, base);
    String from = (branchType == KeyType.PULL_REQUEST ? "#" : "") + fromShortBranchNameOrPR;
    String to = analysisMetadataHolder.isPullRequest() ? ("#" + analysisMetadataHolder.getPullRequestKey()) : analysisMetadataHolder.getBranch().getName();
    raw.setFieldChange(changeContext, IssueFieldsSetter.FROM_SHORT_BRANCH, from, to);
  }

  private void copyAttributesOfIssueFromOtherBranch(DefaultIssue to, DefaultIssue from) {
    to.setCopied(true);
    copyFields(to, from);
    if (from.manualSeverity()) {
      to.setManualSeverity(true);
      to.setSeverity(from.severity());
    }
    copyChangesOfIssueFromOtherBranch(to, from);
  }

  private static void copyChangesOfIssueFromOtherBranch(DefaultIssue raw, DefaultIssue base) {
    base.defaultIssueComments().forEach(c -> raw.addComment(copyComment(raw.key(), c)));
    base.changes().forEach(c -> copyFieldDiffOfIssueFromOtherBranch(raw.key(), c).ifPresent(raw::addChange));
  }

  /**
   * Copy a comment from another issue
   */
  private static DefaultIssueComment copyComment(String issueKey, DefaultIssueComment c) {
    DefaultIssueComment comment = new DefaultIssueComment();
    comment.setIssueKey(issueKey);
    comment.setKey(Uuids.create());
    comment.setUserUuid(c.userUuid());
    comment.setMarkdownText(c.markdownText());
    comment.setCreatedAt(c.createdAt()).setUpdatedAt(c.updatedAt());
    comment.setNew(true);
    return comment;
  }

  /**
   * Copy a diff from another issue
   */
  private static Optional<FieldDiffs> copyFieldDiffOfIssueFromOtherBranch(String issueKey, FieldDiffs c) {
    FieldDiffs result = new FieldDiffs();
    result.setIssueKey(issueKey);
    result.setUserUuid(c.userUuid());
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
    Preconditions.checkArgument(raw.isFromExternalRuleEngine() != (raw.type() == null), "At this stage issue type should be set for and only for external issues");
    Rule rule = ruleRepository.getByKey(raw.ruleKey());
    raw.setKey(base.key());
    raw.setNew(false);
    if (base.isChanged()) {
      // In case issue was moved from module or folder to the root project
      raw.setChanged(true);
    }
    raw.setIsFromHotspot(rule.getType() == RuleType.SECURITY_HOTSPOT);
    setType(raw, rule);
    copyFields(raw, base);
    base.changes().forEach(raw::addChange);
    if (raw.isFromHotspot() != base.isFromHotspot()) {
      // This is to force DB update of the issue
      raw.setChanged(true);
    }
    if (raw.isFromHotspot() && !base.isFromHotspot()) {
      // First analysis after rule type was changed to security_hotspot. Issue will be reset to an open hotspot
      updater.setType(raw, RuleType.SECURITY_HOTSPOT, changeContext);
      updater.setStatus(raw, Issue.STATUS_TO_REVIEW, changeContext);
      updater.setResolution(raw, null, changeContext);
    }

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
    toIssue.setAssigneeUuid(fromIssue.assignee());
    toIssue.setAuthorLogin(fromIssue.authorLogin());
    toIssue.setTags(fromIssue.tags());
    toIssue.setAttributes(fromIssue.attributes());
    toIssue.setEffort(debtCalculator.calculate(toIssue));
    toIssue.setOnDisabledRule(fromIssue.isOnDisabledRule());
    toIssue.setSelectedAt(fromIssue.selectedAt());
  }
}
