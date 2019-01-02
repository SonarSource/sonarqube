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
package org.sonar.server.platform.db.migration.version.v73;

import java.sql.SQLException;
import java.util.stream.Collectors;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PopulateHotspotAdminPermissionOnTemplatesCharacteristicsTest {

  private static final long PAST = 100_000_000_000L;
  private static final long NOW = 500_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateHotspotAdminPermissionOnTemplatesCharacteristicsTest.class, "perm_tpl_characteristics.sql");

  private System2 system2 = mock(System2.class);

  private PopulateHotspotAdminPermissionOnTemplatesCharacteristics underTest = new PopulateHotspotAdminPermissionOnTemplatesCharacteristics(db.database(), system2);

  @Test
  public void insert_missing_permission() throws SQLException {
    when(system2.now()).thenReturn(NOW);
    insertPermTemplateCharacteristic(1, "noissueadmin", true);
    insertPermTemplateCharacteristic(3, "issueadmin", true);
    insertPermTemplateCharacteristic(3, "another", true);
    insertPermTemplateCharacteristic(5, "securityhotspotadmin", true);
    insertPermTemplateCharacteristic(11, "noissueadmin", false);
    insertPermTemplateCharacteristic(13, "issueadmin", false);
    insertPermTemplateCharacteristic(13, "another", false);
    insertPermTemplateCharacteristic(15, "securityhotspotadmin", false);

    underTest.execute();

    assertPermTemplateCharacteristics(
      tuple(1L, "noissueadmin", true, PAST, PAST),
      tuple(3L, "issueadmin", true, PAST, PAST),
      tuple(3L, "another", true, PAST, PAST),
      tuple(3L, "securityhotspotadmin", true, NOW, NOW),
      tuple(5L, "securityhotspotadmin", true, PAST, PAST),
      tuple(11L, "noissueadmin", false, PAST, PAST),
      tuple(13L, "issueadmin", false, PAST, PAST),
      tuple(13L, "another", false, PAST, PAST),
      tuple(15L, "securityhotspotadmin", false, PAST, PAST));
  }

  private void insertPermTemplateCharacteristic(int templateId, String perm, boolean withProjectCreator) {
    db.executeInsert(
      "PERM_TPL_CHARACTERISTICS",
      "TEMPLATE_ID", templateId,
      "PERMISSION_KEY", perm,
      "WITH_PROJECT_CREATOR", withProjectCreator,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
  }

  private void assertPermTemplateCharacteristics(Tuple... expectedTuples) {
    assertThat(db.select("SELECT TEMPLATE_ID, PERMISSION_KEY, WITH_PROJECT_CREATOR, CREATED_AT, UPDATED_AT FROM PERM_TPL_CHARACTERISTICS")
      .stream()
      .map(map -> new Tuple(map.get("TEMPLATE_ID"), map.get("PERMISSION_KEY"), map.get("WITH_PROJECT_CREATOR"), map.get("CREATED_AT"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
      .containsExactlyInAnyOrder(expectedTuples);
  }

}
