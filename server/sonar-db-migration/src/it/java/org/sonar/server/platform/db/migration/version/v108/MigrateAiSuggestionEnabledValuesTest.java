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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v108.MigrateAiSuggestionEnabledValues.AI_CODEFIX_ENABLED_PROP_KEY;
import static org.sonar.server.platform.db.migration.version.v108.MigrateAiSuggestionEnabledValues.DISABLED;
import static org.sonar.server.platform.db.migration.version.v108.MigrateAiSuggestionEnabledValues.ENABLED_FOR_ALL_PROJECTS;

class MigrateAiSuggestionEnabledValuesTest {
  @RegisterExtension
  private final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MigrateAiSuggestionEnabledValues.class);
  private final MigrateAiSuggestionEnabledValues underTest = new MigrateAiSuggestionEnabledValues(db.database());

  @BeforeEach
  void init() {
    insertProjects();
  }

  @Test
  void execute_shouldNotUpdateAnything_whenThePropertyDoesNotExists() throws SQLException {
    underTest.execute();

    assertThat(db.countSql(String.format("select count(1) from properties where prop_key = '%s'", AI_CODEFIX_ENABLED_PROP_KEY))).isZero();
    assertThat(db.countSql("select count(1) from projects where ai_code_fix_enabled is true")).isZero();
  }

  @Test
  void execute_shouldUpdatePropertyToDisabled_whenThePropertyIsFalse() throws SQLException {
    addAISuggestionEnabledProperty(false);
    underTest.execute();

    assertThat(db.select(String.format("select text_value from properties where prop_key = '%s'", AI_CODEFIX_ENABLED_PROP_KEY)))
      .extracting(r -> r.get("TEXT_VALUE"))
      .containsExactly(DISABLED);

    assertThat(db.countSql("select count(1) from projects where ai_code_fix_enabled is true")).isZero();
  }

  @Test
  void execute_shouldUpdatePropertyAndProjects_whenThePropertyIsTrue() throws SQLException {
    addAISuggestionEnabledProperty(true);
    underTest.execute();

    assertThat(db.select(String.format("select text_value from properties where prop_key = '%s'", AI_CODEFIX_ENABLED_PROP_KEY)))
      .extracting(r -> r.get("TEXT_VALUE"))
      .containsExactly(ENABLED_FOR_ALL_PROJECTS);

    assertThat(db.countSql("select count(1) from projects where ai_code_fix_enabled is false")).isZero();
  }

  private void insertProjects() {
    db.executeInsert("projects",
      "kee", "proj1",
      "qualifier", "TRK",
      "uuid", "uuid1",
      "private", false,
      "creation_method", "LOCAL_BROWSER",
      "ai_code_fix_enabled", false,
      "created_at", 1L,
      "updated_at", 1L);
    db.executeInsert("projects",
      "kee", "proj2",
      "qualifier", "TRK",
      "uuid", "uuid2",
      "private", false,
      "creation_method", "LOCAL_BROWSER",
      "ai_code_fix_enabled", false,
      "created_at", 1L,
      "updated_at", 1L);
  }

  private void addAISuggestionEnabledProperty(boolean enabled) {
    db.executeInsert("properties",
      "prop_key", AI_CODEFIX_ENABLED_PROP_KEY,
      "is_empty", "false",
      "uuid", "uuid2",
      "text_value", enabled,
      "created_at", 1L);
  }

}
