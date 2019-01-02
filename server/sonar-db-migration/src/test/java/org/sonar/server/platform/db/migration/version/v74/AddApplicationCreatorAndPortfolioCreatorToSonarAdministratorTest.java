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
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProvider;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProviderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class AddApplicationCreatorAndPortfolioCreatorToSonarAdministratorTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddApplicationCreatorAndPortfolioCreatorToSonarAdministratorTest.class,
    "group_roles_and_internal_properties.sql");

  private UuidFactoryFast uuidFactoryFast = UuidFactoryFast.getInstance();
  private MapSettings settings = new MapSettings();
  private DefaultOrganizationUuidProvider defaultOrganizationUuidProvider = new DefaultOrganizationUuidProviderImpl();
  private AddApplicationCreatorAndPortfolioCreatorToSonarAdministrator underTest = new AddApplicationCreatorAndPortfolioCreatorToSonarAdministrator(db.database(), settings.asConfig(),
    defaultOrganizationUuidProvider);

  @Test
  public void is_reentrant() throws SQLException {
    String orgUuid = uuidFactoryFast.create();
    insertDefaultOrganizationUuid(orgUuid);
    insertGroup(orgUuid, "sonar-administrators");
    Long adminGroupId = getGroupId("sonar-administrators");

    underTest.execute();
    underTest.execute();

    assertGroupRoles(
      tuple(orgUuid, adminGroupId, null, "applicationcreator"),
      tuple(orgUuid, adminGroupId, null, "portfoliocreator"));
  }

  @Test
  public void create_missing_permissions() throws SQLException {
    String orgUuid = uuidFactoryFast.create();
    insertDefaultOrganizationUuid(orgUuid);
    insertGroup(orgUuid, "sonar-administrators");
    Long adminGroupId = getGroupId("sonar-administrators");

    underTest.execute();

    assertGroupRoles(
      tuple(orgUuid, adminGroupId, null, "applicationcreator"),
      tuple(orgUuid, adminGroupId, null, "portfoliocreator"));
  }

  @Test
  public void has_no_effect_if_group_does_not_exist() throws SQLException {
    String orgUuid = uuidFactoryFast.create();
    insertDefaultOrganizationUuid(orgUuid);
    insertGroup(orgUuid, "sonar");

    underTest.execute();

    assertGroupRoles();
  }

  @Test
  public void has_no_effect_if_roles_are_already_present() throws SQLException {
    String orgUuid = uuidFactoryFast.create();
    insertDefaultOrganizationUuid(orgUuid);
    insertGroup(orgUuid, "sonar-administrators");
    Long adminGroupId = getGroupId("sonar-administrators");
    insertGroupRole(orgUuid, adminGroupId, null, "applicationcreator");
    insertGroupRole(orgUuid, adminGroupId, null, "portfoliocreator");

    underTest.execute();

    assertGroupRoles(
      tuple(orgUuid, adminGroupId, null, "applicationcreator"),
      tuple(orgUuid, adminGroupId, null, "portfoliocreator"));
  }

  @Test
  public void has_no_effect_on_SonarCloud() throws SQLException {
    settings.setProperty("sonar.sonarcloud.enabled", true);
    underTest.execute();
    assertGroupRoles();
  }

  private void insertDefaultOrganizationUuid(String uuid) {
    db.executeInsert("INTERNAL_PROPERTIES",
      "KEE", "organization.default",
      "IS_EMPTY", false,
      "TEXT_VALUE", uuid,
      "CREATED_AT", System.currentTimeMillis());
  }

  private void insertGroup(String organizationUuid, String name) {
    db.executeInsert("GROUPS",
      "ORGANIZATION_UUID", organizationUuid,
      "NAME", name,
      "CREATED_AT", new Date(),
      "UPDATED_AT", new Date());
  }

  private void insertGroupRole(String organizationUuid, @Nullable Long groupId, @Nullable Integer resourceId, String role) {
    db.executeInsert("GROUP_ROLES",
      "ORGANIZATION_UUID", organizationUuid,
      "GROUP_ID", groupId,
      "RESOURCE_ID", resourceId,
      "ROLE", role);
  }

  private Long getGroupId(String groupName) {
    return (Long) db.selectFirst("SELECT id FROM groups WHERE name = '" + groupName + "'").get("ID");
  }

  private void assertGroupRoles(Tuple... expectedTuples) {
    assertThat(db.select("SELECT organization_uuid, group_id, resource_id, role FROM group_roles")
      .stream()
      .map(row -> new Tuple(row.get("ORGANIZATION_UUID"), row.get("GROUP_ID"), row.get("RESOURCE_ID"), row.get("ROLE")))
      .collect(Collectors.toList()))
      .containsExactlyInAnyOrder(expectedTuples);
  }
}
