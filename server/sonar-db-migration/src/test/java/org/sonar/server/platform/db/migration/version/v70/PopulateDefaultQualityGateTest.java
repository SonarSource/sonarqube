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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateDefaultQualityGateTest {
  private final static long PAST = 10_000_000_000L;
  private static final long NOW = 50_000_000_000L;
  private static final String TABLE_ORGANIZATIONS = "organizations";
  private static final String TABLE_QUALITY_GATES = "quality_gates";

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateDefaultQualityGateTest.class, "organizations.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PopulateDefaultQualityGate underTest = new PopulateDefaultQualityGate(db.database(), system2);


  @Test
  public void has_no_effect_if_table_is_empty() throws SQLException {
    insertQualityGate(Uuids.createFast(), "Sonar way", true);

    underTest.execute();

    assertThat(db.countRowsOfTable("organizations")).isEqualTo(0);
  }

  @Test
  public void should_populate_defaultQualityGate_column() throws SQLException {
    String builtInQGUuid = Uuids.createFast();
    insertQualityGate(builtInQGUuid, "Sonar way", true);
    String orgUuid1 = Uuids.createFast();
    String orgUuid2 = Uuids.createFast();
    insertOrganization(orgUuid1);
    insertOrganization(orgUuid2);

    underTest.execute();

    // all organizations have the builtIn quality gate
    assertThat(
      db.countSql("select count(uuid) from organizations where default_quality_gate_uuid != '" + builtInQGUuid + "'")
    ).isEqualTo(0);
    assertThat(
      db.countSql("select count(uuid) from organizations where default_quality_gate_uuid = '" + builtInQGUuid + "'")
    ).isEqualTo(2);

    // updated_at must have been updated
    assertThat(
      db.countSql("select count(uuid) from organizations where updated_at = " + NOW )
    ).isEqualTo(2);
  }

  @Test
  public void is_reentrant() throws SQLException {
    String builtInQGUuid = Uuids.createFast();
    insertQualityGate(builtInQGUuid, "Sonar way", true);
    insertOrganization(Uuids.createFast());
    insertOrganization(Uuids.createFast());

    underTest.execute();
    underTest.execute();

    assertThat(
      db.countSql("select count(uuid) from organizations where default_quality_gate_uuid != '" + builtInQGUuid + "'")
    ).isEqualTo(0);

    assertThat(
      db.countSql("select count(uuid) from organizations where default_quality_gate_uuid = '" + builtInQGUuid + "'")
    ).isEqualTo(2);
  }

  @Test
  public void should_fail_with_ISE_when_no_builtIn() throws SQLException {
    insertOrganization(Uuids.createFast());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Unable to find the builtin quality gate");

    underTest.execute();
  }

  @Test
  public void should_fail_if_there_is_multiple_builtin_qualitygates() throws SQLException {
    insertQualityGate(Uuids.createFast(), "Sonar way", true);
    insertQualityGate(Uuids.createFast(), "Sonar way2", true);
    insertOrganization(Uuids.createFast());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("There are too many built in quality gates, one and only one is expected");

    underTest.execute();
  }

  private void insertOrganization(String uuid) {
    db.executeInsert(
      TABLE_ORGANIZATIONS,
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "GUARDED", String.valueOf(false),
      "NEW_PROJECT_PRIVATE", String.valueOf(true),
      "CREATED_AT", "1000",
      "UPDATED_AT", "1000");
  }

  private void insertQualityGate(String uuid, String name, Boolean builtIn) {
    db.executeInsert(
      TABLE_QUALITY_GATES,
      "UUID", uuid,
      "NAME", name,
      "IS_BUILT_IN", builtIn.toString(),
      "CREATED_AT", new Date(PAST),
      "UPDATED_AT", new Date(PAST));
  }
}
