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
package org.sonar.batch.report;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchOutputWriter;
import org.sonar.batch.protocol.output.BatchReport;

import javax.annotation.Nullable;

import java.util.Date;

public class IssuesPublisher implements ReportPublisher {

  private final ResourceCache resourceCache;
  private final IssueCache issueCache;

  public IssuesPublisher(ResourceCache resourceCache, IssueCache issueCache) {
    this.resourceCache = resourceCache;
    this.issueCache = issueCache;
  }

  @Override
  public void publish(BatchOutputWriter writer) {
    for (BatchResource resource : resourceCache.all()) {
      Iterable<DefaultIssue> issues = issueCache.byComponent(resource.resource().getEffectiveKey());
      writer.writeComponentIssues(resource.batchId(), Iterables.transform(issues, new Function<DefaultIssue, BatchReport.Issue>() {
        @Override
        public BatchReport.Issue apply(DefaultIssue input) {
          return toReportIssue(input);
        }
      }));
    }
  }

  private BatchReport.Issue toReportIssue(DefaultIssue issue) {
    BatchReport.Issue.Builder builder = BatchReport.Issue.newBuilder();

    // non-null fields
    builder.setUuid(issue.key());
    builder.setIsNew(issue.isNew());
    builder.setSeverity(Constants.Severity.valueOf(issue.severity()));
    builder.setRuleRepository(issue.ruleKey().repository());
    builder.setRuleKey(issue.ruleKey().rule());
    builder.setAttributes(KeyValueFormat.format(issue.attributes()));
    builder.addAllTags(issue.tags());
    builder.setMustSendNotification(issue.mustSendNotifications());
    builder.setIsChanged(issue.isChanged());

    // nullable fields
    Integer line = issue.line();
    if (line != null) {
      builder.setLine(line);
    }
    builder.setMsg(issue.message());
    if (issue.effortToFix() != null) {
      builder.setEffortToFix(issue.effortToFix());
    }
    if (issue.debtInMinutes() != null) {
      builder.setDebtInMinutes(issue.debtInMinutes());
    }
    if (issue.resolution() != null) {
      builder.setResolution(issue.resolution());
    }
    if (issue.status() != null) {
      builder.setStatus(issue.status());
    }
    if (issue.checksum() != null) {
      builder.setChecksum(issue.checksum());
    }
    builder.setManualSeverity(issue.manualSeverity());
    if (issue.reporter() != null) {
      builder.setReporter(issue.reporter());
    }
    if (issue.assignee() != null) {
      builder.setAssignee(issue.assignee());
    }
    if (issue.actionPlanKey() != null) {
      builder.setActionPlanKey(issue.actionPlanKey());
    }
    if (issue.authorLogin() != null) {
      builder.setAuthorLogin(issue.authorLogin());
    }
    String diff = diffsToString(issue.currentChange());
    if (diff != null) {
      builder.setDiffFields(diff);
    }
    Date creationDate = issue.creationDate();
    if (creationDate != null) {
      builder.setCreationDate(creationDate.getTime());
    }
    Long selectedAt = issue.selectedAt();
    if (selectedAt != null) {
      builder.setSelectedAt(selectedAt);
    }
    Date closeDate = issue.closeDate();
    if (closeDate != null) {
      builder.setCloseDate(closeDate.getTime());
    }
    Date updateDate = issue.updateDate();
    if (updateDate != null) {
      builder.setUpdateDate(updateDate.getTime());
    }
    return builder.build();
  }

  private String diffsToString(@Nullable FieldDiffs diffs) {
    return diffs != null ? diffs.toString() : null;
  }

}
