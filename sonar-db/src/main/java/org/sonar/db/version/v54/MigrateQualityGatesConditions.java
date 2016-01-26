/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.db.version.v54;

import com.google.common.base.Joiner;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

/**
 * Update all quality gates condition periods to leak period when period is provided
 */
public class MigrateQualityGatesConditions extends BaseDataChange {
  private static final Logger LOG = Loggers.get(MigrateQualityGatesConditions.class);

  private final System2 system2;

  public MigrateQualityGatesConditions(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("quality gate conditions");
    update.select("select qgc.id, qg.name " +
      "from quality_gate_conditions qgc " +
      "inner join quality_gates qg on qgc.qgate_id=qg.id " +
      "where qgc.period is not null and qgc.period <> 1");
    update.update("update quality_gate_conditions set period=1, updated_at=? where id=?");
    MigrationHandler migrationHandler = new MigrationHandler(system2.now());
    update.execute(migrationHandler);
    if (!migrationHandler.updatedQualityGates.isEmpty()) {
      LOG.warn("The following Quality Gates have been updated to compare with the leak period: {}.", Joiner.on(", ").join(migrationHandler.updatedQualityGates));
    }
  }

  private static class MigrationHandler implements MassUpdate.Handler {
    private final Date now;
    private final Set<String> updatedQualityGates;

    public MigrationHandler(long now) {
      this.updatedQualityGates = new LinkedHashSet<>();
      this.now = new Date(now);
    }

    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      update.setDate(1, now);
      update.setLong(2, row.getLong(1));
      updatedQualityGates.add(row.getString(2));
      return true;
    }
  }
}
