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
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.issue.DefaultIssue;

public class IssuesPublisher implements ReportPublisherStep {

  private final BatchComponentCache componentCache;
  private final IssueCache issueCache;

  public IssuesPublisher(BatchComponentCache componentCache, IssueCache issueCache) {
    this.componentCache = componentCache;
    this.issueCache = issueCache;

  }

  @Override
  public void publish(BatchReportWriter writer) {
    for (BatchComponent resource : componentCache.all()) {
      String componentKey = resource.resource().getEffectiveKey();
      Iterable<DefaultIssue> issues = issueCache.byComponent(componentKey);
      writer.writeComponentIssues(resource.batchId(), Iterables.transform(issues, new Function<DefaultIssue, BatchReport.Issue>() {
        private BatchReport.Issue.Builder builder = BatchReport.Issue.newBuilder();

        @Override
        public BatchReport.Issue apply(DefaultIssue input) {
          return toReportIssue(builder, input);
        }
      }));
    }
  }

  private BatchReport.Issue toReportIssue(BatchReport.Issue.Builder builder, DefaultIssue issue) {
    builder.clear();
    // non-null fields
    builder.setSeverity(Constants.Severity.valueOf(issue.severity()));
    builder.setRuleRepository(issue.ruleKey().repository());
    builder.setRuleKey(issue.ruleKey().rule());
    builder.setAttributes(KeyValueFormat.format(issue.attributes()));
    builder.addAllTag(issue.tags());

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

    return builder.build();
  }

}
