/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ProjectAnalysisNewValue;
import org.sonar.scanner.protocol.output.ScannerReport.ContextProperty;
import org.sonar.server.project.Project;

public class ProjectAnalysisAuditStep implements ComputationStep {
  private static final String CODESCAN_JOB_ID_SONAR_PARAM = "sonar.analysis.buildId";
  private static final Logger log = LoggerFactory.getLogger(ProjectAnalysisAuditStep.class);

  private final BatchReportReader reportReader;
  private final DbClient dbClient;
  private final AuditPersister auditPersister;
  private final CeTask ceTask;

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public ProjectAnalysisAuditStep(BatchReportReader reportReader, DbClient dbClient, AuditPersister auditPersister,
      CeTask ceTask, AnalysisMetadataHolder analysisMetadataHolder, TreeRootHolder treeRootHolder, MetricRepository metricRepository,
          MeasureRepository measureRepository) {
    this.reportReader = reportReader;
    this.dbClient = dbClient;
    this.auditPersister = auditPersister;
    this.ceTask = ceTask;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute(Context context) {
    // Get CodeScan job id
    log.info("Starting project analysis audit");
    String jobId = null;
    try (CloseableIterator<ContextProperty> it = reportReader.readContextProperties()) {
      while (it.hasNext()) {
        ContextProperty contextProperty = it.next();
        if (CODESCAN_JOB_ID_SONAR_PARAM.equals(contextProperty.getKey())) {
          jobId = contextProperty.getValue();
          log.info("Found code scan job id: {}", jobId);
          break;
        }
      }
    }

    Project project = analysisMetadataHolder.getProject();
    String projectKey = project.getKey();
    String projectName = project.getName();
    log.info("Found project: {}", projectKey);

    int ncloc = getNCLoc();
    log.info("NCLoc: {}", ncloc);

    try (DbSession dbSession = dbClient.openSession(false)) {
      log.info("In open client");
      String organizationUuid = ceTask.getOrganizationUuid();
      log.info("Org: {}", organizationUuid);
      log.info("AuditPersister: {}", auditPersister.getClass().getSimpleName());
      auditPersister.createProjectAnalysis(dbSession, organizationUuid, new ProjectAnalysisNewValue(projectKey, projectName, ncloc, jobId));
      dbSession.commit();
      log.info("Post commit");
    }
  }

  private int getNCLoc() {
    Metric nclocMetric = metricRepository.getByKey(CoreMetrics.NCLOC_KEY);
    Optional<Measure> nclocMeasure = measureRepository.getRawMeasure(treeRootHolder.getRoot(), nclocMetric);
    return nclocMeasure.map(Measure::getIntValue).orElse(0);
  }

  @Override
  public String getDescription() {
    return "Audit project analysis start.";
  }
}