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

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.sonar.api.CoreProperties;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.util.cache.DiskCache;

import javax.annotation.Nullable;

import java.util.Date;

public class IssueComputation {

  private static final Logger LOG = Loggers.get(IssueComputation.class);

  private final RuleCache ruleCache;
  private final ScmAccountCache scmAccountCache;
  private final SourceLinesCache linesCache;
  private final DiskCache<DefaultIssue>.DiskAppender diskIssuesAppender;
  private final UserIndex userIndex;
  private boolean hasAssigneeBeenComputed = false;
  private String defaultAssignee = null;

  public IssueComputation(RuleCache ruleCache, SourceLinesCache linesCache, ScmAccountCache scmAccountCache,
    IssueCache issueCache, UserIndex userIndex) {
    this.ruleCache = ruleCache;
    this.linesCache = linesCache;
    this.scmAccountCache = scmAccountCache;
    this.userIndex = userIndex;
    this.diskIssuesAppender = issueCache.newAppender();
  }

  public void processComponentIssues(ComputationContext context, Iterable<BatchReport.Issue> issues, String componentUuid, @Nullable Integer componentReportRef) {
    linesCache.init(componentUuid, componentReportRef, context.getReportReader());
    computeDefaultAssignee(context.getProjectSettings().getString(CoreProperties.DEFAULT_ISSUE_ASSIGNEE));
    for (BatchReport.Issue reportIssue : issues) {
      DefaultIssue issue = toDefaultIssue(context, componentUuid, reportIssue);
      if (issue.isNew()) {
        guessAuthor(issue);
        autoAssign(issue, defaultAssignee);
        copyRuleTags(issue);
      }
      diskIssuesAppender.append(issue);
    }
    linesCache.clear();
  }

  private DefaultIssue toDefaultIssue(ComputationContext context, String componentUuid, BatchReport.Issue issue) {
    DefaultIssue target = new DefaultIssue();
    target.setKey(issue.getUuid());
    target.setComponentUuid(componentUuid);
    target.setRuleKey(RuleKey.of(issue.getRuleRepository(), issue.getRuleKey()));
    target.setSeverity(issue.getSeverity().name());
    target.setManualSeverity(issue.getManualSeverity());
    target.setMessage(issue.hasMsg() ? issue.getMsg() : null);
    target.setLine(issue.hasLine() ? issue.getLine() : null);
    target.setProjectUuid(context.getProject().uuid());
    target.setProjectKey(context.getProject().key());
    target.setEffortToFix(issue.hasEffortToFix() ? issue.getEffortToFix() : null);
    target.setDebt(issue.hasDebtInMinutes() ? Duration.create(issue.getDebtInMinutes()) : null);
    if (issue.hasDiffFields()) {
      FieldDiffs fieldDiffs = FieldDiffs.parse(issue.getDiffFields());
      fieldDiffs.setCreationDate(new Date(context.getReportMetadata().getAnalysisDate()));
      target.setCurrentChange(fieldDiffs);
    }
    target.setStatus(issue.getStatus());
    target.setTags(issue.getTagList());
    target.setResolution(issue.hasResolution() ? issue.getResolution() : null);
    target.setReporter(issue.hasReporter() ? issue.getReporter() : null);
    target.setAssignee(issue.hasAssignee() ? issue.getAssignee() : null);
    target.setChecksum(issue.hasChecksum() ? issue.getChecksum() : null);
    target.setAttributes(issue.hasAttributes() ? KeyValueFormat.parse(issue.getAttributes()) : null);
    target.setAuthorLogin(issue.hasAuthorLogin() ? issue.getAuthorLogin() : null);
    target.setActionPlanKey(issue.hasActionPlanKey() ? issue.getActionPlanKey() : null);
    target.setCreationDate(issue.hasCreationDate() ? new Date(issue.getCreationDate()) : null);
    target.setUpdateDate(issue.hasUpdateDate() ? new Date(issue.getUpdateDate()) : null);
    target.setCloseDate(issue.hasCloseDate() ? new Date(issue.getCloseDate()) : null);
    target.setChanged(issue.getIsChanged());
    target.setNew(issue.getIsNew());
    target.setSelectedAt(issue.hasSelectedAt() ? issue.getSelectedAt() : null);
    target.setSendNotifications(issue.getMustSendNotification());
    return target;
  }

  public void afterReportProcessing() {
    diskIssuesAppender.close();
  }

  private void guessAuthor(DefaultIssue issue) {
    // issue.authorLogin() can be not-null when old developer cockpit plugin (or other plugin)
    // is still installed and executed during analysis
    if (issue.authorLogin() == null) {
      issue.setAuthorLogin(linesCache.lineAuthor(issue.line()));
    }
  }

  private void autoAssign(DefaultIssue issue, @Nullable String defaultAssignee) {
    // issue.assignee() can be not-null if the issue-assign-plugin is
    // still installed and executed during analysis
    if (issue.assignee() == null) {
      String scmAccount = issue.authorLogin();
      if (scmAccount != null) {
        issue.setAssignee(scmAccountCache.getNullable(scmAccount));
      }
      if (issue.assignee() == null && defaultAssignee != null) {
        issue.setAssignee(defaultAssignee);
      }
    }
  }

  private void copyRuleTags(DefaultIssue issue) {
    RuleDto rule = ruleCache.get(issue.ruleKey());
    issue.setTags(Sets.union(rule.getTags(), rule.getSystemTags()));
  }

  private void computeDefaultAssignee(@Nullable String login) {
    if (hasAssigneeBeenComputed) {
      return;
    }

    hasAssigneeBeenComputed = true;
    if (!Strings.isNullOrEmpty(login)) {
      UserDoc user = userIndex.getNullableByLogin(login);
      if (user == null) {
        LOG.info("the {} property was set with an unknown login: {}", CoreProperties.DEFAULT_ISSUE_ASSIGNEE, login);
      } else {
        defaultAssignee = login;
      }
    }
  }
}
