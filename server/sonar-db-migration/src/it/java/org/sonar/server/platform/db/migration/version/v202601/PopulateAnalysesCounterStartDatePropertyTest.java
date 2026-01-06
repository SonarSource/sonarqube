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
package org.sonar.server.platform.db.migration.version.v202601;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

class PopulateAnalysesCounterStartDatePropertyTest {

  private static final String TARGET_PROPERTY = "analyses.counter.date";
  private static final long NOW = 1_000_000_000L;

  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateAnalysesCounterStartDateProperty.class);

  private final DataChange underTest = new PopulateAnalysesCounterStartDateProperty(db.database(), system2);

  @Test
  void execute_shouldInitializeProperty() throws SQLException {
    underTest.execute();

    assertThat(db.select("select text_value from internal_properties where kee = '" + TARGET_PROPERTY + "'"))
      .hasSize(1)
      .extracting(m -> m.get("text_value"))
      .containsExactly(String.valueOf(NOW));
  }

  @Test
  void execute_shouldKeepExistingValue() throws SQLException {
    db.executeInsert("internal_properties",
      "kee", TARGET_PROPERTY,
      "is_empty", false,
      "text_value", "12",
      "created_at", NOW);

    underTest.execute();

    assertThat(db.select("select text_value from internal_properties where kee = '" + TARGET_PROPERTY + "'"))
      .hasSize(1)
      .extracting(m -> m.get("text_value"))
      .containsExactly("12");
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    assertThat(db.select("select text_value from internal_properties where kee = '" + TARGET_PROPERTY + "'"))
      .hasSize(1);
  }
}
