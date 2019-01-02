/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

/**
 * The goal of this migration is to sanitize disabled USERS, regarding new way of authentication users.
 * Indeed, authentication will search for user but LOGIN but also buy using EXTERNAL_ID and EXTERNAL_PROVIDER.
 *
 * As a consequence, these columns must be set as NOT NULL in order to add a UNIQUE index on them.
 *
 * Unfortunately, these columns were previously set as null when disabling a user, that's why we need to populate them.
 */
public class UpdateNullValuesFromExternalColumnsAndLoginOfUsers extends DataChange {

  private static final Logger LOG = Loggers.get(UpdateNullValuesFromExternalColumnsAndLoginOfUsers.class);

  private final System2 system2;
  private UuidFactory uuidFactory;

  public UpdateNullValuesFromExternalColumnsAndLoginOfUsers(Database db, System2 system2, UuidFactory uuidFactory) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("users");
    massUpdate.select("SELECT id, login FROM users WHERE login IS NULL OR external_login IS NULL OR external_identity_provider IS NULL");
    massUpdate.update("UPDATE users SET login=?, external_login=?, external_identity_provider=?, updated_at=? WHERE id=?");

    long now = system2.now();
    massUpdate.execute((row, update) -> {
      long id = row.getLong(1);
      String login = row.getString(2);
      if (login == null) {
        LOG.warn("No login has been found for user id '{}'. A UUID has been generated to not have null value.", id);
        login = uuidFactory.create();
      }
      update.setString(1, login);
      update.setString(2, login);
      update.setString(3, "sonarqube");
      update.setLong(4, now);
      update.setLong(5, id);
      return true;
    });
  }

}
