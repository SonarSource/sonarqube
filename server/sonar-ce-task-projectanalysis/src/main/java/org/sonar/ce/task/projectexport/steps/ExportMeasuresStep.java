/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.sonar.db.DatabaseUtils.getDouble;
import static org.sonar.db.DatabaseUtils.getString;

public class ExportMeasuresStep implements ComputationStep {

  private static final String QUERY = "select pm.metric_uuid, pm.analysis_uuid, pm.component_uuid, pm.text_value, pm.value," +
    " pm.alert_status, pm.alert_text, m.name" +
    " from project_measures pm" +
    " join metrics m on m.uuid=pm.metric_uuid" +
    " join snapshots s on s.uuid=pm.analysis_uuid" +
    " join components p on p.uuid=pm.component_uuid" +
    " join project_branches pb on pb.uuid=p.uuid" +
    " where pb.project_uuid=? and pb.branch_type = 'BRANCH' and pb.exclude_from_purge=?" +
    " and s.status=? and p.enabled=? and m.enabled=? and pm.person_id is null";

  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final ComponentRepository componentRepository;
  private final MutableMetricRepository metricHolder;
  private final DumpWriter dumpWriter;

  public ExportMeasuresStep(DbClient dbClient, ProjectHolder projectHolder, ComponentRepository componentRepository, MutableMetricRepository metricHolder,
    DumpWriter dumpWriter) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.componentRepository = componentRepository;
    this.metricHolder = metricHolder;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public void execute(Context context) {
    long count = 0L;
    try (
      StreamWriter<ProjectDump.Measure> output = dumpWriter.newStreamWriter(DumpElement.MEASURES);
      DbSession dbSession = dbClient.openSession(false);
      PreparedStatement stmt = createSelectStatement(dbSession);
      ResultSet rs = stmt.executeQuery()) {

      ProjectDump.Measure.Builder measureBuilder = ProjectDump.Measure.newBuilder();
      ProjectDump.DoubleValue.Builder doubleBuilder = ProjectDump.DoubleValue.newBuilder();
      while (rs.next()) {
        ProjectDump.Measure measure = convertToMeasure(rs, measureBuilder, doubleBuilder);
        output.write(measure);
        count++;
      }
      LoggerFactory.getLogger(getClass()).debug("{} measures exported", count);
    } catch (Exception e) {
      throw new IllegalStateException(format("Measure Export failed after processing %d measures successfully", count), e);
    }
  }

  private PreparedStatement createSelectStatement(DbSession dbSession) throws SQLException {
    PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, QUERY);
    try {
      stmt.setString(1, projectHolder.projectDto().getUuid());
      stmt.setBoolean(2, true);
      stmt.setString(3, SnapshotDto.STATUS_PROCESSED);
      stmt.setBoolean(4, true);
      stmt.setBoolean(5, true);
      return stmt;
    } catch (Exception t) {
      DatabaseUtils.closeQuietly(stmt);
      throw t;
    }
  }

  private ProjectDump.Measure convertToMeasure(ResultSet rs, ProjectDump.Measure.Builder builder,
    ProjectDump.DoubleValue.Builder doubleBuilder) throws SQLException {
    long componentRef = componentRepository.getRef(rs.getString(3));
    int metricRef = metricHolder.add(rs.getString(1));
    String metricKey = rs.getString(8);

    builder
      .clear()
      .setMetricRef(metricRef)
      .setAnalysisUuid(rs.getString(2))
      .setComponentRef(componentRef)
      .setTextValue(defaultString(getString(rs, 4)));
    Double value = getDouble(rs, 5);

    if (value != null) {
      if (metricKey.startsWith("new_")) {
        builder.setVariation1(doubleBuilder.setValue(value).build());
      } else {
        builder.setDoubleValue(doubleBuilder.setValue(value).build());
      }
    }
    builder.setAlertStatus(defaultString(getString(rs, 6)));
    builder.setAlertText(defaultString(getString(rs, 7)));
    return builder.build();
  }

  @Override
  public String getDescription() {
    return "Export measures";
  }
}
