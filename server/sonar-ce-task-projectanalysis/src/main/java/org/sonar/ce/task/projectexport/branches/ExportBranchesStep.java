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
package org.sonar.ce.task.projectexport.branches;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.DumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.projectexport.steps.StreamWriter;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultString;

public class ExportBranchesStep implements ComputationStep {

  private final DumpWriter dumpWriter;
  private final DbClient dbClient;
  private final ProjectHolder projectHolder;

  public ExportBranchesStep(DumpWriter dumpWriter, DbClient dbClient, ProjectHolder projectHolder) {
    this.dumpWriter = dumpWriter;
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
  }

  @Override
  public void execute(Context context) {
    long count = 0L;
    try {
      try (DbSession dbSession = dbClient.openSession(false);
        StreamWriter<ProjectDump.Branch> output = dumpWriter.newStreamWriter(DumpElement.BRANCHES)) {
        ProjectDump.Branch.Builder builder = ProjectDump.Branch.newBuilder();
        List<BranchDto> branches = dbClient.projectExportDao()
          .selectBranchesForExport(dbSession, projectHolder.projectDto().getUuid());
        for (BranchDto branch : branches) {
          builder
            .clear()
            .setUuid(branch.getUuid())
            .setProjectUuid(branch.getProjectUuid())
            .setKee(branch.getKey())
            .setIsMain(branch.isMain())
            .setBranchType(branch.getBranchType().name())
            .setMergeBranchUuid(defaultString(branch.getMergeBranchUuid()));
          output.write(builder.build());
          ++count;
        }
        LoggerFactory.getLogger(getClass()).debug("{} branches exported", count);
      }
    } catch (Exception e) {
      throw new IllegalStateException(format("Branch export failed after processing %d branch(es) successfully", count), e);
    }
  }

  @Override
  public String getDescription() {
    return "Export branches";
  }
}
