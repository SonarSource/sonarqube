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
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;

import static java.lang.String.format;

public class ExportNewCodePeriodsStep implements ComputationStep {

  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final DumpWriter dumpWriter;

  public ExportNewCodePeriodsStep(DbClient dbClient, ProjectHolder projectHolder, DumpWriter dumpWriter) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public void execute(Context context) {
    long count = 0L;
    try (
      StreamWriter<ProjectDump.NewCodePeriod> output = dumpWriter.newStreamWriter(DumpElement.NEW_CODE_PERIODS);
      DbSession dbSession = dbClient.openSession(false)) {

      final ProjectDump.NewCodePeriod.Builder builder = ProjectDump.NewCodePeriod.newBuilder();
      final List<NewCodePeriodDto> newCodePeriods = dbClient.projectExportDao()
        .selectNewCodePeriodsForExport(dbSession, projectHolder.projectDto().getUuid());
      for (NewCodePeriodDto newCodePeriod : newCodePeriods) {
        builder.clear()
          .setUuid(newCodePeriod.getUuid())
          .setProjectUuid(newCodePeriod.getProjectUuid())
          .setType(newCodePeriod.getType().name());

        if (newCodePeriod.getBranchUuid() != null) {
          builder.setBranchUuid(newCodePeriod.getBranchUuid());
        }

        if (newCodePeriod.getValue() != null) {
          builder.setValue(newCodePeriod.getValue());
        }
        output.write(builder.build());
        ++count;
      }

      LoggerFactory.getLogger(getClass()).debug("{} new code periods exported", count);
    } catch (Exception e) {
      throw new IllegalStateException(format("New Code Periods Export failed after processing %d new code periods successfully", count), e);
    }
  }

  @Override
  public String getDescription() {
    return "Export new code periods";
  }
}
