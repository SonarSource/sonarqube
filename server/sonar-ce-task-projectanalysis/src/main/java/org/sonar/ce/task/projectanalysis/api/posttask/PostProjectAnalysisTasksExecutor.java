/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.api.posttask;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.Organization;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.ce.task.projectanalysis.qualitygate.ConditionStatus;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateHolder;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateStatus;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateStatusHolder;
import org.sonar.ce.task.step.ComputationStepExecutor;
import org.sonar.core.util.logs.Profiler;
import org.sonar.core.util.stream.MoreCollectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.sonar.api.ce.posttask.CeTask.Status.FAILED;
import static org.sonar.api.ce.posttask.CeTask.Status.SUCCESS;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

/**
 * Responsible for calling {@link PostProjectAnalysisTask} implementations (if any).
 */
public class PostProjectAnalysisTasksExecutor implements ComputationStepExecutor.Listener {
  private static final PostProjectAnalysisTask[] NO_POST_PROJECT_ANALYSIS_TASKS = new PostProjectAnalysisTask[0];

  private static final Logger LOG = Loggers.get(PostProjectAnalysisTasksExecutor.class);

  private final org.sonar.ce.task.CeTask ceTask;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final QualityGateHolder qualityGateHolder;
  private final QualityGateStatusHolder qualityGateStatusHolder;
  private final PostProjectAnalysisTask[] postProjectAnalysisTasks;
  private final BatchReportReader reportReader;
  private final System2 system2;

  /**
   * Constructor used by Pico when there is no {@link PostProjectAnalysisTask} in the container.
   */
  public PostProjectAnalysisTasksExecutor(org.sonar.ce.task.CeTask ceTask,
    AnalysisMetadataHolder analysisMetadataHolder,
    QualityGateHolder qualityGateHolder, QualityGateStatusHolder qualityGateStatusHolder,
    BatchReportReader reportReader, System2 system2) {
    this(ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder, reportReader, system2, null);
  }

  public PostProjectAnalysisTasksExecutor(org.sonar.ce.task.CeTask ceTask,
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

    ProjectAnalysisImpl projectAnalysis = createProjectAnalysis(allStepsExecuted ? SUCCESS : FAILED);
    for (PostProjectAnalysisTask postProjectAnalysisTask : postProjectAnalysisTasks) {
      executeTask(projectAnalysis, postProjectAnalysisTask);
    }
  }

  private static void executeTask(ProjectAnalysisImpl projectAnalysis, PostProjectAnalysisTask postProjectAnalysisTask) {
    String status = "FAILED";
    Profiler task = Profiler.create(LOG).logTimeLast(true);
    try {
      task.start();
      postProjectAnalysisTask.finished(new ContextImpl(projectAnalysis, task));
      status = "SUCCESS";
    } catch (Exception e) {
      LOG.error("Execution of task " + postProjectAnalysisTask.getClass() + " failed", e);
    } finally {
      task.addContext("status", status);
      task.stopInfo("{}", postProjectAnalysisTask.getDescription());
    }
  }

  private static class ContextImpl implements PostProjectAnalysisTask.Context {
    private final ProjectAnalysisImpl projectAnalysis;
    private final Profiler task;

    private ContextImpl(ProjectAnalysisImpl projectAnalysis, Profiler task) {
      this.projectAnalysis = projectAnalysis;
      this.task = task;
    }

    @Override
    public PostProjectAnalysisTask.ProjectAnalysis getProjectAnalysis() {
      return projectAnalysis;
    }

    @Override
    public PostProjectAnalysisTask.LogStatistics getLogStatistics() {
      return new LogStatisticsImpl(task);
    }
  }

  private static class LogStatisticsImpl implements PostProjectAnalysisTask.LogStatistics {
    private final Profiler profiler;

    private LogStatisticsImpl(Profiler profiler) {
      this.profiler = profiler;
    }

    @Override
    public PostProjectAnalysisTask.LogStatistics add(String key, Object value) {
      requireNonNull(key, "Statistic has null key");
      requireNonNull(value, () -> format("Statistic with key [%s] has null value", key));
      checkArgument(!key.equalsIgnoreCase("time") && !key.equalsIgnoreCase("status"),
        "Statistic with key [%s] is not accepted", key);
      checkArgument(!profiler.hasContext(key), "Statistic with key [%s] is already present", key);
      profiler.addContext(key, value);
      return this;
    }
  }

  private ProjectAnalysisImpl createProjectAnalysis(CeTask.Status status) {
    return new ProjectAnalysisImpl(
      createOrganization(),
      new CeTaskImpl(this.ceTask.getUuid(), status),
      createProject(this.ceTask),
      getAnalysis().orElse(null),
      getAnalysis().map(a -> a.getDate().getTime()).orElse(system2.now()),
      ScannerContextImpl.from(reportReader.readContextProperties()),
      status == SUCCESS ? createQualityGate() : null,
      createBranch(),
      reportReader.readMetadata().getScmRevisionId());
  }

  @CheckForNull
  private Organization createOrganization() {
    if (!analysisMetadataHolder.isOrganizationsEnabled()) {
      return null;
    }
    org.sonar.ce.task.projectanalysis.analysis.Organization organization = analysisMetadataHolder.getOrganization();
    return new OrganizationImpl(organization.getName(), organization.getKey());
  }

  private Optional<Analysis> getAnalysis() {
    Long analysisDate = getAnalysisDate();

    if (analysisDate != null) {
      return of(new AnalysisImpl(analysisMetadataHolder.getUuid(), analysisDate, analysisMetadataHolder.getScmRevision()));
    }
    return empty();
  }

  private static Project createProject(org.sonar.ce.task.CeTask ceTask) {
    return ceTask.getMainComponent()
      .map(c -> new ProjectImpl(
        c.getUuid(),
        c.getKey().orElseThrow(() -> new IllegalStateException("Missing project key")),
        c.getName().orElseThrow(() -> new IllegalStateException("Missing project name"))))
      .orElseThrow(() -> new IllegalStateException("Report processed for a task of a deleted component"));
  }

  @CheckForNull
  private Long getAnalysisDate() {
    if (this.analysisMetadataHolder.hasAnalysisDateBeenSet()) {
      return this.analysisMetadataHolder.getAnalysisDate();
    }
    return null;
  }

  @CheckForNull
  private QualityGate createQualityGate() {
    Optional<org.sonar.ce.task.projectanalysis.qualitygate.QualityGate> qualityGateOptional = this.qualityGateHolder.getQualityGate();
    if (qualityGateOptional.isPresent()) {
      org.sonar.ce.task.projectanalysis.qualitygate.QualityGate qualityGate = qualityGateOptional.get();

      return new QualityGateImpl(
        String.valueOf(qualityGate.getId()),
        qualityGate.getName(),
        convert(qualityGateStatusHolder.getStatus()),
        convert(qualityGate.getConditions(), qualityGateStatusHolder.getStatusPerConditions()));
    }
    return null;
  }

  @CheckForNull
  private BranchImpl createBranch() {
    org.sonar.ce.task.projectanalysis.analysis.Branch analysisBranch = analysisMetadataHolder.getBranch();
    String branchKey = analysisBranch.getType() == PULL_REQUEST ? analysisBranch.getPullRequestKey() : analysisBranch.getName();
    return new BranchImpl(analysisBranch.isMain(), branchKey, Branch.Type.valueOf(analysisBranch.getType().name()));
  }

  private static QualityGate.Status convert(QualityGateStatus status) {
    switch (status) {
      case OK:
        return QualityGate.Status.OK;
      case ERROR:
        return QualityGate.Status.ERROR;
      default:
        throw new IllegalArgumentException(format(
          "Unsupported value '%s' of QualityGateStatus can not be converted to QualityGate.Status",
          status));
    }
  }

  private static Collection<QualityGate.Condition> convert(Set<Condition> conditions, Map<Condition, ConditionStatus> statusPerConditions) {
    return conditions.stream()
      .map(new ConditionToCondition(statusPerConditions)::apply)
      .collect(MoreCollectors.toList(statusPerConditions.size()));
  }

  private static class ProjectAnalysisImpl implements PostProjectAnalysisTask.ProjectAnalysis {
    @Nullable
    private final Organization organization;
    private final CeTask ceTask;
    private final Project project;
    private final long date;
    private final ScannerContext scannerContext;
    @Nullable
    private final QualityGate qualityGate;
    @Nullable
    private final Branch branch;
    @Nullable
    private final Analysis analysis;
    private final String scmRevisionId;

    private ProjectAnalysisImpl(@Nullable Organization organization, CeTask ceTask, Project project,
      @Nullable Analysis analysis, long date,
      ScannerContext scannerContext, @Nullable QualityGate qualityGate, @Nullable Branch branch, String scmRevisionId) {
      this.organization = organization;
      this.ceTask = requireNonNull(ceTask, "ceTask can not be null");
      this.project = requireNonNull(project, "project can not be null");
      this.analysis = analysis;
      this.date = date;
      this.scannerContext = requireNonNull(scannerContext, "scannerContext can not be null");
      this.qualityGate = qualityGate;
      this.branch = branch;
      this.scmRevisionId = scmRevisionId;
    }

    @Override
    public Optional<Organization> getOrganization() {
      return Optional.ofNullable(organization);
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
    public Optional<Branch> getBranch() {
      return ofNullable(branch);
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
    public Optional<Date> getAnalysisDate() {
      return analysis == null ? empty() : ofNullable(analysis.getDate());
    }

    @Override
    public Optional<Analysis> getAnalysis() {
      return ofNullable(analysis);
    }

    @Override
    public ScannerContext getScannerContext() {
      return scannerContext;
    }

    @Override
    public String getScmRevisionId() {
      return scmRevisionId;
    }

    @Override
    public String toString() {
      return "ProjectAnalysis{" +
        "ceTask=" + ceTask +
        ", project=" + project +
        ", date=" + date +
        ", scannerContext=" + scannerContext +
        ", qualityGate=" + qualityGate +
        ", analysis=" + analysis +
        '}';
    }
  }

  private static class AnalysisImpl implements Analysis {

    private final String analysisUuid;
    private final long date;
    private final Optional<String> revision;

    private AnalysisImpl(String analysisUuid, long date, Optional<String> revision) {
      this.analysisUuid = analysisUuid;
      this.date = date;
      this.revision = revision;
    }

    @Override
    public String getAnalysisUuid() {
      return analysisUuid;
    }

    @Override
    public Date getDate() {
      return new Date(date);
    }

    @Override
    public Optional<String> getRevision() {
      return revision;
    }
  }

  private static class OrganizationImpl implements Organization {
    private final String name;
    private final String key;

    private OrganizationImpl(String name, String key) {
      this.name = requireNonNull(name, "name can't be null");
      this.key = requireNonNull(key, "key can't be null");
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getKey() {
      return key;
    }
  }
}
