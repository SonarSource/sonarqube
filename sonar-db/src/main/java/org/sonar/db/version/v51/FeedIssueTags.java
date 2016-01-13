/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v51;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.sql.SQLException;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.MassUpdate.Handler;
import org.sonar.db.version.Select.Row;
import org.sonar.db.version.Select.RowHandler;
import org.sonar.db.version.SqlStatement;

/**
 * SONAR-5897
 */
public class FeedIssueTags extends BaseDataChange {

  private static final Joiner TAG_JOINER = Joiner.on(',').skipNulls();

  private final long now;

  public FeedIssueTags(Database db, System2 system) {
    super(db);
    this.now = system.now();
  }

  @Override
  public void execute(Context context) throws SQLException {

    final Map<Integer, String> tagsByRuleId = Maps.newHashMap();
    context.prepareSelect("SELECT id, system_tags, tags FROM rules").scroll(new RowHandler() {
      @Override
      public void handle(Row row) throws SQLException {
        Integer id = row.getNullableInt(1);
        tagsByRuleId.put(id, StringUtils.trimToNull(TAG_JOINER.join(
          StringUtils.trimToNull(row.getNullableString(2)),
          StringUtils.trimToNull(row.getNullableString(3)))));
      }
    });

    MassUpdate update = context.prepareMassUpdate().rowPluralName("issues");
    update.select("SELECT id, rule_id FROM issues WHERE tags IS NULL");
    update.update("UPDATE issues SET tags = ?, updated_at = ? WHERE id = ?");
    update.execute(new Handler() {
      @Override
      public boolean handle(Row row, SqlStatement update) throws SQLException {
        Long id = row.getNullableLong(1);
        Integer ruleId = row.getNullableInt(2);
        boolean updated = false;
        if (tagsByRuleId.get(ruleId) != null) {
          updated = true;
          update.setString(1, tagsByRuleId.get(ruleId));
          update.setLong(2, now);
          update.setLong(3, id);
        }
        return updated;
      }
    });
  }

}
