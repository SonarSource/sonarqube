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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.config.AiCodeFixEnablementConstants.SUGGESTION_FEATURE_ENABLED_PROPERTY;
import static org.sonar.core.config.AiCodeFixEnablementConstants.SUGGESTION_PROVIDER_KEY_PROPERTY;
import static org.sonar.core.config.AiCodeFixEnablementConstants.SUGGESTION_PROVIDER_MODEL_KEY_PROPERTY;
import static org.sonar.server.platform.db.migration.version.v202502.InsertDefaultAiSuggestionProviderKeyAndModelKeyProperties.DEFAULT_PROVIDER_KEY;
import static org.sonar.server.platform.db.migration.version.v202502.InsertDefaultAiSuggestionProviderKeyAndModelKeyProperties.DEFAULT_PROVIDER_MODEL_KEY;
import static org.sonar.server.platform.db.migration.version.v202502.InsertDefaultAiSuggestionProviderKeyAndModelKeyProperties.DISABLED;

class InsertDefaultAiSuggestionProviderKeyAndModelKeyPropertiesTest {
  private static final long NOW = 1;

  @RegisterExtension
  private final MigrationDbTester db = MigrationDbTester.createForMigrationStep(InsertDefaultAiSuggestionProviderKeyAndModelKeyProperties.class);
  private final System2 system2 = new TestSystem2().setNow(NOW);
  private final InsertDefaultAiSuggestionProviderKeyAndModelKeyProperties underTest = new InsertDefaultAiSuggestionProviderKeyAndModelKeyProperties(db.database(), system2,
    new SequenceUuidFactory());

  @Test
  void execute_shouldNotUpdateAnything_whenTheAiCodeFixNotSet() throws SQLException {
    underTest.execute();

    assertThat(db.countSql(String.format("select count(*) from properties where prop_key = '%s'", SUGGESTION_FEATURE_ENABLED_PROPERTY))).isZero();
    assertThat(db.countSql(String.format("select count(*) from properties where prop_key = '%s'", SUGGESTION_PROVIDER_KEY_PROPERTY))).isZero();
    assertThat(db.countSql(String.format("select count(*) from properties where prop_key = '%s'", SUGGESTION_PROVIDER_MODEL_KEY_PROPERTY))).isZero();
  }

  @Test
  void execute_shouldNotUpdateAnything_whenTheAiCodeFixDisabled() throws SQLException {
    insertProperty(SUGGESTION_FEATURE_ENABLED_PROPERTY, DISABLED);
    underTest.execute();

    assertThat(db.selectFirst(String.format("select text_value from properties where prop_key = '%s'", SUGGESTION_FEATURE_ENABLED_PROPERTY))).containsEntry("text_value", DISABLED);
    assertThat(db.countSql(String.format("select count(*) from properties where prop_key = '%s'", SUGGESTION_PROVIDER_KEY_PROPERTY))).isZero();
    assertThat(db.countSql(String.format("select count(*) from properties where prop_key = '%s'", SUGGESTION_PROVIDER_MODEL_KEY_PROPERTY))).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ENABLED_FOR_ALL_PROJECTS", "ENABLED_FOR_SOME_PROJECTS"})
  void execute_shouldSetDefaultValues_whenTheAiCodeFixEnabled(String enablement) throws SQLException {
    insertProperty(SUGGESTION_FEATURE_ENABLED_PROPERTY, enablement);
    underTest.execute();

    assertThat(db.selectFirst(String.format("select text_value from properties where prop_key = '%s'", SUGGESTION_FEATURE_ENABLED_PROPERTY))).containsEntry("text_value",
      enablement);
    assertThat(db.selectFirst(String.format("select text_value from properties where prop_key = '%s'", SUGGESTION_PROVIDER_KEY_PROPERTY))).containsEntry("text_value",
      DEFAULT_PROVIDER_KEY);
    assertThat(db.selectFirst(String.format("select text_value from properties where prop_key = '%s'", SUGGESTION_PROVIDER_MODEL_KEY_PROPERTY))).containsEntry("text_value",
      DEFAULT_PROVIDER_MODEL_KEY);
  }

  private void insertProperty(String key, String value) {
    db.executeInsert("properties",
      "uuid", "uuid-1",
      "prop_key", key,
      "is_empty", false,
      "text_value", value,
      "created_at", NOW);
  }
}
