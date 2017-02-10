/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.api.posttask;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.qualitygate.Condition;
import org.sonar.server.computation.task.projectanalysis.qualitygate.ConditionStatus;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateHolder;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateStatus;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateStatusHolder;
import org.sonar.server.computation.task.step.ComputationStepExecutor;

import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.ce.posttask.CeTask.Status.FAILED;
import static org.sonar.api.ce.posttask.CeTask.Status.SUCCESS;

/**
 * Responsible for calling {@link PostProjectAnalysisTask} implementations (if any).
 */
public class PostProjectAnalysisTasksExecutor implements ComputationStepExecutor.Listener {
  private static final PostProjectAnalysisTask[] NO_POST_PROJECT_ANALYSIS_TASKS = new PostProjectAnalysisTask[0];

  private static final Logger LOG = Loggers.get(PostProjectAnalysisTasksExecutor.class);

  private final org.sonar.ce.queue.CeTask ceTask;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final QualityGateHolder qualityGateHolder;
  private final QualityGateStatusHolder qualityGateStatusHolder;
  private final PostProjectAnalysisTask[] postProjectAnalysisTasks;
  private final BatchReportReader reportReader;
  private final System2 system2;

  /**
   * Constructor used by Pico when there is no {@link PostProjectAnalysisTask} in the container.
   */
  public PostProjectAnalysisTasksExecutor(org.sonar.ce.queue.CeTask ceTask,
    AnalysisMetadataHolder analysisMetadataHolder,
    QualityGateHolder qualityGateHolder, QualityGateStatusHolder qualityGateStatusHolder,
    BatchReportReader reportReader, System2 system2) {
    this(ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder, reportReader, system2, null);
  }

  public PostProjectAnalysisTasksExecutor(org.sonar.ce.queue.CeTask ceTask,
    AnalysisMetadataHolder analysisMetadataHolder,
    QualityGateHolder qualityGateHolder, QualityGateStatusHolder qualityGateStatusHolder,
    BatchReportReader reportReader, System2 system2,
    @Nullable PostProjectAnalysisTask[] postProjectAnalysisTasks) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.qualityGateHolder = qualityGateHolder;
    this.qualityGateStatusHolder = qualityGateStatusHolder;
    this.ceTask = ceTask;
    this.reportReader = reportReader;
    this.postProjectAnalysisTasks = postProjectAnalysisTasks == null ? NO_POST_PROJECT_ANALYSIS_TASKS : postProjectAnalysisTasks;
    this.system2 = system2;
  }

  @Override
  public void finished(boolean allStepsExecuted) {
    if (postProjectAnalysisTasks.length == 0) {
      return;
    }

    ProjectAnalysis projectAnalysis = createProjectAnalysis(allStepsExecuted ? SUCCESS : FAILED);
    for (PostProjectAnalysisTask postProjectAnalysisTask : postProjectAnalysisTasks) {
      executeTask(projectAnalysis, postProjectAnalysisTask);
    }
  }

  private static void executeTask(ProjectAnalysis projectAnalysis, PostProjectAnalysisTask postProjectAnalysisTask) {
    try {
      postProjectAnalysisTask.finished(projectAnalysis);
    } catch (Exception e) {
      LOG.error("Execution of task " + postProjectAnalysisTask.getClass() + " failed", e);
    }
  }

  private ProjectAnalysis createProjectAnalysis(CeTask.Status status) {
    Long analysisDate = getAnalysisDate();
    return new ProjectAnalysis(
      new CeTaskImpl(this.ceTask.getUuid(), status),
      createProject(this.ceTask),
      analysisDate,
      analysisDate == null ? system2.now() : analysisDate,
      ScannerContextImpl.from(reportReader.readContextProperties()),
      status == SUCCESS ? createQualityGate(this.qualityGateHolder) : null);
  }

  private static Project createProject(org.sonar.ce.queue.CeTask ceTask) {
    return new ProjectImpl(
      ceTask.getComponentUuid(),
      ceTask.getComponentKey(),
      ceTask.getComponentName());
  }

  @CheckForNull
  private Long getAnalysisDate() {
    if (this.analysisMetadataHolder.hasAnalysisDateBeenSet()) {
      return this.analysisMetadataHolder.getAnalysisDate();
    }
    return null;
  }

  @CheckForNull
  private QualityGateImpl createQualityGate(QualityGateHolder qualityGateHolder) {
    Optional<org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGate> qualityGateOptional = qualityGateHolder.getQualityGate();
    if (qualityGateOptional.isPresent()) {
      org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGate qualityGate = qualityGateOptional.get();

      return new QualityGateImpl(
        String.valueOf(qualityGate.getId()),
        qualityGate.getName(),
        convert(qualityGateStatusHolder.getStatus()),
        convert(qualityGate.getConditions(), qualityGateStatusHolder.getStatusPerConditions()));
    }
    return null;
  }

  private static QualityGate.Status convert(QualityGateStatus status) {
    switch (status) {
      case OK:
        return QualityGate.Status.OK;
      case WARN:
        return QualityGate.Status.WARN;
      case ERROR:
        return QualityGate.Status.ERROR;
      default:
        throw new IllegalArgumentException(format(
          "Unsupported value '%s' of QualityGateStatus can not be converted to QualityGate.Status",
          status));
    }
  }

  private static Collection<QualityGate.Condition> convert(Set<Condition> conditions, Map<Condition, ConditionStatus> statusPerConditions) {
    return from(conditions)
      .transform(new ConditionToCondition(statusPerConditions))
      .toList();
  }

  private static class ProjectAnalysis implements PostProjectAnalysisTask.ProjectAnalysis {
    private final CeTask ceTask;
    private final Project project;
    @CheckForNull
    private final Long analysisDate;
    private final long date;
    private final ScannerContext scannerContext;
    @CheckForNull
    private final QualityGate qualityGate;

    private ProjectAnalysis(CeTask ceTask, Project project,
      @Nullable Long analysisDate, long date,
      ScannerContext scannerContext, @Nullable QualityGate qualityGate) {
      this.ceTask = requireNonNull(ceTask, "ceTask can not be null");
      this.project = requireNonNull(project, "project can not be null");
      this.analysisDate = analysisDate;
      this.date = date;
      this.scannerContext = requireNonNull(scannerContext, "scannerContext can not be null");
      this.qualityGate = qualityGate;
    }

    @Override
    public CeTask getCeTask() {
      return ceTask;
    }

    @Override
    public Project getProject() {
      return project;
    }

    @Override
    @CheckForNull
    public QualityGate getQualityGate() {
      return qualityGate;
    }

    @Override
    public Date getDate() {
      return new Date(date);
    }

    @Override
    public java.util.Optional<Date> getAnalysisDate() {
      if (analysisDate == null) {
        return java.util.Optional.empty();
      }
      return java.util.Optional.of(new Date(analysisDate));
    }

    @Override
    public ScannerContext getScannerContext() {
      return scannerContext;
    }

    @Override
    public String toString() {
      return "ProjectAnalysis{" +
        "ceTask=" + ceTask +
        ", project=" + project +
        ", analysisDate=" + analysisDate +
        ", date=" + date +
        ", scannerContext=" + scannerContext +
        ", qualityGate=" + qualityGate +
        '}';
    }
  }

}
