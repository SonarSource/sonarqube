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

package org.sonar.db.version.v52;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;

public class FeedMetricsBooleans extends BaseDataChange {

  public FeedMetricsBooleans(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.prepareUpsert("update metrics set optimized_best_value=?, hidden=?, delete_historical_data=? " +
      "where user_managed=? or optimized_best_value is null or hidden is null or delete_historical_data is null")
      .setBoolean(1, false)
      .setBoolean(2, false)
      .setBoolean(3, false)
      .setBoolean(4, true)
      .execute().commit();
  }
}
