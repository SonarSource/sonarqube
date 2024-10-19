/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.server.platform.db.migration.MutableDatabaseMigrationState;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutorImpl;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;
import org.sonar.server.telemetry.TelemetryDbMigrationStepDurationProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationSuccessProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationStepsProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationTotalTimeProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MigrationContainerPopulatorImplTest {
  private final SimpleMigrationContainer migrationContainer = new SimpleMigrationContainer();
  private final MigrationSteps migrationSteps = mock(MigrationSteps.class);
  private final MigrationContainerPopulatorImpl underTest = new MigrationContainerPopulatorImpl();

  @Before
  public void setUp() {
    migrationContainer.add(migrationSteps);
  }

  @Test
  public void populateContainer_adds_MigrationStepsExecutorImpl() {
    when(migrationSteps.readAll()).thenReturn(emptyList());

    // add MigrationStepsExecutorImpl's dependencies
    migrationContainer.add(mock(MigrationHistory.class));
    migrationContainer.add(mock(MutableDatabaseMigrationState.class));
    migrationContainer.add(mock(TelemetryDbMigrationStepsProvider.class));
    migrationContainer.add(mock(TelemetryDbMigrationTotalTimeProvider.class));
    migrationContainer.add(mock(TelemetryDbMigrationSuccessProvider.class));
    migrationContainer.add(mock(TelemetryDbMigrationStepDurationProvider.class));

    migrationContainer.startComponents();
    underTest.populateContainer(migrationContainer);

    assertThat(migrationContainer.getComponentByType(MigrationStepsExecutorImpl.class)).isNotNull();
  }

  @Test
  public void populateContainer_adds_classes_of_all_steps_defined_in_MigrationSteps() {
    when(migrationSteps.readAll()).thenReturn(asList(
      new RegisteredMigrationStep(1, "foo", MigrationStep1.class),
      new RegisteredMigrationStep(2, "bar", MigrationStep2.class),
      new RegisteredMigrationStep(3, "dor", MigrationStep3.class)));

    migrationContainer.startComponents();
    underTest.populateContainer(migrationContainer);

    assertThat(migrationContainer.getComponentsByType(MigrationStep1.class)).isNotNull();
    assertThat(migrationContainer.getComponentsByType(MigrationStep2.class)).isNotNull();
    assertThat(migrationContainer.getComponentsByType(MigrationStep3.class)).isNotNull();
  }

  @Test
  public void populateCotnainer_does_not_fail_if_same_class_is_used_for_more_than_one_migration() {
    when(migrationSteps.readAll()).thenReturn(asList(
      new RegisteredMigrationStep(1, "foo", MigrationStep1.class),
      new RegisteredMigrationStep(2, "bar", MigrationStep2.class),
      new RegisteredMigrationStep(3, "bar2", MigrationStep2.class),
      new RegisteredMigrationStep(4, "foo2", MigrationStep1.class),
      new RegisteredMigrationStep(5, "dor", MigrationStep3.class)));

    migrationContainer.startComponents();
    underTest.populateContainer(migrationContainer);

    assertThat(migrationContainer.getComponentsByType(MigrationStep1.class)).isNotNull();
    assertThat(migrationContainer.getComponentsByType(MigrationStep2.class)).isNotNull();
    assertThat(migrationContainer.getComponentsByType(MigrationStep3.class)).isNotNull();
  }

  private static abstract class NoopMigrationStep implements MigrationStep {
    @Override
    public void execute() {
      throw new UnsupportedOperationException("execute not implemented");
    }
  }

  public static final class MigrationStep1 extends NoopMigrationStep {

  }

  public static final class MigrationStep2 extends NoopMigrationStep {

  }

  public static final class MigrationStep3 extends NoopMigrationStep {

  }

  public static final class Clazz1 {

  }

  public static final class Clazz2 {

  }

  public static final class Clazz3 {

  }
}
