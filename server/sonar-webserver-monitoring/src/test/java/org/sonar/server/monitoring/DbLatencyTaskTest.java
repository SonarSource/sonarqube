/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.monitoring;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.slf4j.event.Level;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.monitoring.DbLatencyTask.WARN_LOG_INTERVAL_IN_MILISECONDS;

class DbLatencyTaskTest {

  private static final String VALIDATION_QUERY = "SELECT 1 FROM DUAL";

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final ServerMonitoringMetrics metrics = mock(ServerMonitoringMetrics.class);
  private final Database database = mock(Database.class);
  private final Dialect dialect = mock(Dialect.class);
  private final DataSource dataSource = mock(DataSource.class);
  private final Connection connection = mock(Connection.class);
  private final Statement statement = mock(Statement.class);
  private final ResultSet resultSet = mock(ResultSet.class);
  private final System2 system = mock(System2.class);
  private final MapSettings settings = new MapSettings();
  private final AtomicLong nanoTime = new AtomicLong();

  private final DbLatencyTask underTest = new DbLatencyTask(metrics, database, settings.asConfig(), system, nanoTime::get);

  @BeforeEach
  void before() throws SQLException {
    settings.setProperty("sonar.performanceMonitoring.enabled", "true");
    when(database.getDialect()).thenReturn(dialect);
    when(database.getDataSource()).thenReturn(dataSource);
    when(dialect.getValidationQuery()).thenReturn(VALIDATION_QUERY);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(VALIDATION_QUERY)).thenAnswer(invocation -> {
      advanceMillis(3);
      return resultSet;
    });
  }

  @Test
  void run_whenPerformanceMonitoringDisabled_setsLatencyToNaNWithoutQueryingDatabase() {
    settings.setProperty("sonar.performanceMonitoring.enabled", "false");

    underTest.run();

    verify(metrics).setDbLatency(Double.NaN);
    verifyNoInteractions(database);
  }

  @Test
  void run_whenPerformanceMonitoringNotSet_setsLatencyToNaNWithoutQueryingDatabase() {
    settings.removeProperty("sonar.performanceMonitoring.enabled");

    underTest.run();

    verify(metrics).setDbLatency(Double.NaN);
    verifyNoInteractions(database);
  }

  @Test
  void run_executesDialectValidationQueryAndSetsLatency() throws SQLException {
    underTest.run();

    InOrder inOrder = inOrder(dataSource, connection, statement, resultSet);
    inOrder.verify(dataSource).getConnection();
    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).executeQuery(VALIDATION_QUERY);
    inOrder.verify(resultSet).next();
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();
    inOrder.verify(connection).close();
    verify(metrics).setDbLatency(0.003);
  }

  @Test
  void run_excludesTimeSpentWaitingForPooledConnection() throws SQLException {
    when(dataSource.getConnection()).thenAnswer(invocation -> {
      advanceMillis(200);
      return connection;
    });

    underTest.run();

    verify(metrics).setDbLatency(0.003);
  }

  @Test
  void run_whenQueryFails_logsErrorAndSetsLatencyToNaN() throws SQLException {
    when(statement.executeQuery(VALIDATION_QUERY)).thenThrow(new SQLException("boom"));

    underTest.run();

    verify(connection).close();
    verify(metrics).setDbLatency(Double.NaN);
    assertThat(logTester.logs(Level.ERROR)).containsExactly("Failed to measure database latency");
  }

  @Test
  void run_whenConnectionCannotBeObtained_logsErrorAndSetsLatencyToNaN() throws SQLException {
    when(dataSource.getConnection()).thenThrow(new SQLException("pool exhausted"));

    underTest.run();

    verify(metrics).setDbLatency(Double.NaN);
    assertThat(logTester.logs(Level.ERROR)).containsExactly("Failed to measure database latency");
  }

  @Test
  void run_whenLatencyBelowThreshold_doesNotLogWarning() {
    underTest.run();

    verify(metrics).setDbLatency(anyDouble());
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void run_whenLatencyAboveThreshold_logsWarningThrottled() {
    settings.setProperty("sonar.server.monitoring.db.latency.warn.threshold", "2");
    long now = 1_000_000L;
    when(system.now()).thenReturn(now);

    underTest.run();
    assertThat(logTester.logs(Level.WARN)).containsExactly(
      "Database latency is 3.0 ms, which is above the threshold of 2 ms. This may degrade SonarQube performance.");

    logTester.clear();
    when(system.now()).thenReturn(now + WARN_LOG_INTERVAL_IN_MILISECONDS - 1);
    underTest.run();
    assertThat(logTester.logs(Level.WARN)).isEmpty();

    when(system.now()).thenReturn(now + WARN_LOG_INTERVAL_IN_MILISECONDS);
    underTest.run();
    assertThat(logTester.logs(Level.WARN)).hasSize(1);
  }

  @Test
  void getDelay_returnNumberIfConfigEmpty() {
    assertThat(underTest.getDelay()).isEqualTo(10_000L);
  }

  @Test
  void getDelay_returnNumberFromConfig() {
    settings.setProperty("sonar.server.monitoring.db.latency.initial.delay", "100000");

    assertThat(underTest.getDelay()).isEqualTo(100_000L);
  }

  @Test
  void getPeriod_returnNumberIfConfigEmpty() {
    assertThat(underTest.getPeriod()).isEqualTo(60_000L);
  }

  @Test
  void getPeriod_returnNumberFromConfig() {
    settings.setProperty("sonar.server.monitoring.db.latency.period", "100000");

    assertThat(underTest.getPeriod()).isEqualTo(100_000L);
  }

  private void advanceMillis(long millis) {
    nanoTime.addAndGet(millis * 1_000_000L);
  }
}
