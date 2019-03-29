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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Arrays;
import java.util.List;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.projectanalysis.filemove.FileMoveDetectionStep;
import org.sonar.ce.task.projectanalysis.measure.PostMeasuresComputationChecksStep;
import org.sonar.ce.task.projectanalysis.purge.PurgeDatastoresStep;
import org.sonar.ce.task.projectanalysis.qualityprofile.RegisterQualityProfileStatusStep;
import org.sonar.ce.task.projectanalysis.source.PersistFileSourcesStep;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.ExecuteStatelessInitExtensionsStep;

/**
 * Ordered list of steps classes and instances to be executed for batch processing
 */
public class ReportComputationSteps extends AbstractComputationSteps {

  private static final List<Class<? extends ComputationStep>> STEPS = Arrays.asList(
    ExtractReportStep.class,
    PersistScannerContextStep.class,
    PersistAnalysisWarningsStep.class,
    DbMigrationsStep.class,
    GenerateAnalysisUuid.class,

    // Builds Component tree
    LoadReportAnalysisMetadataHolderStep.class,
    ExecuteStatelessInitExtensionsStep.class,
    VerifyBillingStep.class,
    BuildComponentTreeStep.class,
    ValidateProjectStep.class,

    LoadQualityProfilesStep.class,

    // load project related stuffs
    LoadQualityGateStep.class,
    LoadPeriodsStep.class,
    FileMoveDetectionStep.class,

    // load duplications related stuff
    LoadDuplicationsFromReportStep.class,
    LoadCrossProjectDuplicationsRepositoryStep.class,

    // data computation
    SizeMeasuresStep.class,
    NewCoverageMeasuresStep.class,
    CoverageMeasuresStep.class,
    CommentMeasuresStep.class,
    CustomMeasuresCopyStep.class,
    DuplicationMeasuresStep.class,
    NewSizeMeasuresStep.class,
    LanguageDistributionMeasuresStep.class,
    UnitTestMeasuresStep.class,
    ComplexityMeasuresStep.class,

    LoadMeasureComputersStep.class,
    RegisterQualityProfileStatusStep.class,
    ExecuteVisitorsStep.class,

    PostMeasuresComputationChecksStep.class,

    QualityGateMeasuresStep.class,
    // Must be executed after computation of language distribution
    ComputeQProfileMeasureStep.class,
    // Must be executed after computation of quality profile measure
    QualityProfileEventsStep.class,

    // Must be executed after computation of quality gate measure
    QualityGateEventsStep.class,

    // Persist data
    PersistComponentsStep.class,
    PersistAnalysisStep.class,
    PersistAnalysisPropertiesStep.class,
    PersistMeasuresStep.class,
    PersistLiveMeasuresStep.class,
    PersistDuplicationDataStep.class,
    PersistAdHocRulesStep.class,
    PersistIssuesStep.class,
    PersistProjectLinksStep.class,
    PersistEventsStep.class,
    PersistFileSourcesStep.class,
    PersistCrossProjectDuplicationIndexStep.class,
    EnableAnalysisStep.class,

    UpdateQualityProfilesLastUsedDateStep.class,
    PurgeDatastoresStep.class,
    IndexAnalysisStep.class,

    // notifications are sent at the end, so that webapp displays up-to-date information
    SendIssueNotificationsStep.class,

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
