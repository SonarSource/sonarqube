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
package org.sonar.server.platform.db.migration.version.v202605;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoryBackfillSupportTest {

  @Test
  void toUtcDayEpoch_truncatesToUtcMidnight() {
    long midday = Instant.parse("2026-09-09T19:42:13Z").toEpochMilli();

    assertThat(HistoryBackfillSupport.toUtcDayEpoch(midday))
      .isEqualTo(Instant.parse("2026-09-09T00:00:00Z").toEpochMilli());
  }

  @Test
  void parseFrozenEpoch_rejectsAnEpochThatIsNotAtUtcMidnight() {
    assertThatThrownBy(() -> HistoryBackfillSupport.parseFrozenEpoch("123"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("History backfill epoch is not a UTC-day epoch: 123");
  }

  @Test
  void formatMeasureValue_matchesHistoryMetricFormatting() {
    assertThat(HistoryBackfillSupport.formatMeasureValue("BOOL", 1.0D)).isEqualTo("true");
    assertThat(HistoryBackfillSupport.formatMeasureValue("BOOL", 0.0D)).isEqualTo("false");
    assertThat(HistoryBackfillSupport.formatMeasureValue("INT", 42.0D)).isEqualTo("42");
    assertThat(HistoryBackfillSupport.formatMeasureValue("RATING", 1.0D)).isEqualTo("1.0");
    assertThat(HistoryBackfillSupport.formatMeasureValue("WORK_DUR", "123.0")).isEqualTo("123");
    assertThat(HistoryBackfillSupport.formatMeasureValue("PERCENT", 42.5D)).isEqualTo("42.5");
    assertThat(HistoryBackfillSupport.formatMeasureValue("INT", "not-a-number")).isEqualTo("not-a-number");
  }

  @Test
  void formatMeasureValue_preservesRatingValuesWithoutIntegerNormalization() {
    assertThat(HistoryBackfillSupport.formatMeasureValue("RATING", 3.0D)).isEqualTo("3.0");
    assertThat(HistoryBackfillSupport.formatMeasureValue("RATING", 5.0D)).isEqualTo("5.0");
    assertThat(HistoryBackfillSupport.formatMeasureValue("RATING", 2.5D)).isEqualTo("2.5");
    assertThat(HistoryBackfillSupport.formatMeasureValue("RATING", "1.0")).isEqualTo("1.0");
    assertThat(HistoryBackfillSupport.formatMeasureValue("RATING", "3.00")).isEqualTo("3.00");
    assertThat(HistoryBackfillSupport.formatMeasureValue("RATING", 1)).isEqualTo("1");
    assertThat(HistoryBackfillSupport.formatMeasureValue("RATING", null)).isNull();
  }

  @Test
  void issueDimension_hash_matchesTheHistoryDimensionHashContract() {
    HistoryBackfillSupport.IssueDimension dimension = new HistoryBackfillSupport.IssueDimension(
      null, "MAIN", "CRITICAL", "FALSE_POSITIVE", "RESOLVED", 2, "java:S123", (short) 0, (short) 4, (short) 3);

    assertThat(dimension.hash()).isEqualTo("b98b93632d1e68bdaf2a43bbeac4f7ae");
  }

  @Test
  void incrementIssueCount_groupsEquivalentDimensions() {
    HistoryBackfillSupport.IssueDimension dimension = new HistoryBackfillSupport.IssueDimension(
      null, "MAIN", "MAJOR", "OPEN", "OPEN", 2, "java:S124", (short) 0, (short) 0, (short) 0);
    Map<HistoryBackfillSupport.IssueDimension, Integer> counts = new HashMap<>();

    HistoryBackfillSupport.incrementIssueCount(counts, dimension);
    HistoryBackfillSupport.incrementIssueCount(counts, dimension);

    assertThat(counts).containsExactly(Map.entry(dimension, 2));
  }

  @Test
  void collectIssueCounts_usesOverridesWithoutCartesianImpactRows() throws SQLException {
    Database database = mock(Database.class);
    Dialect dialect = mock(Dialect.class);
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    HistoryBackfillSupport.PhaseStatistics statistics = new HistoryBackfillSupport.PhaseStatistics();
    HistoryBackfillSupport.IssueDimension dimension = new HistoryBackfillSupport.IssueDimension(
      null, "MAIN", "MAJOR", "OPEN", "OPEN", 2, "java:S123", (short) 2, (short) 4, (short) 0);
    Map<HistoryBackfillSupport.IssueDimension, Integer> counts = new HashMap<>();

    when(database.getDialect()).thenReturn(dialect);
    when(dialect.getScrollDefaultFetchSize()).thenReturn(123);
    when(connection.prepareStatement(any(), eq(ResultSet.TYPE_FORWARD_ONLY), eq(ResultSet.CONCUR_READ_ONLY))).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true, true, true, true, false);
    when(resultSet.getString(1)).thenReturn("issue-1");
    when(resultSet.getInt(2)).thenReturn(2);
    when(resultSet.getString(3)).thenReturn("MAJOR");
    when(resultSet.getString(4)).thenReturn("OPEN");
    when(resultSet.getString(5)).thenReturn(null);
    when(resultSet.getString(6)).thenReturn("FIL");
    when(resultSet.getString(7)).thenReturn("java");
    when(resultSet.getString(8)).thenReturn("S123");
    when(resultSet.getString(9)).thenReturn("MAINTAINABILITY", "SECURITY", "MAINTAINABILITY", "SECURITY");
    when(resultSet.getString(10)).thenReturn("LOW", "HIGH", "LOW", "HIGH");
    when(resultSet.getString(11)).thenReturn("MAINTAINABILITY", "SECURITY", "MAINTAINABILITY", "SECURITY");
    when(resultSet.getString(12)).thenReturn("CRITICAL", "BLOCKER", "CRITICAL", "BLOCKER");

    HistoryBackfillSupport.collectIssueCounts(database, connection, "branch-1", counts, statistics);

    verify(statement).setInt(4, 4);
    assertThat(statistics.issuesProcessed()).isOne();
    assertThat(counts).containsExactly(Map.entry(dimension, 1));
    assertThat(statistics.describe(10)).contains("issuesProcessed=1");
  }

  @Test
  void prepareScrollingSelect_usesForwardOnlyReadOnlyCursorAndDialectFetchSize() throws SQLException {
    Database database = mock(Database.class);
    Dialect dialect = mock(Dialect.class);
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    String sql = "select uuid from projects";
    when(database.getDialect()).thenReturn(dialect);
    when(dialect.getScrollDefaultFetchSize()).thenReturn(123);
    when(connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).thenReturn(statement);

    assertThat(HistoryBackfillSupport.prepareScrollingSelect(database, connection, sql)).isSameAs(statement);

    verify(connection).prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    verify(statement).setFetchSize(123);
  }

  @Test
  void setLargeText_usesVarcharBindingsForPostgreSqlAndClobBindingsForOtherDialects() throws SQLException {
    String largeText = "a".repeat(4_001);
    Database postgreSqlDatabase = mock(Database.class);
    Dialect postgreSqlDialect = mock(Dialect.class);
    PreparedStatement postgreSqlStatement = mock(PreparedStatement.class);
    when(postgreSqlDatabase.getDialect()).thenReturn(postgreSqlDialect);
    when(postgreSqlDialect.getId()).thenReturn(PostgreSql.ID);

    HistoryBackfillSupport.setLargeText(postgreSqlDatabase, postgreSqlStatement, 1, largeText);
    HistoryBackfillSupport.setLargeText(postgreSqlDatabase, postgreSqlStatement, 2, null);

    verify(postgreSqlStatement).setString(1, largeText);
    verify(postgreSqlStatement).setNull(2, Types.VARCHAR);

    Database h2Database = mock(Database.class);
    Dialect h2Dialect = mock(Dialect.class);
    PreparedStatement h2Statement = mock(PreparedStatement.class);
    when(h2Database.getDialect()).thenReturn(h2Dialect);
    when(h2Dialect.getId()).thenReturn(H2.ID);

    HistoryBackfillSupport.setLargeText(h2Database, h2Statement, 1, largeText);
    HistoryBackfillSupport.setLargeText(h2Database, h2Statement, 2, null);

    verify(h2Statement).setCharacterStream(eq(1), any(StringReader.class), eq(largeText.length()));
    verify(h2Statement).setNull(2, Types.CLOB);
  }

  @Test
  void readLargeText_readsAllCharactersFromTheResultSetStream() throws SQLException {
    String largeText = "a".repeat(4_001);
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getCharacterStream(1)).thenReturn(new StringReader(largeText));

    assertThat(HistoryBackfillSupport.readLargeText(resultSet, 1)).isEqualTo(largeText);
  }
}
