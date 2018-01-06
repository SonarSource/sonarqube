/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.engine;

import org.junit.Test;
import org.picocontainer.Startable;
import org.sonar.core.platform.ComponentContainer;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationContainerImplTest {
  private ComponentContainer parent = new ComponentContainer();
  private MigrationContainerPopulator populator = new MigrationContainerPopulator() {
    @Override
    public void populateContainer(MigrationContainer container) {
      container.add(StartCallCounter.class);
    }
  };

  private MigrationContainerImpl underTest = new MigrationContainerImpl(parent, populator);

  @Test
  public void pico_container_of_migration_container_has_pico_container_of_specified_container_as_parent() {
    assertThat(underTest.getPicoContainer().getParent()).isEqualTo(parent.getPicoContainer());
  }

  @Test
  public void pico_container_of_parent_does_not_have_pico_container_of_migration_container_as_child() {
    assertThat(parent.getPicoContainer().removeChildContainer(underTest.getPicoContainer())).isFalse();
  }

  @Test
  public void pico_container_of_migration_container_is_started_in_constructor() {
    assertThat(underTest.getPicoContainer().getLifecycleState().isStarted()).isTrue();
  }

  @Test
  public void migration_container_lazily_instance_components() {
    assertThat(StartCallCounter.startCalls).isEqualTo(0);

    StartCallCounter startCallCounter = underTest.getComponentByType(StartCallCounter.class);

    assertThat(startCallCounter).isNotNull();
    assertThat(StartCallCounter.startCalls).isEqualTo(1);
  }

  @Test
  public void cleanup_does_not_fail_even_if_stop_of_component_fails() {
    MigrationContainerImpl underTest = new MigrationContainerImpl(parent, (container -> container.add(StopFailing.class)));

    underTest.cleanup();
  }

  public static final class StartCallCounter implements Startable {
    private static int startCalls = 0;

    @Override
    public void start() {
      startCalls++;
    }

    @Override
    public void stop() {
      // do nothing
    }
  }

  public static final class StopFailing implements Startable {
    @Override
    public void start() {
      // do nothing
    }

    @Override
    public void stop() {
      throw new RuntimeException("Faking stop call failing");
    }
  }
}
