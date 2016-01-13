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
package org.sonar.db.version.v53;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

/**
 * As the default dashboard 'Dashboard' has been renamed to 'Custom', the corresponding entry in LOADED_TEMPLATES is updated,
 * in order to not have this dashboard twice.
 */
public class UpdateCustomDashboardInLoadedTemplates extends BaseDataChange {

  public UpdateCustomDashboardInLoadedTemplates(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("loaded templates");
    update.select("SELECT l.id FROM loaded_templates l WHERE l.kee='Dashboard' and l.template_type='DASHBOARD'");
    update.update("UPDATE loaded_templates SET kee=? WHERE id=?");
    update.execute(MigrationHandler.INSTANCE);
  }

  private enum MigrationHandler implements MassUpdate.Handler {
    INSTANCE;

    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      update.setString(1, "Custom");
      update.setLong(2, row.getLong(1));
      return true;
    }
  }
}
