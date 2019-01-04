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
package org.sonar.server.platform.db.migration.version.v76;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;

@SupportsBlueGreen
public class FixDirectionOfMetrics extends DataChange {

  public FixDirectionOfMetrics(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String sql = "UPDATE metrics SET direction = ? WHERE name = ? AND direction != ?";

    context.prepareUpsert(sql)
      .setInt(1, 1)
      .setString(2, "tests")
      .setInt(3, 1)
      .execute()
      .commit();

    context.prepareUpsert(sql)
      .setInt(1, -1)
      .setString(2, "conditions_to_cover")
      .setInt(3, -1)
      .execute()
      .commit();

    context.prepareUpsert(sql)
      .setInt(1, -1)
      .setString(2, "new_conditions_to_cover")
      .setInt(3, -1)
      .execute()
      .commit();
  }
}
