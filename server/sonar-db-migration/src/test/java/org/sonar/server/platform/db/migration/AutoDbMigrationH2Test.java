/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.db.migration;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.platform.db.migration.engine.MigrationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AutoDbMigrationH2Test {
  @Rule
  public DbTester dbTester = DbTester.createForSchema(System2.INSTANCE, AutoDbMigrationH2Test.class, "schema_migrations.sql");

  private DbClient dbClient = dbTester.getDbClient();
  private ServerUpgradeStatus serverUpgradeStatus = mock(ServerUpgradeStatus.class);
  private MigrationEngine migrationEngine = mock(MigrationEngine.class);

  private AutoDbMigration underTest = new AutoDbMigration(serverUpgradeStatus, dbClient, migrationEngine);

  @Test
  public void start_creates_fake_rows_in_project_measures_to_fix_sql_plan_of_measure_tree_queries() {
    when(serverUpgradeStatus.isFreshInstall()).thenReturn(true);

    underTest.start();

    verifyNoMoreInteractions(migrationEngine);
    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(999);
  }
}
