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
package org.sonar.ce.task.container;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.Startable;
import org.sonar.core.platform.ContainerPopulator;
import org.sonar.core.platform.SpringComponentContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TaskContainerImplTest {
  private final SpringComponentContainer parent = new SpringComponentContainer();
  private final ContainerPopulator<TaskContainer> populator = spy(new DummyContainerPopulator());

  @Before
  public void before() {
    parent.startComponents();
  }

  @Test
  public void constructor_fails_fast_on_null_container() {
    assertThatThrownBy(() -> new TaskContainerImpl(null, populator))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void constructor_fails_fast_on_null_item() {
    SpringComponentContainer c = new SpringComponentContainer();
    assertThatThrownBy(() -> new TaskContainerImpl(c, null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void calls_method_populateContainer_of_passed_in_populator() {
    TaskContainerImpl ceContainer = new TaskContainerImpl(parent, populator);

    verify(populator).populateContainer(ceContainer);
  }

  @Test
  public void bootup_starts_components_lazily_unless_they_are_annotated_with_EagerStart() {
    DefaultStartable defaultStartable = new DefaultStartable();
    EagerStartable eagerStartable = new EagerStartable();
    TaskContainerImpl ceContainer = new TaskContainerImpl(parent, container -> {
      container.add(defaultStartable);
      container.add(eagerStartable);
    });
    ceContainer.bootup();

    assertThat(defaultStartable.startCalls).isZero();
    assertThat(defaultStartable.stopCalls).isZero();
    assertThat(eagerStartable.startCalls).isOne();
    assertThat(eagerStartable.stopCalls).isZero();
  }

  @Test
  public void close_stops_started_components() {
    final DefaultStartable defaultStartable = new DefaultStartable();
    final EagerStartable eagerStartable = new EagerStartable();
    TaskContainerImpl ceContainer = new TaskContainerImpl(parent, container -> {
      container.add(defaultStartable);
      container.add(eagerStartable);
    });
    ceContainer.bootup();

    ceContainer.close();

    assertThat(defaultStartable.startCalls).isZero();
    assertThat(defaultStartable.stopCalls).isZero();
    assertThat(eagerStartable.startCalls).isOne();
    assertThat(eagerStartable.stopCalls).isOne();
  }

  public static class DefaultStartable implements Startable {
    protected int startCalls = 0;
    protected int stopCalls = 0;

    @Override
    public void start() {
      startCalls++;
    }

    @Override
    public void stop() {
      stopCalls++;
    }
  }

  @EagerStart
  public static class EagerStartable extends DefaultStartable {
  }


  private static class DummyContainerPopulator implements ContainerPopulator<TaskContainer> {
    @Override
    public void populateContainer(TaskContainer container) {

    }
  }
}
