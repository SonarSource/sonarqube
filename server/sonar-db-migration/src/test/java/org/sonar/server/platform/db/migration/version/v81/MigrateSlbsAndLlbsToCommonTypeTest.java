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
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrateSlbsAndLlbsToCommonTypeTest {

  private static final String BRANCHES_TABLE = "project_branches";
  private static final String MAIN_BRANCH_1 = "825644c3-06b2-44b5-833e-13169a5379ad";
  private static final String MAIN_BRANCH_2 = "47a53ace-0297-4a4e-bcab-07a65e3eeac4";
  private static final String PR_1 = "ad85f2ee-7271-407a-b9f7-b0a80343e001";
  private static final String LLB_1 = "08905714-2bfd-48a6-b19d-7801bc4d1ca4";
  private static final String SLB_1 = "a2020607-4134-45f9-87ea-2cdaa84bf386";
  private static final String PR_2 = "834eba05-e4f7-4214-bfec-c7823e101487";
  private static final String LLB_2 = "fea036fe-e830-4d87-85ee-7aaa21729019";
  private static final String SLB_2 = "20166755-953e-4b8e-8c1d-67d812e42418";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(MigrateSlbsAndLlbsToCommonTypeTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MigrateSlbsAndLlbsToCommonType underTest = new MigrateSlbsAndLlbsToCommonType(dbTester.database(), System2.INSTANCE);

  @Before
  public void setup() {
    insertMainBranch(MAIN_BRANCH_1);
    insertBranch(MAIN_BRANCH_1, PR_1, "PULL_REQUEST");
    insertBranch(MAIN_BRANCH_1, LLB_1, "LONG");
    insertBranch(MAIN_BRANCH_1, SLB_1, "SHORT");

    insertMainBranch(MAIN_BRANCH_2);
    insertBranch(MAIN_BRANCH_1, PR_2, "PULL_REQUEST");
    insertBranch(MAIN_BRANCH_1, LLB_2, "LONG");
    insertBranch(MAIN_BRANCH_1, SLB_2, "SHORT");
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
    assertThat(dbTester.countRowsOfTable(BRANCHES_TABLE)).isEqualTo(8);
    assertThat(dbTester.countSql("select count(*) from project_branches where branch_type = 'LONG' or branch_type = 'SHORT'")).isEqualTo(0);
    assertThat(dbTester.select("select uuid from " + BRANCHES_TABLE + " where branch_type = 'BRANCH'")
      .stream()
      .map(e -> e.get("UUID"))
      .collect(Collectors.toSet())).containsExactlyInAnyOrder(MAIN_BRANCH_1, MAIN_BRANCH_2, SLB_1, SLB_2, LLB_1, LLB_2);
    assertThat(dbTester.select("select uuid from " + BRANCHES_TABLE + " where branch_type = 'PULL_REQUEST'")
      .stream()
      .map(e -> e.get("UUID"))
      .collect(Collectors.toSet())).containsExactlyInAnyOrder(PR_1, PR_2);
  }

  private void insertBranch(String mainBranchUuid, String uuid, String type) {
    dbTester.executeInsert(
      BRANCHES_TABLE,
      "UUID", uuid,
      "PROJECT_UUID", mainBranchUuid,
      "KEE", "pb-key-" + uuid,
      "KEY_TYPE", "TSR",
      "BRANCH_TYPE", type,
      "MERGE_BRANCH_UUID", "mb-uuid-" + mainBranchUuid,
      "MANUAL_BASELINE_ANALYSIS_UUID", null,
      "CREATED_AT", System2.INSTANCE.now(),
      "UPDATED_AT", System2.INSTANCE.now());
  }

  private void insertMainBranch(String mainBranchUuid) {
    insertBranch(mainBranchUuid, mainBranchUuid, "LONG");
  }
}
