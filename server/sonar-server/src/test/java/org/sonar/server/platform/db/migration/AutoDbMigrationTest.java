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
package org.sonar.server.platform.db.migration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbClient;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.DefaultServerUpgradeStatus;
import org.sonar.server.platform.db.migration.engine.MigrationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AutoDbMigrationTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  private DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  private DefaultServerUpgradeStatus serverUpgradeStatus = mock(DefaultServerUpgradeStatus.class);
  private MigrationEngine migrationEngine = mock(MigrationEngine.class);
  private AutoDbMigration underTest = new AutoDbMigration(serverUpgradeStatus, migrationEngine);

  @Test
  public void start_runs_MigrationEngine_on_h2_if_fresh_install() {
    start_runs_MigrationEngine_for_dialect_if_fresh_install(new H2());
  }

  @Test
  public void start_runs_MigrationEngine_on_postgre_if_fresh_install() {
    start_runs_MigrationEngine_for_dialect_if_fresh_install(new PostgreSql());
  }

  @Test
  public void start_runs_MigrationEngine_on_Oracle_if_fresh_install() {
    start_runs_MigrationEngine_for_dialect_if_fresh_install(new Oracle());
  }

  @Test
  public void start_runs_MigrationEngine_on_MsSQL_if_fresh_install() {
    start_runs_MigrationEngine_for_dialect_if_fresh_install(new MsSql());
  }

  private void start_runs_MigrationEngine_for_dialect_if_fresh_install(Dialect dialect) {
    mockDialect(dialect);
    mockFreshInstall(true);

    underTest.start();

    verify(migrationEngine).execute();
    verifyInfoLog();
  }

  @Test
  public void start_does_nothing_if_not_fresh_install() {
    mockFreshInstall(false);

    underTest.start();

    verifyZeroInteractions(migrationEngine);
    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
  }

  @Test
  public void start_runs_MigrationEngine_if_blue_green_upgrade() {
    mockFreshInstall(false);
    when(serverUpgradeStatus.isUpgraded()).thenReturn(true);
    when(serverUpgradeStatus.isBlueGreen()).thenReturn(true);

    underTest.start();

    verify(migrationEngine).execute();
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Automatically perform DB migration on blue/green deployment");
  }

  @Test
  public void start_does_nothing_if_blue_green_but_no_upgrade() {
    mockFreshInstall(false);
    when(serverUpgradeStatus.isUpgraded()).thenReturn(false);
    when(serverUpgradeStatus.isBlueGreen()).thenReturn(true);

    underTest.start();

    verifyZeroInteractions(migrationEngine);
    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
  }

  @Test
  public void stop_has_no_effect() {
    underTest.stop();
  }

  private void mockFreshInstall(boolean value) {
    when(serverUpgradeStatus.isFreshInstall()).thenReturn(value);
  }

  private void mockDialect(Dialect dialect) {
    when(dbClient.getDatabase().getDialect()).thenReturn(dialect);
  }

  private void verifyInfoLog() {
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.INFO)).containsExactly("Automatically perform DB migration on fresh install");
  }

}
