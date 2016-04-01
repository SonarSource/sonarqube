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
package org.sonar.server.computation.container;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.settings.SettingsLoader;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.platform.ContainerPopulator;
import org.sonar.plugin.ce.ReportAnalysisComponentProvider;
import org.sonar.server.computation.analysis.AnalysisMetadataHolderImpl;
import org.sonar.server.computation.batch.BatchReportDirectoryHolderImpl;
import org.sonar.server.computation.batch.BatchReportReaderImpl;
import org.sonar.server.computation.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.component.SettingsRepositoryImpl;
import org.sonar.server.computation.component.TreeRootHolderImpl;
import org.sonar.server.computation.duplication.CrossProjectDuplicationStatusHolderImpl;
import org.sonar.server.computation.duplication.DuplicationRepositoryImpl;
import org.sonar.server.computation.duplication.IntegrateCrossProjectDuplications;
import org.sonar.server.computation.event.EventRepositoryImpl;
import org.sonar.server.computation.filesystem.ComputationTempFolderProvider;
import org.sonar.server.computation.issue.BaseIssuesLoader;
import org.sonar.server.computation.issue.CloseIssuesOnRemovedComponentsVisitor;
import org.sonar.server.computation.issue.ComponentIssuesRepositoryImpl;
import org.sonar.server.computation.issue.ComponentsWithUnprocessedIssues;
import org.sonar.server.computation.issue.DebtCalculator;
import org.sonar.server.computation.issue.DefaultAssignee;
import org.sonar.server.computation.issue.EffortAggregator;
import org.sonar.server.computation.issue.IntegrateIssuesVisitor;
import org.sonar.server.computation.issue.IssueAssigner;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.IssueCounter;
import org.sonar.server.computation.issue.IssueLifecycle;
import org.sonar.server.computation.issue.IssueVisitors;
import org.sonar.server.computation.issue.LoadComponentUuidsHavingOpenIssuesVisitor;
import org.sonar.server.computation.issue.NewEffortAggregator;
import org.sonar.server.computation.issue.NewEffortCalculator;
import org.sonar.server.computation.issue.RuleRepositoryImpl;
import org.sonar.server.computation.issue.RuleTagsCopier;
import org.sonar.server.computation.issue.RuleTypeCopier;
import org.sonar.server.computation.issue.ScmAccountToUser;
import org.sonar.server.computation.issue.ScmAccountToUserLoader;
import org.sonar.server.computation.issue.TrackerBaseInputFactory;
import org.sonar.server.computation.issue.TrackerExecution;
import org.sonar.server.computation.issue.TrackerRawInputFactory;
import org.sonar.server.computation.issue.UpdateConflictResolver;
import org.sonar.server.computation.issue.commonrule.BranchCoverageRule;
import org.sonar.server.computation.issue.commonrule.CommentDensityRule;
import org.sonar.server.computation.issue.commonrule.CommonRuleEngineImpl;
import org.sonar.server.computation.issue.commonrule.DuplicatedBlockRule;
import org.sonar.server.computation.issue.commonrule.LineCoverageRule;
import org.sonar.server.computation.issue.commonrule.SkippedTestRule;
import org.sonar.server.computation.issue.commonrule.TestErrorRule;
import org.sonar.server.computation.language.LanguageRepositoryImpl;
import org.sonar.server.computation.measure.MeasureComputersHolderImpl;
import org.sonar.server.computation.measure.MeasureComputersVisitor;
import org.sonar.server.computation.measure.MeasureRepositoryImpl;
import org.sonar.server.computation.measure.MeasureToMeasureDto;
import org.sonar.server.computation.metric.MetricModule;
import org.sonar.server.computation.period.PeriodsHolderImpl;
import org.sonar.server.computation.posttask.PostProjectAnalysisTasksExecutor;
import org.sonar.server.computation.qualitygate.EvaluationResultTextConverterImpl;
import org.sonar.server.computation.qualitygate.QualityGateHolderImpl;
import org.sonar.server.computation.qualitygate.QualityGateServiceImpl;
import org.sonar.server.computation.qualitygate.QualityGateStatusHolderImpl;
import org.sonar.server.computation.qualitymodel.NewQualityModelMeasuresVisitor;
import org.sonar.server.computation.qualitymodel.QualityModelMeasuresVisitor;
import org.sonar.server.computation.qualitymodel.RatingSettings;
import org.sonar.server.computation.qualityprofile.ActiveRulesHolderImpl;
import org.sonar.server.computation.scm.ScmInfoRepositoryImpl;
import org.sonar.server.computation.source.LastCommitVisitor;
import org.sonar.server.computation.source.SourceHashRepositoryImpl;
import org.sonar.server.computation.source.SourceLinesRepositoryImpl;
import org.sonar.server.computation.step.ComputationStepExecutor;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.computation.step.ReportComputationSteps;
import org.sonar.server.computation.taskprocessor.MutableTaskResultHolderImpl;
import org.sonar.server.view.index.ViewIndex;

public final class ReportComputeEngineContainerPopulator implements ContainerPopulator<ComputeEngineContainer> {
  private static final ReportAnalysisComponentProvider[] NO_REPORT_ANALYSIS_COMPONENT_PROVIDERS = new ReportAnalysisComponentProvider[0];

  private final CeTask task;
  private final ReportAnalysisComponentProvider[] componentProviders;

  public ReportComputeEngineContainerPopulator(CeTask task, @Nullable ReportAnalysisComponentProvider[] componentProviders) {
    this.task = task;
    this.componentProviders = componentProviders == null ? NO_REPORT_ANALYSIS_COMPONENT_PROVIDERS : componentProviders;
  }

  @Override
  public void populateContainer(ComputeEngineContainer container) {
    ComputationSteps steps = new ReportComputationSteps(container);
    container.add(SettingsLoader.class);
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
  private static List componentClasses() {
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
      PeriodsHolderImpl.class,
      QualityGateHolderImpl.class,
      QualityGateStatusHolderImpl.class,
      RatingSettings.class,
      ActiveRulesHolderImpl.class,
      MeasureComputersHolderImpl.class,
      MutableTaskResultHolderImpl.class,

      BatchReportReaderImpl.class,

      // repositories
      LanguageRepositoryImpl.class,
      MeasureRepositoryImpl.class,
      EventRepositoryImpl.class,
      SettingsRepositoryImpl.class,
      DbIdsRepositoryImpl.class,
      QualityGateServiceImpl.class,
      EvaluationResultTextConverterImpl.class,
      SourceLinesRepositoryImpl.class,
      SourceHashRepositoryImpl.class,
      ScmInfoRepositoryImpl.class,
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
      DebtCalculator.class,
      EffortAggregator.class,
      NewEffortCalculator.class,
      NewEffortAggregator.class,
      IssueAssigner.class,
      IssueCounter.class,

      // visitors : order is important, measure computers must be executed at the end in order to access to every measures / issues
      LoadComponentUuidsHavingOpenIssuesVisitor.class,
      IntegrateIssuesVisitor.class,
      CloseIssuesOnRemovedComponentsVisitor.class,
      QualityModelMeasuresVisitor.class,
      NewQualityModelMeasuresVisitor.class,
      LastCommitVisitor.class,
      MeasureComputersVisitor.class,

      UpdateConflictResolver.class,
      TrackerBaseInputFactory.class,
      TrackerRawInputFactory.class,
      Tracker.class,
      TrackerExecution.class,
      BaseIssuesLoader.class,

      // duplication
      IntegrateCrossProjectDuplications.class,

      // views
      ViewIndex.class,

      MeasureToMeasureDto.class);
  }

}
