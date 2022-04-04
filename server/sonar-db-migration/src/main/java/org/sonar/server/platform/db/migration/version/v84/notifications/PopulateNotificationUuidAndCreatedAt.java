/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.notifications;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateNotificationUuidAndCreatedAt extends DataChange {

  private final UuidFactory uuidFactory;
  private final System2 system2;

  public PopulateNotificationUuidAndCreatedAt(Database db, UuidFactory uuidFactory, System2 system2) {
    super(db);
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select id from notifications where uuid is null");
    massUpdate.update("update notifications set uuid = ?, created_at = ? where id = ?");

    // now - 7 days, to have previous notification in the past
    long lastWeek = system2.now() - (1000 * 60 * 60 * 24 * 7);

    AtomicLong cpt = new AtomicLong(0);
    massUpdate.execute((row, update) -> {
      update.setString(1, uuidFactory.create());
      update.setLong(2, lastWeek + cpt.longValue());
      update.setLong(3, row.getLong(1));
      cpt.addAndGet(1);
      return true;
    });
  }
}
