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
package org.sonar.server.platform.db.migration.version.v61;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

public class RemoveViewsDefinitionFromProperties extends DataChange {

  private static final String VIEWS_DEFINITION_PROPERTY_KEY = "views.def";
  private static final int VARCHAR_MAX_LENGTH = 4000;

  private final System2 system2;

  public RemoveViewsDefinitionFromProperties(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    String property = context
      .prepareSelect("select text_value from properties where prop_key=?")
      .setString(1, VIEWS_DEFINITION_PROPERTY_KEY)
      .get(row -> row.getNullableString(1));
    Integer hasInternalProperty = context
      .prepareSelect("select 1 from internal_properties where kee=?")
      .setString(1, VIEWS_DEFINITION_PROPERTY_KEY)
      .get(row -> row.getNullableInt(1));

    if (hasInternalProperty == null) {
      addToInternalProperties(context, property);
    }
    deleteFromProperties(context);
  }

  private void addToInternalProperties(Context context, @Nullable String property) throws SQLException {
    if (property != null) {
      boolean mustBeStoredInClob = property.length() > VARCHAR_MAX_LENGTH;
      try (Upsert insert = context.prepareUpsert("insert into internal_properties" +
        " (kee, is_empty, " + (mustBeStoredInClob ? "clob_value" : "text_value") + ", created_at)" +
        " values" +
        " (?,?,?,?)")) {
        long now = system2.now();
        insert
          .setString(1, "views.def")
          .setBoolean(2, false)
          .setString(3, property)
          .setLong(4, now)
          .execute()
          .commit();
      }
    }
  }

  private static void deleteFromProperties(Context context) throws SQLException {
    try (Upsert delete = context.prepareUpsert("delete from properties where prop_key=?")) {
      delete
        .setString(1, VIEWS_DEFINITION_PROPERTY_KEY)
        .execute()
        .commit();
    }
  }

}
