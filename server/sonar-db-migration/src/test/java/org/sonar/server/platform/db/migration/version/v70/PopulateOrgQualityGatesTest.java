/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class PopulateOrgQualityGatesTest {

  private static final long PAST = 10_000_000_000L;
  private static final String TABLE_ORGANIZATIONS = "organizations";
  private static final String TABLE_QUALITY_GATES = "quality_gates";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateOrgQualityGates.class, "org_quality_gates.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PopulateOrgQualityGates underTest = new PopulateOrgQualityGates(db.database(), UuidFactoryFast.getInstance());

  @Test
  public void has_no_effect_if_table_is_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("org_quality_gates")).isEqualTo(0);
  }

  @Test
  public void should_associate_builtin_to_all_organizations() throws SQLException {
    String orgUuid1 = Uuids.createFast();
    String orgUuid2 = Uuids.createFast();
    String qgUuid = Uuids.createFast();
    insertOrganization(orgUuid1);
    insertOrganization(orgUuid2);
    insertQualityGate(qgUuid, "Sonar way", true);

    underTest.execute();

    assertThat(selectAllOrgQualityGates())
      .extracting(map -> map.get("ORGANIZATION_UUID"), map -> map.get("QUALITY_GATE_UUID"))
      .containsExactlyInAnyOrder(
        tuple(orgUuid1, qgUuid),
        tuple(orgUuid2, qgUuid));
  }

  @Test
  public void is_reentrant() throws SQLException {
    String orgUuid1 = Uuids.createFast();
    String orgUuid2 = Uuids.createFast();
    String qgUuid = Uuids.createFast();
    insertOrganization(orgUuid1);
    insertOrganization(orgUuid2);
    insertQualityGate(qgUuid, "Sonar way", true);

    underTest.execute();
    underTest.execute();

    assertThat(selectAllOrgQualityGates())
      .extracting(map -> map.get("ORGANIZATION_UUID"), map -> map.get("QUALITY_GATE_UUID"))
      .containsExactlyInAnyOrder(
        tuple(orgUuid1, qgUuid),
        tuple(orgUuid2, qgUuid));
  }

  @Test
  public void should_fail_with_ISE_when_no_builtIn() throws SQLException {
    insertOrganization(Uuids.createFast());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Unable to find the builtin quality gate");

    underTest.execute();
  }

  @Test
  public void should_fail_if_there_are_two_builtin_qg() throws SQLException {
    String orgUuid1 = Uuids.createFast();
    String orgUuid2 = Uuids.createFast();
    String qgUuid1 = Uuids.createFast();
    String qgUuid2 = Uuids.createFast();
    insertOrganization(orgUuid1);
    insertOrganization(orgUuid2);
    insertQualityGate(qgUuid1, "Sonar way", true);
    insertQualityGate(qgUuid2, "Sonar way 2", true);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("There are too many built in quality gates, one and only one is expected");

    underTest.execute();
  }

  private List<Map<String, Object>> selectAllOrgQualityGates() {
    return db.select("select organization_uuid, quality_gate_uuid from org_quality_gates");
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
