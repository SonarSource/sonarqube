/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.test.db;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.source.db.FileSourceDb.Test.TestStatus;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TestTesting {

  private TestTesting() {
    // only static stuff
  }

  public static void updateDataColumn(Connection connection, String fileUuid, List<FileSourceDb.Test> tests) throws SQLException {
    updateDataColumn(connection, fileUuid, FileSourceDto.encodeTestData(tests));
  }

  public static void updateDataColumn(Connection connection, String fileUuid, byte[] data) throws SQLException {
    PreparedStatement stmt = connection.prepareStatement("UPDATE file_sources SET binary_data = ? WHERE file_uuid=? AND data_type='TEST'");
    stmt.setBytes(1, data);
    stmt.setString(2, fileUuid);
    stmt.executeUpdate();
    stmt.close();
  }

  /**
   * Generate random data.
   */
  public static List<FileSourceDb.Test> newRandomTests(int numberOfTests) throws IOException {
    List<FileSourceDb.Test> tests = new ArrayList<>();
    for (int i = 1; i <= numberOfTests; i++) {
      FileSourceDb.Test.Builder test = FileSourceDb.Test.newBuilder()
        .setUuid(Uuids.create())
        .setName(RandomStringUtils.randomAlphanumeric(20))
        .setStatus(TestStatus.FAILURE)
        .setStacktrace(RandomStringUtils.randomAlphanumeric(50))
        .setMsg(RandomStringUtils.randomAlphanumeric(30))
        .setExecutionTimeMs(RandomUtils.nextLong());
      int numberOfCoveredFiles = RandomUtils.nextInt(10);
      for (int j = 0; j < numberOfCoveredFiles; j++) {
        test.addCoveredFile(
          FileSourceDb.Test.CoveredFile.newBuilder()
            .setFileUuid(Uuids.create())
            .addCoveredLine(RandomUtils.nextInt(500))
          );
      }
      tests.add(test.build());
    }
    return tests;
  }
}
