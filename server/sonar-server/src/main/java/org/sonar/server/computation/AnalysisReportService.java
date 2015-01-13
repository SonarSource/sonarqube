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

package org.sonar.server.computation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.protocol.output.ReportHelper;
import org.sonar.batch.protocol.output.component.ReportComponent;
import org.sonar.batch.protocol.output.component.ReportComponents;
import org.sonar.batch.protocol.output.issue.ReportIssue;
import org.sonar.core.issue.db.IssueStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class AnalysisReportService implements ServerComponent {

  private final ComputeEngineIssueStorageFactory issueStorageFactory;

  public AnalysisReportService(ComputeEngineIssueStorageFactory issueStorageFactory) {
    this.issueStorageFactory = issueStorageFactory;
  }

  public void digest(ComputationContext context) {
    loadResources(context);
    saveIssues(context);
  }

  @VisibleForTesting
  void loadResources(ComputationContext context) {
    File file = new File(context.getReportDirectory(), "components.json");

    try (InputStream resourcesStream = new FileInputStream(file)) {
      String json = IOUtils.toString(resourcesStream);
      ReportComponents reportComponents = ReportComponents.fromJson(json);
      context.addResources(reportComponents);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read issues", e);
    }
  }

  @VisibleForTesting
  void saveIssues(final ComputationContext context) {
    IssueStorage issueStorage = issueStorageFactory.newComputeEngineIssueStorage(context.getProject());

    ReportHelper helper = ReportHelper.create(context.getReportDirectory());

    ReportComponent root = helper.getComponents().root();
    browseComponent(context, helper, issueStorage, root);
  }

  private void browseComponent(ComputationContext context, ReportHelper helper, IssueStorage issueStorage, ReportComponent component) {
    Iterable<ReportIssue> reportIssues = helper.getIssues(component.batchId());
    saveIssues(context, issueStorage, reportIssues);
    for (ReportComponent child : component.children()) {
      browseComponent(context, helper, issueStorage, child);
    }
  }

  private void saveIssues(final ComputationContext context, IssueStorage issueStorage, Iterable<ReportIssue> reportIssues) {
    issueStorage.save(Iterables.transform(reportIssues, new Function<ReportIssue, DefaultIssue>() {
      @Override
      public DefaultIssue apply(ReportIssue input) {
        return toIssue(context, input);
      }
    }));
  }

  private DefaultIssue toIssue(ComputationContext context, ReportIssue issue) {
    DefaultIssue defaultIssue = new DefaultIssue();
    defaultIssue.setKey(issue.key());
    setComponentId(defaultIssue, context.getComponentByBatchId(issue.componentBatchId()));
    defaultIssue.setRuleKey(RuleKey.of(issue.ruleRepo(), issue.ruleKey()));
    defaultIssue.setSeverity(issue.severity());
    defaultIssue.setManualSeverity(issue.isManualSeverity());
    defaultIssue.setMessage(issue.message());
    defaultIssue.setLine(issue.line());
    defaultIssue.setEffortToFix(issue.effortToFix());
    setDebt(defaultIssue, issue.debt());
    setFieldDiffs(defaultIssue, issue.diffFields(), context.getAnalysisDate());
    defaultIssue.setStatus(issue.status());
    defaultIssue.setResolution(issue.resolution());
    defaultIssue.setReporter(issue.reporter());
    defaultIssue.setAssignee(issue.assignee());
    defaultIssue.setChecksum(issue.checksum());
    defaultIssue.setAttributes(KeyValueFormat.parse(issue.issueAttributes()));
    defaultIssue.setAuthorLogin(issue.authorLogin());
    defaultIssue.setActionPlanKey(issue.actionPlanKey());
    defaultIssue.setCreationDate(issue.creationDate());
    defaultIssue.setUpdateDate(issue.updateDate());
    defaultIssue.setCloseDate(issue.closeDate());
    defaultIssue.setChanged(issue.isChanged());
    defaultIssue.setNew(issue.isNew());
    defaultIssue.setSelectedAt(issue.selectedAt());
    return defaultIssue;
  }

  private DefaultIssue setFieldDiffs(DefaultIssue issue, String diffFields, Date analysisDate) {
    FieldDiffs fieldDiffs = FieldDiffs.parse(diffFields);
    fieldDiffs.setCreationDate(analysisDate);
    issue.setCurrentChange(fieldDiffs);

    return issue;
  }

  private DefaultIssue setComponentId(DefaultIssue issue, ReportComponent component) {
    if (component != null) {
      issue.setComponentId((long) component.id());
    }
    return issue;
  }

  private DefaultIssue setDebt(DefaultIssue issue, Long debt) {
    if (debt != null) {
      issue.setDebt(Duration.create(debt));
    }

    return issue;
  }
}
