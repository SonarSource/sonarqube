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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

/**
 * SONAR-10409
 *
 * Update keys of PERMISSION_TEMPLATES that are longer than 40 characters
 */
public class UpdatePermissionTooLongTemplateKeys extends DataChange {

  private final String lengthFunction;
  private final UuidFactory uuidFactory;

  public UpdatePermissionTooLongTemplateKeys(Database db, UuidFactory uuidFactory) {
    super(db);
    this.uuidFactory = uuidFactory;
    if (db.getDialect().getId().equals(MsSql.ID)) {
      lengthFunction = "len";
    } else {
      lengthFunction = "length";
    }
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select kee from permission_templates where " + lengthFunction + "(kee) > ?").setInt(1, VarcharColumnDef.UUID_SIZE);
    massUpdate.update("update permission_templates set kee=? where kee=?");
    massUpdate.rowPluralName("permission templates");

    massUpdate.execute((row, update) -> {
      String oldKey = row.getString(1);
      String newKey = uuidFactory.create();

      update.setString(1, newKey);
      update.setString(2, oldKey);
      return true;
    });
  }
}
