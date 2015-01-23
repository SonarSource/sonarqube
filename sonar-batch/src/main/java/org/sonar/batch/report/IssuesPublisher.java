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
import org.sonar.batch.protocol.output.ReportHelper;
import org.sonar.batch.protocol.output.issue.ReportIssue;

import java.io.IOException;

public class IssuesPublisher implements ReportPublisher {

  private final ResourceCache resourceCache;
  private final IssueCache issueCache;

  public IssuesPublisher(ResourceCache resourceCache, IssueCache issueCache) {
    this.resourceCache = resourceCache;
    this.issueCache = issueCache;
  }

  @Override
  public void export(ReportHelper reportHelper) throws IOException {
    for (BatchResource resource : resourceCache.all()) {
      Iterable<DefaultIssue> issues = issueCache.byComponent(resource.resource().getEffectiveKey());
      reportHelper.saveIssues(resource.batchId(), Iterables.transform(issues, new Function<DefaultIssue, ReportIssue>() {
        @Override
        public ReportIssue apply(DefaultIssue input) {
          return toReportIssue(input);
        }
      }));
    }
  }

  private ReportIssue toReportIssue(DefaultIssue issue) {
    BatchResource batchResource = resourceCache.get(issue.componentKey());
    return new ReportIssue()
      .setKey(issue.key())
      .setComponentBatchId(batchResource != null ? batchResource.batchId() : null)
      .setNew(issue.isNew())
      .setLine(issue.line())
      .setMessage(issue.message())
      .setEffortToFix(issue.effortToFix())
      .setDebt(issue.debtInMinutes())
      .setResolution(issue.resolution())
      .setStatus(issue.status())
      .setSeverity(issue.severity())
      .setChecksum(issue.checksum())
      .setManualSeverity(issue.manualSeverity())
      .setReporter(issue.reporter())
      .setAssignee(issue.assignee())
      .setRuleKey(issue.ruleKey().repository(), issue.ruleKey().rule())
      .setActionPlanKey(issue.actionPlanKey())
      .setAttributes(KeyValueFormat.format(issue.attributes()))
      .setAuthorLogin(issue.authorLogin())
      .setCreationDate(issue.creationDate())
      .setCloseDate(issue.closeDate())
      .setUpdateDate(issue.updateDate())
      .setSelectedAt(issue.selectedAt())
      .setDiffFields(toString(issue.currentChange()))
      .setTags(issue.tags())
      .setChanged(issue.isChanged());
  }

  private String toString(FieldDiffs currentChange) {
    return currentChange != null ? currentChange.toString() : null;
  }

}
