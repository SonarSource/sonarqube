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

import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.issue.db.UpdateConflictResolver;
import org.sonar.server.computation.issue.*;
import org.sonar.server.computation.measure.MetricCache;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.platform.Platform;
import org.sonar.server.view.index.ViewIndex;

import java.util.Arrays;
import java.util.List;

public class ComputationContainer {

  /**
   * List of all objects to be injected in the picocontainer dedicated to computation stack.
   * Does not contain the steps declared in {@link org.sonar.server.computation.step.ComputationSteps#orderedStepClasses()}.
   */
  static List componentClasses() {
    return Arrays.asList(
      ComputationService.class,
      ComputationSteps.class,

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
    ComponentContainer child = container.createChild();
    child.addSingletons(componentClasses());
    child.addSingletons(ComputationSteps.orderedStepClasses());
    child.startComponents();
    try {
      child.getComponentByType(ComputationService.class).process(item);
    } finally {
      child.stopComponents();
      // TODO not possible to have multiple children -> will be
      // a problem when we will have multiple concurrent computation workers
      container.removeChild();
    }
  }
}
