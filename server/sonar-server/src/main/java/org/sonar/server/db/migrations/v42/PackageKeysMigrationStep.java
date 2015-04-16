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
package org.sonar.server.db.migrations.v42;

import java.sql.SQLException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

/**
 * Used in Rails migration 490
 *
 * @since 4.2
 */
public class PackageKeysMigrationStep extends BaseDataChange {

  public PackageKeysMigrationStep(Database database) {
    super(database);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT id, kee FROM projects WHERE qualifier='PAC'");
    massUpdate.update("UPDATE projects SET qualifier='DIR', kee=? WHERE id=?");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Long id = row.getNullableLong(1);
        String key = row.getNullableString(2);
        update.setString(1, convertKey(key));
        update.setLong(2, id);
        return true;
      }
    });
  }

  @CheckForNull
  String convertKey(@Nullable String packageKey) {
    if (packageKey != null) {
      String prefix = StringUtils.substringBeforeLast(packageKey, ":") + ":";
      String key = StringUtils.substringAfterLast(packageKey, ":");
      if ("[default]".equals(key)) {
        return prefix + "[root]";
      }
      return prefix + StringUtils.replace(key, ".", "/");
    }
    return null;
  }
}
