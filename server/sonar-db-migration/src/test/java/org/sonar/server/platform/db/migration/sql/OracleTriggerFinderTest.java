/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.Test;
import org.sonar.db.Database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OracleTriggerFinderTest {

  private final Database mockDb = mock(Database.class);
  private final OracleTriggerFinder underTest = new OracleTriggerFinder(mockDb);

  @Test
  public void execute_query_to_get_trigger_name() throws SQLException {
    DataSource mockDataSource = mock(DataSource.class);
    when(mockDb.getDataSource()).thenReturn(mockDataSource);
    Connection mockConnection = mock(Connection.class);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement("SELECT trigger_name FROM user_triggers WHERE upper(table_name) = upper('table_with_trg')")).thenReturn(mockPreparedStatement);
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString(1)).thenReturn("my_trigger");
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

    Optional<String> foundTrigger = underTest.findTriggerName("table_with_trg");

    assertThat(foundTrigger).hasValue("my_trigger");
  }

  @Test
  public void execute_query_to_get_trigger_name_empty_result() throws SQLException {
    DataSource mockDataSource = mock(DataSource.class);
    when(mockDb.getDataSource()).thenReturn(mockDataSource);
    Connection mockConnection = mock(Connection.class);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
    when(mockConnection.prepareStatement("SELECT trigger_name FROM user_triggers WHERE upper(table_name) = upper('table_with_trg')")).thenReturn(mockPreparedStatement);
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

    Optional<String> foundTrigger = underTest.findTriggerName("table_with_trg");

    assertThat(foundTrigger).isEmpty();
  }

}
