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
package org.sonar.server.platform.db.migration.charset;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostgresMetadataReaderTest {

  private SqlExecutor sqlExecutor = mock(SqlExecutor.class);
  private Connection connection = mock(Connection.class);
  private PostgresMetadataReader underTest = new PostgresMetadataReader(sqlExecutor);

  @Test
  public void test_getDefaultCharset() throws SQLException {
    answerSelect(Arrays.<String[]>asList(new String[] {"latin"}));

    assertThat(underTest.getDefaultCharset(connection)).isEqualTo("latin");
  }

  private void answerSelect(List<String[]> firstRequest) throws SQLException {
    when(sqlExecutor.select(same(connection), anyString(), any(SqlExecutor.StringsConverter.class))).thenReturn(firstRequest);
  }

}
