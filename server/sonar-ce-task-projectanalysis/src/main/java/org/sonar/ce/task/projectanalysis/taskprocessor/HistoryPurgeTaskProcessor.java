/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.container.TaskContainerImpl;
import org.sonar.ce.task.projectanalysis.history.HistoryPurgeStep;
import org.sonar.ce.task.projectanalysis.history.HistoryPurgeTaskComponentProvider;
import org.sonar.ce.task.projectanalysis.step.AbstractComputationSteps;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.ComputationStepExecutor;
import org.sonar.ce.task.taskprocessor.CeTaskProcessor;
import org.sonar.core.platform.Container;
import org.sonar.core.platform.ContainerPopulator;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonarsource.history.server.HistoryServerComponents;

import static org.sonar.db.ce.CeTaskTypes.HISTORY_PURGE;

public class HistoryPurgeTaskProcessor implements CeTaskProcessor {
  private static final Set<String> HANDLED_TYPES = Set.of(HISTORY_PURGE);

  private final SpringComponentContainer ceEngineContainer;
  private final HistoryPurgeTaskComponentProvider[] componentProviders;

  public HistoryPurgeTaskProcessor(SpringComponentContainer ceEngineContainer, @Nullable HistoryPurgeTaskComponentProvider[] componentProviders) {
    this.ceEngineContainer = ceEngineContainer;
    this.componentProviders = componentProviders == null ? new HistoryPurgeTaskComponentProvider[0] : componentProviders;
  }

  @Override
  public Set<String> getHandledCeTaskTypes() {
    return HANDLED_TYPES;
  }

  @CheckForNull
  @Override
  public CeTaskResult process(CeTask task) {
    try (TaskContainer container = new TaskContainerImpl(ceEngineContainer, newContainerPopulator(task, componentProviders))) {
      container.bootup();
      container.getComponentByType(ComputationStepExecutor.class).execute();
    }
    return null;
  }

  static ContainerPopulator<TaskContainer> newContainerPopulator(CeTask task, HistoryPurgeTaskComponentProvider... componentProviders) {
    return taskContainer -> {
      taskContainer.add(task);
      taskContainer.add(HistoryServerComponents.recordingComponents().toArray());
      for (HistoryPurgeTaskComponentProvider componentProvider : componentProviders) {
        taskContainer.add(componentProvider.getComponents().toArray());
      }
      taskContainer.add(HistoryPurgeStep.class);
      taskContainer.add(new HistoryPurgeComputationSteps(taskContainer));
      taskContainer.add(ComputationStepExecutor.class);
    };
  }

  public static final class HistoryPurgeComputationSteps extends AbstractComputationSteps {

    public HistoryPurgeComputationSteps(Container container) {
      super(container);
    }

    @Override
    public List<Class<? extends ComputationStep>> orderedStepClasses() {
      return List.of(HistoryPurgeStep.class);
    }
  }
}
