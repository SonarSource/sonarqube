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
package org.sonar.server.test.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;

import static java.util.Arrays.asList;

public class TestTesting {

  private TestTesting() {
    // only static stuff
  }

  public static void updateDataColumn(DbSession session, String fileUuid, List<DbFileSources.Test> tests) throws SQLException {
    updateDataColumn(session, fileUuid, FileSourceDto.encodeTestData(tests));
  }

  public static void updateDataColumn(DbSession session, String fileUuid, byte[] data) throws SQLException {
    Connection connection = session.getConnection();
    PreparedStatement stmt = connection.prepareStatement("UPDATE file_sources SET binary_data = ? WHERE file_uuid=? AND data_type='TEST'");
    stmt.setBytes(1, data);
    stmt.setString(2, fileUuid);
    stmt.executeUpdate();
    stmt.close();
    connection.commit();
  }

  /**
   * Generate random data.
   */
  public static List<DbFileSources.Test> newRandomTests(int numberOfTests) {
    List<DbFileSources.Test> tests = new ArrayList<>();
    for (int i = 1; i <= numberOfTests; i++) {
      DbFileSources.Test.Builder test = DbFileSources.Test.newBuilder()
        .setUuid(Uuids.create())
        .setName(RandomStringUtils.randomAlphanumeric(20))
        .setStatus(DbFileSources.Test.TestStatus.FAILURE)
        .setStacktrace(RandomStringUtils.randomAlphanumeric(50))
        .setMsg(RandomStringUtils.randomAlphanumeric(30))
        .setExecutionTimeMs(RandomUtils.nextLong());
      for (int j = 0; j < numberOfTests; j++) {
        test.addCoveredFile(
          DbFileSources.Test.CoveredFile.newBuilder()
            .setFileUuid(Uuids.create())
            .addCoveredLine(RandomUtils.nextInt(500)));
      }
      tests.add(test.build());
    }
    return tests;
  }

  public static DbFileSources.Test.Builder newTest(ComponentDto mainFile, Integer... coveredLines) {
    DbFileSources.Test.Builder test = DbFileSources.Test.newBuilder()
      .setUuid(Uuids.create())
      .setName(RandomStringUtils.randomAlphanumeric(20))
      .setStatus(DbFileSources.Test.TestStatus.FAILURE)
      .setStacktrace(RandomStringUtils.randomAlphanumeric(50))
      .setMsg(RandomStringUtils.randomAlphanumeric(30))
      .setExecutionTimeMs(RandomUtils.nextLong());
    test.addCoveredFile(
      DbFileSources.Test.CoveredFile.newBuilder()
        .setFileUuid(mainFile.uuid())
        .addAllCoveredLine(asList(coveredLines)));
    return test;
  }
}
