/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v101;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassRowSplitter;
import org.sonar.server.platform.db.migration.step.Select;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

class MigrateScmAccountsFromUsersToScmAccounts extends DataChange {

  @VisibleForTesting
  static final char SCM_ACCOUNTS_SEPARATOR_CHAR = '\n';

  public MigrateScmAccountsFromUsersToScmAccounts(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (isScmColumnDropped()) {
      return;
    }
    migrateData(context);
  }

  private boolean isScmColumnDropped() throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      return !DatabaseUtils.tableColumnExists(connection, DropScmAccountsInUsers.TABLE_NAME, DropScmAccountsInUsers.COLUMN_NAME);
    }
  }

  private static void migrateData(Context context) throws SQLException {
    MassRowSplitter<ScmAccountRow> massRowSplitter = context.prepareMassRowSplitter();

    massRowSplitter.select("select u.uuid, lower(u.scm_accounts) from users u where u.active=? and not exists (select 1 from scm_accounts sa where sa.user_uuid = u.uuid)")
      .setBoolean(1, true);

    massRowSplitter.insert("insert into scm_accounts (user_uuid, scm_account) values (?, ?)");

    massRowSplitter.splitRow(MigrateScmAccountsFromUsersToScmAccounts::toScmAccountRows);

    massRowSplitter.execute((scmAccountRow, insert) -> {
      insert.setString(1, scmAccountRow.userUuid());
      insert.setString(2, scmAccountRow.scmAccount());
      return true;
    });
  }

  private static Set<ScmAccountRow> toScmAccountRows(Select.Row row) {
    try {
      String userUuid = row.getString(1);
      String[] scmAccounts = StringUtils.split(row.getString(2), SCM_ACCOUNTS_SEPARATOR_CHAR);
      if (scmAccounts == null) {
        return emptySet();
      }
      return Arrays.stream(scmAccounts)
        .map(scmAccount -> new ScmAccountRow(userUuid, scmAccount))
        .collect(toSet());
    } catch (SQLException sqlException) {
      throw new RuntimeException(sqlException);
    }
  }

  @VisibleForTesting
  record ScmAccountRow(String userUuid, String scmAccount) {
  }
}
