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
package org.sonar.server.platform.db.migration.version.v202603;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class BackfillComponentUuidOnScaIssuesReleases extends DataChange {

  private static final Logger LOGGER = LoggerFactory.getLogger(BackfillComponentUuidOnScaIssuesReleases.class);

  public BackfillComponentUuidOnScaIssuesReleases(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    long totalNullRows = countNullComponentUuid(context);

    if (totalNullRows == 0) {
      LOGGER.info("No rows require component_uuid backfill in sca_issues_releases");
      return;
    }

    LOGGER.info("Starting backfill of component_uuid for {} rows in sca_issues_releases", totalNullRows);

    context.prepareUpsert("""
      UPDATE sca_issues_releases
      SET component_uuid = (
        SELECT component_uuid FROM sca_releases
        WHERE uuid = sca_issues_releases.sca_release_uuid
      )
      WHERE component_uuid IS NULL
      """)
      .execute()
      .commit();

    long orphanedRowCount = countNullComponentUuid(context);

    if (orphanedRowCount > 0) {
      LOGGER.warn("Found {} orphaned rows in sca_issues_releases with no matching sca_release. " +
        "These rows are already excluded from all queries (via INNER JOIN) and will be deleted to satisfy NOT NULL constraint.",
        orphanedRowCount);

      // Orphaned rows (no matching sca_release) remain NULL; delete them rather than
      // blocking the NOT NULL constraint in the next migration step.
      // An orphaned row is effectively invisible since queries always join to sca_releases table.
      context.prepareUpsert("""
        DELETE FROM sca_issues_releases
        WHERE component_uuid IS NULL
        """)
        .execute()
        .commit();

      LOGGER.info("Deleted {} orphaned rows from sca_issues_releases", orphanedRowCount);
      LOGGER.info("Successfully backfilled {} rows", totalNullRows - orphanedRowCount);
    } else {
      LOGGER.info("Successfully backfilled all {} rows with no orphaned data", totalNullRows);
    }
  }

  private static long countNullComponentUuid(Context context) throws SQLException {
    return context.prepareSelect("SELECT count(*) FROM sca_issues_releases WHERE component_uuid IS NULL")
      .get(row -> row.getLong(1));
  }
}
