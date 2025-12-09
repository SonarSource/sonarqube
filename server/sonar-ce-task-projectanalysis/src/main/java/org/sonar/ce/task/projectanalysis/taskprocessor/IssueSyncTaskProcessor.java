/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.taskprocessor;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.container.TaskContainerImpl;
import org.sonar.ce.task.projectanalysis.step.AbstractComputationSteps;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.ComputationStepExecutor;
import org.sonar.ce.task.taskprocessor.CeTaskProcessor;
import org.sonar.ce.task.telemetry.StepsTelemetryUnavailableHolderImpl;
import org.sonar.core.platform.Container;
import org.sonar.core.platform.ContainerPopulator;
import org.sonar.core.platform.SpringComponentContainer;

import static org.sonar.db.ce.CeTaskTypes.BRANCH_ISSUE_SYNC;

public class IssueSyncTaskProcessor implements CeTaskProcessor {
  private static final Set<String> HANDLED_TYPES = ImmutableSet.of(BRANCH_ISSUE_SYNC);

  private final SpringComponentContainer ceEngineContainer;

  public IssueSyncTaskProcessor(SpringComponentContainer ceEngineContainer) {
    this.ceEngineContainer = ceEngineContainer;
  }

  @Override
  public Set<String> getHandledCeTaskTypes() {
    return HANDLED_TYPES;
  }

  @CheckForNull
  @Override
  public CeTaskResult process(CeTask task) {
    try (TaskContainer container = new TaskContainerImpl(ceEngineContainer, newContainerPopulator(task))) {
      container.bootup();
      container.getComponentByType(ComputationStepExecutor.class).execute();
    }
    return null;
  }

  static ContainerPopulator<TaskContainer> newContainerPopulator(CeTask task) {
    return taskContainer -> {
      taskContainer.add(task);
      taskContainer.add(IgnoreOrphanBranchStep.class);
      taskContainer.add(IndexIssuesStep.class);
      taskContainer.add(new SyncComputationSteps(taskContainer));
      taskContainer.add(ComputationStepExecutor.class);
      taskContainer.add(StepsTelemetryUnavailableHolderImpl.class);
    };
  }

  public static final class SyncComputationSteps extends AbstractComputationSteps {

    public SyncComputationSteps(Container container) {
      super(container);
    }

    @Override
    public List<Class<? extends ComputationStep>> orderedStepClasses() {
      return Arrays.asList(IgnoreOrphanBranchStep.class, IndexIssuesStep.class);
    }

  }

}
