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
package org.sonar.server.platform.db.migration.step;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationStepExecutionExceptionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private RegisteredMigrationStep step = new RegisteredMigrationStep(1, "foo", MigrationStep.class);
  private MigrationStepExecutionException underTest = new MigrationStepExecutionException(
      step, new IllegalArgumentException("some cause"));

  @Test
  public void MigrationStepExecutionException_is_unchecked() {
    assertThat(RuntimeException.class.isAssignableFrom(MigrationStepExecutionException.class)).isTrue();
  }

  @Test
  public void constructor_throws_NPE_if_step_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("RegisteredMigrationStep can't be null");

    new MigrationStepExecutionException(null, new NullPointerException("Some cause"));
  }

  @Test
  public void constructor_throws_NPE_if_cause_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("cause can't be null");

    new MigrationStepExecutionException(new RegisteredMigrationStep(1, "foo", MigrationStep.class), null);
  }

  @Test
  public void constructor_sets_exception_message_from_step_argument() {
    assertThat(underTest.getMessage()).isEqualTo("Execution of migration step #1 'foo' failed");
  }

  @Test
  public void getFailingStep_returns_constructor_argument() {
    assertThat(underTest.getFailingStep()).isSameAs(step);
  }
}
