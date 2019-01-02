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

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateMembersGroupsInEachOrganizationTest {

  private static final Date PAST = new Date(100_000_000_000L);
  private static final Date NOW = new Date(500_000_000_000L);
  private static final String DEFAULT_ORGANIZATION_UUID = "def-org";
  private static final String ORGANIZATION_1 = "ORGANIZATION_1";
  private static final String ORGANIZATION_2 = "ORGANIZATION_2";
  private static final String TEMPLATE_1 = "TEMPLATE_1";
  private static final String TEMPLATE_2 = "TEMPLATE_2";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CreateMembersGroupsInEachOrganizationTest.class, "initial.sql");

  private System2 system2 = mock(System2.class);

  private CreateMembersGroupsInEachOrganization underTest = new CreateMembersGroupsInEachOrganization(db.database(), system2);

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW.getTime());
  }

  @Test
  public void does_nothing_when_organization_disabled() throws Exception {
    setupDefaultOrganization();
    insertOrganization(ORGANIZATION_1, TEMPLATE_1);
    insertOrganization(ORGANIZATION_2, TEMPLATE_2);
    insertPermissionTemplate(ORGANIZATION_1, TEMPLATE_1, "Default");
    insertPermissionTemplate(ORGANIZATION_2, TEMPLATE_2, "Default");

    underTest.execute();

    checkNoGroups();
    checkNoPermTemplateGroups();
  }

  @Test
  public void insert_members_groups_when_not_existing() throws SQLException {
    setupDefaultOrganization();
    enableOrganization();
    insertOrganization(ORGANIZATION_1, null);
    insertOrganization(ORGANIZATION_2, null);

    underTest.execute();

    checkGroups(
      tuple(ORGANIZATION_1, "Members", "All members of the organization", NOW, NOW),
      tuple(ORGANIZATION_2, "Members", "All members of the organization", NOW, NOW),
      tuple(DEFAULT_ORGANIZATION_UUID, "Members", "All members of the organization", NOW, NOW));
  }

  @Test
  public void does_not_insert_members_group_when_group_already_exist() throws SQLException {
    setupDefaultOrganization();
    enableOrganization();
    insertMembersGroup(DEFAULT_ORGANIZATION_UUID);
    insertOrganization(ORGANIZATION_1, null);
    insertMembersGroup(ORGANIZATION_1);
    insertOrganization(ORGANIZATION_2, null);
    insertMembersGroup(ORGANIZATION_2);

    underTest.execute();

    checkGroups(
      tuple(ORGANIZATION_1, "Members", "All members of the organization", PAST, PAST),
      tuple(ORGANIZATION_2, "Members", "All members of the organization", PAST, PAST),
      tuple(DEFAULT_ORGANIZATION_UUID, "Members", "All members of the organization", PAST, PAST));
  }

  @Test
  public void insert_only_missing_members_group_when_some_groups_already_exist() throws SQLException {
    setupDefaultOrganization();
    enableOrganization();
    insertMembersGroup(DEFAULT_ORGANIZATION_UUID);
    insertOrganization(ORGANIZATION_1, null);
    insertMembersGroup(ORGANIZATION_1);
    insertOrganization(ORGANIZATION_2, null);

    underTest.execute();

    checkGroups(
      tuple(ORGANIZATION_1, "Members", "All members of the organization", PAST, PAST),
      tuple(ORGANIZATION_2, "Members", "All members of the organization", NOW, NOW),
      tuple(DEFAULT_ORGANIZATION_UUID, "Members", "All members of the organization", PAST, PAST));
  }

  @Test
  public void insert_permission_template_groups_when_not_existing() throws Exception {
    setupDefaultOrganization();
    enableOrganization();
    insertOrganization(ORGANIZATION_1, TEMPLATE_1);
    long template1 = insertPermissionTemplate(ORGANIZATION_1, TEMPLATE_1, "Default");
    insertOrganization(ORGANIZATION_2, TEMPLATE_2);
    long template2 = insertPermissionTemplate(ORGANIZATION_2, TEMPLATE_2, "Default");

    underTest.execute();

    long group1 = selectMembersGroupId(ORGANIZATION_1);
    long group2 = selectMembersGroupId(ORGANIZATION_2);
    checkPermTemplateGroups(
      tuple(group1, template1, "user", NOW, NOW),
      tuple(group1, template1, "codeviewer", NOW, NOW),
      tuple(group2, template2, "user", NOW, NOW),
      tuple(group2, template2, "codeviewer", NOW, NOW));
  }

  @Test
  public void does_not_insert_permission_template_groups_when_no_default_permission_template() throws Exception {
    setupDefaultOrganization();
    enableOrganization();
    insertOrganization(ORGANIZATION_1, TEMPLATE_1);
    insertOrganization(ORGANIZATION_2, TEMPLATE_2);

    underTest.execute();

    checkNoPermTemplateGroups();
  }

  @Test
  public void does_not_insert_permission_template_groups_when_already_existing() throws Exception {
    setupDefaultOrganization();
    enableOrganization();
    insertOrganization(ORGANIZATION_1, TEMPLATE_1);
    long template1 = insertPermissionTemplate(ORGANIZATION_1, TEMPLATE_1, "Default");
    long group1 = insertMembersGroup(ORGANIZATION_1);
    insertPermissionTemplateGroup(group1, "user", template1);
    insertPermissionTemplateGroup(group1, "codeviewer", template1);

    insertOrganization(ORGANIZATION_2, TEMPLATE_2);
    long template2 = insertPermissionTemplate(ORGANIZATION_2, TEMPLATE_2, "Default");
    long group2 = insertMembersGroup(ORGANIZATION_2);
    insertPermissionTemplateGroup(group2, "user", template2);
    insertPermissionTemplateGroup(group2, "codeviewer", template2);

    underTest.execute();

    checkPermTemplateGroups(
      tuple(group1, template1, "user", PAST, PAST),
      tuple(group1, template1, "codeviewer", PAST, PAST),
      tuple(group2, template2, "user", PAST, PAST),
      tuple(group2, template2, "codeviewer", PAST, PAST));
  }

  @Test
  public void insert_only_missing_permission_template_groups() throws Exception {
    setupDefaultOrganization();
    enableOrganization();
    insertOrganization(ORGANIZATION_1, TEMPLATE_1);
    long template1 = insertPermissionTemplate(ORGANIZATION_1, TEMPLATE_1, "Default");
    long group1 = insertMembersGroup(ORGANIZATION_1);
    insertPermissionTemplateGroup(group1, "user", template1);
    insertPermissionTemplateGroup(group1, "codeviewer", template1);

    insertOrganization(ORGANIZATION_2, TEMPLATE_2);
    long template2 = insertPermissionTemplate(ORGANIZATION_2, TEMPLATE_2, "Default");
    long group2 = insertMembersGroup(ORGANIZATION_2);

    underTest.execute();

    checkPermTemplateGroups(
      tuple(group1, template1, "user", PAST, PAST),
      tuple(group1, template1, "codeviewer", PAST, PAST),
      tuple(group2, template2, "user", NOW, NOW),
      tuple(group2, template2, "codeviewer", NOW, NOW));
  }

  @Test
  public void migration_is_reentrant() throws Exception {
    setupDefaultOrganization();
    enableOrganization();
    insertOrganization(ORGANIZATION_1, TEMPLATE_1);
    long template1 = insertPermissionTemplate(ORGANIZATION_1, TEMPLATE_1, "Default");
    insertOrganization(ORGANIZATION_2, TEMPLATE_2);
    long template2 = insertPermissionTemplate(ORGANIZATION_2, TEMPLATE_2, "Default");

    underTest.execute();

    long group1 = selectMembersGroupId(ORGANIZATION_1);
    long group2 = selectMembersGroupId(ORGANIZATION_2);
    checkPermTemplateGroups(
      tuple(group1, template1, "user", NOW, NOW),
      tuple(group1, template1, "codeviewer", NOW, NOW),
      tuple(group2, template2, "user", NOW, NOW),
      tuple(group2, template2, "codeviewer", NOW, NOW));
  }

  private void checkGroups(Tuple... expectedTuples) {
    List<Tuple> tuples = db.select("select name, description, organization_uuid, created_at, updated_at from groups").stream()
      .map(map -> new Tuple(map.get("ORGANIZATION_UUID"), map.get("NAME"), map.get("DESCRIPTION"), map.get("CREATED_AT"), map.get("UPDATED_AT")))
      .collect(Collectors.toList());
    assertThat(tuples).containsOnly(expectedTuples);
  }

  private void checkNoGroups(){
    checkGroups();
  }

  private void checkPermTemplateGroups(Tuple... expectedTuples) {
    List<Tuple> tuples = db.select("select group_id, template_id, permission_reference, created_at, updated_at from perm_templates_groups").stream()
      .map(map -> new Tuple(map.get("GROUP_ID"), map.get("TEMPLATE_ID"), map.get("PERMISSION_REFERENCE"), map.get("CREATED_AT"), map.get("UPDATED_AT")))
      .collect(Collectors.toList());
    assertThat(tuples).containsOnly(expectedTuples);
  }

  private void checkNoPermTemplateGroups(){
    checkPermTemplateGroups();
  }

  private long selectMembersGroupId(String organization) {
    return (Long) db.selectFirst(format("select id from groups where name='%s' and organization_uuid='%s'", "Members", organization)).get("ID");
  }

  private void insertOrganization(String uuid, @Nullable String defaultPermissionTemplateProject) {
    db.executeInsert(
      "ORGANIZATIONS",
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "DEFAULT_PERM_TEMPLATE_PROJECT", defaultPermissionTemplateProject,
      "GUARDED", "false",
      "CREATED_AT", 1_000L,
      "UPDATED_AT", 1_000L);
  }

  private long insertPermissionTemplate(String organizationUuid, String key, String name) {
    db.executeInsert(
      "PERMISSION_TEMPLATES",
      "KEE", key,
      "ORGANIZATION_UUID", organizationUuid,
      "NAME", name,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
    return (Long) db.selectFirst(format("select id from permission_templates where kee='%s'", key)).get("ID");
  }

  private void insertPermissionTemplateGroup(@Nullable Long groupId, String permission, @Nullable Long templateId) {
    db.executeInsert(
      "PERM_TEMPLATES_GROUPS",
      "GROUP_ID", groupId,
      "TEMPLATE_ID", templateId,
      "PERMISSION_REFERENCE", permission,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
  }

  private long insertMembersGroup(String organizationUuid) {
    db.executeInsert(
      "GROUPS",
      "NAME", "Members",
      "DESCRIPTION", "All members of the organization",
      "ORGANIZATION_UUID", organizationUuid,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
    return (Long) db.selectFirst(format("select id from groups where name='%s' and organization_uuid='%s'", "Members", organizationUuid)).get("ID");
  }

  private void setupDefaultOrganization() {
    db.executeInsert("ORGANIZATIONS",
      "UUID", DEFAULT_ORGANIZATION_UUID,
      "KEE", DEFAULT_ORGANIZATION_UUID, "NAME",
      DEFAULT_ORGANIZATION_UUID, "GUARDED", false,
      "CREATED_AT", 1_000L,
      "UPDATED_AT", 1_000L);
    db.executeInsert("INTERNAL_PROPERTIES",
      "KEE", "organization.default",
      "IS_EMPTY", "false",
      "TEXT_VALUE", DEFAULT_ORGANIZATION_UUID);
  }

  private void enableOrganization(){
    db.executeInsert("INTERNAL_PROPERTIES",
      "KEE", "organization.enabled",
      "IS_EMPTY", "false",
      "TEXT_VALUE", "true");
  }

}
