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
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;
import static org.sonar.db.permission.PermissionQuery.builder;

public class UserWithPermissionTemplateDaoIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();

  private final PermissionTemplateDao underTest = db.getDbClient().permissionTemplateDao();

  @Test
  public void select_logins() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, ADMIN);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, CODEVIEWER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);
    PermissionTemplateDto anotherPermissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(anotherPermissionTemplate, user1, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession, builder().build(),
      permissionTemplate.getUuid()))
      .containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin(), user3.getLogin());
    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().withAtLeastOnePermission().setPermission(USER).build(),
      permissionTemplate.getUuid()))
      .containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin());
  }

  @Test
  public void return_no_logins_on_unknown_template_key() {
    UserDto user = db.users().insertUser();
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setPermission(USER).withAtLeastOnePermission().build(), "999"))
      .isEmpty();
  }

  @Test
  public void select_only_logins_with_permission() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, ADMIN);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, CODEVIEWER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);
    PermissionTemplateDto anotherPermissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(anotherPermissionTemplate, user1, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setPermission(USER).withAtLeastOnePermission().build(),
      permissionTemplate.getUuid()))
      .containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin());
  }

  @Test
  public void select_only_enable_users() {
    UserDto user = db.users().insertUser();
    UserDto disabledUser = db.users().insertUser(u -> u.setActive(false));
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, disabledUser, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setPermission(USER).build(), permissionTemplate.getUuid()))
      .containsExactlyInAnyOrder(user.getLogin());
  }

  @Test
  public void search_by_user_name() {
    UserDto user1 = db.users().insertUser(u -> u.setName("User1"));
    UserDto user2 = db.users().insertUser(u -> u.setName("User2"));
    UserDto user3 = db.users().insertUser(u -> u.setName("User3"));
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(
      dbSession, builder().withAtLeastOnePermission().setPermission(USER).setSearchQuery("SEr1").build(),
      permissionTemplate.getUuid()))
      .containsExactlyInAnyOrder(user1.getLogin());

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(
      dbSession, builder().withAtLeastOnePermission().setPermission(USER).setSearchQuery("user").build(),
      permissionTemplate.getUuid()))
      .containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin());
  }

  @Test
  public void selectUserLoginsByQueryAndTemplate_is_ordering_result_by_users_with_permissions_then_by_name() {
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate();
    UserDto user1 = db.users().insertUser(u -> u.setName("A"));
    UserDto user2 = db.users().insertUser(u -> u.setName("B"));
    UserDto user3 = db.users().insertUser(u -> u.setName("C"));
    db.permissionTemplates().addUserToTemplate(template.getUuid(), user3.getUuid(), UserRole.USER, template.getName(), user3.getLogin());

    PermissionQuery query = PermissionQuery.builder().build();
    assertThat(underTest.selectUserLoginsByQueryAndTemplate(db.getSession(), query, template.getUuid()))
      .containsExactly(user3.getLogin(), user1.getLogin(), user2.getLogin());
  }

  @Test
  public void selectUserLoginsByQueryAndTemplate_is_order_by_groups_with_permission_when_many_users() {
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate();
    // Add another template having some users with permission to make sure it's correctly ignored
    PermissionTemplateDto otherTemplate = db.permissionTemplates().insertTemplate();
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      UserDto user = db.users().insertUser("User-" + i);
      db.permissionTemplates().addUserToTemplate(otherTemplate, user, UserRole.USER);
    });
    String lastLogin = "User-" + (DEFAULT_PAGE_SIZE + 1);
    db.permissionTemplates().addUserToTemplate(template, db.users().selectUserByLogin(lastLogin).get(), UserRole.USER);

    PermissionQuery query = PermissionQuery.builder().build();
    assertThat(underTest.selectUserLoginsByQueryAndTemplate(db.getSession(), query, template.getUuid()))
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith(lastLogin);
  }

  @Test
  public void should_be_paginated() {
    UserDto user1 = db.users().insertUser(u -> u.setName("User1"));
    UserDto user2 = db.users().insertUser(u -> u.setName("User2"));
    UserDto user3 = db.users().insertUser(u -> u.setName("User3"));
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);

    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setPageIndex(1).setPageSize(2).build(), permissionTemplate.getUuid()))
      .containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin());
    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setPageIndex(2).setPageSize(2).build(), permissionTemplate.getUuid()))
      .containsExactlyInAnyOrder(user3.getLogin());
    assertThat(underTest.selectUserLoginsByQueryAndTemplate(dbSession,
      builder().setPageIndex(3).setPageSize(1).build(), permissionTemplate.getUuid()))
      .containsExactlyInAnyOrder(user3.getLogin());
  }

  @Test
  public void count_users() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);

    assertThat(underTest.countUserLoginsByQueryAndTemplate(dbSession,
      builder().build(), permissionTemplate.getUuid()))
      .isEqualTo(3);
    assertThat(underTest.countUserLoginsByQueryAndTemplate(dbSession,
      builder().withAtLeastOnePermission().setPermission("user").build(), permissionTemplate.getUuid()))
      .isEqualTo(2);
  }

  @Test
  public void select_user_permission_templates_by_template_and_logins() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, USER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, ADMIN);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user1, CODEVIEWER);
    db.permissionTemplates().addUserToTemplate(permissionTemplate, user2, USER);
    PermissionTemplateDto anotherPermissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(anotherPermissionTemplate, user1, USER);

    assertThat(underTest.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, permissionTemplate.getUuid(), singletonList(user1.getLogin())))
      .extracting(PermissionTemplateUserDto::getUserLogin, PermissionTemplateUserDto::getPermission)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), USER),
        tuple(user1.getLogin(), ADMIN),
        tuple(user1.getLogin(), CODEVIEWER));

    assertThat(underTest.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, permissionTemplate.getUuid(), asList(user1.getLogin(), user2.getLogin(), user2.getLogin())))
      .extracting(PermissionTemplateUserDto::getUserLogin, PermissionTemplateUserDto::getPermission)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), USER),
        tuple(user1.getLogin(), ADMIN),
        tuple(user1.getLogin(), CODEVIEWER),
        tuple(user2.getLogin(), USER));

    assertThat(underTest.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, permissionTemplate.getUuid(), singletonList("unknown"))).isEmpty();
    assertThat(underTest.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, permissionTemplate.getUuid(), Collections.emptyList())).isEmpty();
    assertThat(underTest.selectUserPermissionsByTemplateIdAndUserLogins(dbSession, "123", singletonList(user1.getLogin()))).isEmpty();
  }

}
