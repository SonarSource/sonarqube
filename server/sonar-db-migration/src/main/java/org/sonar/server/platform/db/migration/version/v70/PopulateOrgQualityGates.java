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
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static com.google.common.base.Preconditions.checkState;

public class PopulateOrgQualityGates extends DataChange {

  private final UuidFactory uuidFactory;

  public PopulateOrgQualityGates(Database db, UuidFactory uuidFactory) {
    super(db);
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute(Context context) throws SQLException {
    Long nbOfOrganizations = context.prepareSelect("select count(uuid) from organizations")
      .get(row -> row.getLong(1));
    if (nbOfOrganizations == 0) {
      // No need for a migration
      return;
    }

    List<String> builtInQGUuids = context.prepareSelect("select uuid from quality_gates where is_built_in = ?")
      .setBoolean(1, true)
      .list(row -> row.getString(1));

    checkState(!builtInQGUuids.isEmpty(), "Unable to find the builtin quality gate");
    checkState(builtInQGUuids.size() == 1, "There are too many built in quality gates, one and only one is expected");

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid from organizations o " +
      " where " +
      "  not exists (select 1 from org_quality_gates oqg where oqg.quality_gate_uuid = ? and oqg.organization_uuid = o.uuid)")
      .setString(1, builtInQGUuids.get(0));

    massUpdate.rowPluralName("organizations");
    massUpdate.update("insert into org_quality_gates (uuid, quality_gate_uuid, organization_uuid) values(?, ?, ?)");
    massUpdate.execute((row, update) -> {
      update.setString(1, uuidFactory.create());
      update.setString(2, builtInQGUuids.get(0));
      update.setString(3, row.getString(1));
      return true;
    });
  }
}
