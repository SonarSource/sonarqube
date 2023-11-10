/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v100;

import java.sql.SQLException;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.platform.db.migration.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

public class DropSonarScimEnabledPropertyTest {

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DropSonarScimEnabledProperty.class);
  private final DataChange underTest = new DropSonarScimEnabledProperty(db.database());

  @Test
  public void migration_should_remove_scim_property() throws SQLException {
    insertScimProperty(db);

    underTest.execute();

    Assertions.assertThat(db.select("select * from properties")).isEmpty();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertScimProperty(db);

    underTest.execute();
    underTest.execute();

    Assertions.assertThat(db.select("select * from properties")).isEmpty();
  }

  private void insertScimProperty(MigrationDbTester db) {
    db.executeInsert("properties ",
      "prop_key", "sonar.scim.enabled",
      "is_empty", false,
      "text_value", "true",
      "created_at", 100_000L,
      "uuid", "some-random-uuid"
    );
  }
}
