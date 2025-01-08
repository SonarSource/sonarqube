/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;

public class SetAllowQualityProfileDisableInheritedRules extends DataChange {
  private System2 system2;
  private UuidFactory uuidFactory;

  public SetAllowQualityProfileDisableInheritedRules(Database db, System2 system2, UuidFactory uuidFactory) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (!propertyIsAlreadyDefined(context)) {
      insertPropertyWithValueAsFalse(context);
    }
  }

  private static boolean propertyIsAlreadyDefined(Context context) throws SQLException {
    Select select = context.prepareSelect("select p.text_value from properties p where p.prop_key = ?")
      .setString(1, CorePropertyDefinitions.ALLOW_DISABLE_INHERITED_RULES);
    String value = select.get(row -> row.getString(1));
    return value != null;
  }

  private void insertPropertyWithValueAsFalse(Context context) throws SQLException {
    context.prepareUpsert("INSERT INTO properties"
      + "(prop_key, is_empty, text_value, created_at, uuid) "
      + "VALUES(?, ?, ?, ?, ?)")
      .setString(1, CorePropertyDefinitions.ALLOW_DISABLE_INHERITED_RULES)
      .setBoolean(2, false)
      .setString(3, "false")
      .setLong(4, system2.now())
      .setString(5, uuidFactory.create())
      .execute()
      .commit();
  }

}
