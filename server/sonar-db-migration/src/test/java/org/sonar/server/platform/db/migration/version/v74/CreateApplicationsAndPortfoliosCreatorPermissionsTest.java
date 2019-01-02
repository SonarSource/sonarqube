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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import java.util.Date;
import java.util.stream.Collectors;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateApplicationsAndPortfoliosCreatorPermissionsTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CreateApplicationsAndPortfoliosCreatorPermissionsTest.class, "perm_templates_groups.sql");

  private static final Date PAST = new Date(100_000_000_000L);
  private static final Date NOW = new Date(500_000_000_000L);
  private static final String DEFAULT_ORGANIZATION_UUID = UuidFactoryFast.getInstance().create();
  private static final String DEFAULT_PERM_TEMPLATE_VIEW = "default_view_template";
  private static final String ANOTHER_PERM_TEMPLATE_VIEW = "another_template";

  private System2 system2 = mock(System2.class);
  private CreateApplicationsAndPortfoliosCreatorPermissions underTest = new CreateApplicationsAndPortfoliosCreatorPermissions(db.database(), system2);

  @Before
  public void setupDatabase() {
    insertDefaultOrganization();
    insertDefaultGroups();
    insertPermissionTemplate();
  }


  @Test
  public void migration_is_reentrant() throws SQLException {
    when(system2.now()).thenReturn(NOW.getTime());

    underTest.execute();
    underTest.execute();

    Long idOfDefaultPermissionTemplate = getIdOfPermissionTemplate(DEFAULT_PERM_TEMPLATE_VIEW);
    Long idOfAdministratorGroup = getIdOfGroup("sonar-administrators");

    assertPermTemplateGroupRoles(
      tuple(idOfDefaultPermissionTemplate, idOfAdministratorGroup, "applicationcreator", NOW, NOW),
      tuple(idOfDefaultPermissionTemplate, idOfAdministratorGroup, "portfoliocreator", NOW, NOW));
  }

  @Test
  public void insert_missing_permissions() throws SQLException {
    when(system2.now()).thenReturn(NOW.getTime());

    underTest.execute();

    Long idOfDefaultPermissionTemplate = getIdOfPermissionTemplate(DEFAULT_PERM_TEMPLATE_VIEW);
    Long idOfAdministratorGroup = getIdOfGroup("sonar-administrators");

    assertPermTemplateGroupRoles(
      tuple(idOfDefaultPermissionTemplate, idOfAdministratorGroup, "applicationcreator", NOW, NOW),
      tuple(idOfDefaultPermissionTemplate, idOfAdministratorGroup, "portfoliocreator", NOW, NOW));
  }

  @Test
  public void does_nothing_if_template_group_has_the_permissions_already() throws SQLException {
    Long idOfDefaultPermissionTemplate = getIdOfPermissionTemplate(DEFAULT_PERM_TEMPLATE_VIEW);
    Long idOfAdministratorGroup = getIdOfGroup("sonar-administrators");

    insertPermTemplateGroupRole(1, 2, "noissueadmin");
    insertPermTemplateGroupRole(3, 4, "issueadmin");
    insertPermTemplateGroupRole(3, 4, "another");
    insertPermTemplateGroupRole(5, 6, "securityhotspotadmin");
    insertPermTemplateGroupRole(idOfDefaultPermissionTemplate.intValue(), idOfAdministratorGroup.intValue(), "applicationcreator");
    insertPermTemplateGroupRole(idOfDefaultPermissionTemplate.intValue(), idOfAdministratorGroup.intValue(), "portfoliocreator");

    when(system2.now()).thenReturn(NOW.getTime());
    underTest.execute();

    assertPermTemplateGroupRoles(
      tuple(1L, 2L, "noissueadmin", PAST, PAST),
      tuple(3L, 4L, "issueadmin", PAST, PAST),
      tuple(3L, 4L, "another", PAST, PAST),
      tuple(5L, 6L, "securityhotspotadmin", PAST, PAST),
      tuple(idOfDefaultPermissionTemplate, idOfAdministratorGroup, "applicationcreator", PAST, PAST),
      tuple(idOfDefaultPermissionTemplate, idOfAdministratorGroup, "portfoliocreator", PAST, PAST));
  }

  @Test
  public void insert_missing_permission_keeping_other_template_group_permissions() throws SQLException {
    when(system2.now()).thenReturn(NOW.getTime());
    insertPermTemplateGroupRole(1, 2, "noissueadmin");
    insertPermTemplateGroupRole(3, 4, "issueadmin");
    insertPermTemplateGroupRole(3, 4, "another");
    insertPermTemplateGroupRole(5, 6, "securityhotspotadmin");

    underTest.execute();

    Long idOfDefaultPermissionTemplate = getIdOfPermissionTemplate(DEFAULT_PERM_TEMPLATE_VIEW);
    Long idOfAdministratorGroup = getIdOfGroup("sonar-administrators");

    assertPermTemplateGroupRoles(
      tuple(1L, 2L, "noissueadmin", PAST, PAST),
      tuple(3L, 4L, "issueadmin", PAST, PAST),
      tuple(3L, 4L, "another", PAST, PAST),
      tuple(5L, 6L, "securityhotspotadmin", PAST, PAST),
      tuple(idOfDefaultPermissionTemplate, idOfAdministratorGroup, "applicationcreator", NOW, NOW),
      tuple(idOfDefaultPermissionTemplate, idOfAdministratorGroup, "portfoliocreator", NOW, NOW));
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

  private void insertDefaultGroups() {
    db.executeInsert(
      "GROUPS",
      "NAME", "sonar-administrators",
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST,
      "ORGANIZATION_UUID", DEFAULT_ORGANIZATION_UUID);
    db.executeInsert(
      "GROUPS",
      "NAME", "sonar-users",
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST,
      "ORGANIZATION_UUID", DEFAULT_ORGANIZATION_UUID);
  }

  private void insertDefaultOrganization() {
    db.executeInsert(
      "ORGANIZATIONS",
      "UUID", DEFAULT_ORGANIZATION_UUID,
      "KEE", "default-organization",
      "NAME", "Default Organization",
      "GUARDED", true,
      "DEFAULT_PERM_TEMPLATE_VIEW", DEFAULT_PERM_TEMPLATE_VIEW,
      "DEFAULT_QUALITY_GATE_UUID", UuidFactoryFast.getInstance().create(),
      "NEW_PROJECT_PRIVATE", false,
      "SUBSCRIPTION", "SONARQUBE",
      "CREATED_AT", PAST.getTime(),
      "UPDATED_AT", PAST.getTime());
  }

  private void insertPermissionTemplate() {
    db.executeInsert(
      "PERMISSION_TEMPLATES",
      "ORGANIZATION_UUID", DEFAULT_ORGANIZATION_UUID,
      "NAME", "Default template for views",
      "KEE", DEFAULT_PERM_TEMPLATE_VIEW,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
    db.executeInsert(
      "PERMISSION_TEMPLATES",
      "ORGANIZATION_UUID", DEFAULT_ORGANIZATION_UUID,
      "NAME", ANOTHER_PERM_TEMPLATE_VIEW,
      "KEE", ANOTHER_PERM_TEMPLATE_VIEW,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
  }

  private Long getIdOfPermissionTemplate(String key) {
    return (Long) db.selectFirst("SELECT id FROM permission_templates WHERE kee='" + key + "'")
      .get("ID");
  }

  private Long getIdOfGroup(String key) {
    return (Long) db.selectFirst("SELECT id FROM groups WHERE name='" + key + "'")
      .get("ID");
  }

  private void assertPermTemplateGroupRoles(Tuple... expectedTuples) {
    assertThat(db.select("SELECT TEMPLATE_ID, GROUP_ID, PERMISSION_REFERENCE, CREATED_AT, UPDATED_AT FROM PERM_TEMPLATES_GROUPS")
      .stream()
      .map(map -> new Tuple(map.get("TEMPLATE_ID"), map.get("GROUP_ID"), map.get("PERMISSION_REFERENCE"), map.get("CREATED_AT"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
      .containsExactlyInAnyOrder(expectedTuples);
  }
}
