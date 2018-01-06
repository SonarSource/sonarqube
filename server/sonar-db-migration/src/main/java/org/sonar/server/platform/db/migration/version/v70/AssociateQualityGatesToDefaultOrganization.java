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
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static com.google.common.base.Preconditions.checkState;

public class AssociateQualityGatesToDefaultOrganization extends DataChange {

  private final UuidFactory uuidFactory;

  public AssociateQualityGatesToDefaultOrganization(Database db, UuidFactory uuidFactory) {
    super(db);
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String defaultOrgUuid = context.prepareSelect("select text_value from internal_properties where kee = 'organization.default'")
      .get(row -> row.getString(1));

    checkState(defaultOrgUuid != null, "Default organization uuid is missing");

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT qg.uuid from quality_gates qg " +
      " WHERE qg.is_built_in = ? " +
      " AND NOT EXISTS (SELECT 1 FROM org_quality_gates oqg WHERE oqg.quality_gate_uuid = qg.uuid AND oqg.organization_uuid = ?)")
      .setBoolean(1, false)
      .setString(2, defaultOrgUuid);
    massUpdate.rowPluralName("quality gates");
    massUpdate.update("insert into org_quality_gates (uuid, quality_gate_uuid, organization_uuid) values(?, ?, ?)");
    massUpdate.execute((row, update) -> {
      update.setString(1, uuidFactory.create());
      update.setString(2, row.getString(1));
      update.setString(3, defaultOrgUuid);
      return true;
    });
  }
}
