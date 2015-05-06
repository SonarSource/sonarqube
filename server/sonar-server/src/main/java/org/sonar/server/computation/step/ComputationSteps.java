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

package org.sonar.server.computation.step;

import com.google.common.collect.Lists;
import org.sonar.server.computation.ComputationContainer;

import java.util.Arrays;
import java.util.List;

/**
 * Ordered list of steps to be executed
 */
public class ComputationSteps {

  /**
   * List of all {@link org.sonar.server.computation.step.ComputationStep},
   * ordered by execution sequence.
   */
  public static List<Class<? extends ComputationStep>> orderedStepClasses() {
    return Arrays.asList(
      // Read report
      ParseReportStep.class,

      // Compute data
      ComputeFileDependenciesStep.class,

      // Persist data
      PersistNumberOfDaysSinceLastCommitStep.class,
      PersistMeasuresStep.class,
      PersistIssuesStep.class,
      PersistComponentLinksStep.class,
      PersistEventsStep.class,
      PersistDuplicationMeasuresStep.class,
      PersistFileSourcesStep.class,
      PersistTestsStep.class,
      PersistFileDependenciesStep.class,

      // Switch snapshot and purge
      SwitchSnapshotStep.class,
      IndexComponentsStep.class,
      PurgeDatastoresStep.class,

      // ES indexing is done after all db changes
      ApplyPermissionsStep.class,
      IndexIssuesStep.class,
      IndexSourceLinesStep.class,
      IndexTestsStep.class,
      IndexViewsStep.class,

      // Purge of removed views has to be done after Views has been indexed
      PurgeRemovedViewsStep.class,

      // notifications are sent at the end, so that webapp displays up-to-date information
      SendIssueNotificationsStep.class);
  }

  private final List<ComputationStep> orderedSteps;

  public ComputationSteps(ComputationStep... s) {
    this.orderedSteps = order(s);
  }

  public List<ComputationStep> orderedSteps() {
    return orderedSteps;
  }

  private static List<ComputationStep> order(ComputationStep[] steps) {
    List<ComputationStep> result = Lists.newArrayList();
    for (Class<? extends ComputationStep> clazz : orderedStepClasses()) {
      result.add(find(steps, clazz));
    }
    return result;
  }

  private static ComputationStep find(ComputationStep[] steps, Class<? extends ComputationStep> clazz) {
    for (ComputationStep step : steps) {
      if (clazz.isInstance(step)) {
        return step;
      }
    }
    throw new IllegalStateException("Component not found: " + clazz + ". Check " + ComputationContainer.class);
  }
}
