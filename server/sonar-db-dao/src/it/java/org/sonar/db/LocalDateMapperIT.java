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
package org.sonar.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.utils.System2;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.sonar.server.platform.db.migration.def.DateColumnDef.newDateColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

class LocalDateMapperIT {
  private static final String TABLE = "local_date_mapper_it";
  private static final LocalDate REQUIRED_DATE = LocalDate.of(2000, Month.JANUARY, 1);
  private static final Set<String> TIME_ZONE_DATABASES = Set.of("H2", "PostgreSQL", "Oracle");

  @RegisterExtension
  private final DbTester dbTester = DbTester.createWithExtensionMappers(System2.INSTANCE, DateMapper.class);

  private DbSession session;
  private DateMapper mapper;
  private boolean tableCreated;

  @BeforeEach
  void create_table() throws SQLException {
    Database database = dbTester.getDbClient().getDatabase();
    CreateTableBuilder tableBuilder = new CreateTableBuilder(database.getDialect(), TABLE)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName("row_key").setLimit(30).setIsNullable(false).build())
      .addColumn(newDateColumnDefBuilder().setColumnName("date_value").build())
      .addColumn(newDateColumnDefBuilder().setColumnName("required_date").setIsNullable(false).build());

    try (Connection connection = database.getDataSource().getConnection();
         Statement statement = connection.createStatement()) {
      for (String sql : tableBuilder.build()) {
        statement.execute(sql);
        tableCreated = true;
      }
      commitIfNeeded(connection);
    }

    session = dbTester.getSession();
    mapper = session.getMapper(DateMapper.class);
  }

  @AfterEach
  void drop_table() throws SQLException {
    try {
      if (session != null) {
        session.close();
        session = null;
      }
    } finally {
      if (tableCreated) {
        Database database = dbTester.getDbClient().getDatabase();
        try (Connection connection = database.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
          statement.execute("DROP TABLE " + TABLE);
          commitIfNeeded(connection);
        }
        tableCreated = false;
      }
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("local_date_examples")
  void local_date_round_trip_preserves_calendar_date(String rowKey, LocalDate dateValue) {
    // Arrange
    insertDate(rowKey, dateValue);
    session.commit();

    // Act
    LocalDate actual = mapper.selectDate(rowKey);

    // Assert
    assertThat(actual).isEqualTo(dateValue);
  }

  @Test
  void non_nullable_date_columns_reject_null_local_date_inserted_via_mapper() {
    // Arrange
    LocalDate dateValue = LocalDate.of(2024, Month.FEBRUARY, 29);

    // Act
    Throwable thrown = catchThrowable(() -> mapper.insertDate("null_required", dateValue, null));

    // Assert
    assertThat(thrown).isInstanceOf(PersistenceException.class);
  }

  @Test
  void local_date_maps_to_date_with_midnight_time_component_on_oracle() throws SQLException {
    // Arrange
    String databaseProduct = session.getConnection().getMetaData().getDatabaseProductName();
    assumeTrue("Oracle".equals(databaseProduct), "Oracle-specific native DATE assertion");
    insertDate("oracle_midnight", LocalDate.of(2026, Month.MARCH, 8));
    session.commit();

    // Act
    int midnight = mapper.hasMidnightTime("oracle_midnight");

    // Assert
    assertThat(midnight).isEqualTo(1);
  }

  @Test
  void java_local_date_to_sql_date_mapping_is_agnostic_to_session_time_zone() throws SQLException {
    // Arrange
    Connection connection = session.getConnection();
    String databaseProduct = connection.getMetaData().getDatabaseProductName();
    String dialectId = dbTester.getDbClient().getDatabase().getDialect().getId();
    System.out.println("LocalDateMapperIT: systemZone=" + ZoneId.systemDefault() + ", dialect=" + dialectId + ", database=" + databaseProduct);
    String unsupportedDatabaseReason = "Microsoft SQL Server".equals(databaseProduct)
      ? "Not applicable: SQL Server has no session timezone setting"
      : "Session timezone check supports H2, PostgreSQL, and Oracle";
    assumeTrue(TIME_ZONE_DATABASES.contains(databaseProduct), unsupportedDatabaseReason);
    String originalTimeZone = readSessionTimeZone(connection, databaseProduct);
    assertThat(originalTimeZone).isNotBlank();

    try {
      // Act
      setSessionTimeZone(connection, databaseProduct, "UTC");
      insertDate("written_utc", LocalDate.of(2026, Month.MARCH, 8));
      session.commit();

      setSessionTimeZone(connection, databaseProduct, "America/Los_Angeles");
      session.clearCache();
      LocalDate readInNonUtc = mapper.selectDate("written_utc");

      insertDate("written_non_utc", LocalDate.of(2026, Month.NOVEMBER, 1));
      session.commit();

      setSessionTimeZone(connection, databaseProduct, "UTC");
      session.clearCache();
      LocalDate readInUtc = mapper.selectDate("written_non_utc");

      // Assert
      assertThat(readInNonUtc).isEqualTo(LocalDate.of(2026, Month.MARCH, 8));
      assertThat(readInUtc).isEqualTo(LocalDate.of(2026, Month.NOVEMBER, 1));
    } finally {
      try {
        session.rollback(true);
      } finally {
        setSessionTimeZone(connection, databaseProduct, originalTimeZone);
        session.commit(true);
      }
    }
  }

  private static Stream<Arguments> local_date_examples() {
    return Stream.of(
      Arguments.of("leap_day", LocalDate.of(2024, Month.FEBRUARY, 29)),
      Arguments.of("pre_epoch", LocalDate.of(1969, Month.DECEMBER, 31)),
      Arguments.of("year_end_2025", LocalDate.of(2025, Month.DECEMBER, 31)),
      Arguments.of("year_start_2026", LocalDate.of(2026, Month.JANUARY, 1)),
      Arguments.of("spring_dst", LocalDate.of(2026, Month.MARCH, 8)),
      Arguments.of("fall_dst", LocalDate.of(2026, Month.NOVEMBER, 1)),
      Arguments.of("minimum_supported", LocalDate.of(1582, Month.OCTOBER, 15)),
      Arguments.of("maximum_supported", LocalDate.of(9999, Month.DECEMBER, 31)),
      Arguments.of("sql_null", null));
  }

  private void insertDate(String rowKey, LocalDate dateValue) {
    assertThat(mapper.insertDate(rowKey, dateValue, dateValue == null ? REQUIRED_DATE : dateValue)).isEqualTo(1);
  }

  private static String readSessionTimeZone(Connection connection, String databaseProduct) throws SQLException {
    String query = switch (databaseProduct) {
      case "H2" -> "SELECT SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE SETTING_NAME = 'TIME ZONE'";
      case "PostgreSQL" -> "SELECT current_setting('TimeZone')";
      case "Oracle" -> "SELECT SESSIONTIMEZONE FROM dual";
      default -> throw new IllegalArgumentException("Unsupported database product: " + databaseProduct);
    };
    try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {
      if (!resultSet.next()) {
        throw new SQLException("Session timezone query returned no row");
      }
      return resultSet.getString(1);
    }
  }

  private static void setSessionTimeZone(Connection connection, String databaseProduct, String timeZone) throws SQLException {
    String quotedTimeZone = timeZone.replace("'", "''");
    String sql = switch (databaseProduct) {
      case "H2", "PostgreSQL" -> "SET TIME ZONE '" + quotedTimeZone + "'";
      case "Oracle" -> "ALTER SESSION SET TIME_ZONE = '" + quotedTimeZone + "'";
      default -> throw new IllegalArgumentException("Unsupported database product: " + databaseProduct);
    };
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private static void commitIfNeeded(Connection connection) throws SQLException {
    if (!connection.getAutoCommit()) {
      connection.commit();
    }
  }

  public interface DateMapper {
    @Insert("INSERT INTO " + TABLE + " (row_key, date_value, required_date) VALUES "
      + "(#{key,jdbcType=VARCHAR}, #{dateValue,javaType=java.time.LocalDate,jdbcType=DATE}, "
      + "#{requiredDate,javaType=java.time.LocalDate,jdbcType=DATE})")
    int insertDate(@Param("key") String key, @Param("dateValue") LocalDate dateValue, @Param("requiredDate") LocalDate requiredDate);

    @Select("SELECT date_value FROM " + TABLE + " WHERE row_key = #{key,jdbcType=VARCHAR}")
    LocalDate selectDate(@Param("key") String key);

    @Select("SELECT CASE WHEN date_value = TRUNC(date_value) THEN 1 ELSE 0 END FROM " + TABLE + " WHERE row_key = #{key,jdbcType=VARCHAR}")
    int hasMidnightTime(@Param("key") String key);
  }
}
