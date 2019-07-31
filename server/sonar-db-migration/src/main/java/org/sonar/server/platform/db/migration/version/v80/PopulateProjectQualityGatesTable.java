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
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

public class PopulateProjectQualityGatesTable extends DataChange {

  public PopulateProjectQualityGatesTable(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    List<ProjectQualityGate> projectQualityGates = context.prepareSelect(
      "select prj.uuid, qg.uuid from properties p \n" +
        "join projects prj on p.resource_id = prj.id\n" +
        "join quality_gates qg on qg.id = CAST(p.text_value AS int)\n" +
        "where p.prop_key = 'sonar.qualitygate'\n" +
        "and not exists(select pqg.project_uuid from project_qgates pqg where pqg.project_uuid = prj.uuid)")
      .list(row -> {
        String projectUuid = row.getString(1);
        String qualityGateUuid = row.getString(2);
        return new ProjectQualityGate(projectUuid, qualityGateUuid);
      });

    if (!projectQualityGates.isEmpty()) {
      populateProjectQualityGates(context, projectQualityGates);
    }
  }

  private static void populateProjectQualityGates(Context context, List<ProjectQualityGate> projectQualityGates) throws SQLException {
    Upsert insertQuery = prepareInsertProjectQualityGateQuery(context);
    for (ProjectQualityGate projectQualityGate : projectQualityGates) {
      insertQuery
        .setString(1, projectQualityGate.projectUuid)
        .setString(2, projectQualityGate.qualityGateUuid)
        .addBatch();
    }
    insertQuery
      .execute()
      .commit();
  }

  private static Upsert prepareInsertProjectQualityGateQuery(Context context) throws SQLException {
    return context.prepareUpsert("insert into project_qgates(project_uuid, quality_gate_uuid) VALUES (?, ?)");
  }

  private static class ProjectQualityGate {
    private final String projectUuid;
    private final String qualityGateUuid;

    ProjectQualityGate(String projectUuid, String qualityGateUuid) {
      this.projectUuid = projectUuid;
      this.qualityGateUuid = qualityGateUuid;
    }
  }

}
