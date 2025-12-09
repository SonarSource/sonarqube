/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectexport.file;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectexport.component.ComponentRepository;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.DumpWriter;
import org.sonar.ce.task.projectexport.steps.StreamWriter;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static com.google.common.collect.Iterables.partition;
import static java.lang.String.format;
import static org.sonar.ce.task.projectexport.util.ResultSetUtils.emptyIfNull;
import static org.sonar.db.DatabaseUtils.PARTITION_SIZE_FOR_ORACLE;
import static org.sonar.db.DatabaseUtils.closeQuietly;

public class ExportLineHashesStep implements ComputationStep {

  private final DbClient dbClient;
  private final DumpWriter dumpWriter;
  private final ComponentRepository componentRepository;

  public ExportLineHashesStep(DbClient dbClient, DumpWriter dumpWriter, ComponentRepository componentRepository) {
    this.dbClient = dbClient;
    this.dumpWriter = dumpWriter;
    this.componentRepository = componentRepository;
  }

  @Override
  public String getDescription() {
    return "Export line hashes";
  }

  @Override
  public void execute(Context context) {
    Set<String> allFileUuids = componentRepository.getFileUuids();
    PreparedStatement stmt = null;
    ResultSet rs = null;
    long count = 0;
    try (StreamWriter<ProjectDump.LineHashes> output = dumpWriter.newStreamWriter(DumpElement.LINES_HASHES)) {
      if (allFileUuids.isEmpty()) {
        return;
      }
      try (DbSession dbSession = dbClient.openSession(false)) {
        ProjectDump.LineHashes.Builder builder = ProjectDump.LineHashes.newBuilder();
        for (List<String> fileUuids : partition(allFileUuids, PARTITION_SIZE_FOR_ORACLE)) {
          stmt = createStatement(dbSession, fileUuids);
          rs = stmt.executeQuery();
          while (rs.next()) {
            ProjectDump.LineHashes lineHashes = toLinehashes(builder, rs);
            output.write(lineHashes);
            count++;
          }
          closeQuietly(rs);
          closeQuietly(stmt);
        }
        LoggerFactory.getLogger(getClass()).debug("Lines hashes of {} files exported", count);
      } catch (Exception e) {
        throw new IllegalStateException(format("Lines hashes export failed after processing %d files successfully", count), e);
      } finally {
        closeQuietly(rs);
        closeQuietly(stmt);
      }
    }
  }

  private PreparedStatement createStatement(DbSession dbSession, List<String> uuids) throws SQLException {
    String sql = "select" +
      " file_uuid, line_hashes, project_uuid" +
      " FROM file_sources" +
      " WHERE file_uuid in (%s)" +
      " order by created_at, uuid";
    PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, format(sql, wildCardStringFor(uuids)));
    try {
      int i = 1;
      for (String uuid : uuids) {
        stmt.setString(i, uuid);
        i++;
      }
      return stmt;
    } catch (Exception e) {
      DatabaseUtils.closeQuietly(stmt);
      throw e;
    }
  }

  private static String wildCardStringFor(List<String> uuids) {
    switch (uuids.size()) {
      case 0:
        throw new IllegalArgumentException("uuids can not be empty");
      case 1:
        return "?";
      default:
        return createWildCardStringFor(uuids);
    }
  }

  private static String createWildCardStringFor(List<String> uuids) {
    int size = (uuids.size() * 2) - 1;
    char[] res = new char[size];
    for (int j = 0; j < size; j++) {
      if (j % 2 == 0) {
        res[j] = '?';
      } else {
        res[j] = ',';
      }
    }
    return new String(res);
  }

  private ProjectDump.LineHashes toLinehashes(ProjectDump.LineHashes.Builder builder, ResultSet rs) throws SQLException {
    builder.clear();

    return builder
      .setComponentRef(componentRepository.getRef(rs.getString(1)))
      .setHashes(emptyIfNull(rs, 2))
      .setProjectUuid(emptyIfNull(rs, 3))
      .build();
  }
}
