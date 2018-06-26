/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.source.index;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;

public class FileSourceTesting {

  private FileSourceTesting() {
    // only static stuff
  }

  public static void updateDataColumn(Connection connection, String fileUuid, DbFileSources.Data data) throws SQLException {
    updateDataColumn(connection, fileUuid, FileSourceDto.encodeSourceData(data));
  }

  public static void updateDataColumn(Connection connection, String fileUuid, byte[] data) throws SQLException {
    PreparedStatement stmt = connection.prepareStatement("UPDATE file_sources SET binary_data = ? WHERE file_uuid=? AND data_type='" + FileSourceDto.Type.SOURCE + "'");
    stmt.setBytes(1, data);
    stmt.setString(2, fileUuid);
    stmt.executeUpdate();
    stmt.close();
  }

  /**
   * Generate predefined fake data. Result is mutable.
   */
  public static DbFileSources.Data.Builder newFakeData(int numberOfLines) {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    for (int i = 1; i <= numberOfLines; i++) {
      dataBuilder.addLinesBuilder()
        .setLine(i)
        .setScmRevision("REVISION_" + i)
        .setScmAuthor("AUTHOR_" + i)
        .setScmDate(1_500_000_000_00L + i)
        .setSource("SOURCE_" + i)
        .setLineHits(i)
        .setConditions(i + 1)
        .setCoveredConditions(i + 2)
        .setHighlighting("HIGHLIGHTING_" + i)
        .setSymbols("SYMBOLS_" + i)
        .addAllDuplication(Arrays.asList(i))
        .build();
    }
    return dataBuilder;
  }

  /**
   * Generate random data. Result is mutable.
   */
  public static DbFileSources.Data.Builder newRandomData(int numberOfLines) {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    for (int i = 1; i <= numberOfLines; i++) {
      dataBuilder.addLinesBuilder()
        .setLine(i)
        .setScmRevision(RandomStringUtils.randomAlphanumeric(15))
        .setScmAuthor(RandomStringUtils.randomAlphanumeric(10))
        .setScmDate(RandomUtils.nextLong())
        .setSource(RandomStringUtils.randomAlphanumeric(20))
        .setLineHits(RandomUtils.nextInt(4))
        .setConditions(RandomUtils.nextInt(4))
        .setCoveredConditions(RandomUtils.nextInt(4))
        .setHighlighting(RandomStringUtils.randomAlphanumeric(40))
        .setSymbols(RandomStringUtils.randomAlphanumeric(30))
        .addAllDuplication(Arrays.asList(RandomUtils.nextInt(200), RandomUtils.nextInt(200)))
        .build();
    }
    return dataBuilder;
  }
}
