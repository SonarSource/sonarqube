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
import org.picocontainer.ComponentAdapter;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.OptInCaching;
import org.picocontainer.lifecycle.ReflectionLifecycleStrategy;
import org.picocontainer.monitors.NullComponentMonitor;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.Module;
import org.sonar.server.computation.ComputationService;
import org.sonar.server.computation.ComputationTempFolderProvider;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.computation.activity.ActivityManager;
import org.sonar.server.computation.batch.BatchReportDirectoryHolderImpl;
import org.sonar.server.computation.batch.BatchReportReaderImpl;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.ProjectSettingsRepository;
import org.sonar.server.computation.component.TreeRootHolderImpl;
import org.sonar.server.computation.debt.DebtModelHolderImpl;
import org.sonar.server.computation.event.EventRepositoryImpl;
import org.sonar.server.computation.formula.CoreFormulaRepositoryImpl;
import org.sonar.server.computation.issue.BaseIssuesLoader;
import org.sonar.server.computation.issue.DebtAggregator;
import org.sonar.server.computation.issue.DebtCalculator;
import org.sonar.server.computation.issue.DefaultAssignee;
import org.sonar.server.computation.issue.IssueAssigner;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.IssueCounter;
import org.sonar.server.computation.issue.IssueLifecycle;
import org.sonar.server.computation.issue.IssueVisitors;
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
import org.sonar.server.computation.measure.MeasureRepositoryImpl;
import org.sonar.server.computation.measure.newcoverage.NewCoverageMetricKeysModule;
import org.sonar.server.computation.metric.MetricModule;
import org.sonar.server.computation.period.PeriodsHolderImpl;
import org.sonar.server.computation.qualitygate.EvaluationResultTextConverterImpl;
import org.sonar.server.computation.qualitygate.QualityGateHolderImpl;
import org.sonar.server.computation.qualitygate.QualityGateServiceImpl;
import org.sonar.server.computation.qualityprofile.ActiveRulesHolderImpl;
import org.sonar.server.computation.sqale.SqaleRatingSettings;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.view.index.ViewIndex;

import static java.util.Objects.requireNonNull;

public class ComputeEngineContainerImpl extends ComponentContainer implements ComputeEngineContainer {
  private final ReportQueue.Item item;
  private final ComputationSteps steps;

  public ComputeEngineContainerImpl(ComponentContainer parent, ReportQueue.Item item) {
    super(createContainer(requireNonNull(parent)));

    this.item = item;
    this.steps = new ComputationSteps(this);

    populateContainer(requireNonNull(item));
    startComponents();
  }

  @Override
  public ReportQueue.Item getItem() {
    return item;
  }

  private void populateContainer(ReportQueue.Item item) {
    add(item);
    add(steps);
    addSingletons(componentClasses());
    addSingletons(steps.orderedStepClasses());
    populateFromModules();
  }

  private void populateFromModules() {
    List<Module> modules = getComponentsByType(Module.class);
    for (Module module : modules) {
      module.configure(this);
    }
  }

  /**
   * Creates a PicContainer which extends the specified ComponentContainer <strong>but is not referenced in return</strong>
   * and lazily starts its components.
   */
  private static MutablePicoContainer createContainer(ComponentContainer parent) {
    ReflectionLifecycleStrategy lifecycleStrategy = new ReflectionLifecycleStrategy(new NullComponentMonitor(), "start", "stop", "close") {
      @Override
      public boolean isLazy(ComponentAdapter<?> adapter) {
        return true;
      }

      @Override
      public void start(Object component) {
        Profiler profiler = Profiler.createIfTrace(Loggers.get(ComponentContainer.class));
        profiler.start();
        super.start(component);
        profiler.stopTrace(component.getClass().getCanonicalName() + " started");
      }
    };

    return new DefaultPicoContainer(new OptInCaching(), lifecycleStrategy, parent.getPicoContainer());
  }

  /**
   * List of all objects to be injected in the picocontainer dedicated to computation stack.
   * Does not contain the steps declared in {@link org.sonar.server.computation.step.ComputationSteps#orderedStepClasses()}.
   */
  private static List componentClasses() {
    return Arrays.asList(
      new ComputationTempFolderProvider(),

      ActivityManager.class,

      MetricModule.class,

      // holders
      BatchReportDirectoryHolderImpl.class,
      TreeRootHolderImpl.class,
      PeriodsHolderImpl.class,
      QualityGateHolderImpl.class,
      DebtModelHolderImpl.class,
      SqaleRatingSettings.class,
      ActiveRulesHolderImpl.class,

      BatchReportReaderImpl.class,

      // repositories
      LanguageRepositoryImpl.class,
      MeasureRepositoryImpl.class,
      EventRepositoryImpl.class,
      ProjectSettingsRepository.class,
      DbIdsRepository.class,
      CoreFormulaRepositoryImpl.class,
      QualityGateServiceImpl.class,
      EvaluationResultTextConverterImpl.class,

      // new coverage measures
      NewCoverageMetricKeysModule.class,

      // issues
      RuleCacheLoader.class,
      RuleRepositoryImpl.class,
      ScmAccountToUserLoader.class,
      ScmAccountToUser.class,
      IssueCache.class,
      DefaultAssignee.class,
      IssueVisitors.class,
      IssueLifecycle.class,

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

      UpdateConflictResolver.class,
      TrackerBaseInputFactory.class,
      TrackerRawInputFactory.class,
      Tracker.class,
      TrackerExecution.class,
      BaseIssuesLoader.class,

      // views
      ViewIndex.class,

      // ComputationService
      ComputationService.class
      );
  }

  @Override
  public void process() {
    // calls the first
    getComponentByType(ComputationService.class).process();
  }

  @Override
  public void cleanup() {
    try {
      stopComponents();
    } catch (Throwable t) {
      Loggers.get(ComputeEngineContainerImpl.class).error(String.format("Cleanup of container for item '%s' failed", item), t);
    }
  }

  @Override
  public <T extends ComputationStep> T getStep(Class<T> type) {
    return getComponentByType(type);
  }

  @Override
  public String toString() {
    return "ComputeEngineContainerImpl";
  }
}
