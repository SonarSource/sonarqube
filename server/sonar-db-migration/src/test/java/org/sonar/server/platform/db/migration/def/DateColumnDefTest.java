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
package org.sonar.server.platform.db.migration.def;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.db.migration.def.DateColumnDef.newDateColumnDefBuilder;

@RunWith(DataProviderRunner.class)
public class DateColumnDefTest {
  @Test
  @UseDataProvider("supported_dialects")
  public void generates_valid_sql_type_for_supported_dialects(Dialect dialect) {
    // Arrange
    DateColumnDef dateColumnDef = newDateColumnDefBuilder()
      .setColumnName("created_at")
      .build();

    // Act & Assert
    assertThat(dateColumnDef.generateSqlType(dialect)).isEqualTo("DATE");
  }

  @DataProvider
  public static Object[][] supported_dialects() {
    return new Object[][]{
      {new H2()},
      {new PostgreSql()},
      {new Oracle()},
      {new MsSql()}
    };
  }

  @Test
  public void rejects_unsupported_dialect() {
    // Arrange
    DateColumnDef dateColumnDef = newDateColumnDefBuilder()
      .setColumnName("created_at")
      .build();
    Dialect unsupportedDialect = mock(Dialect.class);
    when(unsupportedDialect.getId()).thenReturn("unsupported");

    // Act & Assert
    assertThatThrownBy(() -> dateColumnDef.generateSqlType(unsupportedDialect))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @UseDataProvider("invalid_column_definitions")
  public void rejects_invalid_column_definitions(String description, Consumer<DateColumnDef.Builder> operation, Class<? extends Throwable> expectedException) {
    // Arrange
    DateColumnDef.Builder builder = newDateColumnDefBuilder();

    // Act & Assert
    assertThatThrownBy(() -> operation.accept(builder))
      .as(description)
      .isInstanceOf(expectedException);
  }

  @DataProvider
  public static Object[][] invalid_column_definitions() {
    return new Object[][]{
      {"null column name", setColumnName(null), NullPointerException.class},
      {"missing column name", buildWithoutColumnName(), NullPointerException.class},
      {"invalid column name", setColumnName("INVALID NAME"), IllegalArgumentException.class},
      {"empty column name", setColumnName(""), IllegalArgumentException.class},
      {"blank column name", setColumnName(" "), IllegalArgumentException.class},
      {"uppercase column name", setColumnName("Created_at"), IllegalArgumentException.class},
      {"illegal column name characters", setColumnName("created-at"), IllegalArgumentException.class},
      {"column name starting with a digit", setColumnName("1created"), IllegalArgumentException.class},
      {"column name starting with an underscore", setColumnName("_created"), IllegalArgumentException.class},
      {"reserved keyword column name", setColumnName("select"), IllegalArgumentException.class}
    };
  }

  private static Consumer<DateColumnDef.Builder> setColumnName(String columnName) {
    return builder -> builder.setColumnName(columnName);
  }

  private static Consumer<DateColumnDef.Builder> buildWithoutColumnName() {
    return DateColumnDef.Builder::build;
  }
}
