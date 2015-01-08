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

import org.sonar.batch.protocol.output.component.ReportComponent;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.protocol.GsonHelper;
import org.sonar.batch.protocol.output.ReportHelper;
import org.sonar.batch.protocol.output.issue.ReportIssue;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class AnalysisReportService implements ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(AnalysisReportService.class);
  private static final int MAX_ISSUES_SIZE = 1000;
  private final ComputeEngineIssueStorageFactory issueStorageFactory;
  private final DbClient dbClient;
  private final Gson gson;

  public AnalysisReportService(DbClient dbClient, ComputeEngineIssueStorageFactory issueStorageFactory) {
    this.issueStorageFactory = issueStorageFactory;
    this.dbClient = dbClient;
    gson = GsonHelper.create();
  }

  public void digest(DbSession session, ComputeEngineContext context) {
    decompress(session, context);
    loadComponents(context);
    saveIssues(context);
  }

  @VisibleForTesting
  void loadComponents(ComputeEngineContext context) {
    context.addResources(context.getReportHelper().getComponents());
  }

  @VisibleForTesting
  void decompress(DbSession session, ComputeEngineContext context) {
    AnalysisReportDto report = context.getReportDto();

    File decompressedDirectory = dbClient.analysisReportDao().getDecompressedReport(session, report.getId());
    context.setReportHelper(ReportHelper.create(decompressedDirectory));
  }

  @VisibleForTesting
  void saveIssues(final ComputeEngineContext context) {
    IssueStorage issueStorage = issueStorageFactory.newComputeEngineIssueStorage(context.getProject());

    for (ReportComponent component : context.getComponents().values()) {
      Iterable<ReportIssue> reportIssues = context.getReportHelper().getIssues(component.batchId());
      issueStorage.save(Iterables.transform(reportIssues, new Function<ReportIssue, DefaultIssue>() {
        @Override
        public DefaultIssue apply(ReportIssue input) {
          return toIssue(context, input);
        }
      }));
    }
  }

  private DefaultIssue toIssue(ComputeEngineContext context, ReportIssue issue) {
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

  public void deleteDirectory(@Nullable File directory) {
    if (directory == null) {
      return;
    }

    try {
      FileUtils.deleteDirectory(directory);
    } catch (IOException e) {
      LOG.warn(String.format("Failed to delete directory '%s'", directory.getPath()), e);
    }
  }
}
