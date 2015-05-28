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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import org.sonar.server.computation.container.ComputeEngineContainer;

/**
 * Ordered list of steps to be executed
 */
public class ComputationSteps {

  /**
   * List of all {@link org.sonar.server.computation.step.ComputationStep},
   * ordered by execution sequence.
   */
  public List<Class<? extends ComputationStep>> orderedStepClasses() {
    return Arrays.asList(
      // Builds Component tree
      BuildComponentTreeStep.class,

      PopulateComponentsUuidAndKeyStep.class,

      // Read report
      ParseReportStep.class,

      // data computation
      QualityProfileEventsStep.class,

      // Persist data
      PersistComponentsStep.class,
      PersistNumberOfDaysSinceLastCommitStep.class,
      PersistMeasuresStep.class,
      PersistIssuesStep.class,
      PersistProjectLinksStep.class,
      PersistEventsStep.class,
      PersistDuplicationsStep.class,
      PersistFileSourcesStep.class,
      PersistTestsStep.class,

      // Switch snapshot and purge
      SwitchSnapshotStep.class,
      IndexComponentsStep.class,
      PurgeDatastoresStep.class,

      // ES indexing is done after all db changes
      ApplyPermissionsStep.class,
      IndexIssuesStep.class,
      IndexSourceLinesStep.class,
      IndexTestsStep.class,

      // notifications are sent at the end, so that webapp displays up-to-date information
      SendIssueNotificationsStep.class);
  }

  private final ComputeEngineContainer computeEngineContainer;

  public ComputationSteps(ComputeEngineContainer computeEngineContainer) {
    this.computeEngineContainer = computeEngineContainer;
  }

  public Iterable<ComputationStep> instances() {
    return Iterables.transform(orderedStepClasses(), new Function<Class<? extends ComputationStep>, ComputationStep>() {
      @Override
      public ComputationStep apply(Class<? extends ComputationStep> input) {
        ComputationStep computationStepType = computeEngineContainer.getStep(input);
        if (computationStepType == null) {
          throw new IllegalStateException(String.format("Component not found: %s", input));
        }
        return computationStepType;
      }
    });
  }

}
