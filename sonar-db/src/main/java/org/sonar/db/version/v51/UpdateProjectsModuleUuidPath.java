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

package org.sonar.db.version.v51;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.MassUpdate.Handler;
import org.sonar.db.version.Select.Row;
import org.sonar.db.version.SqlStatement;

/**
 * SONAR-6054
 * SONAR-6119
 */
public class UpdateProjectsModuleUuidPath extends BaseDataChange {

  private static final String SEP = ".";

  public UpdateProjectsModuleUuidPath(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("components");
    update.select("SELECT p.id, p.module_uuid_path, p.uuid, p.scope, p.qualifier FROM projects p");
    update.update("UPDATE projects SET module_uuid_path=? WHERE id=?");
    update.execute(new ModuleUuidPathUpdateHandler());
  }

  private static final class ModuleUuidPathUpdateHandler implements Handler {
    @Override
    public boolean handle(Row row, SqlStatement update) throws SQLException {
      Long id = row.getNullableLong(1);
      String moduleUuidPath = row.getNullableString(2);
      String uuid = row.getNullableString(3);
      String scope = row.getNullableString(4);
      String qualifier = row.getNullableString(5);

      boolean needUpdate = false;
      String newModuleUuidPath = moduleUuidPath;

      if (needUpdateForEmptyPath(newModuleUuidPath)) {
        newModuleUuidPath = SEP + uuid + SEP;
        needUpdate = true;
      }

      if (needUpdateForSeparators(newModuleUuidPath)) {
        newModuleUuidPath = newModuleUuidPathWithSeparators(newModuleUuidPath);
        needUpdate = true;
      }

      if (needUpdateToIncludeItself(newModuleUuidPath, uuid, scope, qualifier)) {
        newModuleUuidPath = newModuleUuidPathIncludingItself(newModuleUuidPath, uuid);
        needUpdate = true;
      }

      if (needUpdate) {
        update.setString(1, newModuleUuidPath);
        update.setLong(2, id);
      }
      return needUpdate;
    }

    private static boolean needUpdateForEmptyPath(@Nullable String moduleUuidPath) {
      return StringUtils.isEmpty(moduleUuidPath) || SEP.equals(moduleUuidPath);
    }

    private static boolean needUpdateForSeparators(String moduleUuidPath) {
      return !(moduleUuidPath.startsWith(SEP) && moduleUuidPath.endsWith(SEP));
    }

    private static String newModuleUuidPathWithSeparators(String oldModuleUuidPath) {
      StringBuilder newModuleUuidPath = new StringBuilder(oldModuleUuidPath);
      newModuleUuidPath.insert(0, SEP);
      newModuleUuidPath.append(SEP);
      return newModuleUuidPath.toString();
    }

    private static boolean needUpdateToIncludeItself(String moduleUuidPath, @Nullable String uuid, @Nullable String scope, @Nullable String qualifier) {
      return "PRJ".equals(scope) && !("DEV_PRJ".equals(qualifier)) && !(moduleUuidPath.contains(uuid));
    }

    private static String newModuleUuidPathIncludingItself(String moduleUuidPath, String uuid) {
      StringBuilder newModuleUuidPath = new StringBuilder(moduleUuidPath);
      newModuleUuidPath.append(uuid);
      newModuleUuidPath.append(SEP);
      return newModuleUuidPath.toString();
    }
  }
}
