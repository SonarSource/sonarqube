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
package org.sonar.server.platform.db.migration.version.v202501;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class LogMessageIfInvalidHashMechanismUsed extends DataChange {
  private static final Logger LOG = LoggerFactory.getLogger(LogMessageIfInvalidHashMechanismUsed.class);

  @VisibleForTesting
  static final String BCRYPT_HASH = "BCRYPT";

  public LogMessageIfInvalidHashMechanismUsed(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {

    List<String> usersOnBcryptHash = getUsersOnBcryptHash(context);

    if (usersOnBcryptHash.isEmpty()) {
      return;
    }

    LOG.atWarn().log("The following active users are still relying on passwords using the unsupported hash mechanism ({}). " +
        "Their passwords should be manually updated by an administrator: {}",
      BCRYPT_HASH, String.join(", ", usersOnBcryptHash));

  }

  private static List<String> getUsersOnBcryptHash(Context context) throws SQLException {
    List<String> usersOnBcryptHash = new ArrayList<>();

    context.prepareSelect("select login from users where active = ? and hash_method = ?")
      .setBoolean(1, true)
      .setString(2, BCRYPT_HASH)
      .scroll(row -> usersOnBcryptHash.add(row.getString(1)));

    return usersOnBcryptHash;
  }
}
