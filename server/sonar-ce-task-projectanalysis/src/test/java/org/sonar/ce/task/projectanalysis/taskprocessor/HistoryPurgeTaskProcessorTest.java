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
import org.junit.Test;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.projectanalysis.history.HistoryPurgeStep;
import org.sonar.ce.task.projectanalysis.history.HistoryPurgeTaskComponentProvider;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.SpringComponentContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.taskprocessor.HistoryPurgeTaskProcessor.HistoryPurgeComputationSteps;
import static org.sonar.db.ce.CeTaskTypes.HISTORY_PURGE;

public class HistoryPurgeTaskProcessorTest {

  private final SpringComponentContainer ceEngineContainer = mock(SpringComponentContainer.class);
  private final HistoryPurgeTaskProcessor underTest = new HistoryPurgeTaskProcessor(ceEngineContainer, null);
  private final TaskContainer container = spy(TaskContainer.class);

  @Test
  public void getHandledCeTaskTypes() {
    assertThat(underTest.getHandledCeTaskTypes()).containsExactly(HISTORY_PURGE);
  }

  @Test
  public void processThrowsNPEIfCeTaskIsNull() {
    assertThatThrownBy(() -> underTest.process(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void newContainerPopulator() {
    CeTask task = new CeTask.Builder()
      .setUuid("TASK_UUID")
      .setType("Type")
      .build();

    HistoryPurgeTaskProcessor.newContainerPopulator(task).populateContainer(container);

    verify(container, times(4)).add(any());
  }

  @Test
  public void newContainerPopulator_whenComponentProvidersArePresent_addsTheirComponents() {
    CeTask task = new CeTask.Builder()
      .setUuid("TASK_UUID")
      .setType("Type")
      .build();
    HistoryPurgeTaskComponentProvider componentProvider = mock(HistoryPurgeTaskComponentProvider.class);
    when(componentProvider.getComponents()).thenReturn(List.of(Object.class));

    HistoryPurgeTaskProcessor.newContainerPopulator(task, componentProvider).populateContainer(container);

    verify(container).add(Object.class);
    verify(container, times(5)).add(any());
  }

  @Test
  public void orderedStepClasses() {
    HistoryPurgeComputationSteps historyPurgeComputationSteps = new HistoryPurgeComputationSteps(null);

    List<Class<? extends ComputationStep>> steps = historyPurgeComputationSteps.orderedStepClasses();

    assertThat(steps).containsExactly(HistoryPurgeStep.class);
  }
}
