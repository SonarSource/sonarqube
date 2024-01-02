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
package org.sonar.db.permission.template;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.GroupDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;
import static org.sonar.db.permission.PermissionQuery.builder;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class GroupWithPermissionTemplateDaoIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession session = db.getSession();
  private PermissionTemplateDbTester permissionTemplateDbTester = db.permissionTemplates();
  private PermissionTemplateDao underTest = db.getDbClient().permissionTemplateDao();

  @Test
  public void select_group_names_by_query_and_template() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    GroupDto group2 = db.users().insertGroup("Group-2");
    GroupDto group3 = db.users().insertGroup("Group-3");

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group1.getUuid(), USER, template.getName(), group1.getName());
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group1.getUuid(), ADMIN, template.getName(), group1.getName());
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group2.getUuid(), PROVISION_PROJECTS.getKey(), template.getName(), group2.getName());

    PermissionTemplateDto anotherTemplate = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getUuid(), null, USER, anotherTemplate.getName(), null);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getUuid(), group1.getUuid(), PROVISION_PROJECTS.getKey(), anotherTemplate.getName(), group1.getName());

    assertThat(selectGroupNamesByQueryAndTemplate(builder(), template))
      .containsOnly("Group-1", "Group-2", "Group-3", "Anyone");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().withAtLeastOnePermission(), template))
      .containsOnly("Group-1", "Group-2");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPermission(USER), template))
      .containsOnly("Group-1");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPermission(USER), anotherTemplate))
      .containsOnly("Anyone");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("groU"), template))
      .containsOnly("Group-1", "Group-2", "Group-3");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("nYo"), template))
      .containsOnly("Anyone");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("p-2"), template))
      .containsOnly("Group-2");

    assertThat(selectGroupNamesByQueryAndTemplate(builder().withAtLeastOnePermission().build(), "123"))
      .isEmpty();
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("unknown"), template))
      .isEmpty();
  }

  @Test
  public void selectGroupNamesByQueryAndTemplate_is_ordering_results_by_groups_with_permission_then_by_name() {
    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    GroupDto group1 = db.users().insertGroup("A");
    GroupDto group2 = db.users().insertGroup("B");
    GroupDto group3 = db.users().insertGroup("C");

    permissionTemplateDbTester.addGroupToTemplate(template, group3, UserRole.USER);

    PermissionQuery query = PermissionQuery.builder().build();
    assertThat(underTest.selectGroupNamesByQueryAndTemplate(db.getSession(), query, template.getUuid()))
      .containsExactly("Anyone", group3.getName(), group1.getName(), group2.getName());
  }

  @Test
  public void selectGroupNamesByQueryAndTemplate_is_order_by_groups_with_permission_then_by_name_when_many_groups() {
    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> db.users().insertGroup("Group-" + i));

    String lastGroupName = "Group-" + (DEFAULT_PAGE_SIZE + 1);
    permissionTemplateDbTester.addGroupToTemplate(template, db.users().selectGroup(lastGroupName).get(), UserRole.USER);

    PermissionQuery query = PermissionQuery.builder().build();
    assertThat(underTest.selectGroupNamesByQueryAndTemplate(db.getSession(), query, template.getUuid()))
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith("Anyone", lastGroupName, "Group-1");
  }

  @Test
  public void selectGroupNamesByQueryAndTemplate_ignores_other_template_and_is_ordered_by_groups_with_permission_then_by_name_when_many_groups() {
    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    PermissionTemplateDto otherTemplate = permissionTemplateDbTester.insertTemplate();
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      GroupDto group = db.users().insertGroup("Group-" + i);
      permissionTemplateDbTester.addGroupToTemplate(otherTemplate, group, UserRole.USER);
    });

    String lastGroupName = "Group-" + (DEFAULT_PAGE_SIZE + 1);
    permissionTemplateDbTester.addGroupToTemplate(template, db.users().selectGroup(lastGroupName).get(), UserRole.USER);

    PermissionQuery query = PermissionQuery.builder().build();
    assertThat(underTest.selectGroupNamesByQueryAndTemplate(db.getSession(), query, template.getUuid()))
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith("Anyone", lastGroupName, "Group-1");
  }

  @Test
  public void select_group_names_by_query_and_template_is_paginated() {
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertGroup(i + "-name"));

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();

    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPageIndex(1).setPageSize(1), template))
      .containsExactly("0-name");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPageIndex(2).setPageSize(3), template))
      .containsExactly("3-name", "4-name", "5-name");
  }

  @Test
  public void select_group_names_by_query_and_template_returns_anyone() {
    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();

    GroupDto group = db.users().insertGroup("Group");
    PermissionTemplateDto otherTemplate = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(otherTemplate.getUuid(), group.getUuid(), USER, otherTemplate.getName(), group.getName());

    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("nyo"), template))
      .containsExactly("Anyone");
  }

  @Test
  public void count_group_names_by_query_and_template() {
    GroupDto group1 = db.users().insertGroup("Group-1");
    GroupDto group2 = db.users().insertGroup("Group-2");
    GroupDto group3 = db.users().insertGroup("Group-3");

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group1.getUuid(), USER, template.getName(), group1.getName());
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group1.getUuid(), ADMIN, template.getName(), group1.getName());
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group2.getUuid(), PROVISION_PROJECTS.getKey(), template.getName(), group2.getName());

    PermissionTemplateDto anotherTemplate = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getUuid(), null, USER, anotherTemplate.getName(), null);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getUuid(), group1.getUuid(), PROVISION_PROJECTS.getKey(), anotherTemplate.getName(), group1.getName());

    assertThat(countGroupNamesByQueryAndTemplate(builder(), template))
      .isEqualTo(4);
    assertThat(countGroupNamesByQueryAndTemplate(builder().withAtLeastOnePermission(), template))
      .isEqualTo(2);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setPermission(USER), template)).isOne();
    assertThat(countGroupNamesByQueryAndTemplate(builder().setPermission(USER), anotherTemplate))
      .isOne();
    assertThat(countGroupNamesByQueryAndTemplate(builder().setSearchQuery("groU"), template))
      .isEqualTo(3);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setSearchQuery("nYo"), template))
      .isOne();
    assertThat(countGroupNamesByQueryAndTemplate(builder().setSearchQuery("p-2"), template))
      .isOne();

    assertThat(countGroupNamesByQueryAndTemplate(builder().withAtLeastOnePermission().build(), "123"))
      .isZero();
    assertThat(countGroupNamesByQueryAndTemplate(builder().setSearchQuery("unknown"), template))
      .isZero();
  }

  @Test
  public void select_group_permissions_by_template_id_and_group_names() {
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = db.users().insertGroup(newGroupDto().setName("Group-3"));

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group1.getUuid(), USER, template.getName(), group1.getName());
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group1.getUuid(), ADMIN, template.getName(), group1.getName());
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group2.getUuid(), PROVISION_PROJECTS.getKey(), template.getName(), group2.getName());

    PermissionTemplateDto anotherTemplate = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getUuid(), null, USER, anotherTemplate.getName(), null);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getUuid(), group1.getUuid(), PROVISION_PROJECTS.getKey(), anotherTemplate.getName(), group1.getName());

    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, template.getUuid(), asList("Group-1")))
      .extracting(PermissionTemplateGroupDto::getGroupUuid, PermissionTemplateGroupDto::getGroupName, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(group1.getUuid(), "Group-1", USER),
        tuple(group1.getUuid(), "Group-1", ADMIN));

    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, anotherTemplate.getUuid(), asList("Group-1")))
      .extracting(PermissionTemplateGroupDto::getGroupUuid, PermissionTemplateGroupDto::getGroupName, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(group1.getUuid(), "Group-1", PROVISION_PROJECTS.getKey()));

    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, anotherTemplate.getUuid(), asList("Anyone")))
      .extracting(PermissionTemplateGroupDto::getGroupUuid, PermissionTemplateGroupDto::getGroupName, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple("Anyone", "Anyone", USER));

    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, template.getUuid(), asList("Group-1", "Group-2", "Anyone"))).hasSize(3);
    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, template.getUuid(), asList("Unknown"))).isEmpty();
    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, template.getUuid(), Collections.emptyList())).isEmpty();
  }

  @Test
  public void select_group_permissions_by_template_id() {
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = db.users().insertGroup(newGroupDto().setName("Group-3"));

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group1.getUuid(), USER, template.getName(), group1.getName());
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group1.getUuid(), ADMIN, template.getName(), group1.getName());
    permissionTemplateDbTester.addGroupToTemplate(template.getUuid(), group2.getUuid(), PROVISION_PROJECTS.getKey(), template.getName(), group2.getName());

    PermissionTemplateDto anotherTemplate = permissionTemplateDbTester.insertTemplate();
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getUuid(), null, USER, anotherTemplate.getName(), null);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getUuid(), group1.getUuid(), PROVISION_PROJECTS.getKey(), anotherTemplate.getName(), group1.getName());

    assertThat(underTest.selectGroupPermissionsByTemplateUuid(session, template.getUuid()))
      .extracting(PermissionTemplateGroupDto::getGroupUuid, PermissionTemplateGroupDto::getGroupName, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(group1.getUuid(), "Group-1", USER),
        tuple(group1.getUuid(), "Group-1", ADMIN),
        tuple(group2.getUuid(), "Group-2", PROVISION_PROJECTS.getKey()));
    assertThat(underTest.selectGroupPermissionsByTemplateUuid(session, anotherTemplate.getUuid()))
      .extracting(PermissionTemplateGroupDto::getGroupUuid, PermissionTemplateGroupDto::getGroupName, PermissionTemplateGroupDto::getPermission)
      .containsOnly(
        tuple(group1.getUuid(), "Group-1", PROVISION_PROJECTS.getKey()),
        tuple("Anyone", "Anyone", USER));

    assertThat(underTest.selectGroupPermissionsByTemplateUuid(session, "321")).isEmpty();
  }

  private List<String> selectGroupNamesByQueryAndTemplate(PermissionQuery.Builder queryBuilder, PermissionTemplateDto permissionTemplateDto) {
    return selectGroupNamesByQueryAndTemplate(queryBuilder.build(), permissionTemplateDto.getUuid());
  }

  private List<String> selectGroupNamesByQueryAndTemplate(PermissionQuery query, String templateUuid) {
    return underTest.selectGroupNamesByQueryAndTemplate(session, query, templateUuid);
  }

  private int countGroupNamesByQueryAndTemplate(PermissionQuery.Builder queryBuilder, PermissionTemplateDto permissionTemplateDto) {
    return countGroupNamesByQueryAndTemplate(queryBuilder.build(), permissionTemplateDto.getUuid());
  }

  private int countGroupNamesByQueryAndTemplate(PermissionQuery query, String templateUuid) {
    return underTest.countGroupNamesByQueryAndTemplate(session, query, templateUuid);
  }

}
