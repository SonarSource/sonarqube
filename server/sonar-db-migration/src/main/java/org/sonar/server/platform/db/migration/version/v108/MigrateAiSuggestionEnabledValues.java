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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.SQLException;
import java.util.Optional;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class MigrateAiSuggestionEnabledValues extends DataChange {

  static final String AI_CODEFIX_ENABLED_PROP_KEY = "sonar.ai.suggestions.enabled";
  static final String ENABLED_FOR_ALL_PROJECTS = "ENABLED_FOR_ALL_PROJECTS";
  static final String DISABLED = "DISABLED";

  public MigrateAiSuggestionEnabledValues(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {

    var isAiCodeFixEnabledOptional = Optional.ofNullable(context.prepareSelect("select text_value from properties where prop_key=?")
      .setString(1, AI_CODEFIX_ENABLED_PROP_KEY)
      .get(r -> r.getBoolean(1)));

    if (isAiCodeFixEnabledOptional.isPresent()) {
      boolean isAiCodeFixEnabled = isAiCodeFixEnabledOptional.get();
      context.prepareUpsert("update properties set text_value=? where prop_key=?")
        .setString(1, isAiCodeFixEnabled ? ENABLED_FOR_ALL_PROJECTS : DISABLED)
        .setString(2, AI_CODEFIX_ENABLED_PROP_KEY)
        .execute()
        .commit();
      if (isAiCodeFixEnabled) {
        context.prepareUpsert("update projects set ai_code_fix_enabled=true")
          .execute()
          .commit();
      }
    }
  }

}
