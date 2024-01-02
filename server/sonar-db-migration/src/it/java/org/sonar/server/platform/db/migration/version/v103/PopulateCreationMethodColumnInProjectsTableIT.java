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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v103.AddCreationMethodColumnInProjectsTable.PROJECTS_TABLE_NAME;

public class PopulateCreationMethodColumnInProjectsTableIT {

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateCreationMethodColumnInProjectsTable.class);
  private final PopulateCreationMethodColumnInProjectsTable underTest = new PopulateCreationMethodColumnInProjectsTable(db.database());

  @Test
  public void execute_whenProjectsTableIsEmpty_shouldDoNothing() throws SQLException {
    underTest.execute();

    assertThat(db.select("select creation_method from projects")).isEmpty();
  }

  @Test
  public void execute_whenProjectsExist_shouldPopulateCreationMethodColumn() throws SQLException {
    insertProject("uuid-1");
    insertProject("uuid-2");

    underTest.execute();

    assertThat(db.select("select creation_method from projects"))
      .extracting(stringObjectMap -> stringObjectMap.get("CREATION_METHOD"))
      .containsExactlyInAnyOrder("UNKNOWN", "UNKNOWN");
  }

  @Test
  public void execute_isReentrant() throws SQLException {
    insertProject("uuid-1");

    underTest.execute();
    underTest.execute();

    assertThat(db.select("select creation_method from projects"))
      .extracting(stringObjectMap -> stringObjectMap.get("CREATION_METHOD"))
      .containsExactlyInAnyOrder("UNKNOWN");
  }

  private void insertProject(String uuid) {
    db.executeInsert(PROJECTS_TABLE_NAME,
      "UUID", uuid,
      "KEE", uuid,
      "QUALIFIER", "TRK",
      "PRIVATE", true,
      "UPDATED_AT", 1);
  }
}
