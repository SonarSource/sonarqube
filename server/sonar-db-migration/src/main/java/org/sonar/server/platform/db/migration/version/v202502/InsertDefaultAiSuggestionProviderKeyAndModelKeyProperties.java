/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202502;

import java.sql.SQLException;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.sonar.core.config.AiCodeFixEnablementConstants.SUGGESTION_FEATURE_ENABLED_PROPERTY;
import static org.sonar.core.config.AiCodeFixEnablementConstants.SUGGESTION_PROVIDER_KEY_PROPERTY;
import static org.sonar.core.config.AiCodeFixEnablementConstants.SUGGESTION_PROVIDER_MODEL_KEY_PROPERTY;

public class InsertDefaultAiSuggestionProviderKeyAndModelKeyProperties extends DataChange {

  static final String DISABLED = "DISABLED";
  static final String DEFAULT_PROVIDER_KEY = "OPENAI";
  static final String DEFAULT_PROVIDER_MODEL_KEY = "OPENAI_GPT_4O";
  private final System2 system2;
  private final UuidFactory uuidFactory;

  public InsertDefaultAiSuggestionProviderKeyAndModelKeyProperties(Database db, System2 system2, UuidFactory uuidFactory) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    boolean isAiCodeFixEnabled = Optional.ofNullable(context.prepareSelect("select text_value from properties where prop_key=?")
      .setString(1, SUGGESTION_FEATURE_ENABLED_PROPERTY)
      .get(r -> r.getString(1)))
      .map(value -> !DISABLED.equals(value))
      .orElse(false);

    if (isAiCodeFixEnabled) {
      insertProperty(context, SUGGESTION_PROVIDER_KEY_PROPERTY, DEFAULT_PROVIDER_KEY);
      insertProperty(context, SUGGESTION_PROVIDER_MODEL_KEY_PROPERTY, DEFAULT_PROVIDER_MODEL_KEY);
    }
  }

  private void insertProperty(Context context, String key, String value) throws SQLException {
    context.prepareUpsert("""
        INSERT INTO properties
        (prop_key, is_empty, text_value, created_at, uuid)
        VALUES(?, ?, ?, ?, ?)
      """)
      .setString(1, key)
      .setBoolean(2, false)
      .setString(3, value)
      .setLong(4, system2.now())
      .setString(5, uuidFactory.create())
      .execute()
      .commit();
  }

}
