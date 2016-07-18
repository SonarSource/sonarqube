/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.permission.template;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GroupWithPermissionDto;
import org.sonar.db.permission.OldPermissionQuery;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.GroupDbTester;
import org.sonar.db.user.GroupDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.db.permission.PermissionQuery.builder;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class GroupWithPermissionTemplateDaoTest {

  private static final long TEMPLATE_ID = 50L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbSession session = dbTester.getSession();

  GroupDbTester groupDb = new GroupDbTester(dbTester);
  PermissionTemplateDbTester permissionTemplateDbTester = new PermissionTemplateDbTester(dbTester);

  PermissionTemplateDao underTest = dbTester.getDbClient().permissionTemplateDao();

  @Test
  public void select_groups() {
    dbTester.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    OldPermissionQuery query = OldPermissionQuery.builder().permission("user").build();
    List<GroupWithPermissionDto> result = underTest.selectGroups(session, query, TEMPLATE_ID);
    int count = underTest.countGroups(session, query, TEMPLATE_ID);

    assertThat(result).hasSize(4);
    assertThat(count).isEqualTo(4);

    GroupWithPermissionDto anyone = result.get(0);
    assertThat(anyone.getName()).isEqualTo("Anyone");
    assertThat(anyone.getDescription()).isNull();
    assertThat(anyone.getPermission()).isNotNull();

    GroupWithPermissionDto group1 = result.get(1);
    assertThat(group1.getName()).isEqualTo("sonar-administrators");
    assertThat(group1.getDescription()).isEqualTo("System administrators");
    assertThat(group1.getPermission()).isNotNull();

    GroupWithPermissionDto group2 = result.get(2);
    assertThat(group2.getName()).isEqualTo("sonar-reviewers");
    assertThat(group2.getDescription()).isEqualTo("Reviewers");
    assertThat(group2.getPermission()).isNull();

    GroupWithPermissionDto group3 = result.get(3);
    assertThat(group3.getName()).isEqualTo("sonar-users");
    assertThat(group3.getDescription()).isEqualTo("Any new users created will automatically join this group");
    assertThat(group3.getPermission()).isNotNull();
  }

  @Test
  public void anyone_group_is_returned_when_it_has_no_permission() {
    dbTester.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    // Anyone group is returned even if it doesn't have the permission
    OldPermissionQuery query = OldPermissionQuery.builder().permission(USER).build();
    List<GroupWithPermissionDto> result = underTest.selectGroups(session, query, TEMPLATE_ID);
    assertThat(result).hasSize(4);

    GroupWithPermissionDto group1 = result.get(1);
    assertThat(group1.getName()).isEqualTo("sonar-administrators");
    assertThat(group1.getPermission()).isNotNull();

    GroupWithPermissionDto group2 = result.get(2);
    assertThat(group2.getName()).isEqualTo("sonar-reviewers");
    assertThat(group2.getPermission()).isNull();

    GroupWithPermissionDto group3 = result.get(3);
    assertThat(group3.getName()).isEqualTo("sonar-users");
    assertThat(group3.getPermission()).isNotNull();
  }

  @Test
  public void search_by_groups_name() {
    dbTester.prepareDbUnit(getClass(), "groups_with_permissions.xml");

    List<GroupWithPermissionDto> result = underTest.selectGroups(session, OldPermissionQuery.builder().permission("user").search("aDMini").build(), TEMPLATE_ID);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("sonar-administrators");

    result = underTest.selectGroups(session, OldPermissionQuery.builder().permission("user").search("sonar").build(), TEMPLATE_ID);
    assertThat(result).hasSize(3);
  }

  @Test
  public void search_groups_should_be_sorted_by_group_name() {
    dbTester.prepareDbUnit(getClass(), "groups_with_permissions_should_be_sorted_by_group_name.xml");

    List<GroupWithPermissionDto> result = underTest.selectGroups(session, OldPermissionQuery.builder().permission("user").build(), TEMPLATE_ID);
    assertThat(result).hasSize(4);
    assertThat(result.get(0).getName()).isEqualTo("Anyone");
    assertThat(result.get(1).getName()).isEqualTo("sonar-administrators");
    assertThat(result.get(2).getName()).isEqualTo("sonar-reviewers");
    assertThat(result.get(3).getName()).isEqualTo("sonar-users");
  }

  @Test
  public void select_group_names_by_query_and_template() {
    GroupDto group1 = groupDb.insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = groupDb.insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = groupDb.insertGroup(newGroupDto().setName("Group-3"));

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), USER);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), ADMIN);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group2.getId(), PROVISIONING);

    PermissionTemplateDto anotherTemplate = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), null, USER);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), group1.getId(), PROVISIONING);

    assertThat(selectGroupNamesByQueryAndTemplate(builder().build(), template.getId())).containsOnly("Group-1", "Group-2", "Group-3", "Anyone");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().withPermissionOnly().build(), template.getId())).containsOnly("Group-1", "Group-2");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPermission(USER).build(), template.getId())).containsOnly("Group-1");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPermission(USER).build(), anotherTemplate.getId())).containsOnly("Anyone");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("groU").build(), template.getId())).containsOnly("Group-1", "Group-2", "Group-3");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("nYo").build(), template.getId())).containsOnly("Anyone");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("p-2").build(), template.getId())).containsOnly("Group-2");

    assertThat(selectGroupNamesByQueryAndTemplate(builder().withPermissionOnly().build(), 123L)).isEmpty();
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("unknown").build(), template.getId())).isEmpty();
  }

  @Test
  public void select_group_names_by_query_and_template_is_ordered_by_group_names() {
    GroupDto group2 = groupDb.insertGroup(newGroupDto().setName("Group-2"));
    groupDb.insertGroup(newGroupDto().setName("Group-3"));
    groupDb.insertGroup(newGroupDto().setName("Group-1"));

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group2.getId(), USER);

    assertThat(selectGroupNamesByQueryAndTemplate(builder().build(), template.getId())).containsExactly("Anyone", "Group-1", "Group-2", "Group-3");
  }

  @Test
  public void select_group_names_by_query_and_template_is_paginated() {
    IntStream.rangeClosed(0, 9).forEach(i -> groupDb.insertGroup(newGroupDto().setName(i + "-name")));

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();

    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPageIndex(1).setPageSize(1).build(), template.getId())).containsExactly("0-name");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPageIndex(2).setPageSize(3).build(), template.getId())).containsExactly("3-name", "4-name", "5-name");
  }

  @Test
  public void select_group_names_by_query_and_template_returns_anyone() {
    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();

    GroupDto group = groupDb.insertGroup(newGroupDto().setName("Group"));
    PermissionTemplateDto otherTemplate = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(otherTemplate.getId(), group.getId(), USER);

    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("nyo").build(), template.getId())).containsExactly("Anyone");
  }

  @Test
  public void count_group_names_by_query_and_template() {
    GroupDto group1 = groupDb.insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = groupDb.insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = groupDb.insertGroup(newGroupDto().setName("Group-3"));

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), USER);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), ADMIN);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group2.getId(), PROVISIONING);

    PermissionTemplateDto anotherTemplate = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), null, USER);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), group1.getId(), PROVISIONING);

    assertThat(countGroupNamesByQueryAndTemplate(builder().build(), template.getId())).isEqualTo(4);
    assertThat(countGroupNamesByQueryAndTemplate(builder().withPermissionOnly().build(), template.getId())).isEqualTo(2);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setPermission(USER).build(), template.getId())).isEqualTo(1);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setPermission(USER).build(), anotherTemplate.getId())).isEqualTo(1);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setSearchQuery("groU").build(), template.getId())).isEqualTo(3);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setSearchQuery("nYo").build(), template.getId())).isEqualTo(1);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setSearchQuery("p-2").build(), template.getId())).isEqualTo(1);

    assertThat(countGroupNamesByQueryAndTemplate(builder().withPermissionOnly().build(), 123L)).isZero();
    assertThat(countGroupNamesByQueryAndTemplate(builder().setSearchQuery("unknown").build(), template.getId())).isZero();
  }

  @Test
  public void select_group_permissions_by_template_id_and_group_names() {
    GroupDto group1 = groupDb.insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = groupDb.insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = groupDb.insertGroup(newGroupDto().setName("Group-3"));

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), USER);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), ADMIN);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group2.getId(), PROVISIONING);

    PermissionTemplateDto anotherTemplate = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), null, USER);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), group1.getId(), PROVISIONING);

    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, template.getId(), asList("Group-1")))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getGroupName, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(group1.getId(), "Group-1", USER),
        tuple(group1.getId(), "Group-1", ADMIN));

    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, anotherTemplate.getId(), asList("Group-1")))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getGroupName, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(group1.getId(), "Group-1", PROVISIONING));

    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, anotherTemplate.getId(), asList("Anyone")))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getGroupName, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(0L, "Anyone", USER));

    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, template.getId(), asList("Group-1", "Group-2", "Anyone"))).hasSize(3);
    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, template.getId(), asList("Unknown"))).isEmpty();
    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, template.getId(), Collections.emptyList())).isEmpty();
  }

  @Test
  public void select_group_permissions_by_template_id() {
    GroupDto group1 = groupDb.insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = groupDb.insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = groupDb.insertGroup(newGroupDto().setName("Group-3"));

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), USER);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), ADMIN);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group2.getId(), PROVISIONING);

    PermissionTemplateDto anotherTemplate = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), null, USER);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), group1.getId(), PROVISIONING);

    assertThat(underTest.selectGroupPermissionsByTemplateId(session, template.getId()))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getGroupName, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(group1.getId(), "Group-1", USER),
        tuple(group1.getId(), "Group-1", ADMIN),
        tuple(group2.getId(), "Group-2", PROVISIONING));
    assertThat(underTest.selectGroupPermissionsByTemplateId(session, anotherTemplate.getId()))
      .extracting(PermissionTemplateGroupDto::getGroupId, PermissionTemplateGroupDto::getGroupName, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(group1.getId(), "Group-1", PROVISIONING),
        tuple(0L, "Anyone", USER));

    assertThat(underTest.selectGroupPermissionsByTemplateId(session, 321L)).isEmpty();
  }

  private List<String> selectGroupNamesByQueryAndTemplate(PermissionQuery query, long templateId) {
    return underTest.selectGroupNamesByQueryAndTemplate(session, query, templateId);
  }

  private int countGroupNamesByQueryAndTemplate(PermissionQuery query, long templateId) {
    return underTest.countGroupNamesByQueryAndTemplate(session, query, templateId);
  }

}
