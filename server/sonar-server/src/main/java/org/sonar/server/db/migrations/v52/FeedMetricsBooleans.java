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

package org.sonar.server.db.migrations.v52;

import java.sql.SQLException;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;

public class FeedMetricsBooleans extends BaseDataChange {

  public FeedMetricsBooleans(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.prepareUpsert("update metrics set OPTIMIZED_BEST_VALUE=?, HIDDEN=?, DELETE_HISTORICAL_DATA=? where user_managed=? or OPTIMIZED_BEST_VALUE is null or HIDDEN is null or DELETE_HISTORICAL_DATA is null")
      .setBoolean(1, false)
      .setBoolean(2, false)
      .setBoolean(3, false)
      .setBoolean(4, true)
      .execute().commit();
  }
}
