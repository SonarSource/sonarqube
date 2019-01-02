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
package org.sonar.db.qualityprofile;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.qualityprofile.SearchGroupsQuery.ANY;
import static org.sonar.db.qualityprofile.SearchGroupsQuery.IN;
import static org.sonar.db.qualityprofile.SearchGroupsQuery.OUT;
import static org.sonar.db.qualityprofile.SearchGroupsQuery.builder;

public class QProfileEditGroupsDaoTest {

  private static final long NOW = 10_000_000_000L;

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);

  private QProfileEditGroupsDao underTest = db.getDbClient().qProfileEditGroupsDao();

  @Test
  public void exists() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    QProfileDto anotherProfile = db.qualityProfiles().insert(organization);
    GroupDto group = db.users().insertGroup(organization);
    GroupDto anotherGroup = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group);

    assertThat(underTest.exists(db.getSession(), profile, group)).isTrue();
    assertThat(underTest.exists(db.getSession(), profile, anotherGroup)).isFalse();
    assertThat(underTest.exists(db.getSession(), anotherProfile, group)).isFalse();
    assertThat(underTest.exists(db.getSession(), anotherProfile, anotherGroup)).isFalse();
    assertThat(underTest.exists(db.getSession(), profile, asList(group, anotherGroup))).isTrue();
    assertThat(underTest.exists(db.getSession(), profile, singletonList(anotherGroup))).isFalse();
    assertThat(underTest.exists(db.getSession(), profile, emptyList())).isFalse();
  }

  @Test
  public void countByQuery() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    GroupDto group3 = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setOrganization(organization)
      .setProfile(profile)
      .setMembership(ANY).build()))
      .isEqualTo(3);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setOrganization(organization)
      .setProfile(profile)
      .setMembership(IN).build()))
      .isEqualTo(2);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setOrganization(organization)
      .setProfile(profile)
      .setMembership(OUT).build()))
      .isEqualTo(1);
  }

  @Test
  public void selectByQuery() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    GroupDto group3 = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
      .setOrganization(organization)
      .setProfile(profile)
      .setMembership(ANY).build(), Pagination.all()))
      .extracting(GroupMembershipDto::getGroupId, GroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(
        tuple(group1.getId(), true),
        tuple(group2.getId(), true),
        tuple(group3.getId(), false));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(IN).build(),
      Pagination.all()))
      .extracting(GroupMembershipDto::getGroupId, GroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(group1.getId(), true), tuple(group2.getId(), true));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(OUT).build(),
      Pagination.all()))
      .extracting(GroupMembershipDto::getGroupId, GroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(group3.getId(), false));
  }

  @Test
  public void selectByQuery_search_by_name() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    GroupDto group1 = db.users().insertGroup(organization, "sonar-users-project");
    GroupDto group2 = db.users().insertGroup(organization, "sonar-users-qprofile");
    GroupDto group3 = db.users().insertGroup(organization, "sonar-admin");
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);
    db.qualityProfiles().addGroupPermission(profile, group3);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(IN)
        .setQuery("project").build(),
      Pagination.all()))
      .extracting(GroupMembershipDto::getGroupId)
      .containsExactlyInAnyOrder(group1.getId());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(IN)
        .setQuery("UserS").build(),
      Pagination.all()))
      .extracting(GroupMembershipDto::getGroupId)
      .containsExactlyInAnyOrder(group1.getId(), group2.getId());
  }

  @Test
  public void selectByQuery_with_paging() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    GroupDto group1 = db.users().insertGroup(organization, "group1");
    GroupDto group2 = db.users().insertGroup(organization, "group2");
    GroupDto group3 = db.users().insertGroup(organization, "group3");
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(1)))
      .extracting(GroupMembershipDto::getGroupId)
      .containsExactly(group1.getId());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(3).andSize(1)))
      .extracting(GroupMembershipDto::getGroupId)
      .containsExactly(group3.getId());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(10)))
      .extracting(GroupMembershipDto::getGroupId)
      .containsExactly(group1.getId(), group2.getId(), group3.getId());
  }

  @Test
  public void selectQProfileUuidsByOrganizationAndGroups() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    QProfileDto profile1 = db.qualityProfiles().insert(organization);
    QProfileDto profile2 = db.qualityProfiles().insert(organization);
    QProfileDto anotherProfile = db.qualityProfiles().insert(anotherOrganization);
    GroupDto group1 = db.users().insertGroup(organization, "group1");
    GroupDto group2 = db.users().insertGroup(organization, "group2");
    GroupDto group3 = db.users().insertGroup(organization, "group3");
    db.qualityProfiles().addGroupPermission(profile1, group1);
    db.qualityProfiles().addGroupPermission(profile1, group2);
    db.qualityProfiles().addGroupPermission(profile2, group2);
    db.qualityProfiles().addGroupPermission(anotherProfile, group1);
    db.qualityProfiles().addGroupPermission(anotherProfile, group3);

    assertThat(underTest.selectQProfileUuidsByOrganizationAndGroups(db.getSession(), organization, asList(group1, group2)))
      .containsExactlyInAnyOrder(profile1.getKee(), profile2.getKee())
      .doesNotContain(anotherProfile.getKee());
    assertThat(underTest.selectQProfileUuidsByOrganizationAndGroups(db.getSession(), organization, asList(group1, group2, group3)))
      .containsExactlyInAnyOrder(profile1.getKee(), profile2.getKee());
    assertThat(underTest.selectQProfileUuidsByOrganizationAndGroups(db.getSession(), organization, emptyList())).isEmpty();
  }

  @Test
  public void insert() {
    underTest.insert(db.getSession(), new QProfileEditGroupsDto()
      .setUuid("ABCD")
      .setGroupId(100)
      .setQProfileUuid("QPROFILE")
    );

    assertThat(db.selectFirst(db.getSession(), "select uuid as \"uuid\", group_id as \"groupId\", qprofile_uuid as \"qProfileUuid\", created_at as \"createdAt\" from qprofile_edit_groups")).contains(
      entry("uuid", "ABCD"),
      entry("groupId", 100L),
      entry("qProfileUuid", "QPROFILE"),
      entry("createdAt", NOW));
  }

  @Test
  public void deleteByQProfileAndGroup() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    GroupDto group = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group);
    assertThat(underTest.exists(db.getSession(), profile, group)).isTrue();

    underTest.deleteByQProfileAndGroup(db.getSession(), profile, group);

    assertThat(underTest.exists(db.getSession(), profile, group)).isFalse();
  }

  @Test
  public void deleteByQProfiles() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    QProfileDto profile1 = db.qualityProfiles().insert(organization);
    QProfileDto profile2 = db.qualityProfiles().insert(organization);
    QProfileDto profile3 = db.qualityProfiles().insert(organization);
    QProfileDto anotherProfile = db.qualityProfiles().insert(anotherOrganization);
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile1, group1);
    db.qualityProfiles().addGroupPermission(profile2, group2);
    db.qualityProfiles().addGroupPermission(profile3, group1);
    db.qualityProfiles().addGroupPermission(anotherProfile, group1);

    underTest.deleteByQProfiles(db.getSession(), asList(profile1, profile2));

    assertThat(underTest.exists(db.getSession(), profile1, group1)).isFalse();
    assertThat(underTest.exists(db.getSession(), profile2, group2)).isFalse();
    assertThat(underTest.exists(db.getSession(), profile3, group1)).isTrue();
    assertThat(underTest.exists(db.getSession(), anotherProfile, group1)).isTrue();
  }

  @Test
  public void deleteByGroup() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    QProfileDto profile1 = db.qualityProfiles().insert(organization);
    QProfileDto profile2 = db.qualityProfiles().insert(organization);
    QProfileDto profile3 = db.qualityProfiles().insert(organization);
    QProfileDto anotherProfile = db.qualityProfiles().insert(anotherOrganization);
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile1, group1);
    db.qualityProfiles().addGroupPermission(profile2, group2);
    db.qualityProfiles().addGroupPermission(profile3, group1);
    db.qualityProfiles().addGroupPermission(anotherProfile, group1);

    underTest.deleteByGroup(db.getSession(), group1);

    assertThat(underTest.exists(db.getSession(), profile1, group1)).isFalse();
    assertThat(underTest.exists(db.getSession(), profile2, group2)).isTrue();
    assertThat(underTest.exists(db.getSession(), profile3, group1)).isFalse();
    assertThat(underTest.exists(db.getSession(), anotherProfile, group1)).isFalse();
  }

}
