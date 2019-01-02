/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.MigrationSteps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.db.migration.version.DatabaseVersion.Status.FRESH_INSTALL;
import static org.sonar.server.platform.db.migration.version.DatabaseVersion.Status.REQUIRES_DOWNGRADE;
import static org.sonar.server.platform.db.migration.version.DatabaseVersion.Status.REQUIRES_UPGRADE;
import static org.sonar.server.platform.db.migration.version.DatabaseVersion.Status.UP_TO_DATE;

public class DatabaseVersionTest {

  private MigrationHistory migrationHistory = mock(MigrationHistory.class);
  private MigrationSteps migrationSteps = mock(MigrationSteps.class);
  private DatabaseVersion underTest = new DatabaseVersion(migrationSteps, migrationHistory);

  @Test
  public void getStatus_returns_FRESH_INSTALL_when_table_is_empty() {
    mockMaxMigrationNumberInDb(null);
    mockMaxMigrationNumberInConfig(150L);
    assertThat(underTest.getStatus()).isEqualTo(FRESH_INSTALL);
  }

  @Test
  public void getStatus_returns_REQUIRES_UPGRADE_when_max_migration_number_in_table_is_less_than_max_migration_number_in_configuration() {
    mockMaxMigrationNumberInDb(123L);
    mockMaxMigrationNumberInConfig(150L);

    assertThat(underTest.getStatus()).isEqualTo(REQUIRES_UPGRADE);
  }

  @Test
  public void getStatus_returns_UP_TO_DATE_when_max_migration_number_in_table_is_equal_to_max_migration_number_in_configuration() {
    mockMaxMigrationNumberInDb(150L);
    mockMaxMigrationNumberInConfig(150L);

    assertThat(underTest.getStatus()).isEqualTo(UP_TO_DATE);
  }

  @Test
  public void getStatus_returns_REQUIRES_DOWNGRADE_when_max_migration_number_in_table_is_greater_than_max_migration_number_in_configuration() {
    mockMaxMigrationNumberInDb(200L);
    mockMaxMigrationNumberInConfig(150L);

    assertThat(underTest.getStatus()).isEqualTo(REQUIRES_DOWNGRADE);
  }

  private void mockMaxMigrationNumberInDb(@Nullable Long value1) {
    when(migrationHistory.getLastMigrationNumber()).thenReturn(Optional.ofNullable(value1));
  }

  private void mockMaxMigrationNumberInConfig(long value) {
    when(migrationSteps.getMaxMigrationNumber()).thenReturn(value);
  }
}
