/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v89;

import java.sql.SQLException;
import java.util.Collection;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static java.util.Arrays.asList;

public class PopulateQualityGateConditionsMinimumEffectiveLinesDefaultValue extends DataChange {

  private static final Collection<String> HISTORICALLY_EXCLUDED_SMALL_CHANGESET_METRICS = asList(
    CoreMetrics.NEW_COVERAGE_KEY,
    CoreMetrics.NEW_LINE_COVERAGE_KEY,
    CoreMetrics.NEW_BRANCH_COVERAGE_KEY,
    CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY,
    CoreMetrics.NEW_DUPLICATED_LINES_KEY,
    CoreMetrics.NEW_BLOCKS_DUPLICATED_KEY);

  public PopulateQualityGateConditionsMinimumEffectiveLinesDefaultValue(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    for (String metric : HISTORICALLY_EXCLUDED_SMALL_CHANGESET_METRICS) {
      MassUpdate massUpdate = context.prepareMassUpdate();
      massUpdate.select("select uuid from quality_gate_conditions where metric_uuid in (select uuid from metrics where name = ?) and minimum_effective_lines is null").setString(1,
        metric);
      massUpdate.update(
        "update quality_gate_conditions set minimum_effective_lines = ? where metric_uuid in (select uuid from metrics where name = ?) and minimum_effective_lines is null");
      massUpdate.execute((row, update) -> {
        update.setInt(1, 20);
        update.setString(2, metric);
        return true;
      });
    }

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid from quality_gate_conditions where minimum_effective_lines is null");
    massUpdate.update("update quality_gate_conditions set minimum_effective_lines = ? where minimum_effective_lines is null");
    massUpdate.execute((row, update) -> {
      update.setInt(1, 0);
      return true;
    });
  }
}
