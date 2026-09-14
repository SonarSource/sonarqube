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

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

/**
 * Persists the UTC day used by the history backfill steps.
 *
 * <p>Migration steps are retried independently. Keeping the first day in {@code internal_properties} prevents a
 * retry after midnight from producing a second artificial history day.
 */
public class PersistHistoryBackfillUtcDayEpoch extends DataChange {

  static final String FROZEN_EPOCH_PROPERTY = "history.backfill.epoch";

  private final System2 system2;

  public PersistHistoryBackfillUtcDayEpoch(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String existingValue = context.prepareSelect("select text_value from internal_properties where kee = ?")
      .setString(1, FROZEN_EPOCH_PROPERTY)
      .get(row -> row.getNullableString(1));
    if (existingValue != null) {
      HistoryBackfillSupport.parseFrozenEpoch(existingValue);
      return;
    }

    long now = system2.now();
    long frozenEpoch = HistoryBackfillSupport.toUtcDayEpoch(now);
    context.prepareUpsert("""
      insert into internal_properties (kee, is_empty, text_value, created_at)
      values (?, ?, ?, ?)
      """)
      .setString(1, FROZEN_EPOCH_PROPERTY)
      .setBoolean(2, false)
      .setString(3, Long.toString(frozenEpoch))
      .setLong(4, now)
      .execute()
      .commit();
  }
}
