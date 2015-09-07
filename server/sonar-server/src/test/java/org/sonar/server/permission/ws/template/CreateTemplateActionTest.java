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

package org.sonar.server.permission.ws.template;

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
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_DESCRIPTION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_NAME;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PATTERN;
import static org.sonar.test.JsonAssert.assertJson;

public class CreateTemplateActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  WsActionTester ws;
  DbClient dbClient;
  DbSession dbSession;
  System2 system = mock(System2.class);

  @Before
  public void setUp() {
    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    when(system.now()).thenReturn(1440512328743L);

    dbClient = db.getDbClient();
    dbSession = db.getSession();
    ws = new WsActionTester(new CreateTemplateAction(dbClient, userSession, system));
  }

  @Test
  public void create_full_permission_template() {
    TestResponse result = newRequest("Finance", "Permissions for financially related projects", ".*\\.finance\\..*");

    assertJson(result.getInput())
      .ignoreFields("id")
      .isSimilarTo(getClass().getResource("create_template-example.json"));
    PermissionTemplateDto finance = dbClient.permissionTemplateDao().selectByName(dbSession, "Finance");
    assertThat(finance.getName()).isEqualTo("Finance");
    assertThat(finance.getDescription()).isEqualTo("Permissions for financially related projects");
    assertThat(finance.getKeyPattern()).isEqualTo(".*\\.finance\\..*");
    assertThat(finance.getKee()).isNotEmpty();
    assertThat(finance.getCreatedAt().getTime()).isEqualTo(1440512328743L);
    assertThat(finance.getUpdatedAt().getTime()).isEqualTo(1440512328743L);
  }

  @Test
  public void create_minimalist_permission_template() {
    newRequest("Finance", null, null);

    PermissionTemplateDto finance = dbClient.permissionTemplateDao().selectByName(dbSession, "Finance");
    assertThat(finance.getName()).isEqualTo("Finance");
    assertThat(finance.getDescription()).isNullOrEmpty();
    assertThat(finance.getKeyPattern()).isNullOrEmpty();
    assertThat(finance.getKee()).isNotEmpty();
    assertThat(finance.getCreatedAt().getTime()).isEqualTo(1440512328743L);
    assertThat(finance.getUpdatedAt().getTime()).isEqualTo(1440512328743L);
  }

  @Test
  public void fail_if_name_not_provided() {
    expectedException.expect(IllegalArgumentException.class);

    newRequest(null, null, null);
  }

  @Test
  public void fail_if_name_empty() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The template name must not be blank");

    newRequest("", null, null);
  }

  @Test
  public void fail_if_regexp_if_not_valid() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'projectKeyPattern' parameter must be a valid Java regular expression. '[azerty' was passed");

    newRequest("Finance", null, "[azerty");
  }

  @Test
  public void fail_if_name_already_exists_in_database_case_insensitive() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A template with the name 'Finance' already exists (case insensitive).");
    insertTemplate(newPermissionTemplateDto().setName("finance"));
    commit();

    newRequest("Finance", null, null);
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest("Finance", null, null);
  }

  @Test
  public void fail_if_not_admin() {
    expectedException.expect(ForbiddenException.class);
    userSession.setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    newRequest("Finance", null, null);
  }

  private PermissionTemplateDto insertTemplate(PermissionTemplateDto template) {
    return dbClient.permissionTemplateDao().insert(dbSession, template);
  }

  private void commit() {
    dbSession.commit();
  }

  private TestResponse newRequest(@Nullable String name, @Nullable String description, @Nullable String projectPattern) {
    TestRequest request = ws.newRequest();
    if (name != null) {
      request.setParam(PARAM_NAME, name);
    }
    if (description != null) {
      request.setParam(PARAM_DESCRIPTION, description);
    }
    if (projectPattern != null) {
      request.setParam(PARAM_PATTERN, projectPattern);
    }

    return request.execute();
  }
}
