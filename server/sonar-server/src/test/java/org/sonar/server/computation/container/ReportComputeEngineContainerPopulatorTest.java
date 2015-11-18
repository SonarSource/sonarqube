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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.computation.queue.CeTask;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.computation.step.PersistComponentsStep;
import org.sonar.server.computation.step.PersistDevelopersStep;
import org.sonar.server.devcockpit.DevCockpitBridge;
import org.sonar.server.devcockpit.PersistDevelopersDelegate;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Sets.difference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportComputeEngineContainerPopulatorTest {
  private CeTask task = mock(CeTask.class);
  private ReportComputeEngineContainerPopulator underTest;

  @Test
  public void item_is_added_to_the_container() {
    underTest = new ReportComputeEngineContainerPopulator(task, null);
    AddedObjectsRecorderComputeEngineContainer container = new AddedObjectsRecorderComputeEngineContainer();
    underTest.populateContainer(container);

    assertThat(container.added).contains(task);
  }

  @Test
  public void all_computation_steps_are_added_in_order_to_the_container() {
    underTest = new ReportComputeEngineContainerPopulator(task, null);
    AddedObjectsRecorderComputeEngineContainer container = new AddedObjectsRecorderComputeEngineContainer();
    underTest.populateContainer(container);

    Set<String> computationStepClassNames = from(container.added)
      .transform(new Function<Object, Class<?>>() {
        @Nullable
        @Override
        public Class<?> apply(Object input) {
          if (input instanceof Class) {
            return (Class<?>) input;
          }
          return null;
        }
      })
      .filter(notNull())
      .filter(IsComputationStep.INSTANCE)
      .transform(StepsExplorer.toCanonicalName())
      .toSet();

    // PersistDevelopersStep is the only step that is not in the report container (it's only added when Dev Cockpit plugin is installed)
    assertThat(difference(StepsExplorer.retrieveStepPackageStepsCanonicalNames(), computationStepClassNames)).containsOnly(PersistDevelopersStep.class.getCanonicalName());
  }

  @Test
  public void at_least_one_core_step_is_added_to_the_container() {
    underTest = new ReportComputeEngineContainerPopulator(task, null);
    AddedObjectsRecorderComputeEngineContainer container = new AddedObjectsRecorderComputeEngineContainer();
    underTest.populateContainer(container);

    assertThat(container.added).contains(PersistComponentsStep.class);
  }

  @Test
  public void PersistDevelopersStep_is_not_added_to_the_container_when_DevCockpitBridge_is_null() {
    underTest = new ReportComputeEngineContainerPopulator(task, null);
    AddedObjectsRecorderComputeEngineContainer container = new AddedObjectsRecorderComputeEngineContainer();
    underTest.populateContainer(container);

    assertThat(container.added).doesNotContain(PersistDevelopersStep.class);
  }

  @Test
  public void PersistDevelopersStep_is_added_to_the_container_when_DevCockpitBridge_exist() {
    DevCockpitBridge devCockpitBridge = mock(DevCockpitBridge.class);
    when(devCockpitBridge.getCeComponents()).thenReturn(Arrays.<Object>asList(PersistDevelopersDelegateImpl.class));

    underTest = new ReportComputeEngineContainerPopulator(task, devCockpitBridge);
    AddedObjectsRecorderComputeEngineContainer container = new AddedObjectsRecorderComputeEngineContainer();
    container.add(devCockpitBridge);
    underTest.populateContainer(container);

    assertThat(container.added).contains(PersistDevelopersStep.class);
  }

  @Test
  public void components_from_DevCockpitBridge_are_added_to_the_container_when_DevCockpitBridge_exist() {
    DevCockpitBridge devCockpitBridge = mock(DevCockpitBridge.class);
    when(devCockpitBridge.getCeComponents()).thenReturn(Arrays.<Object>asList(PersistDevelopersDelegateImpl.class));

    underTest = new ReportComputeEngineContainerPopulator(task, devCockpitBridge);
    AddedObjectsRecorderComputeEngineContainer container = new AddedObjectsRecorderComputeEngineContainer();
    container.add(devCockpitBridge);
    underTest.populateContainer(container);

    assertThat(container.added).contains(PersistDevelopersDelegateImpl.class);
  }

  private enum IsComputationStep implements Predicate<Class<?>> {
    INSTANCE;

    @Override
    public boolean apply(Class<?> input) {
      return ComputationStep.class.isAssignableFrom(input);
    }
  }

  private static class AddedObjectsRecorderComputeEngineContainer implements ComputeEngineContainer {
    private List<Object> added = new ArrayList<>();

    @Override
    public ComponentContainer getParent() {
      throw new UnsupportedOperationException("getParent is not implemented");
    }

    @Override
    public void cleanup() {
      throw new UnsupportedOperationException("cleanup is not implemented");
    }

    @Override
    public ComponentContainer add(Object... objects) {
      added.addAll(Arrays.asList(objects));
      return null; // not used anyway
    }

    @Override
    public ComponentContainer addSingletons(Iterable<?> components) {
      for (Object component : components) {
        added.add(component);
      }
      return null; // not used anyway
    }

    @Override
    public <T> T getComponentByType(Class<T> type) {
      for (Object add : added) {
        if (add.getClass().getSimpleName().contains(type.getSimpleName())) {
          return (T) add;
        }
      }
      return null;
    }
  }

  private static class PersistDevelopersDelegateImpl implements PersistDevelopersDelegate {
    @Override
    public void execute() {
      // nothing to do
    }
  }
}
