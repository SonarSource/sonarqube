/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.List;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.DeduplicateTableBuilder;
import org.sonar.server.platform.db.migration.step.DataChange;

public class DeduplicatePropertiesTable extends DataChange {
  private static final String PROPERTIES_TABLE = "properties";

  public DeduplicatePropertiesTable(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    List<String> queries = new DeduplicateTableBuilder(PROPERTIES_TABLE)
      .addReferenceColumn("prop_key")
      .addReferenceColumn("entity_uuid")
      .addReferenceColumn("user_uuid")
      .setIdentityColumn("uuid")
      .build();

    for (String q : queries) {
      context.prepareUpsert(q)
        .execute()
        .commit();
    }
  }
}
