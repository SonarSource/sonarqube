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
package org.sonar.server.platform.db.migration.version.v64;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PopulateColumnDefaultGroupIdOfOrganizationsTest {

  private static final long PAST = 100_000_000_000L;
  private static final long NOW = 500_000_000_000L;

  private static final String DEFAULT_ORGANIZATION_UUID = "def-org";
  private static final String ORGANIZATION_1 = "ORGANIZATION_1";
  private static final String ORGANIZATION_2 = "ORGANIZATION_2";
  private static final String SONAR_USERS_NAME = "sonar-users";
  private static final String MEMBERS_NAME = "Members";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateColumnDefaultGroupIdOfOrganizationsTest.class, "initial.sql");

  private System2 system2 = mock(System2.class);

  private PopulateColumnDefaultGroupIdOfOrganizations underTest = new PopulateColumnDefaultGroupIdOfOrganizations(db.database(), system2);

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void set_sonar_users_group_id_on_default_organization_when_organization_disabled() throws Exception {
    setupDefaultOrganization();
    long groupId = insertGroup(DEFAULT_ORGANIZATION_UUID, SONAR_USERS_NAME);

    underTest.execute();

    checkOrganizations(tuple(DEFAULT_ORGANIZATION_UUID, groupId, NOW));
  }

  @Test
  public void set_members_group_id_on_organizations_when_organization_enabled() throws Exception {
    setupDefaultOrganization();
    enableOrganization();
    long group1 = insertGroup(ORGANIZATION_1, MEMBERS_NAME);
    insertOrganization(ORGANIZATION_1, null);
    long group2 = insertGroup(ORGANIZATION_2, MEMBERS_NAME);
    insertOrganization(ORGANIZATION_2, null);

    underTest.execute();

    checkOrganizations(tuple(ORGANIZATION_1, group1, NOW), tuple(ORGANIZATION_2, group2, NOW), tuple(DEFAULT_ORGANIZATION_UUID, null, PAST));
  }

  @Test
  public void does_nothing_when_default_group_id_already_set() throws Exception {
    setupDefaultOrganization();
    enableOrganization();
    long group1 = insertGroup(ORGANIZATION_1, MEMBERS_NAME);
    insertOrganization(ORGANIZATION_1, group1);
    long group2 = insertGroup(ORGANIZATION_2, MEMBERS_NAME);
    insertOrganization(ORGANIZATION_2, group2);

    underTest.execute();

    checkOrganizations(tuple(ORGANIZATION_1, group1, PAST), tuple(ORGANIZATION_2, group2, PAST), tuple(DEFAULT_ORGANIZATION_UUID, null, PAST));
  }

  @Test
  public void set_members_group_id_on_organizations_only_when_not_already_et() throws Exception {
    setupDefaultOrganization();
    enableOrganization();
    long group1 = insertGroup(ORGANIZATION_1, MEMBERS_NAME);
    insertOrganization(ORGANIZATION_1, null);
    long group2 = insertGroup(ORGANIZATION_2, MEMBERS_NAME);
    insertOrganization(ORGANIZATION_2, group2);

    underTest.execute();

    checkOrganizations(tuple(ORGANIZATION_1, group1, NOW), tuple(ORGANIZATION_2, group2, PAST), tuple(DEFAULT_ORGANIZATION_UUID, null, PAST));
  }

  @Test
  public void migration_is_reentrant() throws Exception {
    setupDefaultOrganization();
    enableOrganization();
    long group1 = insertGroup(ORGANIZATION_1, MEMBERS_NAME);
    insertOrganization(ORGANIZATION_1, null);
    long group2 = insertGroup(ORGANIZATION_2, MEMBERS_NAME);
    insertOrganization(ORGANIZATION_2, null);

    underTest.execute();
    checkOrganizations(tuple(ORGANIZATION_1, group1, NOW), tuple(ORGANIZATION_2, group2, NOW), tuple(DEFAULT_ORGANIZATION_UUID, null, PAST));

    underTest.execute();
    checkOrganizations(tuple(ORGANIZATION_1, group1, NOW), tuple(ORGANIZATION_2, group2, NOW), tuple(DEFAULT_ORGANIZATION_UUID, null, PAST));
  }

  private void checkOrganizations(Tuple... expectedTuples) {
    List<Tuple> tuples = db.select("select o.uuid, o.default_group_id, o.updated_at from organizations o").stream()
      .map(map -> new Tuple(map.get("UUID"), map.get("DEFAULT_GROUP_ID"), map.get("UPDATED_AT")))
      .collect(Collectors.toList());
    assertThat(tuples).containsOnly(expectedTuples);
  }

  private void insertOrganization(String uuid, @Nullable Long defaultGroupId) {
    db.executeInsert(
      "ORGANIZATIONS",
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "DEFAULT_GROUP_ID", defaultGroupId,
      "GUARDED", "false",
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
  }

  private long insertGroup(String organization, String name) {
    db.executeInsert(
      "GROUPS",
      "NAME", name,
      "DESCRIPTION", name,
      "ORGANIZATION_UUID", organization,
      "CREATED_AT", new Date(PAST),
      "UPDATED_AT", new Date(PAST));
    return (Long) db.selectFirst(format("select id from groups where name='%s' and organization_uuid='%s'", name, organization)).get("ID");
  }

  private void setupDefaultOrganization() {
    db.executeInsert("ORGANIZATIONS",
      "UUID", DEFAULT_ORGANIZATION_UUID,
      "KEE", DEFAULT_ORGANIZATION_UUID, "NAME",
      DEFAULT_ORGANIZATION_UUID, "GUARDED", false,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
    db.executeInsert("INTERNAL_PROPERTIES",
      "KEE", "organization.default",
      "IS_EMPTY", "false",
      "TEXT_VALUE", DEFAULT_ORGANIZATION_UUID);
  }

  private void enableOrganization() {
    db.executeInsert("INTERNAL_PROPERTIES",
      "KEE", "organization.enabled",
      "IS_EMPTY", "false",
      "TEXT_VALUE", "true");
  }
}
