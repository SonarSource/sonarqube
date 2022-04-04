/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v87;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class MoveDefaultProjectVisibilityToGlobalProperties extends DataChange {

  private final UuidFactory uuidFactory;
  private final System2 system2;

  public MoveDefaultProjectVisibilityToGlobalProperties(Database db, UuidFactory uuidFactory, System2 system2) {
    super(db);
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    Boolean newProjectPrivate = context.prepareSelect("select new_project_private from organizations where kee = ?")
      .setString(1, "default-organization")
      .get(row -> row.getNullableBoolean(1));

    if (newProjectPrivate == null) {
      throw new IllegalStateException("Default organization is missing");
    } else {
      context.prepareUpsert("insert into properties (uuid, prop_key, is_empty, text_value, created_at) values (?, ?, ?, ?, ?)")
        .setString(1, uuidFactory.create())
        .setString(2, "projects.default.visibility")
        .setBoolean(3, false)
        .setString(4, Boolean.TRUE.equals(newProjectPrivate) ? "private" : "public")
        .setLong(5, system2.now())
        .execute()
        .commit();
    }
  }
}
