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
package org.sonar.db.version.v50;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

/**
 * Used in the Active Record Migration 716
 *
 * @since 5.0
 */
public class InsertProjectsAuthorizationUpdatedAtMigrationStep extends BaseDataChange {

  private final System2 system;

  public InsertProjectsAuthorizationUpdatedAtMigrationStep(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT p.id FROM projects p WHERE p.scope=? AND p.enabled=? and p.authorization_updated_at IS NULL").setString(1, "PRJ").setBoolean(2, true);
    massUpdate.update("UPDATE projects SET authorization_updated_at=? WHERE id=?");
    massUpdate.rowPluralName("projects");
    massUpdate.execute(new MigrationHandler());
  }

  private class MigrationHandler implements MassUpdate.Handler {
    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      Long id = row.getNullableLong(1);
      update.setLong(1, system.now());
      update.setLong(2, id);
      return true;
    }
  }
}
