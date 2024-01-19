/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.GroupEditorNewValue;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.SearchGroupMembershipDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.ANY;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.IN;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.OUT;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.builder;

public class QProfileEditGroupsDaoIT {

  private static final long NOW = 10_000_000_000L;

  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final ArgumentCaptor<GroupEditorNewValue> newValueCaptor = ArgumentCaptor.forClass(GroupEditorNewValue.class);

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2, auditPersister);

  private QProfileEditGroupsDao underTest = db.getDbClient().qProfileEditGroupsDao();

  @Test
  public void exists() {
    QProfileDto profile = db.qualityProfiles().insert();
    QProfileDto anotherProfile = db.qualityProfiles().insert();
    GroupDto group = db.users().insertGroup();
    GroupDto anotherGroup = db.users().insertGroup();
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
    QProfileDto profile = db.qualityProfiles().insert();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setProfile(profile)
      .setMembership(ANY).build()))
      .isEqualTo(3);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setProfile(profile)
      .setMembership(IN).build()))
      .isEqualTo(2);

    assertThat(underTest.countByQuery(db.getSession(), builder()
      .setProfile(profile)
      .setMembership(OUT).build()))
      .isOne();
  }

  @Test
  public void selectByQuery() {
    QProfileDto profile = db.qualityProfiles().insert();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    GroupDto group3 = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
      .setProfile(profile)
      .setMembership(ANY).build(), Pagination.all()))
      .extracting(SearchGroupMembershipDto::getGroupUuid, SearchGroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(
        tuple(group1.getUuid(), true),
        tuple(group2.getUuid(), true),
        tuple(group3.getUuid(), false));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(IN).build(),
      Pagination.all()))
      .extracting(SearchGroupMembershipDto::getGroupUuid, SearchGroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(group1.getUuid(), true), tuple(group2.getUuid(), true));

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(OUT).build(),
      Pagination.all()))
      .extracting(SearchGroupMembershipDto::getGroupUuid, SearchGroupMembershipDto::isSelected)
      .containsExactlyInAnyOrder(tuple(group3.getUuid(), false));
  }

  @Test
  public void selectByQuery_search_by_name() {
    QProfileDto profile = db.qualityProfiles().insert();
    GroupDto group1 = db.users().insertGroup("sonar-users-project");
    GroupDto group2 = db.users().insertGroup("sonar-users-qprofile");
    GroupDto group3 = db.users().insertGroup("sonar-admin");
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);
    db.qualityProfiles().addGroupPermission(profile, group3);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(IN)
        .setQuery("project").build(),
      Pagination.all()))
      .extracting(SearchGroupMembershipDto::getGroupUuid)
      .containsExactlyInAnyOrder(group1.getUuid());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(IN)
        .setQuery("UserS").build(),
      Pagination.all()))
      .extracting(SearchGroupMembershipDto::getGroupUuid)
      .containsExactlyInAnyOrder(group1.getUuid(), group2.getUuid());
  }

  @Test
  public void selectByQuery_with_paging() {
    QProfileDto profile = db.qualityProfiles().insert();
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    GroupDto group3 = db.users().insertGroup("group3");
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(1)))
      .extracting(SearchGroupMembershipDto::getGroupUuid)
      .containsExactly(group1.getUuid());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(3).andSize(1)))
      .extracting(SearchGroupMembershipDto::getGroupUuid)
      .containsExactly(group3.getUuid());

    assertThat(underTest.selectByQuery(db.getSession(), builder()
        .setProfile(profile)
        .setMembership(ANY)
        .build(),
      Pagination.forPage(1).andSize(10)))
      .extracting(SearchGroupMembershipDto::getGroupUuid)
      .containsExactly(group1.getUuid(), group2.getUuid(), group3.getUuid());
  }

  @Test
  public void selectQProfileUuidsByGroups() {
    QProfileDto profile1 = db.qualityProfiles().insert();
    QProfileDto profile2 = db.qualityProfiles().insert();
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    GroupDto group3 = db.users().insertGroup("group3");
    db.qualityProfiles().addGroupPermission(profile1, group1);
    db.qualityProfiles().addGroupPermission(profile1, group2);
    db.qualityProfiles().addGroupPermission(profile2, group2);

    assertThat(underTest.selectQProfileUuidsByGroups(db.getSession(), asList(group1, group2)))
      .containsExactlyInAnyOrder(profile1.getKee(), profile2.getKee());
    assertThat(underTest.selectQProfileUuidsByGroups(db.getSession(), asList(group1, group2, group3)))
      .containsExactlyInAnyOrder(profile1.getKee(), profile2.getKee());
    assertThat(underTest.selectQProfileUuidsByGroups(db.getSession(), emptyList())).isEmpty();
  }

  @Test
  public void insert() {
    String qualityProfileName = "QPROFILE_NAME";
    String qualityProfileKee = "QPROFILE";
    String groupUuid = "100";
    String groupName = "GROUP_NAME";
    underTest.insert(db.getSession(), new QProfileEditGroupsDto()
        .setUuid("ABCD")
        .setGroupUuid(groupUuid)
        .setQProfileUuid(qualityProfileKee),
      qualityProfileName,
      groupName
    );

    verify(auditPersister).addQualityProfileEditor(eq(db.getSession()), newValueCaptor.capture());

    GroupEditorNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupEditorNewValue::getQualityProfileName, GroupEditorNewValue::getQualityProfileUuid,
        GroupEditorNewValue::getGroupName, GroupEditorNewValue::getGroupUuid)
      .containsExactly(qualityProfileName, qualityProfileKee, groupName, groupUuid);
    assertThat(newValue.toString()).contains("\"qualityProfileName\"").contains("\"groupName\"");

    assertThat(db.selectFirst(db.getSession(),
      "select uuid as \"uuid\", group_uuid as \"groupUuid\", qprofile_uuid as \"qProfileUuid\", created_at as \"createdAt\" from qprofile_edit_groups")).contains(
      entry("uuid", "ABCD"),
      entry("groupUuid", groupUuid),
      entry("qProfileUuid", qualityProfileKee),
      entry("createdAt", NOW));
  }

  @Test
  public void deleteByQProfileAndGroup() {
    QProfileDto profile = db.qualityProfiles().insert();
    GroupDto group = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group);
    assertThat(underTest.exists(db.getSession(), profile, group)).isTrue();

    underTest.deleteByQProfileAndGroup(db.getSession(), profile, group);

    verify(auditPersister).deleteQualityProfileEditor(eq(db.getSession()), newValueCaptor.capture());

    GroupEditorNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupEditorNewValue::getQualityProfileName, GroupEditorNewValue::getQualityProfileName,
        GroupEditorNewValue::getGroupName, GroupEditorNewValue::getGroupUuid)
      .containsExactly(profile.getName(), profile.getKee(), group.getName(), group.getUuid());
    assertThat(newValue.toString()).contains("\"qualityProfileName\"").contains("\"groupName\"");

    assertThat(underTest.exists(db.getSession(), profile, group)).isFalse();
  }

  @Test
  public void deleteByQProfiles() {
    QProfileDto profile1 = db.qualityProfiles().insert();
    QProfileDto profile2 = db.qualityProfiles().insert();
    QProfileDto profile3 = db.qualityProfiles().insert();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile1, group1);
    db.qualityProfiles().addGroupPermission(profile2, group2);
    db.qualityProfiles().addGroupPermission(profile3, group1);

    underTest.deleteByQProfiles(db.getSession(), asList(profile1, profile2));

    verify(auditPersister, times(2)).deleteQualityProfileEditor(eq(db.getSession()), newValueCaptor.capture());

    Map<String, GroupEditorNewValue> newValues = newValueCaptor.getAllValues().stream()
      .collect(toMap(GroupEditorNewValue::getQualityProfileName, identity()));

    assertThat(newValues.get(profile1.getName()))
      .extracting(GroupEditorNewValue::getQualityProfileName, GroupEditorNewValue::getQualityProfileUuid,
        GroupEditorNewValue::getGroupName, GroupEditorNewValue::getGroupUuid)
      .containsExactly(profile1.getName(), profile1.getKee(), null, null);
    assertThat(newValues.get(profile1.getName()).toString()).contains("\"qualityProfileName\"").doesNotContain("\"groupName\"");
    assertThat(newValues.get(profile2.getName()))
      .extracting(GroupEditorNewValue::getQualityProfileName, GroupEditorNewValue::getQualityProfileUuid,
        GroupEditorNewValue::getGroupName, GroupEditorNewValue::getGroupUuid)
      .containsExactly(profile2.getName(), profile2.getKee(), null, null);
    assertThat(newValues.get(profile2.getName()).toString()).contains("\"qualityProfileName\"").doesNotContain("\"groupName\"");

    assertThat(underTest.exists(db.getSession(), profile1, group1)).isFalse();
    assertThat(underTest.exists(db.getSession(), profile2, group2)).isFalse();
    assertThat(underTest.exists(db.getSession(), profile3, group1)).isTrue();
  }

  @Test
  public void deleteByGroup() {
    QProfileDto profile1 = db.qualityProfiles().insert();
    QProfileDto profile2 = db.qualityProfiles().insert();
    QProfileDto profile3 = db.qualityProfiles().insert();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile1, group1);
    db.qualityProfiles().addGroupPermission(profile2, group2);
    db.qualityProfiles().addGroupPermission(profile3, group1);

    underTest.deleteByGroup(db.getSession(), group1);

    verify(auditPersister).deleteQualityProfileEditor(eq(db.getSession()), newValueCaptor.capture());

    GroupEditorNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupEditorNewValue::getQualityProfileName, GroupEditorNewValue::getQualityProfileName,
        GroupEditorNewValue::getGroupName, GroupEditorNewValue::getGroupUuid)
      .containsExactly(null, null, group1.getName(), group1.getUuid());
    assertThat(newValue.toString()).doesNotContain("\"qualityProfileName\"").contains("\"groupName\"");

    assertThat(underTest.exists(db.getSession(), profile1, group1)).isFalse();
    assertThat(underTest.exists(db.getSession(), profile2, group2)).isTrue();
    assertThat(underTest.exists(db.getSession(), profile3, group1)).isFalse();
  }

}
