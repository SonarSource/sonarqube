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
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.qualityprofile.SearchUsersQuery.ANY;
import static org.sonar.db.qualityprofile.SearchUsersQuery.IN;
import static org.sonar.db.qualityprofile.SearchUsersQuery.OUT;
import static org.sonar.db.qualityprofile.SearchUsersQuery.builder;

public class QProfileEditUsersDaoTest {

  private static final long NOW = 10_000_000_000L;

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);

  private QProfileEditUsersDao underTest = db.getDbClient().qProfileEditUsersDao();

  @Test
  public void exists() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    QProfileDto anotherProfile = db.qualityProfiles().insert(organization);
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);

    assertThat(underTest.exists(db.getSession(), profile, user)).isTrue();
    assertThat(underTest.exists(db.getSession(), profile, anotherUser)).isFalse();
    assertThat(underTest.exists(db.getSession(), anotherProfile, user)).isFalse();
    assertThat(underTest.exists(db.getSession(), anotherProfile, anotherUser)).isFalse();
  }

  @Test
  public void countByQuery() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);
    db.organizations().addMember(organization, user3);
    db.qualityProfiles().addUserPermission(profile, user1);
    db.qualityProfiles().addUserPermission(profile, user2);

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
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);
    db.organizations().addMember(organization, user3);
    db.qualityProfiles().addUserPermission(profile, user1);
    db.qualityProfiles().addUserPermission(profile, user2);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
      .setOrganization(organization)
      .setProfile(profile)
      .setMembership(ANY).build(), Pagination.all()))
      .extracting(UserMembershipDto::getUserId, UserMembershipDto::isSelected)
      .containsExactlyInAnyOrder(
        tuple(user1.getId(), true),
        tuple(user2.getId(), true),
        tuple(user3.getId(), false));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(IN).build(),
      Pagination.all()))
      .extracting(UserMembershipDto::getUserId, UserMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(user1.getId(), true), tuple(user2.getId(), true));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(OUT).build(),
      Pagination.all()))
      .extracting(UserMembershipDto::getUserId, UserMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(user3.getId(), false));
  }

  @Test
  public void selectByQuery_search_by_name_or_login() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    UserDto user1 = db.users().insertUser(u -> u.setLogin("user1").setName("John Doe"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("user2").setName("John Smith"));
    UserDto user3 = db.users().insertUser(u -> u.setLogin("user3").setName("Jane Doe"));
    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);
    db.organizations().addMember(organization, user3);
    db.qualityProfiles().addUserPermission(profile, user1);
    db.qualityProfiles().addUserPermission(profile, user2);
    db.qualityProfiles().addUserPermission(profile, user3);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(IN)
        .setQuery("user2").build(),
      Pagination.all()))
      .extracting(UserMembershipDto::getUserId)
      .containsExactlyInAnyOrder(user2.getId());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(IN)
        .setQuery("joh").build(),
      Pagination.all()))
      .extracting(UserMembershipDto::getUserId)
      .containsExactlyInAnyOrder(user1.getId(), user2.getId());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(IN)
        .setQuery("Doe").build(),
      Pagination.all()))
      .extracting(UserMembershipDto::getUserId)
      .containsExactlyInAnyOrder(user1.getId(), user3.getId());
  }

  @Test
  public void selectByQuery_with_paging() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    UserDto user1 = db.users().insertUser(u -> u.setName("user1"));
    UserDto user2 = db.users().insertUser(u -> u.setName("user2"));
    UserDto user3 = db.users().insertUser(u -> u.setName("user3"));
    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);
    db.organizations().addMember(organization, user3);
    db.qualityProfiles().addUserPermission(profile, user1);
    db.qualityProfiles().addUserPermission(profile, user2);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(1)))
      .extracting(UserMembershipDto::getUserId)
      .containsExactly(user1.getId());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(3).andSize(1)))
      .extracting(UserMembershipDto::getUserId)
      .containsExactly(user3.getId());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(10)))
      .extracting(UserMembershipDto::getUserId)
      .containsExactly(user1.getId(), user2.getId(), user3.getId());
  }

  @Test
  public void insert() {
    underTest.insert(db.getSession(), new QProfileEditUsersDto()
      .setUuid("ABCD")
      .setUserId(100)
      .setQProfileUuid("QPROFILE")
    );

    assertThat(db.selectFirst(db.getSession(), "select uuid as \"uuid\", user_id as \"userId\", qprofile_uuid as \"qProfileUuid\", created_at as \"createdAt\" from qprofile_edit_users")).contains(
      entry("uuid", "ABCD"),
      entry("userId", 100L),
      entry("qProfileUuid", "QPROFILE"),
      entry("createdAt", NOW));
  }

  @Test
  public void deleteByQProfileAndUser() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    assertThat(underTest.exists(db.getSession(), profile, user)).isTrue();

    underTest.deleteByQProfileAndUser(db.getSession(), profile, user);

    assertThat(underTest.exists(db.getSession(), profile, user)).isFalse();
  }

}
