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
package org.sonar.server.computation;

import java.util.Arrays;
import java.util.List;
import org.sonar.core.issue.db.UpdateConflictResolver;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.computation.activity.CEActivityManager;
import org.sonar.server.computation.batch.CEBatchReportReader;
import org.sonar.server.computation.batch.ReportExtractor;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.ProjectSettingsRepository;
import org.sonar.server.computation.container.CEContainer;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.IssueComputation;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.issue.RuleCacheLoader;
import org.sonar.server.computation.issue.ScmAccountCache;
import org.sonar.server.computation.issue.ScmAccountCacheLoader;
import org.sonar.server.computation.issue.SourceLinesCache;
import org.sonar.server.computation.language.PlatformLanguageRepository;
import org.sonar.server.computation.measure.MetricCache;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.platform.Platform;
import org.sonar.server.view.index.ViewIndex;

public class ComputationContainer {

  /**
   * List of all objects to be injected in the picocontainer dedicated to computation stack.
   * Does not contain the steps declared in {@link org.sonar.server.computation.step.ComputationSteps#orderedStepClasses()}.
   */
  static List componentClasses() {
    return Arrays.asList(
      CEActivityManager.class,
      ReportExtractor.class,
      CEBatchReportReader.class,

      // repositories
      PlatformLanguageRepository.class,
      ProjectSettingsRepository.class,

      // component caches
      DbComponentsRefCache.class,

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
      ViewIndex.class);
  }

  public void execute(ReportQueue.Item item) {
    ComponentContainer container = Platform.getInstance().getContainer();

    ComponentContainer ceContainer = new CEContainer(container);
    ceContainer.add(ceContainer);
    ceContainer.add(item);
    ceContainer.addSingletons(componentClasses());
    ceContainer.addSingletons(ComputationSteps.orderedStepClasses());
    try {
      ceContainer.getComponentByType(ComputationService.class).process();
    } finally {
      ceContainer.stopComponents();
      // TODO not possible to have multiple children -> will be
      // a problem when we will have multiple concurrent computation workers
      container.removeChild();
    }
  }

}
