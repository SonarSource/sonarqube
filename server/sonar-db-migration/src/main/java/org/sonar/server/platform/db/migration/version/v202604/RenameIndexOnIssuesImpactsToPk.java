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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.dialect.Oracle;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.DatabaseUtils.findExistingIndex;

/**
 * On Oracle, {@link DropUniqueIndexOnIssuesImpacts} leaves the {@code uniq_iss_key_sof_qual} index in place
 * because Oracle reuses it as the backing index for {@code PK_ISSUES_IMPACTS}. This migration renames it to
 * match the PK constraint name for consistency. No-op on every other dialect.
 */
public class RenameIndexOnIssuesImpactsToPk extends DdlChange {
  private static final String TABLE_NAME = "issues_impacts";
  private static final String OLD_INDEX_NAME = "uniq_iss_key_sof_qual";
  private static final String NEW_INDEX_NAME = "pk_issues_impacts";

  public RenameIndexOnIssuesImpactsToPk(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (!Oracle.ID.equals(getDialect().getId())) {
      return;
    }
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      findExistingIndex(connection, TABLE_NAME, OLD_INDEX_NAME)
        .ifPresent(existingIndex -> context.execute("ALTER INDEX " + existingIndex + " RENAME TO " + NEW_INDEX_NAME));
    }
  }
}
