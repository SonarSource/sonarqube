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
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.protocol.GsonHelper;
import org.sonar.batch.protocol.output.issue.ReportIssue;
import org.sonar.batch.protocol.output.resource.ReportComponent;
import org.sonar.batch.protocol.output.resource.ReportComponents;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;

import javax.annotation.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
    loadResources(context);
    // saveIssues(context);
  }

  @VisibleForTesting
  void loadResources(ComputeEngineContext context) {
    File file = new File(context.getReportDirectory(), "components.json");

    try {
      InputStream resourcesStream = new FileInputStream(file);
      String json = IOUtils.toString(resourcesStream);
      ReportComponents reportComponents = ReportComponents.fromJson(json);
      context.addResources(reportComponents);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read issues", e);
    }
  }

  @VisibleForTesting
  void decompress(DbSession session, ComputeEngineContext context) {
    AnalysisReportDto report = context.getReportDto();

    File decompressedDirectory = dbClient.analysisReportDao().getDecompressedReport(session, report.getId());
    String path = decompressedDirectory == null ? "no path" : decompressedDirectory.getAbsolutePath();
    context.setReportDirectory(decompressedDirectory);
    LOG.info(String.format("report decompressed at '%s'", path));
  }

  @VisibleForTesting
  void saveIssues(ComputeEngineContext context) {
    IssueStorage issueStorage = issueStorageFactory.newComputeEngineIssueStorage(context.getProject());

    File issuesFile = new File(context.getReportDirectory(), "issues.json");
    List<DefaultIssue> issues = new ArrayList<>(MAX_ISSUES_SIZE);

    try {
      InputStream issuesStream = new FileInputStream(issuesFile);
      JsonReader reader = new JsonReader(new InputStreamReader(issuesStream));
      reader.beginArray();
      while (reader.hasNext()) {
        ReportIssue reportIssue = gson.fromJson(reader, ReportIssue.class);
        DefaultIssue defaultIssue = toIssue(context, reportIssue);
        issues.add(defaultIssue);
        if (shouldPersistIssues(issues, reader)) {
          issueStorage.save(issues);
          issues.clear();
        }
      }

      reader.endArray();
      reader.close();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read issues", e);
    }
  }

  private boolean shouldPersistIssues(List<DefaultIssue> issues, JsonReader reader) throws IOException {
    return issues.size() == MAX_ISSUES_SIZE || !reader.hasNext();
  }

  private DefaultIssue toIssue(ComputeEngineContext context, ReportIssue issue) {
    ReportComponent component = context.getComponentByBatchId(issue.componentBatchId());
    DefaultIssue defaultIssue = new DefaultIssue();
    defaultIssue.setKey(issue.key());
    defaultIssue.setComponentId((long) component.id());
    defaultIssue.setRuleKey(RuleKey.of(issue.ruleRepo(), issue.ruleKey()));
    defaultIssue.setSeverity(issue.severity());
    defaultIssue.setManualSeverity(issue.isManualSeverity());
    defaultIssue.setMessage(issue.message());
    defaultIssue.setLine(issue.line());
    defaultIssue.setEffortToFix(issue.effortToFix());
    setDebt(defaultIssue, issue.debt());
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
    defaultIssue.setCurrentChange(FieldDiffs.parse(issue.diffFields()));
    defaultIssue.setChanged(issue.isChanged());
    defaultIssue.setNew(issue.isNew());
    defaultIssue.setSelectedAt(issue.selectedAt());
    return defaultIssue;
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
