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
package org.sonar.ce.task.projectexport.analysis;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectexport.component.ComponentRepository;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.DumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.projectexport.steps.StreamWriter;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.sonar.db.DatabaseUtils.getString;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;

public class ExportAnalysesStep implements ComputationStep {

  // contrary to naming convention used in other tables, the column snapshots.created_at is
  // the functional date (for instance when using property sonar.projectDate=2010-01-01). The
  // column "build_date" is the technical date of scanner execution. It must not be exported.
  private static final String QUERY = "select" +
    " p.uuid, s.version, s.created_at," +
    " s.period1_mode, s.period1_param, s.period1_date," +
    " s.uuid, s.build_string" +
    " from snapshots s" +
    " inner join components p on s.root_component_uuid=p.uuid" +
    " inner join project_branches pb on pb.uuid=p.uuid" +
    " where pb.project_uuid=? and pb.branch_type = 'BRANCH' and pb.exclude_from_purge=? and s.status=? and p.enabled=?" +
    " order by s.analysis_date asc";

  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final ComponentRepository componentRepository;
  private final DumpWriter dumpWriter;

  public ExportAnalysesStep(DbClient dbClient, ProjectHolder projectHolder, ComponentRepository componentRepository, DumpWriter dumpWriter) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.componentRepository = componentRepository;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public void execute(Context context) {
    long count = 0L;
    try (
      StreamWriter<ProjectDump.Analysis> output = dumpWriter.newStreamWriter(DumpElement.ANALYSES);
      DbSession dbSession = dbClient.openSession(false);
      PreparedStatement stmt = buildSelectStatement(dbSession);
      ResultSet rs = stmt.executeQuery()) {

      ProjectDump.Analysis.Builder builder = ProjectDump.Analysis.newBuilder();
      while (rs.next()) {
        // Results are ordered by ascending id so that any parent is located
        // before its children.
        ProjectDump.Analysis analysis = convertToAnalysis(rs, builder);
        output.write(analysis);
        count++;
      }
      LoggerFactory.getLogger(getClass()).debug("{} analyses exported", count);

    } catch (Exception e) {
      throw new IllegalStateException(format("Analysis Export failed after processing %d analyses successfully", count), e);
    }
  }

  private PreparedStatement buildSelectStatement(DbSession dbSession) throws SQLException {
    PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, QUERY);
    try {
      stmt.setString(1, projectHolder.projectDto().getUuid());
      stmt.setBoolean(2, true);
      stmt.setString(3, STATUS_PROCESSED);
      stmt.setBoolean(4, true);
      return stmt;
    } catch (Exception t) {
      DatabaseUtils.closeQuietly(stmt);
      throw t;
    }
  }

  private ProjectDump.Analysis convertToAnalysis(ResultSet rs, ProjectDump.Analysis.Builder builder) throws SQLException {
    builder.clear();
    long componentRef = componentRepository.getRef(rs.getString(1));
    return builder
      .setComponentRef(componentRef)
      .setProjectVersion(defaultString(getString(rs, 2)))
      .setDate(rs.getLong(3))
      .setPeriod1Mode(defaultString(getString(rs, 4)))
      .setPeriod1Param(defaultString(getString(rs, 5)))
      .setPeriod1Date(rs.getLong(6))
      .setUuid(rs.getString(7))
      .setBuildString(defaultString(getString(rs, 8)))
      .build();
  }

  @Override
  public String getDescription() {
    return "Export analyses";
  }

}
