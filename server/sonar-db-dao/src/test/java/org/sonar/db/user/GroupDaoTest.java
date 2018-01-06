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
package org.sonar.db.user;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class GroupDaoTest {

  private static final long NOW = 1_500_000L;
  private static final int MISSING_ID = -1;
  private static final OrganizationDto AN_ORGANIZATION = new OrganizationDto()
    .setKey("an-org")
    .setName("An Org")
    .setDefaultQualityGateUuid("1")
    .setUuid("abcde");

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);
  private DbSession dbSession = db.getSession();
  private GroupDao underTest = db.getDbClient().groupDao();

  // not static as group id is changed in each test
  private final GroupDto aGroup = new GroupDto()
    .setName("the-name")
    .setDescription("the description")
    .setOrganizationUuid(AN_ORGANIZATION.getUuid());

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(NOW);
    db.getDbClient().organizationDao().insert(dbSession, AN_ORGANIZATION, false);
  }

  @Test
  public void selectByName() {
    db.getDbClient().groupDao().insert(dbSession, aGroup);

    GroupDto group = underTest.selectByName(dbSession, AN_ORGANIZATION.getUuid(), aGroup.getName()).get();

    assertThat(group.getId()).isNotNull();
    assertThat(group.getOrganizationUuid()).isEqualTo(aGroup.getOrganizationUuid());
    assertThat(group.getName()).isEqualTo(aGroup.getName());
    assertThat(group.getDescription()).isEqualTo(aGroup.getDescription());
    assertThat(group.getCreatedAt()).isEqualTo(new Date(NOW));
    assertThat(group.getUpdatedAt()).isEqualTo(new Date(NOW));
  }

  @Test
  public void selectByName_returns_absent() {
    Optional<GroupDto> group = underTest.selectByName(dbSession, AN_ORGANIZATION.getUuid(), "missing");

    assertThat(group).isNotPresent();
  }

  @Test
  public void selectByUserLogin() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    assertThat(underTest.selectByUserLogin(dbSession, user.getLogin())).hasSize(2);
    assertThat(underTest.selectByUserLogin(dbSession, "unknown")).isEmpty();
  }

  @Test
  public void selectByNames() {
    GroupDto group1InOrg1 = underTest.insert(dbSession, newGroupDto().setName("group1").setOrganizationUuid("org1"));
    GroupDto group2InOrg1 = underTest.insert(dbSession, newGroupDto().setName("group2").setOrganizationUuid("org1"));
    GroupDto group1InOrg2 = underTest.insert(dbSession, newGroupDto().setName("group1").setOrganizationUuid("org2"));
    GroupDto group3InOrg2 = underTest.insert(dbSession, newGroupDto().setName("group3").setOrganizationUuid("org2"));
    dbSession.commit();

    assertThat(underTest.selectByNames(dbSession, "org1", asList("group1", "group2", "group3", "missingGroup"))).extracting(GroupDto::getId)
      .containsOnly(group1InOrg1.getId(), group2InOrg1.getId());

    assertThat(underTest.selectByNames(dbSession, "org1", Collections.emptyList())).isEmpty();
    assertThat(underTest.selectByNames(dbSession, "missingOrg", asList("group1"))).isEmpty();
  }

  @Test
  public void selectByIds() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();

    assertThat(underTest.selectByIds(dbSession, asList(group1.getId(), group2.getId())))
      .extracting(GroupDto::getId).containsOnly(group1.getId(), group2.getId());

    assertThat(underTest.selectByIds(dbSession, asList(group1.getId(), MISSING_ID)))
      .extracting(GroupDto::getId).containsOnly(group1.getId());

    assertThat(underTest.selectByIds(dbSession, Collections.emptyList())).isEmpty();
  }

  @Test
  public void update() {
    db.getDbClient().groupDao().insert(dbSession, aGroup);
    GroupDto dto = new GroupDto()
      .setId(aGroup.getId())
      .setName("new-name")
      .setDescription("New description")
      .setOrganizationUuid("another-org")
      .setCreatedAt(new Date(NOW + 1_000L));

    underTest.update(dbSession, dto);

    GroupDto reloaded = underTest.selectById(dbSession, aGroup.getId());

    // verify mutable fields
    assertThat(reloaded.getName()).isEqualTo("new-name");
    assertThat(reloaded.getDescription()).isEqualTo("New description");

    // immutable fields --> to be ignored
    assertThat(reloaded.getOrganizationUuid()).isEqualTo(aGroup.getOrganizationUuid());
    assertThat(reloaded.getCreatedAt()).isEqualTo(aGroup.getCreatedAt());
  }

  @Test
  public void selectByQuery() {
    OrganizationDto organization1 = db.organizations().insert(organizationDto -> organizationDto.setUuid("org1"));
    OrganizationDto organization2 = db.organizations().insert(organizationDto -> organizationDto.setUuid("org2"));
    db.users().insertGroup(organization1, "sonar-users");
    db.users().insertGroup(organization1, "SONAR-ADMINS");
    db.users().insertGroup(organization1, "customers-group1");
    db.users().insertGroup(organization1, "customers-group2");
    db.users().insertGroup(organization1, "customers-group3");
    // Group on another organization
    db.users().insertGroup(organization2, "customers-group4");

    /*
     * Ordering and paging are not fully tested, case insensitive sort is broken on MySQL
     */

    // Null query
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 0, 10))
      .hasSize(5)
      .extracting("name").containsOnly("customers-group1", "customers-group2", "customers-group3", "SONAR-ADMINS", "sonar-users");

    // Empty query
    assertThat(underTest.selectByQuery(dbSession, "org1", "", 0, 10))
      .hasSize(5)
      .extracting("name").containsOnly("customers-group1", "customers-group2", "customers-group3", "SONAR-ADMINS", "sonar-users");

    // Filter on name
    assertThat(underTest.selectByQuery(dbSession, "org1", "sonar", 0, 10))
      .hasSize(2)
      .extracting("name").containsOnly("SONAR-ADMINS", "sonar-users");

    // Pagination
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 0, 3))
      .hasSize(3);
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 3, 3))
      .hasSize(2);
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 6, 3)).isEmpty();
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 0, 5))
      .hasSize(5);
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 5, 5)).isEmpty();
  }

  @Test
  public void select_by_query_with_special_characters() {
    String groupNameWithSpecialCharacters = "group%_%/name";
    underTest.insert(dbSession, newGroupDto().setName(groupNameWithSpecialCharacters).setOrganizationUuid("org1"));
    db.commit();

    List<GroupDto> result = underTest.selectByQuery(dbSession, "org1", "roup%_%/nam", 0, 10);
    int resultCount = underTest.countByQuery(dbSession, "org1", "roup%_%/nam");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo(groupNameWithSpecialCharacters);
    assertThat(resultCount).isEqualTo(1);
  }

  @Test
  public void countByQuery() {
    OrganizationDto organization1 = db.organizations().insert(organizationDto -> organizationDto.setUuid("org1"));
    OrganizationDto organization2 = db.organizations().insert(organizationDto -> organizationDto.setUuid("org2"));
    db.users().insertGroup(organization1, "sonar-users");
    db.users().insertGroup(organization1, "SONAR-ADMINS");
    db.users().insertGroup(organization1, "customers-group1");
    db.users().insertGroup(organization1, "customers-group2");
    db.users().insertGroup(organization1, "customers-group3");
    // Group on another organization
    db.users().insertGroup(organization2, "customers-group4");

    // Null query
    assertThat(underTest.countByQuery(dbSession, "org1", null)).isEqualTo(5);

    // Empty query
    assertThat(underTest.countByQuery(dbSession, "org1", "")).isEqualTo(5);

    // Filter on name
    assertThat(underTest.countByQuery(dbSession, "org1", "sonar")).isEqualTo(2);
  }

  @Test
  public void deleteById() {
    db.getDbClient().groupDao().insert(dbSession, aGroup);

    underTest.deleteById(dbSession, aGroup.getId());

    assertThat(db.countRowsOfTable(dbSession, "groups")).isEqualTo(0);
  }

  @Test
  public void deleteByOrganization_does_not_fail_when_table_is_empty() {
    underTest.deleteByOrganization(dbSession, "some uuid");
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_does_not_fail_when_organization_has_no_group() {
    OrganizationDto organization = db.organizations().insert();

    underTest.deleteByOrganization(dbSession, organization.getUuid());
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_deletes_all_groups_in_specified_organization() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    OrganizationDto organization3 = db.organizations().insert();

    db.users().insertGroup(organization1);
    db.users().insertGroup(organization2);
    db.users().insertGroup(organization3);
    db.users().insertGroup(organization3);
    db.users().insertGroup(organization2);

    underTest.deleteByOrganization(dbSession, organization2.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable(organization1.getUuid(), organization3.getUuid());

    underTest.deleteByOrganization(dbSession, organization1.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable(organization3.getUuid());

    underTest.deleteByOrganization(dbSession, organization3.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable();
  }

  private void verifyOrganizationUuidsInTable(String... organizationUuids) {
    assertThat(db.select("select distinct organization_uuid as \"organizationUuid\" from groups"))
      .extracting(row -> (String) row.get("organizationUuid"))
      .containsOnly(organizationUuids);
  }
}
