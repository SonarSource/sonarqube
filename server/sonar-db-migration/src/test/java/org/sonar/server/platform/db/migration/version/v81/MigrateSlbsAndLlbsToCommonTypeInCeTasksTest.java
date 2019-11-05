/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v81;

import java.sql.SQLException;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v81.MigrateSlbsAndLlbsToCommonTypeInCeTasks.TABLE;

public class MigrateSlbsAndLlbsToCommonTypeInCeTasksTest {

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(MigrateSlbsAndLlbsToCommonTypeInCeTasksTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MigrateSlbsAndLlbsToCommonTypeInCeTasks underTest = new MigrateSlbsAndLlbsToCommonTypeInCeTasks(dbTester.database());

  @Before
  public void setup() {
    insertCeTaskCharacteristic("char-uuid-1", "task-uuid-1", "branchType", "LONG");
    insertCeTaskCharacteristic("char-uuid-2", "task-uuid-1", "branch", "foobar");
    insertCeTaskCharacteristic("char-uuid-3", "task-uuid-1", "someKey", "someValue");

    insertCeTaskCharacteristic("char-uuid-21", "task-uuid-2", "branchType", "SHORT");
    insertCeTaskCharacteristic("char-uuid-22", "task-uuid-2", "branch", "feature/1");
    insertCeTaskCharacteristic("char-uuid-23", "task-uuid-2", "someKey", "anotherValue");

    insertCeTaskCharacteristic("char-uuid-31", "task-uuid-3", "pullRequest", "1");
    insertCeTaskCharacteristic("char-uuid-32", "task-uuid-3", "someKey", "yetAnotherValue");

    assertThat(dbTester.countRowsOfTable(TABLE)).isEqualTo(8);
  }

  @Test
  public void execute() throws SQLException {
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
    assertThat(dbTester.countRowsOfTable(TABLE)).isEqualTo(8);
    assertThat(dbTester.countSql("select count(*) from " + TABLE + " where kee = 'branchType' and text_value in ('LONG', 'SHORT')")).isEqualTo(0);
    assertThat(dbTester.select("select uuid from " + TABLE + " where kee = 'branchType' and text_value = 'BRANCH'")
      .stream()
      .map(e -> e.get("UUID"))
      .collect(Collectors.toSet())).containsExactlyInAnyOrder("char-uuid-1", "char-uuid-21");
  }

  private void insertCeTaskCharacteristic(String uuid, String taskUuid, String key, String value) {
    dbTester.executeInsert(
      TABLE,
      "UUID", uuid,
      "TASK_UUID", taskUuid,
      "KEE", key,
      "TEXT_VALUE", value);
  }
}
