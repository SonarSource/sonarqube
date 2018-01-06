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
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class AssociateQualityGatesToDefaultOrganizationTest {
  private static final long PAST = 10_000_000_000L;
  private static final String TABLE_ORGANIZATIONS = "organizations";
  private static final String TABLE_QUALITY_GATES = "quality_gates";
  private static final String TABLE_INTERNAL_PROPERTIES = "internal_properties";
  private static final String DEFAULT_ORGANIZATION_KEE = "organization.default";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AssociateQualityGatesToDefaultOrganizationTest.class, "org_quality_gates.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private AssociateQualityGatesToDefaultOrganization underTest = new AssociateQualityGatesToDefaultOrganization(db.database(), uuidFactory);

  @Test
  public void should_throw_ISE_if_no_default_organization() throws SQLException {
    insertOrganization(uuidFactory.create());
    insertOrganization(uuidFactory.create());
    insertQualityGate(uuidFactory.create(), "QualityGate1", false);
    insertQualityGate(uuidFactory.create(), "QualityGate2", true);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default organization uuid is missing");

    underTest.execute();
  }

  @Test
  public void should_associate_all_quality_gates_to_default_organization() throws SQLException {
    String defaultOrgUuid = uuidFactory.create();
    String anotherOrgUuid = uuidFactory.create();
    insertOrganization(defaultOrgUuid);
    insertOrganization(anotherOrgUuid);

    String qgUuid1 = uuidFactory.create();
    String qgUuid2 = uuidFactory.create();
    String qgUuid3 = uuidFactory.create();
    insertQualityGate(qgUuid1, "QualityGate1", false);
    insertQualityGate(qgUuid2, "QualityGate2", false);
    insertQualityGate(qgUuid3, "QualityGate3", true);

    insertDefaultOrgProperty(defaultOrgUuid);

    underTest.execute();

    assertThat(selectAllOrgQualityGates())
      .extracting(map -> map.get("ORGANIZATION_UUID"), map -> map.get("QUALITY_GATE_UUID"))
      .containsExactlyInAnyOrder(
        tuple(defaultOrgUuid, qgUuid1),
        tuple(defaultOrgUuid, qgUuid2)
      );
  }

  @Test
  public void is_reentrant() throws SQLException {
    String orgUuid1 = uuidFactory.create();
    String orgUuid2 = uuidFactory.create();
    insertOrganization(orgUuid1);
    insertOrganization(orgUuid2);

    String qgUuid1 = uuidFactory.create();
    String qgUuid2 = uuidFactory.create();
    String qgUuid3 = uuidFactory.create();
    insertQualityGate(qgUuid1, "QualityGate1", false);
    insertQualityGate(qgUuid2, "QualityGate2", false);
    insertQualityGate(qgUuid3, "QualityGate3", true);

    insertDefaultOrgProperty(orgUuid1);

    underTest.execute();
    underTest.execute();

    assertThat(selectAllOrgQualityGates())
      .extracting(map -> map.get("ORGANIZATION_UUID"), map -> map.get("QUALITY_GATE_UUID"))
      .containsExactlyInAnyOrder(
        tuple(orgUuid1, qgUuid1),
        tuple(orgUuid1, qgUuid2)
      );
  }

  private List<Map<String, Object>> selectAllOrgQualityGates() {
    return db.select("select organization_uuid, quality_gate_uuid from org_quality_gates");
  }

  private void insertDefaultOrgProperty(String uuid) {
    db.executeInsert(
      TABLE_INTERNAL_PROPERTIES,
      "KEE", DEFAULT_ORGANIZATION_KEE,
      "IS_EMPTY", String.valueOf(false),
      "TEXT_VALUE", uuid,
      "CREATED_AT", PAST);
  }

  private void insertOrganization(String uuid) {
    db.executeInsert(
      TABLE_ORGANIZATIONS,
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "GUARDED", String.valueOf(false),
      "NEW_PROJECT_PRIVATE", String.valueOf(true),
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
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
