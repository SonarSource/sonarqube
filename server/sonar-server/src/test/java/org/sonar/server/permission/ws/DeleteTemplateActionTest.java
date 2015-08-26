/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission.ws;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.server.permission.ws.Parameters.PARAM_TEMPLATE_KEY;

public class DeleteTemplateActionTest {

  static final String TEMPLATE_KEY = "permission-template-key";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  WsActionTester ws;
  DbClient dbClient;
  DbSession dbSession;
  PermissionTemplateDto permissionTemplate;

  @Before
  public void setUp() {
    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    dbClient = db.getDbClient();
    dbSession = db.getSession();
    ws = new WsActionTester(new DeleteTemplateAction(dbClient, userSession));

    permissionTemplate = insertTemplate(newPermissionTemplateDto().setKee("permission-template-key"));
    commit();
    assertThat(dbClient.permissionTemplateDao().selectByKey(dbSession, TEMPLATE_KEY)).isNotNull();
  }

  @Test
  public void delete_template_in_db() {

    TestResponse result = newRequest(TEMPLATE_KEY);

    assertThat(result.getInput()).isEmpty();
    assertThat(dbClient.permissionTemplateDao().selectByKey(dbSession, TEMPLATE_KEY)).isNull();
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(TEMPLATE_KEY);
  }

  @Test
  public void fail_if_not_admin() {
    expectedException.expect(ForbiddenException.class);
    userSession.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    newRequest(TEMPLATE_KEY);
  }

  @Test
  public void fail_if_key_is_not_provided() {
    expectedException.expect(IllegalArgumentException.class);

    newRequest(null);
  }

  private PermissionTemplateDto insertTemplate(PermissionTemplateDto template) {
    return dbClient.permissionTemplateDao().insert(dbSession, template);
  }

  private void commit() {
    dbSession.commit();
  }

  private TestResponse newRequest(@Nullable String key) {
    TestRequest request = ws.newRequest();
    if (key != null) {
      request.setParam(PARAM_TEMPLATE_KEY, key);
    }

    TestResponse result = executeRequest(request);
    commit();
    return result;
  }

  private static TestResponse executeRequest(TestRequest request) {
    return request.execute();
  }

}
