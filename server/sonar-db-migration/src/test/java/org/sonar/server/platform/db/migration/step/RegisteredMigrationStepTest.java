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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegisteredMigrationStepTest {

  @Test
  public void constructor_throws_NPE_if_description_is_null() {
    assertThatThrownBy(() -> new RegisteredMigrationStep(1, null, MigrationStep.class))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("description can't be null");
  }

  @Test
  public void constructor_throws_NPE_if_MigrationStep_class_is_null() {
    assertThatThrownBy(() -> new RegisteredMigrationStep(1, "", null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("MigrationStep class can't be null");
  }

  @Test
  public void verify_getters() {
    RegisteredMigrationStep underTest = new RegisteredMigrationStep(3, "foo", MyMigrationStep.class);
    assertThat(underTest.getMigrationNumber()).isEqualTo(3L);
    assertThat(underTest.getDescription()).isEqualTo("foo");
    assertThat(underTest.getStepClass()).isEqualTo(MyMigrationStep.class);
  }

  private static abstract class MyMigrationStep implements MigrationStep {

  }
}
