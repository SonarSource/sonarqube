/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static com.google.common.base.Preconditions.checkState;

public class PopulateDefaultQualityGate extends DataChange {

  private final System2 system2;

  public PopulateDefaultQualityGate(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    List<String> builtInQGUuids = context.prepareSelect("select uuid from quality_gates where is_built_in = ?")
      .setBoolean(1, true)
      .list(row -> row.getString(1));

    checkState(!builtInQGUuids.isEmpty(), "Unable to find the builtin quality gate");
    checkState(builtInQGUuids.size() == 1, "There are too many built in quality gates, one and only one is expected");

    final long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid from organizations " +
      " where default_quality_gate_uuid is null");
    massUpdate.rowPluralName("organizations");
    massUpdate.update("update organizations set default_quality_gate_uuid = ?, updated_at=? where uuid = ?");
    massUpdate.execute((row, update) -> {
      update.setString(1, builtInQGUuids.get(0));
      update.setLong(2, now);
      update.setString(3, row.getString(1));
      return true;
    });

  }
}
