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
import org.sonar.core.issue.db.UpdateConflictResolver;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.computation.ComputationService;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.computation.activity.ActivityManager;
import org.sonar.server.computation.batch.BatchReportDirectoryHolderImpl;
import org.sonar.server.computation.batch.BatchReportReaderImpl;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.ProjectSettingsRepository;
import org.sonar.server.computation.component.TreeRootHolderImpl;
import org.sonar.server.computation.event.EventRepositoryImpl;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.IssueComputation;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.issue.RuleCacheLoader;
import org.sonar.server.computation.issue.ScmAccountCache;
import org.sonar.server.computation.issue.ScmAccountCacheLoader;
import org.sonar.server.computation.issue.SourceLinesCache;
import org.sonar.server.computation.language.PlatformLanguageRepository;
import org.sonar.server.computation.measure.MeasureRepositoryImpl;
import org.sonar.server.computation.measure.MetricCache;
import org.sonar.server.computation.period.PeriodsHolderImpl;
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
      ActivityManager.class,

      TreeRootHolderImpl.class,

      BatchReportReaderImpl.class,

      BatchReportDirectoryHolderImpl.class,

      // repositories
      PlatformLanguageRepository.class,
      MeasureRepositoryImpl.class,
      EventRepositoryImpl.class,
      ProjectSettingsRepository.class,
      PeriodsHolderImpl.class,
      DbIdsRepository.class,

      // issues
      ScmAccountCacheLoader.class,
      ScmAccountCache.class,
      SourceLinesCache.class,
      IssueComputation.class,
      RuleCache.class,
      RuleCacheLoader.class,
      IssueCache.class,
      MetricCache.class,
      UpdateConflictResolver.class,

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
