/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectexport.steps;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectexport.component.ComponentRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.sonar.db.DatabaseUtils.getString;

public class ExportEventsStep implements ComputationStep {

  private static final String QUERY = "select" +
    " p.uuid, e.name, e.analysis_uuid, e.category, e.description, e.event_data, e.event_date, e.uuid" +
    " from events e" +
    " join snapshots s on s.uuid=e.analysis_uuid" +
    " join components p on p.uuid=s.root_component_uuid" +
    " join project_branches pb on pb.uuid=p.uuid" +
    " where pb.project_uuid=? and pb.branch_type = 'BRANCH' and pb.exclude_from_purge=? and s.status=? and p.enabled=?";

  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final ComponentRepository componentRepository;
  private final DumpWriter dumpWriter;

  public ExportEventsStep(DbClient dbClient, ProjectHolder projectHolder, ComponentRepository componentRepository, DumpWriter dumpWriter) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.componentRepository = componentRepository;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public void execute(Context context) {
    long count = 0L;
    try (
      StreamWriter<ProjectDump.Event> output = dumpWriter.newStreamWriter(DumpElement.EVENTS);
      DbSession dbSession = dbClient.openSession(false);
      PreparedStatement stmt = buildSelectStatement(dbSession);
      ResultSet rs = stmt.executeQuery()) {

      ProjectDump.Event.Builder builder = ProjectDump.Event.newBuilder();
      while (rs.next()) {
        ProjectDump.Event event = convertToEvent(rs, builder);
        output.write(event);
        count++;
      }
      LoggerFactory.getLogger(getClass()).debug("{} events exported", count);

    } catch (Exception e) {
      throw new IllegalStateException(format("Event Export failed after processing %d events successfully", count), e);
    }
  }

  private PreparedStatement buildSelectStatement(DbSession dbSession) throws SQLException {
    PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, QUERY);
    try {
      stmt.setString(1, projectHolder.projectDto().getUuid());
      stmt.setBoolean(2, true);
      stmt.setString(3, SnapshotDto.STATUS_PROCESSED);
      stmt.setBoolean(4, true);
      return stmt;
    } catch (Exception e) {
      DatabaseUtils.closeQuietly(stmt);
      throw e;
    }
  }

  private ProjectDump.Event convertToEvent(ResultSet rs, ProjectDump.Event.Builder builder) throws SQLException {
    long componentRef = componentRepository.getRef(rs.getString(1));
    return builder
      .clear()
      .setComponentRef(componentRef)
      .setName(defaultString(getString(rs, 2)))
      .setAnalysisUuid(getString(rs, 3))
      .setCategory(defaultString(getString(rs, 4)))
      .setDescription(defaultString(getString(rs, 5)))
      .setData(defaultString(getString(rs, 6)))
      .setDate(rs.getLong(7))
      .setUuid(rs.getString(8))
      .build();

  }

  @Override
  public String getDescription() {
    return "Export events";
  }
}
