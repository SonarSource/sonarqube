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
package org.sonar.server.platform.db.migration.version;

import java.util.HashSet;
import java.util.Set;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.step.MigrationNumber.validate;

public class DbVersionTestUtils {

  public static void verifyMinimumMigrationNumber(DbVersion underTest, int minimumMigrationNumber) {
    TestMigrationStepRegistry registry = new TestMigrationStepRegistry() {
      @Override
      public <T extends MigrationStep> MigrationStepRegistry add(long migrationNumber, String description, Class<T> stepClass) {
        super.add(migrationNumber, description, MigrationStep.class);

        assertThat(migrationNumber).isGreaterThanOrEqualTo(minimumMigrationNumber);
        return this;
      }
    };

    underTest.addSteps(registry);

    assertThat(registry.migrationNumbers).describedAs("No migration added to registry").isNotEmpty();
    assertThat(registry.migrationNumbers.stream().sorted().findFirst().get()).isEqualTo(minimumMigrationNumber);
  }

  public static void verifyMigrationCount(DbVersion underTest, int migrationCount) {
    TestMigrationStepRegistry registry = new TestMigrationStepRegistry();
    underTest.addSteps(registry);
    assertThat(registry.migrationNumbers).hasSize(migrationCount);
  }

  private static class TestMigrationStepRegistry implements MigrationStepRegistry {
    private Set<Long> migrationNumbers = new HashSet<>();

    @Override
    public <T extends MigrationStep> MigrationStepRegistry add(long migrationNumber, String description, Class<T> stepClass) {
      validate(migrationNumber);
      assertThat(description).isNotEmpty();
      assertThat(stepClass).isNotNull();
      assertThat(migrationNumbers.add(migrationNumber)).isTrue();
      return this;
    }
  }
}
