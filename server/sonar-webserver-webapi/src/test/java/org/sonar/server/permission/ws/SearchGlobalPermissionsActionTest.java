/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.permission.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonarqube.ws.Permissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.db.permission.GlobalPermission.SCAN;

public class SearchGlobalPermissionsActionTest extends BasePermissionWsTest<SearchGlobalPermissionsAction> {

  private I18nRule i18n = new I18nRule();
  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private PermissionService permissionService = new PermissionServiceImpl(resourceTypes);

  @Override
  protected SearchGlobalPermissionsAction buildWsAction() {
    return new SearchGlobalPermissionsAction(db.getDbClient(), userSession, i18n, permissionService);
  }

  @Before
  public void setUp() {
    initI18nMessages();
  }

  @Test
  public void search() {
    loginAsAdmin();

    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(user, SCAN);

    Permissions.WsSearchGlobalPermissionsResponse result = newRequest()
      .executeProtobuf(Permissions.WsSearchGlobalPermissionsResponse.class);

    assertThat(result.getPermissionsCount()).isEqualTo(GlobalPermissions.ALL.size());
    for (Permissions.Permission permission : result.getPermissionsList()) {
      if (permission.getKey().equals(SCAN_EXECUTION)) {
        assertThat(permission.getUsersCount()).isEqualTo(1);
      } else {
        assertThat(permission.getUsersCount()).isEqualTo(0);
      }
    }
  }

  @Test
  public void supports_protobuf_response() {
    loginAsAdmin();

    Permissions.WsSearchGlobalPermissionsResponse result = newRequest()
      .executeProtobuf(Permissions.WsSearchGlobalPermissionsResponse.class);

    assertThat(result).isNotNull();
  }

  @Test
  public void fail_if_not_admin() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest().execute();
  }

  private void initI18nMessages() {
    i18n.put("global_permissions.admin", "Administer System");
    i18n.put("global_permissions.admin.desc", "Ability to perform all administration functions for the instance: " +
      "global configuration and personalization of default dashboards.");
    i18n.put("global_permissions.profileadmin", "Administer Quality Profiles");
    i18n.put("global_permissions.profileadmin.desc", "Ability to perform any action on the quality profiles.");
    i18n.put("global_permissions.gateadmin", "Administer Quality Gates");
    i18n.put("global_permissions.gateadmin.desc", "Ability to perform any action on the quality gates.");
    i18n.put("global_permissions.scan", "Execute Analysis");
    i18n.put("global_permissions.scan.desc", "Ability to execute analyses, and to get all settings required to perform the analysis, " +
      "even the secured ones like the scm account password, the jira account password, and so on.");
    i18n.put("global_permissions.provisioning", "Create Projects");
    i18n.put("global_permissions.provisioning.desc", "Ability to initialize project structure before first analysis.");
  }
}
