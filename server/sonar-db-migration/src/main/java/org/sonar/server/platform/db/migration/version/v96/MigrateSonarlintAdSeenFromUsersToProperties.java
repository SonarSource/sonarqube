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
package org.sonar.server.platform.db.migration.version.v96;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class MigrateSonarlintAdSeenFromUsersToProperties extends DataChange {

  public static final String USER_DISMISSED_NOTICES_SONARLINT_AD = "user.dismissedNotices.sonarlintAd";

  private final UuidFactory uuidFactory;
  private final System2 system2;

  public MigrateSonarlintAdSeenFromUsersToProperties(Database db, UuidFactory uuidFactory, System2 system2) {
    super(db);
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select u.uuid, u.sonarlint_ad_seen, p.uuid from users u" +
        " left join properties p on u.uuid = p.user_uuid and p.prop_key = ?" +
        " where u.sonarlint_ad_seen = ?" +
        " and p.uuid is null")
      .setString(1, USER_DISMISSED_NOTICES_SONARLINT_AD)
      .setBoolean(2, true);

    massUpdate.update("insert into properties (uuid,prop_key,user_uuid,is_empty,created_at) values (?, ?, ?, ?, ?)");

    massUpdate.execute((row, update) -> {
      update.setString(1, uuidFactory.create());
      update.setString(2, USER_DISMISSED_NOTICES_SONARLINT_AD);
      update.setString(3, row.getString(1));
      update.setBoolean(4, true);
      update.setLong(5, system2.now());

      return true;
    });

  }
}
