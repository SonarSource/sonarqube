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
package org.sonar.ce.task.projectanalysis.container;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.log.CeTaskMessagesImpl;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderImpl;
import org.sonar.ce.task.projectanalysis.api.posttask.PostProjectAnalysisTasksExecutor;
import org.sonar.ce.task.projectanalysis.batch.BatchReportDirectoryHolderImpl;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderImpl;
import org.sonar.ce.task.projectanalysis.component.BranchLoader;
import org.sonar.ce.task.projectanalysis.component.BranchPersisterImpl;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepositoryImpl;
import org.sonar.ce.task.projectanalysis.component.DbIdsRepositoryImpl;
import org.sonar.ce.task.projectanalysis.component.DisabledComponentsHolderImpl;
import org.sonar.ce.task.projectanalysis.component.MergeBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.component.ReportModulesPath;
import org.sonar.ce.task.projectanalysis.component.ShortBranchComponentsWithIssues;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderImpl;
import org.sonar.ce.task.projectanalysis.dbmigration.DbMigrationModule;
import org.sonar.ce.task.projectanalysis.duplication.CrossProjectDuplicationStatusHolderImpl;
import org.sonar.ce.task.projectanalysis.duplication.DuplicationMeasures;
import org.sonar.ce.task.projectanalysis.duplication.DuplicationRepositoryImpl;
import org.sonar.ce.task.projectanalysis.duplication.IntegrateCrossProjectDuplications;
import org.sonar.ce.task.projectanalysis.event.EventRepositoryImpl;
import org.sonar.ce.task.projectanalysis.filemove.AddedFileRepositoryImpl;
import org.sonar.ce.task.projectanalysis.filemove.FileSimilarityImpl;
import org.sonar.ce.task.projectanalysis.filemove.MutableMovedFilesRepositoryImpl;
import org.sonar.ce.task.projectanalysis.filemove.ScoreMatrixDumperImpl;
import org.sonar.ce.task.projectanalysis.filemove.SourceSimilarityImpl;
import org.sonar.ce.task.projectanalysis.filesystem.ComputationTempFolderProvider;
import org.sonar.ce.task.projectanalysis.issue.BaseIssuesLoader;
import org.sonar.ce.task.projectanalysis.issue.CloseIssuesOnRemovedComponentsVisitor;
import org.sonar.ce.task.projectanalysis.issue.ClosedIssuesInputFactory;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesLoader;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepositoryImpl;
import org.sonar.ce.task.projectanalysis.issue.ComponentsWithUnprocessedIssues;
import org.sonar.ce.task.projectanalysis.issue.DebtCalculator;
import org.sonar.ce.task.projectanalysis.issue.DefaultAssignee;
import org.sonar.ce.task.projectanalysis.issue.EffortAggregator;
import org.sonar.ce.task.projectanalysis.issue.IntegrateIssuesVisitor;
import org.sonar.ce.task.projectanalysis.issue.IssueAssigner;
import org.sonar.ce.task.projectanalysis.issue.IssueCache;
import org.sonar.ce.task.projectanalysis.issue.IssueCounter;
import org.sonar.ce.task.projectanalysis.issue.IssueCreationDateCalculator;
import org.sonar.ce.task.projectanalysis.issue.IssueLifecycle;
import org.sonar.ce.task.projectanalysis.issue.IssueTrackingDelegator;
import org.sonar.ce.task.projectanalysis.issue.IssueVisitors;
import org.sonar.ce.task.projectanalysis.issue.IssuesRepositoryVisitor;
import org.sonar.ce.task.projectanalysis.issue.LoadComponentUuidsHavingOpenIssuesVisitor;
import org.sonar.ce.task.projectanalysis.issue.MergeBranchTrackerExecution;
import org.sonar.ce.task.projectanalysis.issue.MovedIssueVisitor;
import org.sonar.ce.task.projectanalysis.issue.NewEffortAggregator;
import org.sonar.ce.task.projectanalysis.issue.RemoveProcessedComponentsVisitor;
import org.sonar.ce.task.projectanalysis.issue.RuleRepositoryImpl;
import org.sonar.ce.task.projectanalysis.issue.RuleTagsCopier;
import org.sonar.ce.task.projectanalysis.issue.ScmAccountToUser;
import org.sonar.ce.task.projectanalysis.issue.ScmAccountToUserLoader;
import org.sonar.ce.task.projectanalysis.issue.ShortBranchIssueMerger;
import org.sonar.ce.task.projectanalysis.issue.ShortBranchIssuesLoader;
import org.sonar.ce.task.projectanalysis.issue.ShortBranchOrPullRequestTrackerExecution;
import org.sonar.ce.task.projectanalysis.issue.TrackerBaseInputFactory;
import org.sonar.ce.task.projectanalysis.issue.TrackerExecution;
import org.sonar.ce.task.projectanalysis.issue.TrackerMergeBranchInputFactory;
import org.sonar.ce.task.projectanalysis.issue.TrackerRawInputFactory;
import org.sonar.ce.task.projectanalysis.issue.UpdateConflictResolver;
import org.sonar.ce.task.projectanalysis.issue.commonrule.BranchCoverageRule;
import org.sonar.ce.task.projectanalysis.issue.commonrule.CommentDensityRule;
import org.sonar.ce.task.projectanalysis.issue.commonrule.CommonRuleEngineImpl;
import org.sonar.ce.task.projectanalysis.issue.commonrule.DuplicatedBlockRule;
import org.sonar.ce.task.projectanalysis.issue.commonrule.LineCoverageRule;
import org.sonar.ce.task.projectanalysis.issue.commonrule.SkippedTestRule;
import org.sonar.ce.task.projectanalysis.issue.commonrule.TestErrorRule;
import org.sonar.ce.task.projectanalysis.issue.filter.IssueFilter;
import org.sonar.ce.task.projectanalysis.language.LanguageRepositoryImpl;
import org.sonar.ce.task.projectanalysis.measure.MeasureComputersHolderImpl;
import org.sonar.ce.task.projectanalysis.measure.MeasureComputersVisitor;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryImpl;
import org.sonar.ce.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.ce.task.projectanalysis.metric.MetricModule;
import org.sonar.ce.task.projectanalysis.organization.DefaultOrganizationLoader;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderImpl;
import org.sonar.ce.task.projectanalysis.qualitygate.EvaluationResultTextConverterImpl;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateHolderImpl;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateServiceImpl;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateStatusHolderImpl;
import org.sonar.ce.task.projectanalysis.qualitymodel.MaintainabilityMeasuresVisitor;
import org.sonar.ce.task.projectanalysis.qualitymodel.NewMaintainabilityMeasuresVisitor;
import org.sonar.ce.task.projectanalysis.qualitymodel.NewReliabilityAndSecurityRatingMeasuresVisitor;
import org.sonar.ce.task.projectanalysis.qualitymodel.RatingSettings;
import org.sonar.ce.task.projectanalysis.qualitymodel.ReliabilityAndSecurityRatingMeasuresVisitor;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolderImpl;
import org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepositoryImpl;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoDbLoader;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepositoryImpl;
import org.sonar.ce.task.projectanalysis.source.DbLineHashVersion;
import org.sonar.ce.task.projectanalysis.source.FileSourceDataComputer;
import org.sonar.ce.task.projectanalysis.source.FileSourceDataWarnings;
import org.sonar.ce.task.projectanalysis.source.LastCommitVisitor;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.ce.task.projectanalysis.source.SignificantCodeRepository;
import org.sonar.ce.task.projectanalysis.source.SourceHashRepositoryImpl;
import org.sonar.ce.task.projectanalysis.source.SourceLineReadersFactory;
import org.sonar.ce.task.projectanalysis.source.SourceLinesDiffImpl;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashCache;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashRepositoryImpl;
import org.sonar.ce.task.projectanalysis.source.SourceLinesRepositoryImpl;
import org.sonar.ce.task.projectanalysis.step.ReportComputationSteps;
import org.sonar.ce.task.projectanalysis.step.SmallChangesetQualityGateSpecialCase;
import org.sonar.ce.task.projectanalysis.webhook.WebhookPostTask;
import org.sonar.ce.task.setting.SettingsLoader;
import org.sonar.ce.task.step.ComputationStepExecutor;
import org.sonar.ce.task.step.ComputationSteps;
import org.sonar.ce.task.taskprocessor.MutableTaskResultHolderImpl;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.platform.ContainerPopulator;
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

      // messages/warnings
      CeTaskMessagesImpl.class,
      FileSourceDataWarnings.class,

      // File System
      new ComputationTempFolderProvider(),

      DbMigrationModule.class,
      ReportModulesPath.class,
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
      SourceLinesHashRepositoryImpl.class,
      DbLineHashVersion.class,
      SignificantCodeRepository.class,
      SourceLinesHashCache.class,
      NewLinesRepository.class,
      FileSourceDataComputer.class,
      SourceLineReadersFactory.class,
      QProfileStatusRepositoryImpl.class,

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
      ClosedIssuesInputFactory.class,
      Tracker.class,
      TrackerExecution.class,
      ShortBranchOrPullRequestTrackerExecution.class,
      MergeBranchTrackerExecution.class,
      ComponentIssuesLoader.class,
      BaseIssuesLoader.class,
      IssueTrackingDelegator.class,
      BranchPersisterImpl.class,
      ShortBranchIssuesLoader.class,
      ShortBranchIssueMerger.class,

      // filemove
      ScoreMatrixDumperImpl.class,
      SourceSimilarityImpl.class,
      FileSimilarityImpl.class,
      MutableMovedFilesRepositoryImpl.class,
      AddedFileRepositoryImpl.class,

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
