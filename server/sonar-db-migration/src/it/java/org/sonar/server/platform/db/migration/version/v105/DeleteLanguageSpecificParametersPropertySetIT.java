/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v105;

import java.sql.SQLException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

class DeleteLanguageSpecificParametersPropertySetIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DeleteLanguageSpecificParametersPropertySet.class);
  private final DataChange underTest = new DeleteLanguageSpecificParametersPropertySet(db.database());

  @Test
  void migration_should_remove_root_property_and_children() throws SQLException {
    insertLanguageSpecificParametersPropertySet(db);

    underTest.execute();

    Assertions.assertThat(db.select("select * from properties")).isEmpty();
  }

  @Test
  void migration_is_reentrant() throws SQLException {
    insertLanguageSpecificParametersPropertySet(db);

    underTest.execute();
    underTest.execute();

    Assertions.assertThat(db.select("select * from properties")).isEmpty();
  }

  private void insertLanguageSpecificParametersPropertySet(MigrationDbTester db) {
    db.executeInsert("properties ",
      "prop_key", "languageSpecificParameters",
      "is_empty", false,
      "text_value", "0,1",
      "created_at", 100_000L,
      "uuid", "some-random-uuid1");
    db.executeInsert("properties ",
      "prop_key", "languageSpecificParameters.0.language",
      "is_empty", false,
      "text_value", "java",
      "created_at", 100_000L,
      "uuid", "some-random-uuid2");
    db.executeInsert("properties ",
      "prop_key", "languageSpecificParameters.0.man_days",
      "is_empty", false,
      "text_value", "10",
      "created_at", 100_000L,
      "uuid", "some-random-uuid3");
    db.executeInsert("properties ",
      "prop_key", "languageSpecificParameters.1.language",
      "is_empty", false,
      "text_value", "php",
      "created_at", 100_000L,
      "uuid", "some-random-uuid4");
    db.executeInsert("properties ",
      "prop_key", "languageSpecificParameters.1.man_days",
      "is_empty", false,
      "text_value", "20",
      "created_at", 100_000L,
      "uuid", "some-random-uuid5");
  }
}
