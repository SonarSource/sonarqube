/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202601;

import java.sql.SQLException;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class PopulateAnalysesCounterStartDateProperty extends DataChange {

  private static final String ANALYSES_COUNTER_DATE = "analyses.counter.date";
  private final System2 system2;

  PopulateAnalysesCounterStartDateProperty(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    final String existingValue = context.prepareSelect("select text_value from internal_properties where kee = ?")
      .setString(1, ANALYSES_COUNTER_DATE)
      .get(r -> r.getString(1));

    if (StringUtils.isEmpty(existingValue)) {
      long now = system2.now();
      context.prepareUpsert("""
            insert into internal_properties (kee, is_empty, text_value, created_at)
            values (?, ?, ?, ?)
          """)
        .setString(1, ANALYSES_COUNTER_DATE)
        .setBoolean(2, false)
        .setString(3, String.valueOf(now))
        .setLong(4, now)
        .execute()
        .commit();
    }
  }
}
