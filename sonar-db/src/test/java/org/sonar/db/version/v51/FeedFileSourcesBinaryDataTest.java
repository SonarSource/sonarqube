/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v51;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.dbutils.DbUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbTester;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedFileSourcesBinaryDataTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, FeedFileSourcesBinaryDataTest.class, "schema.sql");

  @Rule
  public LogTester logTester = new LogTester().setLevel(LoggerLevel.DEBUG);

  @Test
  public void convert_csv_to_protobuf() throws Exception {
    db.prepareDbUnit(getClass(), "data.xml");

    MigrationStep migration = new FeedFileSourcesBinaryData(db.database());
    migration.execute();

    int count = db.countSql("select count(1) from file_sources where binary_data is not null");
    assertThat(count).isEqualTo(3);

    try (Connection connection = db.openConnection()) {
      DbFileSources.Data data = selectData(connection, 1L);
      assertThat(data.getLinesCount()).isEqualTo(4);
      assertThat(data.getLines(0).getScmRevision()).isEqualTo("aef12a");

      data = selectData(connection, 2L);
      assertThat(data.getLinesCount()).isEqualTo(4);
      assertThat(data.getLines(0).hasScmRevision()).isFalse();

      data = selectData(connection, 3L);
      assertThat(data.getLinesCount()).isEqualTo(0);
    }
  }

  @Test
  public void execute_does_not_fail_when_data_can_not_be_parsed_and_row_is_ignored() throws Exception {
    db.prepareDbUnit(getClass(), "bad_data.xml");

    MigrationStep migration = new FeedFileSourcesBinaryData(db.database());

    migration.execute();

    try (Connection connection = db.openConnection()) {
      DbFileSources.Data data = selectData(connection, 1L);

      assertThat(data.getLinesList()).isEmpty();
      List<String> debugLogs = logTester.logs(LoggerLevel.DEBUG);
      assertThat(debugLogs.stream()
        .anyMatch(s -> s.startsWith("Invalid FILE_SOURCES.DATA on row with ID 1, data will be ignored"))).isTrue();
    }
  }

  private DbFileSources.Data selectData(Connection connection, long fileSourceId) throws SQLException {
    PreparedStatement pstmt = connection.prepareStatement("select binary_data from file_sources where id=?");
    ResultSet rs = null;
    try {
      pstmt.setLong(1, fileSourceId);
      rs = pstmt.executeQuery();
      rs.next();
      return new FileSourceDto().decodeSourceData(rs.getBytes(1));
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(pstmt);
    }
  }
}
