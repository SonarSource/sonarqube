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
package org.sonar.server.platform.db.migration.version.v202501;

import java.sql.SQLException;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

class MigrateConfidentialHeaderPropertyTest {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MigrateConfidentialHeaderProperty.class);
  private final DataChange underTest = new MigrateConfidentialHeaderProperty(db.database());

  private static final String NEW_PROPERTY_NAME = "sonar.pdf.confidential.header.enabled";
  private static final String OLD_PROPERTY_NAME = "sonar.portfolios.confidential.header";

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void execute_shouldCreateNewPropertyWithCorrectValue(Boolean propertyValue) throws SQLException {
    createConfidentialPortfolioHeaderProperty(db, propertyValue);

    underTest.execute();

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + NEW_PROPERTY_NAME + "'"))
      .hasSize(1)
      .containsExactly(Map.of("text_value", propertyValue.toString()));

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + OLD_PROPERTY_NAME + "'"))
      .isEmpty();
  }

  @Test
  void execute_whenSourcePropertyDoesntExist_shouldNotCreateNewProperty() throws SQLException {

    underTest.execute();

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + OLD_PROPERTY_NAME + "'"))
      .isEmpty();
    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + NEW_PROPERTY_NAME + "'"))
      .isEmpty();
  }

  @Test
  void execute_whenReentrant_shouldNotFail() throws SQLException {
    createConfidentialPortfolioHeaderProperty(db, true);

    underTest.execute();
    underTest.execute();

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + NEW_PROPERTY_NAME + "'"))
      .hasSize(1)
      .containsExactly(Map.of("text_value", "true"));

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + OLD_PROPERTY_NAME + "'"))
      .isEmpty();
  }

  private void createConfidentialPortfolioHeaderProperty(MigrationDbTester db, Boolean value) {
    db.executeInsert("properties ",
      "prop_key", "sonar.portfolios.confidential.header",
      "is_empty", false,
      "text_value", value.toString(),
      "created_at", 100_000L,
      "uuid", "some-random-uuid1");
  }

}
