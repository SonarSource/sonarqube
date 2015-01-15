/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.db.migrations.v51;

import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.MassUpdate.Handler;
import org.sonar.server.db.migrations.Select.Row;
import org.sonar.server.db.migrations.SqlStatement;

import javax.annotation.Nullable;

import java.sql.SQLException;

/**
 * SONAR-6054
 */
public class UpdateProjectsModuleUuidPath extends BaseDataChange {

  private static final String SEP = ".";

  public UpdateProjectsModuleUuidPath(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("projects");
    update.select("SELECT p.id, p.module_uuid_path FROM projects p");
    update.update("UPDATE projects SET module_uuid_path=? WHERE id=?");
    update.execute(new Handler() {
      @Override
      public boolean handle(Row row, SqlStatement update) throws SQLException {
        Long id = row.getLong(1);
        String moduleUuidPath = row.getString(2);
        if (needUpdate(moduleUuidPath)) {
          update.setString(1, newModuleUuidPath(moduleUuidPath));
          update.setLong(2, id);
          return true;
        }
        return false;
      }
    });
  }

  private static boolean needUpdate(@Nullable String moduleUuidPath) {
    return moduleUuidPath == null || !(moduleUuidPath.startsWith(SEP) && moduleUuidPath.endsWith(SEP));
  }

  private static String newModuleUuidPath(@Nullable String oldModuleUuidPath) {
    if (oldModuleUuidPath == null || oldModuleUuidPath.isEmpty()) {
      return SEP;
    } else {
      StringBuilder newModuleUuidPath = new StringBuilder(oldModuleUuidPath);
      newModuleUuidPath.insert(0, SEP);
      newModuleUuidPath.append(SEP);
      return newModuleUuidPath.toString();
    }
  }

}
