/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.scannercache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbInputStream;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScannerCacheDaoTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = dbTester.getSession();
  private final ScannerCacheDao underTest = dbTester.getDbClient().scannerCacheDao();

  @Test
  public void insert_should_insert_in_db() throws IOException {
    underTest.insert(dbSession, "branch1", stringToInputStream("test data"));
    dbSession.commit();
    assertThat(dbTester.countRowsOfTable("scanner_cache")).isOne();
    assertThat(dataStreamToString(underTest.selectData(dbSession, "branch1"))).isEqualTo("test data");
  }

  @Test
  public void select_returns_empty_if_entry_doesnt_exist() {
    underTest.insert(dbSession, "branch1", stringToInputStream("test data"));
    assertThat(underTest.selectData(dbSession, "branch2")).isNull();
  }

  @Test
  public void remove_all_should_delete_all() {
    underTest.insert(dbSession, "branch1", stringToInputStream("test data"));
    underTest.insert(dbSession, "branch2", stringToInputStream("test data"));

    assertThat(dbTester.countRowsOfTable("scanner_cache")).isEqualTo(2);
    underTest.removeAll(dbSession);
    dbSession.commit();
    assertThat(dbTester.countRowsOfTable("scanner_cache")).isZero();
  }

  @Test
  public void throw_illegalstateexception_when_sql_excpetion() throws SQLException {
    var dbSession = mock(DbSession.class);
    var connection = mock(Connection.class);
    when(dbSession.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException());

    assertThatThrownBy(() -> underTest.selectData(dbSession, "uuid"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to select cache for branch uuid");

    assertThatThrownBy(() -> underTest.insert(dbSession, "uuid", mock(InputStream.class)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to insert cache for branch uuid");
  }

  private static String dataStreamToString(DbInputStream dbInputStream) throws IOException {
    try (DbInputStream is = dbInputStream) {
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    }
  }

  private static InputStream stringToInputStream(String str) {
    return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
  }

}
