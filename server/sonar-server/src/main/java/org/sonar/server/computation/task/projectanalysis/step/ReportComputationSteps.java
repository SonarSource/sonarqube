/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.base.Predicate;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import org.picocontainer.ComponentAdapter;
import org.sonar.server.computation.task.container.TaskContainer;
import org.sonar.server.computation.task.projectanalysis.api.developer.PersistDevelopersDelegate;
import org.sonar.server.computation.task.projectanalysis.filemove.FileMoveDetectionStep;
import org.sonar.server.computation.task.step.ComputationStep;

import static com.google.common.collect.FluentIterable.from;

/**
 * Ordered list of steps classes and instances to be executed for batch processing
 */
public class ReportComputationSteps extends AbstractComputationSteps {

  private static final List<Class<? extends ComputationStep>> STEPS = Arrays.asList(
    ExtractReportStep.class,
    PersistScannerContextStep.class,
    GenerateAnalysisUuid.class,

    // Builds Component tree
    LoadReportAnalysisMetadataHolderStep.class,
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
    DuplicationDataMeasuresStep.class,
    NewSizeMeasuresStep.class,
    LanguageDistributionMeasuresStep.class,
    UnitTestMeasuresStep.class,
    ComplexityMeasuresStep.class,

    LoadMeasureComputersStep.class,
    ExecuteVisitorsStep.class,

    // Must be executed after computation of all measures
    ComputeMeasureVariationsStep.class,

    // Must be executed after computation of differential measures
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
    PersistDevelopersStep.class,
    PersistMeasuresStep.class,
    PersistIssuesStep.class,
    PersistProjectLinksStep.class,
    PersistEventsStep.class,
    PersistFileSourcesStep.class,
    PersistTestsStep.class,
    PersistCrossProjectDuplicationIndexStep.class,
    EnableAnalysisStep.class,

    UpdateQualityProfilesLastUsedDateStep.class,
    IndexComponentsStep.class,
    PurgeDatastoresStep.class,
    ApplyPermissionsStep.class,

    // ES indexing is done after all db changes
    IndexIssuesStep.class,
    IndexTestsStep.class,

    // notifications are sent at the end, so that webapp displays up-to-date information
    SendIssueNotificationsStep.class,

    PublishTaskResultStep.class);

  private final TaskContainer taskContainer;

  public ReportComputationSteps(TaskContainer taskContainer) {
    super(taskContainer);
    this.taskContainer = taskContainer;
  }

  /**
   * List of all {@link ComputationStep},
   * ordered by execution sequence.
   */
  @Override
  public List<Class<? extends ComputationStep>> orderedStepClasses() {
    return from(STEPS)
      .filter(new AllowPersistDevelopersStepIfDevCockpitPluginInstalled(taskContainer))
      .toList();
  }

  private static class AllowPersistDevelopersStepIfDevCockpitPluginInstalled implements Predicate<Class<? extends ComputationStep>> {
    private final boolean devCockpitIsInstalled;

    private AllowPersistDevelopersStepIfDevCockpitPluginInstalled(TaskContainer taskContainer) {
      this.devCockpitIsInstalled = isDevCockpitInstalled(taskContainer);
    }

    private static boolean isDevCockpitInstalled(TaskContainer taskContainer) {
      List<ComponentAdapter<PersistDevelopersDelegate>> componentAdapters = taskContainer.getPicoContainer().getComponentAdapters(PersistDevelopersDelegate.class);
      return !componentAdapters.isEmpty();
    }

    @Override
    public boolean apply(@Nonnull Class<? extends ComputationStep> input) {
      return devCockpitIsInstalled || !input.equals(PersistDevelopersStep.class);
    }
  }

}
