/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

/**
 * Fills {@code issue_count_latest} with the newest row of each (entity, entity type, dimension) of {@code issue_count_history}.
 * A single statement, written with the ranking function every supported database has. A key that already has a latest row is left
 * alone, so running the step again changes nothing.
 */
public class PopulateIssueCountLatest extends DataChange {

  public PopulateIssueCountLatest(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    context.prepareUpsert("""
      INSERT INTO issue_count_latest (entity_id, entity_type, dimension_id, recorded_at_epoch, issue_count)
      SELECT ranked.entity_id, ranked.entity_type, ranked.dimension_id, ranked.recorded_at_epoch, ranked.issue_count
      FROM (
        SELECT entity_id, entity_type, dimension_id, recorded_at_epoch, issue_count,
          ROW_NUMBER() OVER (PARTITION BY entity_id, entity_type, dimension_id ORDER BY recorded_at_epoch DESC) AS rank_in_key
        FROM issue_count_history
      ) ranked
      WHERE ranked.rank_in_key = 1
        AND NOT EXISTS (
          SELECT 1 FROM issue_count_latest latest
          WHERE latest.entity_id = ranked.entity_id
            AND latest.entity_type = ranked.entity_type
            AND latest.dimension_id = ranked.dimension_id)
      """)
      .execute()
      .commit();
  }
}
