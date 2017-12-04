/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateOrgQualityGates extends DataChange {

  private final UuidFactory uuidFactory;

  public PopulateOrgQualityGates(Database db, UuidFactory uuidFactory) {
    super(db);
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (context.prepareSelect("select count(uuid) from organizations")
      .get(row -> row.getLong(1)) > 0) {
      String builtInQGUuid = context.prepareSelect("select uuid from quality_gates where is_built_in = ?")
        .setBoolean(1, true)
        .get(row -> row.getString(1));

      if (builtInQGUuid == null) {
        throw new IllegalStateException("Unable to find the builtin quality gate");
      }

      MassUpdate massUpdate = context.prepareMassUpdate();
      massUpdate.select("select uuid from organizations o " +
        " where " +
        "  (select count(oqg.uuid) from org_quality_gates oqg where oqg.quality_gate_uuid = ? and oqg.organization_uuid = o.uuid) = 0")
        .setString(1, builtInQGUuid);

      massUpdate.rowPluralName("organizations");
      massUpdate.update("insert into org_quality_gates (uuid, quality_gate_uuid, organization_uuid) values(?, ?, ?)");
      massUpdate.execute((row, update) -> {
        update.setString(1, uuidFactory.create());
        update.setString(2, builtInQGUuid);
        update.setString(3, row.getString(1));
        return true;
      });
    }
  }
}
