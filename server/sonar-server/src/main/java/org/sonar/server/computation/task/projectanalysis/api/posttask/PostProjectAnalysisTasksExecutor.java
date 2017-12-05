/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.qualitygate.Condition;
import org.sonar.server.computation.task.projectanalysis.qualitygate.ConditionStatus;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateHolder;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateStatus;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateStatusHolder;
import org.sonar.server.computation.task.step.ComputationStepExecutor;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
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

    ProjectAnalysisImpl projectAnalysis = createProjectAnalysis(allStepsExecuted ? SUCCESS : FAILED);
    for (PostProjectAnalysisTask postProjectAnalysisTask : postProjectAnalysisTasks) {
      executeTask(projectAnalysis, postProjectAnalysisTask);
    }
  }

  private static void executeTask(ProjectAnalysisImpl projectAnalysis, PostProjectAnalysisTask postProjectAnalysisTask) {
    try {
      postProjectAnalysisTask.finished(projectAnalysis);
    } catch (Exception e) {
      LOG.error("Execution of task " + postProjectAnalysisTask.getClass() + " failed", e);
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
      createBranch());
  }

  @CheckForNull
  private Organization createOrganization() {
    if (!analysisMetadataHolder.isOrganizationsEnabled()) {
      return null;
    }
    org.sonar.server.computation.task.projectanalysis.analysis.Organization organization = analysisMetadataHolder.getOrganization();
    return new OrganizationImpl(organization.getName(), organization.getKey());
  }

  private Optional<Analysis> getAnalysis() {
    Long analysisDate = getAnalysisDate();

    if (analysisDate != null) {
      return of(new AnalysisImpl(analysisMetadataHolder.getUuid(), analysisDate));
    } else {
      return empty();
    }
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
  private QualityGateImpl createQualityGate() {
    Optional<org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGate> qualityGateOptional = this.qualityGateHolder.getQualityGate();
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

  @CheckForNull
  private BranchImpl createBranch() {
    org.sonar.server.computation.task.projectanalysis.analysis.Branch analysisBranch = analysisMetadataHolder.getBranch();
    if (!analysisBranch.isLegacyFeature()) {
      return new BranchImpl(analysisBranch.isMain(), analysisBranch.getName(), Branch.Type.valueOf(analysisBranch.getType().name()));
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

    private ProjectAnalysisImpl(@Nullable Organization organization, CeTask ceTask, Project project,
      @Nullable Analysis analysis, long date,
      ScannerContext scannerContext, @Nullable QualityGate qualityGate, @Nullable Branch branch) {
      this.organization = organization;
      this.ceTask = requireNonNull(ceTask, "ceTask can not be null");
      this.project = requireNonNull(project, "project can not be null");
      this.analysis = analysis;
      this.date = date;
      this.scannerContext = requireNonNull(scannerContext, "scannerContext can not be null");
      this.qualityGate = qualityGate;
      this.branch = branch;
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

    private AnalysisImpl(String analysisUuid, long date) {
      this.analysisUuid = analysisUuid;
      this.date = date;
    }

    @Override
    public String getAnalysisUuid() {
      return analysisUuid;
    }

    @Override
    public Date getDate() {
      return new Date(date);
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
