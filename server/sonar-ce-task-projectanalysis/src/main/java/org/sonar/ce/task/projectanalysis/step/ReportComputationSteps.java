/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.projectanalysis.dependency.PersistProjectDependenciesStep;
import org.sonar.ce.task.projectanalysis.filemove.FileMoveDetectionStep;
import org.sonar.ce.task.projectanalysis.filemove.PullRequestFileMoveDetectionStep;
import org.sonar.ce.task.projectanalysis.language.HandleUnanalyzedLanguagesStep;
import org.sonar.ce.task.projectanalysis.measure.PostMeasuresComputationChecksStep;
import org.sonar.ce.task.projectanalysis.measure.PreMeasuresComputationChecksStep;
import org.sonar.ce.task.projectanalysis.purge.PurgeDatastoresStep;
import org.sonar.ce.task.projectanalysis.qualityprofile.RegisterQualityProfileStatusStep;
import org.sonar.ce.task.projectanalysis.source.PersistFileSourcesStep;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.ExecuteStatelessInitExtensionsStep;
import org.sonar.ce.task.step.ExecuteStatelessOnFinishStep;

/**
 * Ordered list of steps classes and instances to be executed for batch processing
 */
public class ReportComputationSteps extends AbstractComputationSteps {

  private static final List<Class<? extends ComputationStep>> STEPS = Arrays.asList(
    ExtractReportStep.class,
    PersistScannerContextStep.class,
    PersistAnalysisWarningsStep.class,
    GenerateAnalysisUuid.class,

    // Builds Component tree
    LoadReportAnalysisMetadataHolderStep.class,
    ExecuteStatelessInitExtensionsStep.class,
    BuildComponentTreeStep.class,
    ValidateProjectStep.class,
    LoadQualityProfilesStep.class,

    SendAnalysisTelemetryStep.class,

    // Dependencies
    BuildProjectDependenciesStep.class,

    // Pre analysis operations
    PreMeasuresComputationChecksStep.class,
    SqUpgradeDetectionEventsStep.class,

    // load project related stuffs
    LoadFileHashesAndStatusStep.class,
    LoadQualityGateStep.class,
    LoadPeriodsStep.class,
    LoadPrioritizedRulesStep.class,
    FileMoveDetectionStep.class,
    PullRequestFileMoveDetectionStep.class,

    // load duplications related stuff
    LoadDuplicationsFromReportStep.class,
    LoadCrossProjectDuplicationsRepositoryStep.class,

    // data computation
    SizeMeasuresStep.class,
    NewCoverageMeasuresStep.class,
    CoverageMeasuresStep.class,
    CommentMeasuresStep.class,
    DuplicationMeasuresStep.class,
    NewSizeMeasuresStep.class,
    LanguageDistributionMeasuresStep.class,
    UnitTestMeasuresStep.class,
    ComplexityMeasuresStep.class,

    LoadMeasureComputersStep.class,
    RegisterQualityProfileStatusStep.class,
    ExecuteVisitorsStep.class,

    PostMeasuresComputationChecksStep.class,

    // Must be executed after visitors execution
    PullRequestFixedIssuesMeasureStep.class,

    QualityGateMeasuresStep.class,
    // Must be executed after computation of language distribution
    ComputeQProfileMeasureStep.class,
    // Must be executed after computation of quality profile measure
    QualityProfileEventsStep.class,

    // Must be executed after computation of quality gate measure
    QualityGateEventsStep.class,
    IssueDetectionEventsStep.class,

    HandleUnanalyzedLanguagesStep.class,

    // Persist data
    PersistScannerAnalysisCacheStep.class,
    PersistComponentsStep.class,
    PersistProjectDependenciesStep.class,
    PersistAnalysisStep.class,
    PersistAnalysisPropertiesStep.class,
    PersistProjectMeasuresStep.class,
    PersistMeasuresStep.class,
    PersistAdHocRulesStep.class,
    PersistIssuesStep.class,
    CleanIssueChangesStep.class,
    PersistProjectLinksStep.class,
    PersistEventsStep.class,
    PersistFileSourcesStep.class,
    PersistCrossProjectDuplicationIndexStep.class,
    EnableAnalysisStep.class,
    PersistPullRequestFixedIssueStep.class,

    UpdateQualityProfilesLastUsedDateStep.class,
    PurgeDatastoresStep.class,
    LoadChangedIssuesStep.class,
    IndexAnalysisStep.class,
    UpdateNeedIssueSyncStep.class,
    ProjectNclocComputationStep.class,
    PersistPushEventsStep.class,

    // notifications are sent at the end, so that webapp displays up-to-date information
    SendIssueNotificationsStep.class,

    ExecuteStatelessOnFinishStep.class,
    PublishTaskResultStep.class,
    TriggerViewRefreshStep.class);

  public ReportComputationSteps(TaskContainer taskContainer) {
    super(taskContainer);
  }

  /**
   * List of all {@link ComputationStep},
   * ordered by execution sequence.
   */
  @Override
  public List<Class<? extends ComputationStep>> orderedStepClasses() {
    return STEPS;
  }

}
