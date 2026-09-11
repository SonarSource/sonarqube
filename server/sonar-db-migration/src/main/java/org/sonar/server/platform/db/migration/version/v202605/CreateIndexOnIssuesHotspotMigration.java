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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.lang.String.format;

/**
 * Index supporting the keyset pagination of the Hotspots-to-Issues migration (MMF-5734, SONAR-32194):
 * {@code IssueMapper.selectHotspotKeysForMigration} and {@code countHotspotsForMigration}.
 *
 * <p>Security Hotspot findings are a fraction of a percent of {@code issues} (0.2% on a 175M-row reference
 * instance), and {@code issue_type} appears in no other index, so without this the key query can only be served
 * by a full scan. The key column order matches the query's {@code ORDER BY project_uuid, kee} and its keyset
 * predicate, so the index serves the filter, the range seek and the ordering at once, with no sort and with
 * {@code LIMIT} stopping immediately.</p>
 *
 * <p>{@code rule_uuid} is covered too, so that the query's semi-join on {@code rules} does not force a heap fetch
 * per candidate row. Without it the scan could not be index-only, because {@code rule_uuid} is referenced by a
 * predicate even though it is not selected.</p>
 *
 * <p>The shape differs by dialect because only PostgreSQL and SQL Server support partial and INCLUDE-d indexes:</p>
 * <ul>
 *   <li>PostgreSQL / SQL Server: {@code (project_uuid, kee) INCLUDE (rule_uuid) WHERE issue_type = 4}. Only the
 *   hotspot rows are stored, so it is a few tens of MB rather than a full 175M-entry index, and rows leave it as
 *   the migration rewrites {@code issue_type} - it shrinks to empty as the migration converges.</li>
 *   <li>Oracle / H2: {@code (issue_type, project_uuid, kee, rule_uuid)}. Neither supports a partial predicate, so
 *   it becomes a leading equality key column instead. Oracle's habit of omitting index entries cannot substitute
 *   for that: it only omits an entry when the <em>whole</em> key is NULL, and {@code kee} is NOT NULL, so every row
 *   gets an entry regardless of {@code issue_type} (which is itself nullable). Neither dialect supports INCLUDE
 *   either, so {@code rule_uuid} is a trailing key column - harmless here, since nothing seeks or orders past
 *   {@code kee}. This indexes every row, so unlike the partial form it costs a full build in the upgrade window
 *   and does not shrink as the migration progresses.</li>
 * </ul>
 *
 * <p>The index is temporary on every dialect: SONAR-32235 drops it once migrating all hotspots is enforced (not
 * before SQS 2027.2). Until then the partial form self-empties, while the Oracle/H2 form stays full size.</p>
 */
public class CreateIndexOnIssuesHotspotMigration extends DdlChange {

  static final String TABLE_NAME = "issues";
  static final String INDEX_NAME = "issues_hotspot_migration";
  static final String COLUMNS = "project_uuid, kee";
  // Needed by the rules semi-join. INCLUDE-d where supported so it takes no part in the keyset seek or the
  // ORDER BY; a trailing key column elsewhere, which has the same effect for this query.
  static final String COVERED_COLUMNS = "rule_uuid";
  static final String ISSUE_TYPE_COLUMN = "issue_type";
  // org.sonar.core.rule.RuleType.SECURITY_HOTSPOT - the value the migration scans for and clears.
  static final int SECURITY_HOTSPOT_TYPE = 4;

  public CreateIndexOnIssuesHotspotMigration(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createIndex(context, connection);
    }
  }

  private void createIndex(Context context, Connection connection) {
    if (DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
      return;
    }
    switch (getDialect().getId()) {
      // Partial and covering: only the hotspot rows are stored, and rule_uuid rides along without joining the
      // key. See the class javadoc for why the shape differs per dialect.
      case PostgreSql.ID, MsSql.ID -> context.execute(
        format("CREATE INDEX %s ON %s (%s) INCLUDE (%s) WHERE %s = %d",
          INDEX_NAME, TABLE_NAME, COLUMNS, COVERED_COLUMNS, ISSUE_TYPE_COLUMN, SECURITY_HOTSPOT_TYPE));
      // No partial predicate and no INCLUDE here, so issue_type leads as a seekable equality and rule_uuid
      // trails as a key column. Indexes every row, so this one does not shrink as the migration progresses.
      case Oracle.ID, H2.ID -> context.execute(
        format("CREATE INDEX %s ON %s (%s, %s, %s)",
          INDEX_NAME, TABLE_NAME, ISSUE_TYPE_COLUMN, COLUMNS, COVERED_COLUMNS));
      default -> throw new IllegalArgumentException("Unsupported dialect id " + getDialect().getId());
    }
  }
}
