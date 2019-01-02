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
package org.sonar.server.platform.db.migration.version.v64;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class DeletePermissionTemplatesLinkedToRemovedUsersTest {

  private static long PAST_TIME = 10_000_000_000L;
  private static long TEMPLATE_ID = 1000L;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeletePermissionTemplatesLinkedToRemovedUsersTest.class, "initial.sql");

  private DeletePermissionTemplatesLinkedToRemovedUsers underTest = new DeletePermissionTemplatesLinkedToRemovedUsers(db.database());

  @Test
  public void remove_permission_template_users_from_disabled_users() throws Exception {
    long userId1 = insertUser(false);
    long userId2 = insertUser(false);
    insertPermissionTemplateUser(userId1, TEMPLATE_ID, "user");
    insertPermissionTemplateUser(userId1, TEMPLATE_ID, "codeviewer");
    insertPermissionTemplateUser(userId2, TEMPLATE_ID, "user");

    underTest.execute();

    checkNoCheckPermissionTemplateUsers();
  }

  @Test
  public void remove_permission_template_users_from_non_existing_users() throws Exception {
    insertPermissionTemplateUser(123L, TEMPLATE_ID, "user");
    insertPermissionTemplateUser(321L, TEMPLATE_ID, "codeviewer");

    underTest.execute();

    checkNoCheckPermissionTemplateUsers();
  }

  @Test
  public void does_not_remove_permission_template_users_from_active_user() throws Exception {
    long activeUserId = insertUser(true);
    long inactiveUserId = insertUser(false);
    long permissionTemplateUserOnActiveUser = insertPermissionTemplateUser(activeUserId, TEMPLATE_ID, "user");
    insertPermissionTemplateUser(inactiveUserId, TEMPLATE_ID, "user");

    underTest.execute();

    checkPermissionTemplateUsers(permissionTemplateUserOnActiveUser);
  }

  @Test
  public void does_not_fail_when_no_permission_template_users() throws Exception {
    insertUser(false);

    underTest.execute();

    checkNoCheckPermissionTemplateUsers();
  }

  @Test
  public void migration_is_reentrant() throws Exception {
    long userId = insertUser(false);
    insertPermissionTemplateUser(userId, TEMPLATE_ID, "user");

    underTest.execute();
    checkNoCheckPermissionTemplateUsers();

    underTest.execute();
    checkNoCheckPermissionTemplateUsers();
  }

  private void checkNoCheckPermissionTemplateUsers() {
    checkPermissionTemplateUsers();
  }

  private void checkPermissionTemplateUsers(Long... expectedIds) {
    List<Long> ids = db.select("select ptu.id from perm_templates_users ptu").stream()
      .map(map -> (Long) map.get("ID"))
      .collect(Collectors.toList());
    assertThat(ids).containsOnly(expectedIds);
  }

  private long insertPermissionTemplateUser(long userId, long templateId, String permission) {
    db.executeInsert(
      "PERM_TEMPLATES_USERS",
      "USER_ID", userId,
      "TEMPLATE_ID", templateId,
      "PERMISSION_REFERENCE", permission,
      "CREATED_AT", new Date(PAST_TIME),
      "UPDATED_AT", new Date(PAST_TIME));
    return (long) db.selectFirst(format("select id from perm_templates_users where template_id='%s' and user_id='%s' and permission_reference='%s'",
      templateId, userId, permission)).get("ID");
  }

  private long insertUser(boolean enabled) {
    String login = randomAlphabetic(10);
    db.executeInsert(
      "USERS",
      "LOGIN", login,
      "ACTIVE", Boolean.toString(enabled),
      "IS_ROOT", "false",
      "CREATED_AT", "1000",
      "UPDATED_AT", "1000");
    return (Long) db.selectFirst(format("select id from users where login='%s'", login)).get("ID");
  }

}
