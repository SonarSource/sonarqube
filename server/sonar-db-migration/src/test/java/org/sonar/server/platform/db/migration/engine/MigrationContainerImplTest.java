/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.sql.SQLException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.Startable;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.platform.db.migration.step.InternalMigrationStepRegistry;
import org.sonar.server.platform.db.migration.step.MigrationStatusListener;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistryImpl;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutor;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationContainerImplTest {
  private final SpringComponentContainer parent = new SpringComponentContainer();
  private MigrationContainerImpl underTest;

  @Before
  public void setUp() {
    InternalMigrationStepRegistry registry = new MigrationStepRegistryImpl();
    registry.add(1, "test", NoOpMigrationStep.class);

    parent.add(registry.build());
    parent.startComponents();
    underTest = new MigrationContainerImpl(parent, NoOpExecutor.class);
    underTest.add(StartCallCounter.class);
  }

  @Test
  public void adds_migration_steps_to_migration_container() {
    assertThat(underTest.getComponentByType(MigrationStep.class)).isInstanceOf(NoOpMigrationStep.class);
  }

  @Test
  public void context_of_migration_container_has_specified_context_as_parent() {
    assertThat(underTest.context().getParent()).isEqualTo(parent.context());
  }

  @Test
  public void context_of_migration_container_is_started_in_constructor() {
    assertThat(underTest.context().isActive()).isTrue();
  }

  @Test
  public void add_duplicate_steps_has_no_effect() {
    InternalMigrationStepRegistry registry = new MigrationStepRegistryImpl();
    registry.add(1, "test", NoOpMigrationStep.class);
    registry.add(2, "test2", NoOpMigrationStep.class);

    SpringComponentContainer parent = new SpringComponentContainer();
    parent.add(registry.build());
    parent.startComponents();
    MigrationContainerImpl underTest = new MigrationContainerImpl(parent, NoOpExecutor.class);
    assertThat(underTest.getComponentsByType(MigrationStep.class)).hasSize(1);
  }

  @Test
  public void migration_container_lazily_instance_components() {
    assertThat(StartCallCounter.startCalls).isZero();

    StartCallCounter startCallCounter = underTest.getComponentByType(StartCallCounter.class);

    assertThat(startCallCounter).isNotNull();
    assertThat(StartCallCounter.startCalls).isOne();
  }

  @Test
  public void cleanup_does_not_fail_even_if_stop_of_component_fails() {
    parent.add(StopFailing.class);
    MigrationContainerImpl underTest = new MigrationContainerImpl(parent, NoOpExecutor.class);

    underTest.cleanup();
  }

  private static class NoOpExecutor implements MigrationStepsExecutor {
    @Override
    public void execute(List<RegisteredMigrationStep> steps, MigrationStatusListener listener) {
      // do nothing
    }
  }

  private static class NoOpMigrationStep implements MigrationStep {
    @Override
    public void execute() throws SQLException {
      // do nothing
    }
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
