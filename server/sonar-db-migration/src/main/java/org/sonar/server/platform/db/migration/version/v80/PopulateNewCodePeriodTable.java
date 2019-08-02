/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v80;

import java.sql.SQLException;
import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

public class PopulateNewCodePeriodTable extends DataChange {

  private final UuidFactory uuidFactory;
  private final System2 system2;

  public PopulateNewCodePeriodTable(Database db, UuidFactory uuidFactory, System2 system2) {
    super(db);
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    List<ProjectBranchCodePeriod> projectBranchCodePeriods = context.prepareSelect(
      "select pb.uuid, pb.project_uuid, pb.manual_baseline_analysis_uuid from project_branches pb where pb.manual_baseline_analysis_uuid is not null")
      .list(row -> {
        String branchUuid = row.getString(1);
        String projectUuid = row.getString(2);
        String manualBaselineAnalysisUuid = row.getString(3);
        return new ProjectBranchCodePeriod(branchUuid, projectUuid, manualBaselineAnalysisUuid);
      });

    if (!projectBranchCodePeriods.isEmpty()) {
      populateProjectBranchCodePeriods(context, projectBranchCodePeriods);
    }
  }

  private void populateProjectBranchCodePeriods(Context context, List<ProjectBranchCodePeriod> projectBranchCodePeriods) throws SQLException {
    Upsert insertQuery = prepareInsertProjectQualityGateQuery(context);
    for (ProjectBranchCodePeriod projectBranchCodePeriod : projectBranchCodePeriods) {
      long currenTime = system2.now();
      insertQuery
        .setString(1, uuidFactory.create())
        .setString(2, projectBranchCodePeriod.projectUuid)
        .setString(3, projectBranchCodePeriod.branchUuid)
        .setString(4, "SPECIFIC_ANALYSIS")
        .setString(5, projectBranchCodePeriod.manualBaselineAnalysisUuid)
        .setLong(6, currenTime)
        .setLong(7, currenTime)
        .addBatch();
    }
    insertQuery
      .execute()
      .commit();
  }

  private static Upsert prepareInsertProjectQualityGateQuery(Context context) throws SQLException {
    return context.prepareUpsert("insert into new_code_periods(" +
      "uuid, " +
      "project_uuid," +
      "branch_uuid," +
      "type," +
      "value," +
      "updated_at," +
      "created_at" +
      ") VALUES (?, ?, ?, ?, ?, ?, ?)");
  }

  private static class ProjectBranchCodePeriod {
    private final String branchUuid;
    private final String projectUuid;
    private final String manualBaselineAnalysisUuid;

    ProjectBranchCodePeriod(String branchUuid, String projectUuid, String manualBaselineAnalysisUuid) {
      this.branchUuid = branchUuid;
      this.projectUuid = projectUuid;
      this.manualBaselineAnalysisUuid = manualBaselineAnalysisUuid;
    }
  }

}
