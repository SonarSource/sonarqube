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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.DataChange;

public class PopulateLastUsedColumnOfRulesProfiles extends DataChange {

  private static final Pattern PATTERN_QP_KEY = Pattern.compile("\"key\"\\s*:\\s*\"(.*?)\"");

  public PopulateLastUsedColumnOfRulesProfiles(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    Map<String, Long> lastAnalysisDatesByQualityProfileKey = buildQualityProfilesMap(context);
    if (lastAnalysisDatesByQualityProfileKey.isEmpty()) {
      return;
    }

    populateLastUsedColumn(context, lastAnalysisDatesByQualityProfileKey);
  }

  private static Map<String, Long> buildQualityProfilesMap(Context context) throws SQLException {
    Map<String, Long> lastAnalysisDatesByQPKeys = new HashMap<>();

    context.prepareSelect("select s.created_at, pm.text_value " +
      "from project_measures pm " +
      "  inner join snapshots s on pm.snapshot_id = s.id " +
      "  inner join metrics m on pm.metric_id=m.id " +
      "where s.islast=? " +
      "  and m.name='quality_profiles' " +
      "order by s.created_at ")
      .setBoolean(1, true)
      .scroll(row -> {
        long analysisDate = row.getLong(1);
        String json = row.getString(2);
        Matcher matcher = PATTERN_QP_KEY.matcher(json);
        while (matcher.find()) {
          lastAnalysisDatesByQPKeys.put(matcher.group(1), analysisDate);
        }
      });
    return lastAnalysisDatesByQPKeys;
  }

  private static void populateLastUsedColumn(Context context, Map<String, Long> lastAnalysisDatesByQualityProfileKey) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id, kee from rules_profiles where last_used is null");
    massUpdate.update("update rules_profiles set last_used=? where id=?");
    massUpdate.rowPluralName("rules_profiles");
    massUpdate.execute((row, update) -> handle(lastAnalysisDatesByQualityProfileKey, row, update));
  }

  private static boolean handle(Map<String, Long> lastAnalysisDatesByQualityProfileKey, Select.Row row, SqlStatement update) throws SQLException {
    int qualityProfileId = row.getInt(1);
    String qualityProfileKey = row.getString(2);

    update.setLong(1, lastAnalysisDatesByQualityProfileKey.get(qualityProfileKey));
    update.setInt(2, qualityProfileId);

    return true;
  }
}
