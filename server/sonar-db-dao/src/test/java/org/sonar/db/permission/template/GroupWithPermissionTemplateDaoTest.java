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
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.PermissionQuery;
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

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession session = db.getSession();
  private PermissionTemplateDbTester permissionTemplateDbTester = db.permissionTemplates();
  private PermissionTemplateDao underTest = db.getDbClient().permissionTemplateDao();

  @Test
  public void select_group_names_by_query_and_template() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(organization, "Group-1");
    GroupDto group2 = db.users().insertGroup(organization, "Group-2");
    GroupDto group3 = db.users().insertGroup(organization, "Group-3");

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate(organization);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), USER);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), ADMIN);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group2.getId(), PROVISIONING);

    PermissionTemplateDto anotherTemplate = permissionTemplateDbTester.insertTemplate(organization);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), null, USER);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), group1.getId(), PROVISIONING);

    assertThat(selectGroupNamesByQueryAndTemplate(builder(), organization, template))
      .containsOnly("Group-1", "Group-2", "Group-3", "Anyone");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().withAtLeastOnePermission(), organization, template))
      .containsOnly("Group-1", "Group-2");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPermission(USER), organization, template))
      .containsOnly("Group-1");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPermission(USER), organization, anotherTemplate))
      .containsOnly("Anyone");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("groU"), organization, template))
      .containsOnly("Group-1", "Group-2", "Group-3");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("nYo"), organization, template))
      .containsOnly("Anyone");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("p-2"), organization, template))
      .containsOnly("Group-2");

    assertThat(selectGroupNamesByQueryAndTemplate(builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission().build(), organization, 123L))
      .isEmpty();
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("unknown"), organization, template))
      .isEmpty();
  }

  @Test
  public void selectGroupNamesByQueryAndTemplate_is_ordering_results_by_groups_with_permission_then_by_name() {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate(organization);

    GroupDto group1 = db.users().insertGroup(organization, "A");
    GroupDto group2 = db.users().insertGroup(organization, "B");
    GroupDto group3 = db.users().insertGroup(organization, "C");

    permissionTemplateDbTester.addGroupToTemplate(template, group3, UserRole.USER);

    PermissionQuery query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).build();
    assertThat(underTest.selectGroupNamesByQueryAndTemplate(db.getSession(), query, template.getId()))
      .containsExactly("Anyone", group3.getName(), group1.getName(), group2.getName());
  }

  @Test
  public void select_group_names_by_query_and_template_is_paginated() {
    OrganizationDto organization = db.organizations().insert();
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertGroup(organization, i + "-name"));

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate(organization);

    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPageIndex(1).setPageSize(1), organization, template))
      .containsExactly("0-name");
    assertThat(selectGroupNamesByQueryAndTemplate(builder().setPageIndex(2).setPageSize(3), organization, template))
      .containsExactly("3-name", "4-name", "5-name");
  }

  @Test
  public void select_group_names_by_query_and_template_returns_anyone() {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate(organization);

    GroupDto group = db.users().insertGroup(newGroupDto().setName("Group"));
    PermissionTemplateDto otherTemplate = permissionTemplateDbTester.insertTemplate(organization);
    permissionTemplateDbTester.addGroupToTemplate(otherTemplate.getId(), group.getId(), USER);

    assertThat(selectGroupNamesByQueryAndTemplate(builder().setSearchQuery("nyo"), organization, template))
      .containsExactly("Anyone");
  }

  @Test
  public void count_group_names_by_query_and_template() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(organization, "Group-1");
    GroupDto group2 = db.users().insertGroup(organization, "Group-2");
    GroupDto group3 = db.users().insertGroup(organization, "Group-3");

    PermissionTemplateDto template = permissionTemplateDbTester.insertTemplate(organization);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), USER);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group1.getId(), ADMIN);
    permissionTemplateDbTester.addGroupToTemplate(template.getId(), group2.getId(), PROVISIONING);

    PermissionTemplateDto anotherTemplate = permissionTemplateDbTester.insertTemplate(organization);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), null, USER);
    permissionTemplateDbTester.addGroupToTemplate(anotherTemplate.getId(), group1.getId(), PROVISIONING);

    assertThat(countGroupNamesByQueryAndTemplate(builder().setOrganizationUuid(organization.getUuid()), organization, template))
      .isEqualTo(4);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission(), organization, template))
      .isEqualTo(2);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setOrganizationUuid(organization.getUuid()).setPermission(USER), organization, template)).isEqualTo(1);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setOrganizationUuid(organization.getUuid()).setPermission(USER), organization, anotherTemplate))
      .isEqualTo(1);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setOrganizationUuid(organization.getUuid()).setSearchQuery("groU"), organization, template))
      .isEqualTo(3);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setOrganizationUuid(organization.getUuid()).setSearchQuery("nYo"), organization, template))
      .isEqualTo(1);
    assertThat(countGroupNamesByQueryAndTemplate(builder().setOrganizationUuid(organization.getUuid()).setSearchQuery("p-2"), organization, template))
      .isEqualTo(1);

    assertThat(countGroupNamesByQueryAndTemplate(builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission().build(), organization, 123L))
      .isZero();
    assertThat(countGroupNamesByQueryAndTemplate(builder().setOrganizationUuid(organization.getUuid()).setSearchQuery("unknown"), organization, template))
      .isZero();
  }

  @Test
  public void select_group_permissions_by_template_id_and_group_names() {
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = db.users().insertGroup(newGroupDto().setName("Group-3"));

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
        tuple(0, "Anyone", USER));

    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, template.getId(), asList("Group-1", "Group-2", "Anyone"))).hasSize(3);
    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, template.getId(), asList("Unknown"))).isEmpty();
    assertThat(underTest.selectGroupPermissionsByTemplateIdAndGroupNames(session, template.getId(), Collections.emptyList())).isEmpty();
  }

  @Test
  public void select_group_permissions_by_template_id() {
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = db.users().insertGroup(newGroupDto().setName("Group-3"));

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
        tuple(0, "Anyone", USER));

    assertThat(underTest.selectGroupPermissionsByTemplateId(session, 321L)).isEmpty();
  }

  private List<String> selectGroupNamesByQueryAndTemplate(PermissionQuery.Builder queryBuilder, OrganizationDto organization, PermissionTemplateDto permissionTemplateDto) {
    return selectGroupNamesByQueryAndTemplate(queryBuilder.setOrganizationUuid(organization.getUuid()).build(), organization, permissionTemplateDto.getId());
  }

  private List<String> selectGroupNamesByQueryAndTemplate(PermissionQuery query, OrganizationDto organization, long templateId) {
    return underTest.selectGroupNamesByQueryAndTemplate(session, query, templateId);
  }

  private int countGroupNamesByQueryAndTemplate(PermissionQuery.Builder queryBuilder, OrganizationDto organization, PermissionTemplateDto permissionTemplateDto) {
    return countGroupNamesByQueryAndTemplate(queryBuilder.build(), organization, permissionTemplateDto.getId());
  }

  private int countGroupNamesByQueryAndTemplate(PermissionQuery query, OrganizationDto organization, long templateId) {
    return underTest.countGroupNamesByQueryAndTemplate(session, query, organization.getUuid(), templateId);
  }

}
