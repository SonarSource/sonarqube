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
package org.sonar.server.platform.db.migration.version.v80;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateNewCodePeriodTableTest {
  private static final String NEW_CODE_PERIODS_TABLE_NAME = "new_code_periods";
  private static final String PROJECT_BRANCHES_TABLE_NAME = "project_branches";
  private static final int NUMBER_OF_PROJECT_BRANCHES_TO_INSERT = 10;

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateNewCodePeriodTableTest.class, "schema.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PopulateNewCodePeriodTable underTest = new PopulateNewCodePeriodTable(dbTester.database(), UuidFactoryImpl.INSTANCE, System2.INSTANCE);

  @Test
  public void copy_manual_baseline_analysis_to_new_code_period_table() throws SQLException {
    for (long i = 0; i < NUMBER_OF_PROJECT_BRANCHES_TO_INSERT; i++) {
      insertMainBranch(i, true);
    }

    underTest.execute();
    assertThat(dbTester.countRowsOfTable(NEW_CODE_PERIODS_TABLE_NAME)).isEqualTo(10);

    for (int i = 0; i < NUMBER_OF_PROJECT_BRANCHES_TO_INSERT; i++) {
      assertNewCodePeriod(i, "pb-uuid-" + i, "pb-uuid-" + i, "SPECIFIC_ANALYSIS", "mba-uuid" + i);
    }

    //should not fail if executed twice
    underTest.execute();
  }

  @Test
  public void do_nothing_if_cant_find_matching_analysis() throws SQLException {
    insertMainBranch(0, false);
    insertMainBranch(1, false);

    insertProperty(0, "2.0");
    insertProperty(0, "2019-04-05");

    underTest.execute();
    assertThat(dbTester.countRowsOfTable(NEW_CODE_PERIODS_TABLE_NAME)).isEqualTo(0);
  }

  @Test
  public void do_nothing_if_cant_migrate_global_property() throws SQLException {
    insertProperty(null, "2.0");

    underTest.execute();
    assertThat(dbTester.countRowsOfTable(NEW_CODE_PERIODS_TABLE_NAME)).isEqualTo(0);
  }

  @Test
  public void migrate_project_property_set_to_previous_version() throws SQLException {
    insertMainBranch(0, true);
    insertMainBranch(1, false);
    // no property defined for it
    insertMainBranch(2, false);

    // doesn't get copied since there is a manual baseline taking precedence
    insertProperty(0, "20");
    insertProperty(1, "previous_version");
    // doesn't exist
    insertProperty(3, "previous_version");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(NEW_CODE_PERIODS_TABLE_NAME)).isEqualTo(2);
    assertNewCodePeriod(0, "pb-uuid-" + 0, "pb-uuid-" + 0, "SPECIFIC_ANALYSIS", "mba-uuid" + 0);
    assertNewCodePeriod(1, "pb-uuid-" + 1, null, "PREVIOUS_VERSION", null);
  }

  @Test
  public void migrate_project_property_set_to_number_of_days() throws SQLException {
    insertMainBranch(0, false);
    insertBranch(0, 1, false);
    insertProperty(1, "20");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(NEW_CODE_PERIODS_TABLE_NAME)).isEqualTo(1);
    assertNewCodePeriod(0, "pb-uuid-" + 0, "pb-uuid-" + 1, "NUMBER_OF_DAYS", "20");
  }

  @Test
  public void migrate_branch_property_set_to_number_of_days() throws SQLException {
    insertMainBranch(0, false);
    insertProperty(0, "20");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(NEW_CODE_PERIODS_TABLE_NAME)).isEqualTo(1);
    assertNewCodePeriod(0, "pb-uuid-" + 0, null, "NUMBER_OF_DAYS", "20");
  }

  @Test
  public void migrate_global_property_set_to_number_of_days() throws SQLException {
    insertProperty(null, "20");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(NEW_CODE_PERIODS_TABLE_NAME)).isEqualTo(1);
    assertNewCodePeriod(0, null, null, "NUMBER_OF_DAYS", "20");
  }


  @Test
  public void migrate_project_property_set_to_version() throws SQLException {
    insertMainBranch(0, false);
    insertProperty(0, "2.0");

    insertVersionEvent(0, 10L, "1.0");
    insertVersionEvent(0, 20L, "2.0");
    insertVersionEvent(0, 30L, "3.0");
    insertVersionEvent(0, 40L, "2.0");
    insertVersionEvent(0, 50L, "3.0");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(NEW_CODE_PERIODS_TABLE_NAME)).isEqualTo(1);
    // we don't support specific analysis for projects, so we set it for the branch
    assertNewCodePeriod(0, "pb-uuid-" + 0, "pb-uuid-" + 0, "SPECIFIC_ANALYSIS", "analysis-40");
  }

  @Test
  public void migrate_project_property_set_to_date() throws SQLException {
    insertMainBranch(0, false);
    insertProperty(0, "2019-04-05");

    long reference = LocalDate.parse("2019-04-05").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    insertSnapshot(100, 0, reference - 100);
    insertSnapshot(200, 0, reference + 100);
    insertSnapshot(300, 0, reference + 200);

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(NEW_CODE_PERIODS_TABLE_NAME)).isEqualTo(1);
    // we don't support specific analysis for projects, so we set it for the branch
    assertNewCodePeriod(0, "pb-uuid-" + 0, "pb-uuid-" + 0, "SPECIFIC_ANALYSIS", "snapshot-200");
  }

  private void assertNewCodePeriod(int row, @Nullable String projectUuid, @Nullable String branchUuid, String type, @Nullable String value) {
    Optional<Map<String, Object>> r = dbTester.select("SELECT PROJECT_UUID, BRANCH_UUID, TYPE, VALUE FROM " + NEW_CODE_PERIODS_TABLE_NAME)
      .stream()
      .skip(row)
      .findFirst();

    assertThat(r).isPresent();

    assertThat(r.get().get("PROJECT_UUID")).isEqualTo(projectUuid);
    assertThat(r.get().get("BRANCH_UUID")).isEqualTo(branchUuid);
    assertThat(r.get().get("TYPE")).isEqualTo(type);
    assertThat(r.get().get("VALUE")).isEqualTo(value);
  }

  private void insertBranch(long mainBranchUid, long uid, boolean withBaseLine) {
    String manualBaseline = withBaseLine ? "mba-uuid" + uid : null;

    String mainBranchProjectUuid = mainBranchUid == uid ? null : "pb-uuid-" + mainBranchUid;
    insertComponent(uid, "pb-uuid-" + uid, mainBranchProjectUuid);

    dbTester.executeInsert(
      PROJECT_BRANCHES_TABLE_NAME,
      "UUID", "pb-uuid-" + uid,
      "PROJECT_UUID", "pb-uuid-" + mainBranchUid,
      "KEE", "pb-key-" + uid,
      "KEY_TYPE", "TSR",
      "BRANCH_TYPE", "BRANCH",
      "MERGE_BRANCH_UUID", "mb-uuid-" + mainBranchUid,
      "MANUAL_BASELINE_ANALYSIS_UUID", manualBaseline,
      "CREATED_AT", System2.INSTANCE.now(),
      "UPDATED_AT", System2.INSTANCE.now()
    );
  }

  private void insertSnapshot(int uid, int branchUid, long creationDate) {
    dbTester.executeInsert(
      "SNAPSHOTS",
      "UUID", "snapshot-" + uid,
      "COMPONENT_UUID", "pb-uuid-" + branchUid,
      "STATUS", "P",
      "CREATED_AT", creationDate
    );
  }

  private void insertProperty(@Nullable Integer uid, String value) {
    dbTester.executeInsert(
      "PROPERTIES",
      "PROP_KEY", "sonar.leak.period",
      "RESOURCE_ID", uid,
      "USER_ID", null,
      "IS_EMPTY", false,
      "TEXT_VALUE", value,
      "CLOB_VALUE", null,
      "CREATED_AT", System2.INSTANCE.now()
    );
  }

  private void insertComponent(long id, String uuid, @Nullable String mainBranchProjectUuid) {
    dbTester.executeInsert(
      "projects",
      "ID", valueOf(id),
      "UUID", uuid,
      "ROOT_UUID", uuid,
      "PROJECT_UUID", uuid,
      "MAIN_BRANCH_PROJECT_UUID", mainBranchProjectUuid,
      "SCOPE", Scopes.PROJECT,
      "QUALIFIER", "TRK",
      "NAME", "name-" + id);
  }

  private void insertVersionEvent(long id, long createdAt, String version) {
    dbTester.executeInsert(
      "events",
      "ANALYSIS_UUID", "analysis-" + createdAt,
      "NAME", version,
      "COMPONENT_UUID", "pb-uuid-" + id,
      "CATEGORY", "Version",
      "CREATED_AT", createdAt);
  }

  private void insertMainBranch(long uid, boolean withBaseLine) {
    insertBranch(uid, uid, withBaseLine);
  }
}
