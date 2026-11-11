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
package org.sonar.server.platform.db.migration.charset;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MssqlMetadataReaderTest {

  private SqlExecutor sqlExecutor = mock(SqlExecutor.class);
  private Connection connection = mock(Connection.class);
  private MssqlMetadataReader underTest = new MssqlMetadataReader(sqlExecutor);

  @Test
  public void getDependentComputedColumns_readsColumnDependencies() throws SQLException {
    when(sqlExecutor.select(same(connection), anyString(), any())).thenReturn(List.of());
    ColumnDef column = new ColumnDef("components", "kee", "Latin1_General", "Latin1_General_CS_AS", "nvarchar", 1000, true);

    assertThat(underTest.getDependentComputedColumns(connection, column)).isEmpty();

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(sqlExecutor).select(same(connection), sql.capture(), any());
    assertThat(sql.getValue())
      .contains("sys.dm_sql_referenced_entities")
      .contains("ref.referencing_minor_id = cc.column_id")
      .contains("ref.referenced_id = t.object_id")
      .contains("'components'")
      .contains("'kee'")
      .doesNotContain("sys.sql_dependencies");
  }

  @Test
  public void test_getDefaultCollation() throws SQLException {
    answerSelect("Latin1_General_CS_AS");

    assertThat(underTest.getDefaultCollation(connection)).isEqualTo("Latin1_General_CS_AS");
  }

  private void answerSelect(String charset) throws SQLException {
    when(sqlExecutor.selectSingleString(same(connection), anyString())).thenReturn(charset);
  }
}
