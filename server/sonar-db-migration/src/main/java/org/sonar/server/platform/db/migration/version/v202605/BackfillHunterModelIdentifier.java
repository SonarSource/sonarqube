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
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class BackfillHunterModelIdentifier extends DataChange {

  static final String HUNTER_AGENT = "HUNTER_AGENT";
  static final String HUNTER_MODEL_IDENTIFIER = "claude-opus-4-8";

  public BackfillHunterModelIdentifier(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    context.prepareUpsert("update llm_provider_mappings set model_identifier = ? where ai_capability = ? and nullif(trim(model_identifier), '') is null")
      .setString(1, HUNTER_MODEL_IDENTIFIER)
      .setString(2, HUNTER_AGENT)
      .execute()
      .commit();
  }
}
