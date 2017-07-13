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
package org.sonar.server.platform.db.migration.version.v66;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateDefaultPermTemplateAppColumnOfOrganizations extends DataChange {

  private final System2 system2;

  public PopulateDefaultPermTemplateAppColumnOfOrganizations(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("Organizations without default template for applications");
    massUpdate.select("SELECT o.uuid, o.default_perm_template_view FROM organizations o " +
      "WHERE o.default_perm_template_view IS NOT NULL AND o.default_perm_template_app IS NULL");
    massUpdate.update("UPDATE organizations SET default_perm_template_app=?, updated_at=? WHERE uuid=?");
    massUpdate.execute((row, update) -> {
      update.setString(1, row.getNullableString(2));
      update.setLong(2, now);
      update.setString(3, row.getString(1));
      return true;
    });
  }
}
