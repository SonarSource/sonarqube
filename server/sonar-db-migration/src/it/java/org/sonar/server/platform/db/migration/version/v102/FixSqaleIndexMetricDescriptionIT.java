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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class FixSqaleIndexMetricDescriptionIT {

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(FixSqaleIndexMetricDescription.class);
  private final FixSqaleIndexMetricDescription underTest = new FixSqaleIndexMetricDescription(db.database());
  private final String OLD_DESCRIPTION = "Total effort (in hours) to fix all the issues on the component and therefore to comply to all the requirements.";
  private final String NEW_DESCRIPTION = "Total effort (in minutes) to fix all the issues on the component and therefore to comply to all the requirements.";

  @Before
  public void setUp() {
    db.executeInsert("metrics",
      "uuid", "uuid",
      "name", "sqale_index",
      "description", OLD_DESCRIPTION);
  }

  @Test
  public void execute_whenExecuted_shouldUpdateSqaleIndexDescription() throws SQLException {
    assertThat(select()).isEqualTo(OLD_DESCRIPTION);
    underTest.execute();
    assertThat(select()).isEqualTo(NEW_DESCRIPTION);
  }

  @Test
  public void execute_WhenExecutedTwice_shouldBeReentrant() throws SQLException {
    assertThat(select()).isEqualTo(OLD_DESCRIPTION);
    underTest.execute();
    underTest.execute();
    assertThat(select()).isEqualTo(NEW_DESCRIPTION);
  }

  private String select() {
    return (String) db.selectFirst("SELECT description FROM metrics WHERE name = 'sqale_index'").get("description");
  }

}
