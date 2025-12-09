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
package org.sonar.ce.task.projectexport.steps;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump.LiveMeasure;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectexport.component.ComponentRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static java.lang.String.format;

public class ExportLiveMeasuresStep implements ComputationStep {

  private static final String QUERY = "select m.component_uuid, m.json_value" +
    " from measures m" +
    " join components p on p.uuid = m.component_uuid" +
    " join components pp on pp.uuid = m.branch_uuid" +
    " join project_branches pb on pb.uuid=pp.uuid" +
    " where pb.project_uuid=? and pb.branch_type = 'BRANCH' and pb.exclude_from_purge=? and p.enabled=?";

  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final ComponentRepository componentRepository;
  private final DumpWriter dumpWriter;

  public ExportLiveMeasuresStep(DbClient dbClient, ProjectHolder projectHolder, ComponentRepository componentRepository, DumpWriter dumpWriter) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.componentRepository = componentRepository;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public void execute(Context context) {
    long count = 0L;
    try (
      StreamWriter<LiveMeasure> output = dumpWriter.newStreamWriter(DumpElement.LIVE_MEASURES);
      DbSession dbSession = dbClient.openSession(false);
      PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, QUERY)) {

      stmt.setString(1, projectHolder.projectDto().getUuid());
      stmt.setBoolean(2, true);
      stmt.setBoolean(3, true);
      try (ResultSet rs = stmt.executeQuery()) {
        LiveMeasure.Builder liveMeasureBuilder = LiveMeasure.newBuilder();
        while (rs.next()) {
          LiveMeasure measure = convertToLiveMeasure(rs, liveMeasureBuilder);
          output.write(measure);
          count++;
        }
        LoggerFactory.getLogger(getClass()).debug("{} live measures exported", count);
      }
    } catch (Exception e) {
      throw new IllegalStateException(format("Live Measure Export failed after processing %d measures successfully", count), e);
    }
  }

  private LiveMeasure convertToLiveMeasure(ResultSet rs, LiveMeasure.Builder builder) throws SQLException {
    builder
      .clear()
      .setComponentRef(componentRepository.getRef(rs.getString(1)))
      .setJsonValue(rs.getString(2));
    return builder.build();
  }

  @Override
  public String getDescription() {
    return "Export live measures";
  }
}
