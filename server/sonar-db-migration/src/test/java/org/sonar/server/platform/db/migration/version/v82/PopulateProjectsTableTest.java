/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v82;

import java.sql.SQLException;
import java.util.Date;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateProjectsTableTest {

  private static final String TABLE_COMPONENTS = "components";
  private static final String TABLE_PROJECTS = "projects";
  private final static long PAST = 10_000_000_000L;
  private static final long NOW = 50_000_000_000L;
  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateProjectsTableTest.class, "schema.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DataChange underTest = new PopulateProjectsTable(db.database(), system2);

  @Before
  public void setup() {
    insertComponent("uuid-1", "PRJ", "TRK", null);
    insertComponent("uuid-2", "PRJ", "VW", null);
    insertComponent("uuid-3", "PRJ", "SVW", null);
    insertComponent("uuid-4", "PRJ", "APP", null);
    insertComponent("uuid-5", "PRJ", "TRK", null);
    insertComponent("uuid-6", "FIL", "FIL", null);
    insertComponent("uuid-5-branch", "PRJ", "TRK", "uuid-5");
  }

  @Test
  public void migrate() throws SQLException {
    underTest.execute();

    verifyMigrationResult();
  }

  @Test
  public void migration_is_re_entrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    verifyMigrationResult();
  }

  private void verifyMigrationResult() {
    assertThat(db.countRowsOfTable(TABLE_COMPONENTS)).isEqualTo(7);
    assertThat(db.countRowsOfTable(TABLE_PROJECTS)).isEqualTo(3);

    assertThat(db.select("select UUID, KEE, QUALIFIER, ORGANIZATION_UUID, NAME, DESCRIPTION, PRIVATE, TAGS, CREATED_AT, UPDATED_AT from " + TABLE_PROJECTS)
      .stream()
      .map(e -> new Tuple(
        e.get("UUID"),
        e.get("KEE"),
        e.get("QUALIFIER"),
        e.get("ORGANIZATION_UUID"),
        e.get("NAME"),
        e.get("DESCRIPTION"),
        e.get("PRIVATE"),
        e.get("TAGS"),
        e.get("CREATED_AT"),
        e.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(
          new Tuple("uuid-1", "uuid-1-key", "TRK", "default", "uuid-1-name", "uuid-1-description", false, "uuid-1-tags", PAST, 50_000_000_000L),
          new Tuple("uuid-5", "uuid-5-key", "TRK", "default", "uuid-5-name", "uuid-5-description", false, "uuid-5-tags", PAST, 50_000_000_000L),
          new Tuple("uuid-4", "uuid-4-key", "APP", "default", "uuid-4-name", "uuid-4-description", false, "uuid-4-tags", PAST, 50_000_000_000L));
  }

  private void insertComponent(String uuid, String scope, String qualifier, @Nullable String mainBranchProjectUuid) {
    int id = nextInt();
    db.executeInsert("COMPONENTS",
      "ID", id,
      "NAME", uuid + "-name",
      "DESCRIPTION", uuid + "-description",
      "ORGANIZATION_UUID", "default",
      "TAGS", uuid + "-tags",
      "CREATED_AT", new Date(PAST),
      "KEE", uuid + "-key",
      "UUID", uuid,
      "PROJECT_UUID", uuid,
      "MAIN_BRANCH_PROJECT_UUID", mainBranchProjectUuid,
      "UUID_PATH", ".",
      "ROOT_UUID", uuid,
      "PRIVATE", Boolean.toString(false),
      "SCOPE", scope,
      "QUALIFIER", qualifier);
  }

}
