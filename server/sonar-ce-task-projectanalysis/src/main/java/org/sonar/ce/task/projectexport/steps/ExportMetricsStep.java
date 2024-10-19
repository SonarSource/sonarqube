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
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultString;

public class ExportMetricsStep implements ComputationStep {

  private final DbClient dbClient;
  private final MetricRepository metricsHolder;
  private final DumpWriter dumpWriter;

  public ExportMetricsStep(DbClient dbClient, MetricRepository metricsHolder, DumpWriter dumpWriter) {
    this.dbClient = dbClient;
    this.metricsHolder = metricsHolder;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public void execute(Context context) {
    int count = 0;
    try (
      StreamWriter<ProjectDump.Metric> output = dumpWriter.newStreamWriter(DumpElement.METRICS);
      DbSession dbSession = dbClient.openSession(false)) {

      ProjectDump.Metric.Builder builder = ProjectDump.Metric.newBuilder();
      Map<String, Integer> refByUuid = metricsHolder.getRefByUuid();
      List<MetricDto> dtos = dbClient.metricDao().selectByUuids(dbSession, refByUuid.keySet());
      for (MetricDto dto : dtos) {
        builder
          .clear()
          .setRef(refByUuid.get(dto.getUuid()))
          .setKey(dto.getKey())
          .setName(defaultString(dto.getShortName()));
        output.write(builder.build());
        count++;
      }
      LoggerFactory.getLogger(getClass()).debug("{} metrics exported", count);
    } catch (Exception e) {
      throw new IllegalStateException(format("Metric Export failed after processing %d metrics successfully", count), e);
    }
  }

  @Override
  public String getDescription() {
    return "Export metrics";
  }
}
