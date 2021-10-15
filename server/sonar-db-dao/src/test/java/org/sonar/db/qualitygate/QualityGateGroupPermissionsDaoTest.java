/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.db.qualitygate;

import java.util.Date;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.SearchGroupMembershipDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.qualitygate.SearchQualityGateGroupsQuery.builder;
import static org.sonar.db.user.SearchGroupsQuery.ANY;
import static org.sonar.db.user.SearchGroupsQuery.IN;
import static org.sonar.db.user.SearchGroupsQuery.OUT;

public class QualityGateGroupPermissionsDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = dbTester.getSession();
  private QualityGateGroupPermissionsDao underTest = dbTester.getDbClient().qualityGateGroupPermissionsDao();

  @Test
  public void itInsertsAndExistsReturnsTrue() {
    GroupDto group = GroupTesting.newGroupDto();
    QualityGateDto qualityGateDto = insertQualityGate();
    QualityGateGroupPermissionsDto qualityGateGroupPermission = insertQualityGateGroupPermission(qualityGateDto.getUuid(), group.getUuid());

    assertThat(qualityGateGroupPermission.getUuid()).isNotNull();
    assertThat(underTest.exists(dbSession, qualityGateDto, group)).isTrue();
    assertThat(underTest.exists(dbSession, qualityGateDto.getUuid(), group.getUuid())).isTrue();
  }

  @Test
  public void existsReturnsTrueForListOfGroups() {
    GroupDto group1 = GroupTesting.newGroupDto();
    GroupDto group2 = GroupTesting.newGroupDto();
    QualityGateDto qualityGateDto = insertQualityGate();
    QualityGateGroupPermissionsDto qualityGateGroupPermission = insertQualityGateGroupPermission(qualityGateDto.getUuid(), group1.getUuid());

    assertThat(qualityGateGroupPermission.getUuid()).isNotNull();
    assertThat(underTest.exists(dbSession, qualityGateDto, List.of(group1, group2))).isTrue();
    assertThat(underTest.exists(dbSession, qualityGateDto.getUuid(), group1.getUuid())).isTrue();
    assertThat(underTest.exists(dbSession, qualityGateDto.getUuid(), group2.getUuid())).isFalse();
  }

  @Test
  public void existsReturnsFalseWhenQGEditGroupsDoesNotExist() {
    assertThat(underTest.exists(dbSession, randomAlphabetic(5), randomAlphabetic(5))).isFalse();
  }

  @Test
  public void countByQuery() {
    QualityGateDto qualityGateDto = insertQualityGate();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    GroupDto group3 = dbTester.users().insertGroup();

    insertQualityGateGroupPermission(qualityGateDto.getUuid(), group1.getUuid());
    insertQualityGateGroupPermission(qualityGateDto.getUuid(), group2.getUuid());

    assertThat(underTest.exists(dbSession, qualityGateDto, List.of(group1, group2))).isTrue();

    assertThat(underTest.countByQuery(dbSession, builder()
      .setQualityGate(qualityGateDto)
      .setMembership(ANY).build()))
      .isEqualTo(3);

    assertThat(underTest.countByQuery(dbSession, builder()
      .setQualityGate(qualityGateDto)
      .setMembership(IN).build()))
      .isEqualTo(2);

    assertThat(underTest.countByQuery(dbSession, builder()
      .setQualityGate(qualityGateDto)
      .setMembership(OUT).build()))
      .isEqualTo(1);
  }

  @Test
  public void selectByQuery() {
    QualityGateDto qualityGateDto = insertQualityGate();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    GroupDto group3 = dbTester.users().insertGroup();

    insertQualityGateGroupPermission(qualityGateDto.getUuid(), group1.getUuid());
    insertQualityGateGroupPermission(qualityGateDto.getUuid(), group2.getUuid());

    assertThat(underTest.selectByQuery(dbSession, builder()
      .setQualityGate(qualityGateDto)
      .setMembership(ANY).build(), Pagination.all()))
      .extracting(SearchGroupMembershipDto::getGroupUuid, SearchGroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(
        tuple(group1.getUuid(), true),
        tuple(group2.getUuid(), true),
        tuple(group3.getUuid(), false));

    assertThat(underTest.selectByQuery(dbSession, builder()
        .setQualityGate(qualityGateDto)
        .setMembership(IN).build(),
      Pagination.all()))
      .extracting(SearchGroupMembershipDto::getGroupUuid, SearchGroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(group1.getUuid(), true), tuple(group2.getUuid(), true));

    assertThat(underTest.selectByQuery(dbSession, builder()
        .setQualityGate(qualityGateDto)
        .setMembership(OUT).build(),
      Pagination.all()))
      .extracting(SearchGroupMembershipDto::getGroupUuid, SearchGroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(group3.getUuid(), false));
  }

  @Test
  public void selectByQuery_search_by_name() {
    QualityGateDto qualityGateDto = insertQualityGate();
    GroupDto group1 = dbTester.users().insertGroup("sonar-users-project");
    GroupDto group2 = dbTester.users().insertGroup("sonar-users-qprofile");
    GroupDto group3 = dbTester.users().insertGroup("sonar-admin");

    insertQualityGateGroupPermission(qualityGateDto.getUuid(), group1.getUuid());
    insertQualityGateGroupPermission(qualityGateDto.getUuid(), group2.getUuid());
    insertQualityGateGroupPermission(qualityGateDto.getUuid(), group3.getUuid());

    assertThat(underTest.selectByQuery(dbSession, builder()
        .setQualityGate(qualityGateDto)
        .setMembership(IN)
        .setQuery("project").build(),
      Pagination.all()))
      .extracting(SearchGroupMembershipDto::getGroupUuid)
      .containsExactlyInAnyOrder(group1.getUuid());

    assertThat(underTest.selectByQuery(dbSession, builder()
        .setQualityGate(qualityGateDto)
        .setMembership(IN)
        .setQuery("UserS").build(),
      Pagination.all()))
      .extracting(SearchGroupMembershipDto::getGroupUuid)
      .containsExactlyInAnyOrder(group1.getUuid(), group2.getUuid());
  }

  @Test
  public void selectByQuery_with_paging() {
    QualityGateDto qualityGateDto = insertQualityGate();
    GroupDto group1 = dbTester.users().insertGroup("group1");
    GroupDto group2 = dbTester.users().insertGroup("group2");
    GroupDto group3 = dbTester.users().insertGroup("group3");

    insertQualityGateGroupPermission(qualityGateDto.getUuid(), group1.getUuid());
    insertQualityGateGroupPermission(qualityGateDto.getUuid(), group2.getUuid());

    assertThat(underTest.selectByQuery(dbSession, builder()
        .setQualityGate(qualityGateDto)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(1)))
      .extracting(SearchGroupMembershipDto::getGroupUuid)
      .containsExactly(group1.getUuid());

    assertThat(underTest.selectByQuery(dbSession, builder()
        .setQualityGate(qualityGateDto)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(3).andSize(1)))
      .extracting(SearchGroupMembershipDto::getGroupUuid)
      .containsExactly(group3.getUuid());

    assertThat(underTest.selectByQuery(dbSession, builder()
        .setQualityGate(qualityGateDto)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(10)))
      .extracting(SearchGroupMembershipDto::getGroupUuid)
      .containsExactly(group1.getUuid(), group2.getUuid(), group3.getUuid());
  }

  private QualityGateDto insertQualityGate() {
    QualityGateDto qg = new QualityGateDto()
      .setUuid(randomAlphabetic(5))
      .setName(randomAlphabetic(5));
    dbTester.getDbClient().qualityGateDao().insert(dbTester.getSession(), qg);
    dbTester.commit();
    return qg;
  }

  private QualityGateGroupPermissionsDto insertQualityGateGroupPermission(String qualityGateUuid, String groupUuid) {
    QualityGateGroupPermissionsDto qgg = new QualityGateGroupPermissionsDto()
      .setUuid(Uuids.create())
      .setQualityGateUuid(qualityGateUuid)
      .setGroupUuid(groupUuid)
      .setCreatedAt(new Date());
    underTest.insert(dbTester.getSession(), qgg);
    dbTester.commit();
    return qgg;
  }
}
