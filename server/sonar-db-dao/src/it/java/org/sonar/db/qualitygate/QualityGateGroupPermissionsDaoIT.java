/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.GroupEditorNewValue;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.SearchGroupMembershipDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.qualitygate.SearchQualityGatePermissionQuery.builder;
import static org.sonar.db.user.SearchPermissionQuery.ANY;
import static org.sonar.db.user.SearchPermissionQuery.IN;
import static org.sonar.db.user.SearchPermissionQuery.OUT;

class QualityGateGroupPermissionsDaoIT {
  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final ArgumentCaptor<GroupEditorNewValue> newValueCaptor = ArgumentCaptor.forClass(GroupEditorNewValue.class);

  @RegisterExtension
  private final DbTester dbTester = DbTester.create(System2.INSTANCE, auditPersister);

  private final DbSession dbSession = dbTester.getSession();
  private final QualityGateGroupPermissionsDao underTest = dbTester.getDbClient().qualityGateGroupPermissionsDao();

  @Test
  void itInsertsAndExistsReturnsTrue() {
    GroupDto group = GroupTesting.newGroupDto();
    QualityGateDto qualityGateDto = insertQualityGate();
    QualityGateGroupPermissionsDto qualityGateGroupPermission = insertQualityGateGroupPermission(qualityGateDto.getUuid(),
      qualityGateDto.getName(), group.getUuid(), group.getName());

    verify(auditPersister).addQualityGateEditor(eq(dbSession), newValueCaptor.capture());

    GroupEditorNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupEditorNewValue::getQualityGateName, GroupEditorNewValue::getQualityGateUuid,
        GroupEditorNewValue::getGroupName, GroupEditorNewValue::getGroupUuid)
      .containsExactly(qualityGateDto.getName(), qualityGateDto.getUuid(), group.getName(), group.getUuid());
    assertThat(newValue.toString()).contains("\"qualityGateName\"").contains("\"groupName\"");

    assertThat(qualityGateGroupPermission.getUuid()).isNotNull();
    assertThat(underTest.exists(dbSession, qualityGateDto, group)).isTrue();
    assertThat(underTest.exists(dbSession, qualityGateDto.getUuid(), group.getUuid())).isTrue();
  }

  @Test
  void existsReturnsTrueForListOfGroups() {
    GroupDto group1 = GroupTesting.newGroupDto();
    GroupDto group2 = GroupTesting.newGroupDto();
    QualityGateDto qualityGateDto = insertQualityGate();
    QualityGateGroupPermissionsDto qualityGateGroupPermission = insertQualityGateGroupPermission(qualityGateDto.getUuid(),
      qualityGateDto.getName(),
      group1.getUuid(), group1.getName());

    assertThat(qualityGateGroupPermission.getUuid()).isNotNull();
    assertThat(underTest.exists(dbSession, qualityGateDto, List.of(group1, group2))).isTrue();
    assertThat(underTest.exists(dbSession, qualityGateDto.getUuid(), group1.getUuid())).isTrue();
    assertThat(underTest.exists(dbSession, qualityGateDto.getUuid(), group2.getUuid())).isFalse();
  }

  @Test
  void existsReturnsFalseWhenQGEditGroupsDoesNotExist() {
    assertThat(underTest.exists(dbSession, secure().nextAlphabetic(5), secure().nextAlphabetic(5))).isFalse();
  }

  @Test
  void countByQuery() {
    QualityGateDto qualityGateDto = insertQualityGate();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    GroupDto group3 = dbTester.users().insertGroup();

    insertQualityGateGroupPermission(qualityGateDto.getUuid(), qualityGateDto.getName(), group1.getUuid(), group1.getName());
    insertQualityGateGroupPermission(qualityGateDto.getUuid(), qualityGateDto.getName(), group2.getUuid(), group2.getName());

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
      .isOne();
  }

  @Test
  void selectByQuery() {
    QualityGateDto qualityGateDto = insertQualityGate();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    GroupDto group3 = dbTester.users().insertGroup();

    insertQualityGateGroupPermission(qualityGateDto.getUuid(), qualityGateDto.getName(), group1.getUuid(), group1.getName());
    insertQualityGateGroupPermission(qualityGateDto.getUuid(), qualityGateDto.getName(), group2.getUuid(), group2.getName());

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
  void selectByQuery_search_by_name() {
    QualityGateDto qualityGateDto = insertQualityGate();
    GroupDto group1 = dbTester.users().insertGroup("sonar-users-project");
    GroupDto group2 = dbTester.users().insertGroup("sonar-users-qprofile");
    GroupDto group3 = dbTester.users().insertGroup("sonar-admin");

    insertQualityGateGroupPermission(qualityGateDto.getUuid(), qualityGateDto.getName(), group1.getUuid(), group1.getName());
    insertQualityGateGroupPermission(qualityGateDto.getUuid(), qualityGateDto.getName(), group2.getUuid(), group2.getName());
    insertQualityGateGroupPermission(qualityGateDto.getUuid(), qualityGateDto.getName(), group3.getUuid(), group3.getName());

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
  void selectByQuery_with_paging() {
    QualityGateDto qualityGateDto = insertQualityGate();
    GroupDto group1 = dbTester.users().insertGroup("group1");
    GroupDto group2 = dbTester.users().insertGroup("group2");
    GroupDto group3 = dbTester.users().insertGroup("group3");

    insertQualityGateGroupPermission(qualityGateDto.getUuid(), qualityGateDto.getName(), group1.getUuid(), group1.getName());
    insertQualityGateGroupPermission(qualityGateDto.getUuid(), qualityGateDto.getName(), group2.getUuid(), group2.getName());

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

  @Test
  void deleteByGroup() {
    QualityGateDto qualityGateDto1 = insertQualityGate();
    QualityGateDto qualityGateDto2 = insertQualityGate();
    QualityGateDto qualityGateDto3 = insertQualityGate();

    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    dbTester.qualityGates().addGroupPermission(qualityGateDto1, group1);
    dbTester.qualityGates().addGroupPermission(qualityGateDto2, group2);
    dbTester.qualityGates().addGroupPermission(qualityGateDto3, group1);

    underTest.deleteByGroup(dbSession, group1);

    verify(auditPersister).deleteQualityGateEditor(eq(dbSession), newValueCaptor.capture());

    GroupEditorNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupEditorNewValue::getQualityGateName, GroupEditorNewValue::getQualityGateUuid,
        GroupEditorNewValue::getGroupName, GroupEditorNewValue::getGroupUuid)
      .containsExactly(null, null, group1.getName(), group1.getUuid());
    assertThat(newValue.toString()).doesNotContain("\"qualityGateName\"").contains("\"groupName\"");

    assertThat(underTest.exists(dbSession, qualityGateDto1, group1)).isFalse();
    assertThat(underTest.exists(dbSession, qualityGateDto2, group2)).isTrue();
    assertThat(underTest.exists(dbSession, qualityGateDto3, group1)).isFalse();
  }

  @Test
  void deleteByQProfileAndGroup() {
    QualityGateDto qualityGateDto = insertQualityGate();
    GroupDto group = dbTester.users().insertGroup();
    insertQualityGateGroupPermission(qualityGateDto.getUuid(), qualityGateDto.getName(), group.getUuid(), group.getName());

    assertThat(underTest.exists(dbSession, qualityGateDto, group)).isTrue();

    underTest.deleteByQualityGateAndGroup(dbSession, qualityGateDto, group);

    verify(auditPersister).deleteQualityGateEditor(eq(dbSession), newValueCaptor.capture());

    GroupEditorNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupEditorNewValue::getQualityGateName, GroupEditorNewValue::getQualityGateUuid,
        GroupEditorNewValue::getGroupName, GroupEditorNewValue::getGroupUuid)
      .containsExactly(qualityGateDto.getName(), qualityGateDto.getUuid(), group.getName(), group.getUuid());
    assertThat(newValue.toString()).contains("\"qualityGateName\"").contains("\"groupName\"");

    assertThat(underTest.exists(dbSession, qualityGateDto, group)).isFalse();
  }

  @Test
  void deleteByQualityGate() {
    QualityGateDto qualityGateDto1 = insertQualityGate();
    QualityGateDto qualityGateDto2 = insertQualityGate();
    QualityGateDto qualityGateDto3 = insertQualityGate();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    dbTester.qualityGates().addGroupPermission(qualityGateDto1, group1);
    dbTester.qualityGates().addGroupPermission(qualityGateDto2, group2);
    dbTester.qualityGates().addGroupPermission(qualityGateDto3, group1);

    underTest.deleteByQualityGate(dbSession, qualityGateDto1);

    verify(auditPersister).deleteQualityGateEditor(eq(dbSession), newValueCaptor.capture());

    GroupEditorNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupEditorNewValue::getQualityGateName, GroupEditorNewValue::getQualityGateUuid,
        GroupEditorNewValue::getGroupName, GroupEditorNewValue::getGroupUuid)
      .containsExactly(qualityGateDto1.getName(), qualityGateDto1.getUuid(), null, null);
    assertThat(newValue.toString()).contains("\"qualityGateName\"").doesNotContain("\"groupName\"");

    assertThat(underTest.exists(dbSession, qualityGateDto1, group1)).isFalse();
    assertThat(underTest.exists(dbSession, qualityGateDto2, group2)).isTrue();
    assertThat(underTest.exists(dbSession, qualityGateDto3, group1)).isTrue();
  }

  private QualityGateDto insertQualityGate() {
    QualityGateDto qg = new QualityGateDto()
      .setUuid(secure().nextAlphabetic(5))
      .setName(secure().nextAlphabetic(5));
    dbTester.getDbClient().qualityGateDao().insert(dbTester.getSession(), qg);
    dbTester.commit();
    return qg;
  }

  private QualityGateGroupPermissionsDto insertQualityGateGroupPermission(String qualityGateUuid, String qualityGateName,
    String groupUuid, String groupName) {
    QualityGateGroupPermissionsDto qgg = new QualityGateGroupPermissionsDto()
      .setUuid(Uuids.create())
      .setQualityGateUuid(qualityGateUuid)
      .setGroupUuid(groupUuid)
      .setCreatedAt(new Date());
    underTest.insert(dbTester.getSession(), qgg, qualityGateName, groupName);
    dbTester.commit();
    return qgg;
  }
}
