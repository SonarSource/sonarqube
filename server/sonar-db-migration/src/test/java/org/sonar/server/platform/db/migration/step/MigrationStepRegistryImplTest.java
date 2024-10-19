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
package org.sonar.server.platform.db.migration.step;

import java.util.List;
import java.util.Random;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MigrationStepRegistryImplTest {

  private MigrationStepRegistryImpl underTest = new MigrationStepRegistryImpl();

  @Test
  public void add_fails_with_IAE_if_migrationNumber_is_less_than_0() {
    assertThatThrownBy(() -> {
      underTest.add(-Math.abs(new Random().nextLong() + 1), "sdsd", MigrationStep.class);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Migration number must be >= 0");
  }

  @Test
  public void add_fails_with_NPE_if_description_is_null() {
    assertThatThrownBy(() -> {
      underTest.add(12, null, MigrationStep.class);
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("description can't be null");
  }

  @Test
  public void add_fails_with_IAE_if_description_is_empty() {
    assertThatThrownBy(() -> {
      underTest.add(12, "", MigrationStep.class);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("description can't be empty");
  }

  @Test
  public void add_fails_with_NPE_is_migrationstep_class_is_null() {
    assertThatThrownBy(() -> {
      underTest.add(12, "sdsd", null);
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("MigrationStep class can't be null");
  }

  @Test
  public void add_fails_with_ISE_when_called_twice_with_same_migration_number() {
    underTest.add(12, "dsd", MigrationStep.class);

    assertThatThrownBy(() -> underTest.add(12, "dfsdf", MigrationStep.class))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("A migration is already registered for migration number '12'");
  }

  @Test
  public void build_fails_with_ISE_if_registry_is_empty() {
    assertThatThrownBy(() -> underTest.build())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Registry is empty");
  }

  @Test
  public void build_returns_a_MigrationStepsImpl_with_all_steps_added_to_registry_ordered_by_migration_number() {
    underTest.add(343, "sss", MigrationStep2.class);
    underTest.add(5, "aazsa", MigrationStep1.class);
    underTest.add(66, "bbb", MigrationStep3.class);
    underTest.add(2, "aaaa", MigrationStep4.class);

    MigrationSteps migrationSteps = underTest.build();
    assertThat(migrationSteps).isInstanceOf(MigrationStepsImpl.class);
    List<RegisteredMigrationStep> registeredMigrationSteps = migrationSteps.readAll();
    assertThat(registeredMigrationSteps).hasSize(4);
    verify(registeredMigrationSteps.get(0), 2, "aaaa", MigrationStep4.class);
    verify(registeredMigrationSteps.get(1), 5, "aazsa", MigrationStep1.class);
    verify(registeredMigrationSteps.get(2), 66, "bbb", MigrationStep3.class);
    verify(registeredMigrationSteps.get(3), 343, "sss", MigrationStep2.class);
  }

  private static void verify(RegisteredMigrationStep step, int migrationNUmber, String description, Class<? extends MigrationStep> stepClass) {
    assertThat(step.getMigrationNumber()).isEqualTo(migrationNUmber);
    assertThat(step.getDescription()).isEqualTo(description);
    assertThat(step.getStepClass()).isEqualTo(stepClass);
  }

  private static abstract class NoopMigrationStep implements MigrationStep {
    @Override
    public void execute() {
      throw new IllegalStateException("execute is not implemented");
    }
  }

  private static class MigrationStep1 extends NoopMigrationStep {

  }

  private static class MigrationStep2 extends NoopMigrationStep {

  }

  private static class MigrationStep3 extends NoopMigrationStep {

  }

  private static class MigrationStep4 extends NoopMigrationStep {

  }
}
