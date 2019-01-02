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
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ReadGlobalSonarQualityGateSettingToDefaultOrgTest {

  private static final long PAST = 10_000_000_000L;
  private static final long NOW = 20_000_000_000L;

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(ReadGlobalSonarQualityGateSettingToDefaultOrgTest.class, "initial.sql");

  private DataChange underTest = new ReadGlobalSonarQualityGateSettingToDefaultOrg(db.database(), system2);

  @Test
  public void read_sonar_quality_gate_setting_and_update_default_organization() throws SQLException {
    String defaultQualityGate = insertQualityGate();
    String otherQualityGate = insertQualityGate();
    String defaultOrganization = insertOrganization(otherQualityGate);
    String otherOrganization = insertOrganization(otherQualityGate);
    insertDefaultOrgProperty(defaultOrganization);
    insertSetting("sonar.qualitygate", selectQualityGateId(defaultQualityGate));

    underTest.execute();

    assertDefaultQualityGate(defaultOrganization, tuple(defaultQualityGate, NOW));
  }

  @Test
  public void does_nothing_when_no_default_quality_gate_setting() throws Exception {
    String defaultQualityGate = insertQualityGate();
    String defaultOrganization = insertOrganization(defaultQualityGate);
    insertDefaultOrgProperty(defaultOrganization);
    insertQualityGate();

    underTest.execute();

    assertDefaultQualityGate(defaultOrganization, tuple(defaultQualityGate, PAST));
  }

  @Test
  public void migration_is_reentrant() throws Exception {
    String defaultOrganization = insertOrganization(null);
    insertDefaultOrgProperty(defaultOrganization);
    String qualityGate = insertQualityGate();
    insertSetting("sonar.qualitygate", selectQualityGateId(qualityGate));

    underTest.execute();
    assertDefaultQualityGate(defaultOrganization, tuple(qualityGate, NOW));

    underTest.execute();
    assertDefaultQualityGate(defaultOrganization, tuple(qualityGate, NOW));
  }

  @Test
  public void fail_when_no_default_organization() throws Exception {
    String qualityGate = insertQualityGate();
    insertSetting("sonar.qualitygate", selectQualityGateId(qualityGate));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default organization uuid is missing");

    underTest.execute();
  }

  private void assertDefaultQualityGate(String uuid, Tuple... expectedTuples) {
    assertThat(db.select(String.format("SELECT DEFAULT_QUALITY_GATE_UUID, UPDATED_AT FROM ORGANIZATIONS WHERE UUID='%s'", uuid))
      .stream()
      .map(map -> new Tuple(map.get("DEFAULT_QUALITY_GATE_UUID"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private long selectQualityGateId(String uuid){
    return ((Long) db.selectFirst("select id as \"ID\" from quality_gates where uuid='" + uuid + "'").get("ID")).intValue();
  }

  private String insertOrganization(@Nullable String defaultQualityGateUuid) {
    String uuid = Uuids.createFast();
    db.executeInsert(
      "organizations",
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "GUARDED", String.valueOf(false),
      "NEW_PROJECT_PRIVATE", String.valueOf(true),
      "DEFAULT_QUALITY_GATE_UUID", defaultQualityGateUuid,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
    return uuid;
  }

  private void insertDefaultOrgProperty(String uuid) {
    db.executeInsert(
      "internal_properties",
      "KEE", "organization.default",
      "IS_EMPTY", String.valueOf(false),
      "TEXT_VALUE", uuid,
      "CREATED_AT", PAST);
  }

  private String insertQualityGate() {
    String uuid = Uuids.createFast();
    db.executeInsert(
      "quality_gates",
      "UUID", uuid,
      "NAME", uuid,
      "IS_BUILT_IN", String.valueOf(false),
      "CREATED_AT", new Date(PAST),
      "UPDATED_AT", new Date(PAST));
    return uuid;
  }

  private void insertSetting(String key, Long value) {
    db.executeInsert(
      "properties",
      "PROP_KEY", key,
      "TEXT_VALUE", value,
      "IS_EMPTY", false,
      "CREATED_AT", PAST);
  }

}
