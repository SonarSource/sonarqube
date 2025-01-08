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
package org.sonar.server.platform.db.migration.version.v202501;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

public class UpdateDefaultAdminPasswordIfInvalidHashMechanism extends DataChange {

  static final String ADMIN_DEFAULT_PASSWORD = "100000$t2h8AtNs1AlCHuLobDjHQTn9XppwTIx88UjqUm4s8RsfTuXQHSd/fpFexAnewwPsO6jGFQUv/24DnO55hY6Xew==";
  static final String ADMIN_SALT = "k9x9eN127/3e/hf38iNiKwVfaVk=";

  public UpdateDefaultAdminPasswordIfInvalidHashMechanism(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {

    String upsertQuery = """
      update users set
      crypted_password = ?,
      salt = ?,
      hash_method = ?
      where login = 'admin'
      and crypted_password = ?
      and user_local = ?
      and active = ?
      and hash_method = 'BCRYPT'
      """;

    Upsert upsert = context.prepareUpsert(upsertQuery);
    upsert.setString(1, ADMIN_DEFAULT_PASSWORD);
    upsert.setString(2, ADMIN_SALT);
    upsert.setString(3, "PBKDF2");
    upsert.setString(4, "$2a$12$uCkkXmhW5ThVK8mpBvnXOOJRLd64LJeHTeCkSuB3lfaR2N0AYBaSi");
    upsert.setBoolean(5, true);
    upsert.setBoolean(6, true);
    upsert.execute();
    upsert.commit();
  }
}
