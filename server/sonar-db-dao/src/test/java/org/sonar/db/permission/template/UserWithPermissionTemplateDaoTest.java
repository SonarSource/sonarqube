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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.PermissionQuery.builder;

public class UserWithPermissionTemplateDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = db.getSession();

  private PermissionTemplateDao underTest = db.getDbClient().permissionTemplateDao();

  @Test
  public void select_logins() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    db.organizations().addMember(organization, user1, user2, user3);
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, ADMIN);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, CODEVIEWER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);
    PermissionTemplateDto anotherPermissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(anotherPermissionTemplate, user1, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setOrganizationUuid(organization.getUuid()).build(),
      permissionTemplate.getId()))
        .containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin(), user3.getLogin());
    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission().setPermission(USER).build(),
      permissionTemplate.getId()))
        .containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin());
  }

  @Test
  public void return_no_logins_on_unknown_template_key() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setOrganizationUuid(organization.getUuid()).setPermission(USER).withAtLeastOnePermission().build(), 999L))
        .isEmpty();
  }

  @Test
  public void select_only_logins_with_permission() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    db.organizations().addMember(organization, user1, user2, user3);
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, ADMIN);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, CODEVIEWER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);
    PermissionTemplateDto anotherPermissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(anotherPermissionTemplate, user1, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setOrganizationUuid(organization.getUuid()).setPermission(USER).withAtLeastOnePermission().build(),
      permissionTemplate.getId()))
        .containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin());
  }

  @Test
  public void select_only_enable_users() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    UserDto disabledUser = db.users().insertUser(u -> u.setActive(false));
    db.organizations().addMember(organization, user, disabledUser);
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, disabledUser, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setOrganizationUuid(organization.getUuid()).setPermission(USER).build(), permissionTemplate.getId()))
        .containsExactlyInAnyOrder(user.getLogin());
  }

  @Test
  public void search_by_user_name() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = db.users().insertUser(u -> u.setName("User1"));
    UserDto user2 = db.users().insertUser(u -> u.setName("User2"));
    UserDto user3 = db.users().insertUser(u -> u.setName("User3"));
    db.organizations().addMember(organization, user1, user2, user3);
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(
      dbSession, builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission().setPermission(USER).setSearchQuery("SEr1").build(),
      permissionTemplate.getId()))
        .containsExactlyInAnyOrder(user1.getLogin());

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(
      dbSession, builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission().setPermission(USER).setSearchQuery("user").build(),
      permissionTemplate.getId()))
        .containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin());
  }

  @Test
  public void selectUserLoginsByQueryAndTemplate_is_ordering_result_by_users_with_permissions_then_by_name() {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate(organization);
    UserDto user1 = db.users().insertUser(u -> u.setName("A"));
    UserDto user2 = db.users().insertUser(u -> u.setName("B"));
    UserDto user3 = db.users().insertUser(u -> u.setName("C"));
    db.organizations().addMember(organization, user1, user2, user3);
    db.permissionTemplates().addUserToTemplate(template.getId(), user3.getId(), UserRole.USER);

    PermissionQuery query = PermissionQuery.builder().setOrganizationUuid(organization.getUuid()).build();
    assertThat(underTest.selectUserLoginsByQueryAndTemplate(db.getSession(), query, template.getId()))
      .containsExactly(user3.getLogin(), user1.getLogin(), user2.getLogin());
  }

  @Test
  public void should_be_paginated() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = db.users().insertUser(u -> u.setName("User1"));
    UserDto user2 = db.users().insertUser(u -> u.setName("User2"));
    UserDto user3 = db.users().insertUser(u -> u.setName("User3"));
    db.organizations().addMember(organization, user1, user2, user3);
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setOrganizationUuid(organization.getUuid()).setPageIndex(1).setPageSize(2).build(), permissionTemplate.getId()))
        .containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin());
    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setOrganizationUuid(organization.getUuid()).setPageIndex(2).setPageSize(2).build(), permissionTemplate.getId()))
        .containsExactlyInAnyOrder(user3.getLogin());
    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setOrganizationUuid(organization.getUuid()).setPageIndex(3).setPageSize(1).build(), permissionTemplate.getId()))
        .containsExactlyInAnyOrder(user3.getLogin());
  }

  @Test
  public void count_users() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    db.organizations().addMember(organization, user1, user2, user3);
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);

    assertThat(underTest.countUserLoginsByQueryAndTemplate(dbSession,
      builder().setOrganizationUuid(organization.getUuid()).build(), permissionTemplate.getId()))
        .isEqualTo(3);
    assertThat(underTest.countUserLoginsByQueryAndTemplate(dbSession,
      builder().setOrganizationUuid(organization.getUuid()).withAtLeastOnePermission().setPermission("user").build(), permissionTemplate.getId()))
        .isEqualTo(2);
  }

  @Test
  public void select_user_permission_templates_by_template_and_logins() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    db.organizations().addMember(organization, user1, user2, user3);
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, ADMIN);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, CODEVIEWER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);
    PermissionTemplateDto anotherPermissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(anotherPermissionTemplate, user1, USER);

    assertThat(underTest.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, permissionTemplate.getId(), singletonList(user1.getLogin())))
      .extracting(PermissionTemplateUserDto::getUserLogin, PermissionTemplateUserDto::getPermission)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), USER),
        tuple(user1.getLogin(), ADMIN),
        tuple(user1.getLogin(), CODEVIEWER));

    assertThat(underTest.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, permissionTemplate.getId(), asList(user1.getLogin(), user2.getLogin(), user2.getLogin())))
      .extracting(PermissionTemplateUserDto::getUserLogin, PermissionTemplateUserDto::getPermission)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), USER),
        tuple(user1.getLogin(), ADMIN),
        tuple(user1.getLogin(), CODEVIEWER),
        tuple(user2.getLogin(), USER));

    assertThat(underTest.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, permissionTemplate.getId(), singletonList("unknown"))).isEmpty();
    assertThat(underTest.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, permissionTemplate.getId(), Collections.emptyList())).isEmpty();
    assertThat(underTest.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, 123L, singletonList(user1.getLogin()))).isEmpty();
  }
}
