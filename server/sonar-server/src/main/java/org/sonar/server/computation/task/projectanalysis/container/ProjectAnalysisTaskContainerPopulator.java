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
package org.sonar.server.computation.task.projectanalysis.container;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.ce.organization.DefaultOrganizationLoader;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.settings.SettingsLoader;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.platform.ContainerPopulator;
import org.sonar.plugin.ce.ReportAnalysisComponentProvider;
import org.sonar.server.computation.task.container.TaskContainer;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderImpl;
import org.sonar.server.computation.task.projectanalysis.api.posttask.PostProjectAnalysisTasksExecutor;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportDirectoryHolderImpl;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderImpl;
import org.sonar.server.computation.task.projectanalysis.component.BranchLoader;
import org.sonar.server.computation.task.projectanalysis.component.BranchPersisterImpl;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.component.DisabledComponentsHolderImpl;
import org.sonar.server.computation.task.projectanalysis.component.MergeBranchComponentUuids;
import org.sonar.server.computation.task.projectanalysis.component.ShortBranchComponentsWithIssues;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderImpl;
import org.sonar.server.computation.task.projectanalysis.duplication.CrossProjectDuplicationStatusHolderImpl;
import org.sonar.server.computation.task.projectanalysis.duplication.DuplicationMeasures;
import org.sonar.server.computation.task.projectanalysis.duplication.DuplicationRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.duplication.IntegrateCrossProjectDuplications;
import org.sonar.server.computation.task.projectanalysis.event.EventRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.filemove.FileSimilarityImpl;
import org.sonar.server.computation.task.projectanalysis.filemove.MutableMovedFilesRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.filemove.SourceSimilarityImpl;
import org.sonar.server.computation.task.projectanalysis.filesystem.ComputationTempFolderProvider;
import org.sonar.server.computation.task.projectanalysis.issue.BaseIssuesLoader;
import org.sonar.server.computation.task.projectanalysis.issue.CloseIssuesOnRemovedComponentsVisitor;
import org.sonar.server.computation.task.projectanalysis.issue.ComponentIssuesLoader;
import org.sonar.server.computation.task.projectanalysis.issue.ComponentIssuesRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.issue.ComponentsWithUnprocessedIssues;
import org.sonar.server.computation.task.projectanalysis.issue.DebtCalculator;
import org.sonar.server.computation.task.projectanalysis.issue.DefaultAssignee;
import org.sonar.server.computation.task.projectanalysis.issue.EffortAggregator;
import org.sonar.server.computation.task.projectanalysis.issue.IntegrateIssuesVisitor;
import org.sonar.server.computation.task.projectanalysis.issue.IssueAssigner;
import org.sonar.server.computation.task.projectanalysis.issue.IssueCache;
import org.sonar.server.computation.task.projectanalysis.issue.IssueCounter;
import org.sonar.server.computation.task.projectanalysis.issue.IssueCreationDateCalculator;
import org.sonar.server.computation.task.projectanalysis.issue.IssueLifecycle;
import org.sonar.server.computation.task.projectanalysis.issue.IssueTrackingDelegator;
import org.sonar.server.computation.task.projectanalysis.issue.IssueVisitors;
import org.sonar.server.computation.task.projectanalysis.issue.IssuesRepositoryVisitor;
import org.sonar.server.computation.task.projectanalysis.issue.LoadComponentUuidsHavingOpenIssuesVisitor;
import org.sonar.server.computation.task.projectanalysis.issue.MergeBranchTrackerExecution;
import org.sonar.server.computation.task.projectanalysis.issue.MovedIssueVisitor;
import org.sonar.server.computation.task.projectanalysis.issue.NewEffortAggregator;
import org.sonar.server.computation.task.projectanalysis.issue.RemoveProcessedComponentsVisitor;
import org.sonar.server.computation.task.projectanalysis.issue.RuleRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.issue.RuleTagsCopier;
import org.sonar.server.computation.task.projectanalysis.issue.RuleTypeCopier;
import org.sonar.server.computation.task.projectanalysis.issue.ScmAccountToUser;
import org.sonar.server.computation.task.projectanalysis.issue.ScmAccountToUserLoader;
import org.sonar.server.computation.task.projectanalysis.issue.ShortBranchIssueMerger;
import org.sonar.server.computation.task.projectanalysis.issue.ShortBranchIssuesLoader;
import org.sonar.server.computation.task.projectanalysis.issue.ShortBranchTrackerExecution;
import org.sonar.server.computation.task.projectanalysis.issue.TrackerBaseInputFactory;
import org.sonar.server.computation.task.projectanalysis.issue.TrackerExecution;
import org.sonar.server.computation.task.projectanalysis.issue.TrackerMergeBranchInputFactory;
import org.sonar.server.computation.task.projectanalysis.issue.TrackerRawInputFactory;
import org.sonar.server.computation.task.projectanalysis.issue.UpdateConflictResolver;
import org.sonar.server.computation.task.projectanalysis.issue.commonrule.BranchCoverageRule;
import org.sonar.server.computation.task.projectanalysis.issue.commonrule.CommentDensityRule;
import org.sonar.server.computation.task.projectanalysis.issue.commonrule.CommonRuleEngineImpl;
import org.sonar.server.computation.task.projectanalysis.issue.commonrule.DuplicatedBlockRule;
import org.sonar.server.computation.task.projectanalysis.issue.commonrule.LineCoverageRule;
import org.sonar.server.computation.task.projectanalysis.issue.commonrule.SkippedTestRule;
import org.sonar.server.computation.task.projectanalysis.issue.commonrule.TestErrorRule;
import org.sonar.server.computation.task.projectanalysis.issue.filter.IssueFilter;
import org.sonar.server.computation.task.projectanalysis.language.LanguageRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureComputersHolderImpl;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureComputersVisitor;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.server.computation.task.projectanalysis.metric.MetricModule;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderImpl;
import org.sonar.server.computation.task.projectanalysis.qualitygate.EvaluationResultTextConverterImpl;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateHolderImpl;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateServiceImpl;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateStatusHolderImpl;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.MaintainabilityMeasuresVisitor;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.NewMaintainabilityMeasuresVisitor;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.NewReliabilityAndSecurityRatingMeasuresVisitor;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingSettings;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.ReliabilityAndSecurityRatingMeasuresVisitor;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRulesHolderImpl;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoDbLoader;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.source.LastCommitVisitor;
import org.sonar.server.computation.task.projectanalysis.source.SourceHashRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.source.SourceLinesDiffImpl;
import org.sonar.server.computation.task.projectanalysis.source.SourceLinesRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.step.ReportComputationSteps;
import org.sonar.server.computation.task.projectanalysis.step.SmallChangesetQualityGateSpecialCase;
import org.sonar.server.computation.task.projectanalysis.webhook.WebhookPostTask;
import org.sonar.server.computation.task.step.ComputationStepExecutor;
import org.sonar.server.computation.task.step.ComputationSteps;
import org.sonar.server.computation.taskprocessor.MutableTaskResultHolderImpl;
import org.sonar.server.view.index.ViewIndex;

public final class ProjectAnalysisTaskContainerPopulator implements ContainerPopulator<TaskContainer> {
  private static final ReportAnalysisComponentProvider[] NO_REPORT_ANALYSIS_COMPONENT_PROVIDERS = new ReportAnalysisComponentProvider[0];

  private final CeTask task;
  private final ReportAnalysisComponentProvider[] componentProviders;

  public ProjectAnalysisTaskContainerPopulator(CeTask task, @Nullable ReportAnalysisComponentProvider[] componentProviders) {
    this.task = task;
    this.componentProviders = componentProviders == null ? NO_REPORT_ANALYSIS_COMPONENT_PROVIDERS : componentProviders;
  }

  @Override
  public void populateContainer(TaskContainer container) {
    ComputationSteps steps = new ReportComputationSteps(container);
    container.add(SettingsLoader.class);
    container.add(DefaultOrganizationLoader.class);
    container.add(task);
    container.add(steps);
    container.addSingletons(componentClasses());
    for (ReportAnalysisComponentProvider componentProvider : componentProviders) {
      container.addSingletons(componentProvider.getComponents());
    }
    container.addSingletons(steps.orderedStepClasses());
  }

  /**
   * List of all objects to be injected in the picocontainer dedicated to computation stack.
   * Does not contain the steps declared in {@link ReportComputationSteps#orderedStepClasses()}.
   */
  private static List<Object> componentClasses() {
    return Arrays.asList(
      PostProjectAnalysisTasksExecutor.class,
      ComputationStepExecutor.class,

      // File System
      new ComputationTempFolderProvider(),

      MetricModule.class,

      // holders
      AnalysisMetadataHolderImpl.class,
      CrossProjectDuplicationStatusHolderImpl.class,
      BatchReportDirectoryHolderImpl.class,
      TreeRootHolderImpl.class,
      PeriodHolderImpl.class,
      QualityGateHolderImpl.class,
      QualityGateStatusHolderImpl.class,
      RatingSettings.class,
      ActiveRulesHolderImpl.class,
      MeasureComputersHolderImpl.class,
      MutableTaskResultHolderImpl.class,
      BatchReportReaderImpl.class,
      MergeBranchComponentUuids.class,
      ShortBranchComponentsWithIssues.class,

      // repositories
      LanguageRepositoryImpl.class,
      MeasureRepositoryImpl.class,
      EventRepositoryImpl.class,
      ConfigurationRepositoryImpl.class,
      DbIdsRepositoryImpl.class,
      DisabledComponentsHolderImpl.class,
      QualityGateServiceImpl.class,
      EvaluationResultTextConverterImpl.class,
      SourceLinesRepositoryImpl.class,
      SourceHashRepositoryImpl.class,
      SourceLinesDiffImpl.class,
      ScmInfoRepositoryImpl.class,
      ScmInfoDbLoader.class,
      DuplicationRepositoryImpl.class,

      // issues
      RuleRepositoryImpl.class,
      ScmAccountToUserLoader.class,
      ScmAccountToUser.class,
      IssueCache.class,
      DefaultAssignee.class,
      IssueVisitors.class,
      IssueLifecycle.class,
      ComponentsWithUnprocessedIssues.class,
      ComponentIssuesRepositoryImpl.class,
      IssueFilter.class,

      // common rules
      CommonRuleEngineImpl.class,
      BranchCoverageRule.class,
      LineCoverageRule.class,
      CommentDensityRule.class,
      DuplicatedBlockRule.class,
      TestErrorRule.class,
      SkippedTestRule.class,

      // order is important: RuleTypeCopier must be the first one. And DebtAggregator must be before NewDebtAggregator (new debt requires
      // debt)
      RuleTypeCopier.class,
      RuleTagsCopier.class,
      IssueCreationDateCalculator.class,
      DebtCalculator.class,
      EffortAggregator.class,
      NewEffortAggregator.class,
      IssueAssigner.class,
      IssueCounter.class,
      MovedIssueVisitor.class,
      IssuesRepositoryVisitor.class,
      RemoveProcessedComponentsVisitor.class,

      // visitors : order is important, measure computers must be executed at the end in order to access to every measures / issues
      LoadComponentUuidsHavingOpenIssuesVisitor.class,
      IntegrateIssuesVisitor.class,
      CloseIssuesOnRemovedComponentsVisitor.class,
      MaintainabilityMeasuresVisitor.class,
      NewMaintainabilityMeasuresVisitor.class,
      ReliabilityAndSecurityRatingMeasuresVisitor.class,
      NewReliabilityAndSecurityRatingMeasuresVisitor.class,
      LastCommitVisitor.class,
      MeasureComputersVisitor.class,

      UpdateConflictResolver.class,
      TrackerBaseInputFactory.class,
      TrackerRawInputFactory.class,
      TrackerMergeBranchInputFactory.class,
      Tracker.class,
      TrackerExecution.class,
      ShortBranchTrackerExecution.class,
      MergeBranchTrackerExecution.class,
      ComponentIssuesLoader.class,
      BaseIssuesLoader.class,
      IssueTrackingDelegator.class,
      BranchPersisterImpl.class,
      ShortBranchIssuesLoader.class,
      ShortBranchIssueMerger.class,

      // filemove
      SourceSimilarityImpl.class,
      FileSimilarityImpl.class,
      MutableMovedFilesRepositoryImpl.class,

      // duplication
      IntegrateCrossProjectDuplications.class,
      DuplicationMeasures.class,

      // views
      ViewIndex.class,

      BranchLoader.class,
      MeasureToMeasureDto.class,
      SmallChangesetQualityGateSpecialCase.class,

      // webhooks
      WebhookPostTask.class);
  }

}
