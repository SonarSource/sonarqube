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
package org.sonar.server.platform.db.migration.version.v67;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class DropOldLicenses extends DataChange {

  private static final String LICENSE_HASH_SECURED_SUFFIX = ".licenseHash.secured";
  private static final String LICENSE_SECURED_SUFFIX = ".license.secured";

  public DropOldLicenses(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select prop_key from properties where prop_key like ?")
      .setString(1, "%" + LICENSE_HASH_SECURED_SUFFIX);
    massUpdate.update("delete from properties where prop_key = ? or prop_key = ?");
    massUpdate.rowPluralName("old license properties");
    massUpdate.execute((row, update) -> {
      String licenseHashKey = row.getString(1);
      String licenseKey = licenseHashKey.replace(LICENSE_HASH_SECURED_SUFFIX, "") + LICENSE_SECURED_SUFFIX;
      update.setString(1, licenseHashKey);
      update.setString(2, licenseKey);
      return true;
    });
  }
}
