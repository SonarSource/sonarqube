/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.platform.ContainerPopulator;
import org.sonar.server.computation.queue.CeTask;
import org.sonar.server.computation.ComputationStepExecutor;
import org.sonar.server.computation.ComputationTempFolderProvider;
import org.sonar.server.computation.analysis.ReportAnalysisMetadataHolder;
import org.sonar.server.computation.batch.BatchReportDirectoryHolderImpl;
import org.sonar.server.computation.batch.BatchReportReaderImpl;
import org.sonar.server.computation.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.component.ReportTreeRootHolderImpl;
import org.sonar.server.computation.component.SettingsRepositoryImpl;
import org.sonar.server.computation.debt.DebtModelHolderImpl;
import org.sonar.server.computation.event.EventRepositoryImpl;
import org.sonar.server.computation.issue.BaseIssuesLoader;
import org.sonar.server.computation.issue.CloseIssuesOnRemovedComponentsVisitor;
import org.sonar.server.computation.issue.ComponentIssuesRepositoryImpl;
import org.sonar.server.computation.issue.ComponentsWithUnprocessedIssues;
import org.sonar.server.computation.issue.DebtAggregator;
import org.sonar.server.computation.issue.DebtCalculator;
import org.sonar.server.computation.issue.DefaultAssignee;
import org.sonar.server.computation.issue.IntegrateIssuesVisitor;
import org.sonar.server.computation.issue.IssueAssigner;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.IssueCounter;
import org.sonar.server.computation.issue.IssueLifecycle;
import org.sonar.server.computation.issue.IssueVisitors;
import org.sonar.server.computation.issue.LoadComponentUuidsHavingOpenIssuesVisitor;
import org.sonar.server.computation.issue.NewDebtAggregator;
import org.sonar.server.computation.issue.NewDebtCalculator;
import org.sonar.server.computation.issue.RuleCacheLoader;
import org.sonar.server.computation.issue.RuleRepositoryImpl;
import org.sonar.server.computation.issue.RuleTagsCopier;
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
import org.sonar.server.computation.metric.MetricModule;
import org.sonar.server.computation.period.PeriodsHolderImpl;
import org.sonar.server.computation.qualitygate.EvaluationResultTextConverterImpl;
import org.sonar.server.computation.qualitygate.QualityGateHolderImpl;
import org.sonar.server.computation.qualitygate.QualityGateServiceImpl;
import org.sonar.server.computation.qualityprofile.ActiveRulesHolderImpl;
import org.sonar.server.computation.source.LastCommitVisitor;
import org.sonar.server.computation.source.SourceLinesRepositoryImpl;
import org.sonar.server.computation.sqale.SqaleMeasuresVisitor;
import org.sonar.server.computation.sqale.SqaleRatingSettings;
import org.sonar.server.computation.sqale.SqaleNewMeasuresVisitor;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.computation.step.ReportComputationSteps;
import org.sonar.server.view.index.ViewIndex;

public final class ReportComputeEngineContainerPopulator implements ContainerPopulator<ComputeEngineContainer> {
  private final CeTask task;

  public ReportComputeEngineContainerPopulator(CeTask task) {
    this.task = task;
  }

  @Override
  public void populateContainer(ComputeEngineContainer container) {
    ComputationSteps steps = new ReportComputationSteps(container);
    container.add(task);
    container.add(steps);
    container.addSingletons(componentClasses());
    container.addSingletons(steps.orderedStepClasses());
  }

  /**
   * List of all objects to be injected in the picocontainer dedicated to computation stack.
   * Does not contain the steps declared in {@link ReportComputationSteps#orderedStepClasses()}.
   */
  private static List componentClasses() {
    return Arrays.asList(
      ComputationStepExecutor.class,
      new ComputationTempFolderProvider(),

      MetricModule.class,

      // holders
      ReportAnalysisMetadataHolder.class,
      BatchReportDirectoryHolderImpl.class,
      ReportTreeRootHolderImpl.class,
      PeriodsHolderImpl.class,
      QualityGateHolderImpl.class,
      DebtModelHolderImpl.class,
      SqaleRatingSettings.class,
      ActiveRulesHolderImpl.class,
      MeasureComputersHolderImpl.class,

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

      // issues
      RuleCacheLoader.class,
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

      // order is important: DebtAggregator then NewDebtAggregator (new debt requires debt)
      DebtCalculator.class,
      DebtAggregator.class,
      NewDebtCalculator.class,
      NewDebtAggregator.class,
      IssueAssigner.class,
      RuleTagsCopier.class,
      IssueCounter.class,

      // visitors : order is important, measure computers must be executed at the end in order to access to every measures / issues
      LoadComponentUuidsHavingOpenIssuesVisitor.class,
      IntegrateIssuesVisitor.class,
      CloseIssuesOnRemovedComponentsVisitor.class,
      SqaleMeasuresVisitor.class,
      SqaleNewMeasuresVisitor.class,
      LastCommitVisitor.class,
      MeasureComputersVisitor.class,

      UpdateConflictResolver.class,
      TrackerBaseInputFactory.class,
      TrackerRawInputFactory.class,
      Tracker.class,
      TrackerExecution.class,
      BaseIssuesLoader.class,

      // views
      ViewIndex.class);
  }

}
