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
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

public class IssuesPublisher implements ReportPublisherStep {

  private final ResourceCache resourceCache;
  private final IssueCache issueCache;
  private final ProjectReactor reactor;

  public IssuesPublisher(ProjectReactor reactor, ResourceCache resourceCache, IssueCache issueCache) {
    this.reactor = reactor;
    this.resourceCache = resourceCache;
    this.issueCache = issueCache;
  }

  @Override
  public void publish(BatchReportWriter writer) {
    Collection<Object> deletedComponentKeys = issueCache.componentKeys();
    for (BatchResource resource : resourceCache.all()) {
      String componentKey = resource.resource().getEffectiveKey();
      Iterable<DefaultIssue> issues = issueCache.byComponent(componentKey);
      writer.writeComponentIssues(resource.batchId(), Iterables.transform(issues, new Function<DefaultIssue, BatchReport.Issue>() {
        private BatchReport.Issue.Builder builder = BatchReport.Issue.newBuilder();

        @Override
        public BatchReport.Issue apply(DefaultIssue input) {
          return toReportIssue(builder, input);
        }
      }));
      deletedComponentKeys.remove(componentKey);
    }

    int count = exportIssuesOfDeletedComponents(deletedComponentKeys, writer);

    exportMetadata(writer, count);
  }

  private void exportMetadata(BatchReportWriter writer, int count) {
    BatchResource rootProject = resourceCache.get(reactor.getRoot().getKeyWithBranch());
    BatchReport.Metadata.Builder builder = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(((Project) rootProject.resource()).getAnalysisDate().getTime())
      .setProjectKey(((Project) rootProject.resource()).key())
      .setRootComponentRef(rootProject.batchId())
      .setDeletedComponentsCount(count);
    Integer sid = rootProject.snapshotId();
    if (sid != null) {
      builder.setSnapshotId(sid);
    }
    writer.writeMetadata(builder.build());
  }

  private int exportIssuesOfDeletedComponents(Collection<Object> deletedComponentKeys, BatchReportWriter writer) {
    int deletedComponentCount = 0;
    for (Object componentKey : deletedComponentKeys) {
      deletedComponentCount++;
      Iterable<DefaultIssue> issues = issueCache.byComponent(componentKey.toString());
      Iterator<DefaultIssue> iterator = issues.iterator();
      if (iterator.hasNext()) {
        String componentUuid = iterator.next().componentUuid();
        writer.writeDeletedComponentIssues(deletedComponentCount, componentUuid, Iterables.transform(issues, new Function<DefaultIssue, BatchReport.Issue>() {
          private BatchReport.Issue.Builder builder = BatchReport.Issue.newBuilder();

          @Override
          public BatchReport.Issue apply(DefaultIssue input) {
            return toReportIssue(builder, input);
          }
        }));
      }
    }
    return deletedComponentCount;
  }

  private BatchReport.Issue toReportIssue(BatchReport.Issue.Builder builder, DefaultIssue issue) {
    builder.clear();
    // non-null fields
    builder.setUuid(issue.key());
    builder.setIsNew(issue.isNew());
    builder.setSeverity(Constants.Severity.valueOf(issue.severity()));
    builder.setRuleRepository(issue.ruleKey().repository());
    builder.setRuleKey(issue.ruleKey().rule());
    builder.setAttributes(KeyValueFormat.format(issue.attributes()));
    builder.addAllTag(issue.tags());
    builder.setMustSendNotification(issue.mustSendNotifications());
    builder.setIsChanged(issue.isChanged());

    // nullable fields
    Integer line = issue.line();
    if (line != null) {
      builder.setLine(line);
    }
    String message = issue.message();
    if (message != null) {
      builder.setMsg(message);
    }
    Double effortToFix = issue.effortToFix();
    if (effortToFix != null) {
      builder.setEffortToFix(effortToFix);
    }

    Long debtInMinutes = issue.debtInMinutes();
    if (debtInMinutes != null) {
      builder.setDebtInMinutes(debtInMinutes);
    }
    String resolution = issue.resolution();
    if (resolution != null) {
      builder.setResolution(resolution);
    }
    String status = issue.status();
    if (status != null) {
      builder.setStatus(status);
    }
    String checksum = issue.checksum();
    if (checksum != null) {
      builder.setChecksum(checksum);
    }
    builder.setManualSeverity(issue.manualSeverity());
    String reporter = issue.reporter();
    if (reporter != null) {
      builder.setReporter(reporter);
    }
    String assignee = issue.assignee();
    if (assignee != null) {
      builder.setAssignee(assignee);
    }
    String actionPlanKey = issue.actionPlanKey();
    if (actionPlanKey != null) {
      builder.setActionPlanKey(actionPlanKey);
    }
    String authorLogin = issue.authorLogin();
    if (authorLogin != null) {
      builder.setAuthorLogin(authorLogin);
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
