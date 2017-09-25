/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
      .extracting(QProfileEditGroupMembershipDto::getName, QProfileEditGroupMembershipDto::getDescription, QProfileEditGroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(
        tuple(group1.getName(), group1.getDescription(), true),
        tuple(group2.getName(), group2.getDescription(), true),
        tuple(group3.getName(), group3.getDescription(), false));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(IN).build(),
      Pagination.all()))
      .extracting(QProfileEditGroupMembershipDto::getName, QProfileEditGroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(group1.getName(), true), tuple(group2.getName(), true));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(OUT).build(),
      Pagination.all()))
      .extracting(QProfileEditGroupMembershipDto::getName, QProfileEditGroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(group3.getName(), false));
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
      .extracting(QProfileEditGroupMembershipDto::getName)
      .containsExactlyInAnyOrder(group1.getName());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(IN)
        .setQuery("UserS").build(),
      Pagination.all()))
      .extracting(QProfileEditGroupMembershipDto::getName)
      .containsExactlyInAnyOrder(group1.getName(), group2.getName());
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
      .extracting(QProfileEditGroupMembershipDto::getName)
      .containsExactly(group1.getName());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(3).andSize(1)))
      .extracting(QProfileEditGroupMembershipDto::getName)
      .containsExactly(group3.getName());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(10)))
      .extracting(QProfileEditGroupMembershipDto::getName)
      .containsExactly(group1.getName(), group2.getName(), group3.getName());
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

}
