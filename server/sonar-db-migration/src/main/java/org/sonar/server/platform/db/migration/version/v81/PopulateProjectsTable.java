/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v81;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

@SupportsBlueGreen
public class PopulateProjectsTable extends DataChange {
  private final System2 system;

  public PopulateProjectsTable(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select p.uuid, p.kee, p.qualifier, p.organization_uuid, p.name, p.description, p.private, p.tags, p.created_at  " +
      "from components p " +
      "left join projects np on np.uuid = p.uuid " +
      "where p.scope = 'PRJ' and p.qualifier in ('TRK', 'APP') and np.uuid is null");
    massUpdate.rowPluralName("projects");
    massUpdate.update("insert into projects (uuid, kee, qualifier, organization_uuid, name, description, private, tags, created_at, updated_at) " +
      "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(1));
      update.setString(2, row.getString(2));
      update.setString(3, row.getString(3));
      update.setString(4, row.getString(4));
      update.setString(5, row.getNullableString(5));
      update.setString(6, row.getNullableString(6));
      update.setBoolean(7, row.getBoolean(7));
      update.setString(8, row.getNullableString(8));
      update.setLong(9, row.getDate(9).getTime());
      update.setLong(10, system.now());
      return true;
    });
  }
}
