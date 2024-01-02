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
package org.sonar.server.platform.db.migration.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.Test;
import org.sonar.db.Database;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DropMsSQLDefaultConstraintsBuilderTest {

  private final Database db = mock(Database.class);

  @Test
  public void fail_if_oracle() throws Exception {
    when(db.getDialect()).thenReturn(new Oracle());
    assertThatThrownBy(() -> {
      new DropMsSQLDefaultConstraintsBuilder(db).setTable("snapshots").setColumns("variation_value_2", "variation_value_3").build();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_h2() throws Exception {
    when(db.getDialect()).thenReturn(new H2());
    assertThatThrownBy(() -> {
      new DropMsSQLDefaultConstraintsBuilder(db).setTable("snapshots").setColumns("variation_value_2", "variation_value_3").build();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_postgres() throws Exception {
    when(db.getDialect()).thenReturn(new PostgreSql());
    assertThatThrownBy(() -> {
      new DropMsSQLDefaultConstraintsBuilder(db).setTable("snapshots").setColumns("variation_value_2", "variation_value_3").build();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void generate_queries_for_mssql() throws Exception {
    when(db.getDialect()).thenReturn(new MsSql());
    DataSource dataSource = mock(DataSource.class);
    when(db.getDataSource()).thenReturn(dataSource);
    Connection connection = mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);
    PreparedStatement statement = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    ResultSet rsMock = mock(ResultSet.class);
    when(statement.executeQuery()).thenReturn(rsMock);
    when(rsMock.next()).thenReturn(true, true, false);
    when(rsMock.getString(1)).thenReturn("DF__A1", "DF__A2");

    assertThat(new DropMsSQLDefaultConstraintsBuilder(db).setTable("snapshots").setColumns("variation_value_2", "variation_value_3").build())
      .containsExactly("ALTER TABLE snapshots DROP CONSTRAINT DF__A1", "ALTER TABLE snapshots DROP CONSTRAINT DF__A2");
  }
}
