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
import java.util.Date;
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

public class PopulateHotspotAdminPermissionOnTemplatesGroupsTest {

  private static final Date PAST = new Date(100_000_000_000L);
  private static final Date NOW = new Date(500_000_000_000L);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateHotspotAdminPermissionOnTemplatesGroupsTest.class, "perm_templates_groups.sql");

  private System2 system2 = mock(System2.class);

  private PopulateHotspotAdminPermissionOnTemplatesGroups underTest = new PopulateHotspotAdminPermissionOnTemplatesGroups(db.database(), system2);

  @Test
  public void insert_missing_permission() throws SQLException {
    when(system2.now()).thenReturn(NOW.getTime());
    insertPermTemplateGroupRole(1, 2, "noissueadmin");
    insertPermTemplateGroupRole(3, 4, "issueadmin");
    insertPermTemplateGroupRole(3, 4, "another");
    insertPermTemplateGroupRole(5, 6, "securityhotspotadmin");

    underTest.execute();

    assertPermTemplateGroupRoles(
      tuple(1L, 2L, "noissueadmin", PAST, PAST),
      tuple(3L, 4L, "issueadmin", PAST, PAST),
      tuple(3L, 4L, "another", PAST, PAST),
      tuple(3L, 4L, "securityhotspotadmin", NOW, NOW),
      tuple(5L, 6L, "securityhotspotadmin", PAST, PAST));
  }

  private void insertPermTemplateGroupRole(int templateId, int groupId, String role) {
    db.executeInsert(
      "PERM_TEMPLATES_GROUPS",
      "TEMPLATE_ID", templateId,
      "GROUP_ID", groupId,
      "PERMISSION_REFERENCE", role,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
  }

  private void assertPermTemplateGroupRoles(Tuple... expectedTuples) {
    assertThat(db.select("SELECT TEMPLATE_ID, GROUP_ID, PERMISSION_REFERENCE, CREATED_AT, UPDATED_AT FROM PERM_TEMPLATES_GROUPS")
      .stream()
      .map(map -> new Tuple(map.get("TEMPLATE_ID"), map.get("GROUP_ID"), map.get("PERMISSION_REFERENCE"), map.get("CREATED_AT"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

}
