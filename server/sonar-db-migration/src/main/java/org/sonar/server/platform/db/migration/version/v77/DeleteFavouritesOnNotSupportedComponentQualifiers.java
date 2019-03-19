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
package org.sonar.server.platform.db.migration.version.v77;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

@SupportsBlueGreen
public class DeleteFavouritesOnNotSupportedComponentQualifiers extends DataChange {

  public DeleteFavouritesOnNotSupportedComponentQualifiers(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("favourites");
    massUpdate.select("SELECT prop.id FROM properties prop " +
      "INNER JOIN projects p ON p.id=prop.resource_id AND p.qualifier NOT IN ('TRK', 'FIL', 'VW', 'SVW', 'APP', 'UTS') " +
      "WHERE prop_key=? AND user_id IS NOT NULL")
      .setString(1, "favourite");
    massUpdate.update("DELETE FROM properties WHERE id=?");
    massUpdate.execute((row, update) -> {
      int id = row.getInt(1);
      update.setInt(1, id);
      return true;
    });
  }

}
