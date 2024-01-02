/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v94;

import java.sql.SQLException;
import java.util.List;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;

public class SelectUsersWithSha1HashMethod extends DataChange {
  private static final Logger LOG = Loggers.get(SelectUsersWithSha1HashMethod.class);

  private static final String UNSUPPORTED_HASH_METHOD = "SHA1";

  public SelectUsersWithSha1HashMethod(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Select select = context.prepareSelect("select login from users where hash_method = ?");
    select.setString(1, UNSUPPORTED_HASH_METHOD);
    List<String> logins = select.list(row -> row.getString(1));
    if (!logins.isEmpty()) {
      LOG.warn("The following local accounts have their password hashed with an algorithm which is not longer supported. "
        + "They will not be able to login anymore. Please reset their password if the accounts need to be kept. {}", logins);
    }
  }
}
