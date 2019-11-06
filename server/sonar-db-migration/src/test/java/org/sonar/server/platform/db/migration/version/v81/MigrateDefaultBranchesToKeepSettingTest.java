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
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrateDefaultBranchesToKeepSettingTest {

  private static final String PROPS_TABLE = "properties";
  private static final String PATTERN_1 = "(branch|release|llb)-.*";
  private static final String PATTERN_2 = "(branch|llb)-.*";
  private static final String PATTERN_3 = "llb-.*";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(MigrateDefaultBranchesToKeepSettingTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MigrateDefaultBranchesToKeepSetting underTest = new MigrateDefaultBranchesToKeepSetting(dbTester.database(), System2.INSTANCE);

  @Test
  public void migrate_overridden_old_setting() throws SQLException {
    setupOverriddenSetting();

    underTest.execute();

    verifyMigrationOfOverriddenSetting();
  }

  @Test
  public void migration_of_overridden_setting_is_re_entrant() throws SQLException {
    setupOverriddenSetting();

    underTest.execute();
    underTest.execute();

    verifyMigrationOfOverriddenSetting();
  }

  @Test
  public void migrate_default_old_setting_on_fresh_install() throws SQLException {
    setupDefaultSetting();

    underTest.execute();

    verifyMigrationOfDefaultSetting("master,develop,trunk");
  }

  @Test
  public void migrate_default_old_setting_on_existing_install() throws SQLException {
    setupDefaultSetting();
    insertProject();

    underTest.execute();

    verifyMigrationOfDefaultSetting("master,develop,trunk,branch-.*,release-.*");
  }

  @Test
  public void migration_of_default_old_setting_is_re_entrant() throws SQLException {
    setupDefaultSetting();

    underTest.execute();
    underTest.execute();

    verifyMigrationOfDefaultSetting("master,develop,trunk");
  }

  private void setupOverriddenSetting() {
    insertProperty(1, "some.key", "some.value", 1001L);
    insertProperty(2, "sonar.branch.longLivedBranches.regex", PATTERN_1, 1001L);
    insertProperty(3, "some.other.key", "some.other.value", 1001L);
    insertProperty(4, "sonar.branch.longLivedBranches.regex", PATTERN_2, 1002L);
    insertProperty(5, "some.other.key", "some.other.value", null);
    insertProperty(6, "sonar.branch.longLivedBranches.regex", PATTERN_3, null);
  }

  private void setupDefaultSetting() {
    insertProperty(1, "some.key", "some.value", 1001L);
    insertProperty(3, "some.other.key", "some.other.value", 1001L);
    insertProperty(5, "some.other.key", "some.other.value", null);
  }

  private void verifyMigrationOfOverriddenSetting() {
    assertThat(dbTester.countRowsOfTable(PROPS_TABLE)).isEqualTo(9);
    assertThat(dbTester.countSql("select count(*) from " + PROPS_TABLE + " where prop_key = 'sonar.branch.longLivedBranches.regex'")).isEqualTo(3);
    assertThat(dbTester.countSql("select count(*) from " + PROPS_TABLE + " where prop_key = 'sonar.dbcleaner.branchesToKeepWhenInactive'")).isEqualTo(3);
    assertThat(dbTester.select("select resource_id, text_value from " + PROPS_TABLE + " where prop_key = 'sonar.dbcleaner.branchesToKeepWhenInactive'")
      .stream()
      .map(e -> new Tuple(e.get("TEXT_VALUE"), e.get("RESOURCE_ID")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(
          new Tuple(PATTERN_1, 1001L),
          new Tuple(PATTERN_2, 1002L),
          new Tuple(PATTERN_3, null));
  }

  private void verifyMigrationOfDefaultSetting(String expectedValue) {
    assertThat(dbTester.countRowsOfTable(PROPS_TABLE)).isEqualTo(4);
    assertThat(dbTester.countSql("select count(*) from " + PROPS_TABLE + " where prop_key = 'sonar.branch.longLivedBranches.regex'")).isEqualTo(0);
    assertThat(dbTester.countSql("select count(*) from " + PROPS_TABLE + " where prop_key = 'sonar.dbcleaner.branchesToKeepWhenInactive'")).isEqualTo(1);
    assertThat(dbTester.select("select resource_id, text_value from " + PROPS_TABLE + " where prop_key = 'sonar.dbcleaner.branchesToKeepWhenInactive'")
      .stream()
      .map(e -> new Tuple(e.get("TEXT_VALUE"), e.get("RESOURCE_ID")))
      .collect(Collectors.toList()))
        .containsExactly(new Tuple(expectedValue, null));
  }

  private void insertProperty(int id, String key, String value, @Nullable Long resourceId) {
    dbTester.executeInsert(
      PROPS_TABLE,
      "ID", id,
      "PROP_KEY", key,
      "RESOURCE_ID", resourceId,
      "USER_ID", null,
      "IS_EMPTY", false,
      "TEXT_VALUE", value,
      "CLOB_VALUE", null,
      "CREATED_AT", System2.INSTANCE.now());
  }

  private void insertProject() {
    String uuid = Uuids.createFast();
    dbTester.executeInsert("PROJECTS",
      "ORGANIZATION_UUID", "default-org",
      "KEE", uuid + "-key",
      "UUID", uuid,
      "PROJECT_UUID", uuid,
      "main_branch_project_uuid", uuid,
      "UUID_PATH", ".",
      "ROOT_UUID", uuid,
      "PRIVATE", "true",
      "qualifier", "TRK",
      "scope", "PRJ");
  }
}
