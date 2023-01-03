/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.DropIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class DropUnusedIndexes extends DdlChange {
  public DropUnusedIndexes(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    // duplicated by its PK
    context.execute(new DropIndexBuilder(getDialect()).setTable("events").setName("events_uuid").build());
    // duplicated by its PK
    context.execute(new DropIndexBuilder(getDialect()).setTable("ce_activity").setName("ce_activity_uuid").build());
    // no query filtering or ordering by users_updated_at
    context.execute(new DropIndexBuilder(getDialect()).setTable("file_sources").setName("file_sources_updated_at").build());
    // no query filtering or ordering by users_updated_at
    context.execute(new DropIndexBuilder(getDialect()).setTable("users").setName("users_updated_at").build());
    // not used
    context.execute(new DropIndexBuilder(getDialect()).setTable("alm_app_installs").setName("alm_app_installs_external_id").build());
  }
}
