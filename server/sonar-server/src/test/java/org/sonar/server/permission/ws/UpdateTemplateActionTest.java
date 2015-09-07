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

import java.util.Date;
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
import org.sonar.db.user.GroupDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
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
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_ID;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_NAME;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PATTERN;
import static org.sonar.test.JsonAssert.assertJson;

public class UpdateTemplateActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  WsActionTester ws;
  DbClient dbClient;
  DbSession dbSession;

  PermissionTemplateDto templateDto;

  @Before
  public void setUp() {
    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    System2 system = mock(System2.class);
    when(system.now()).thenReturn(1_440_512_328_743L);

    dbClient = db.getDbClient();
    dbSession = db.getSession();
    PermissionDependenciesFinder finder = new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient));

    ws = new WsActionTester(new UpdateTemplateAction(dbClient, userSession, system, finder));

    templateDto = insertTemplate(newPermissionTemplateDto()
      .setName("Permission Template Name")
      .setDescription("Permission Template Description")
      .setKeyPattern(".*\\.pattern\\..*")
      .setCreatedAt(new Date(1_000_000_000_000L))
      .setUpdatedAt(new Date(1_000_000_000_000L)));
    commit();
  }

  @Test
  public void update_all_permission_template_fields() {
    TestResponse result = newRequest(templateDto.getKee(), "Finance", "Permissions for financially related projects", ".*\\.finance\\..*");

    assertJson(result.getInput())
      .ignoreFields("id")
      .isSimilarTo(getClass().getResource("update_template-example.json"));
    PermissionTemplateDto finance = dbClient.permissionTemplateDao().selectByName(dbSession, "Finance");
    assertThat(finance.getName()).isEqualTo("Finance");
    assertThat(finance.getDescription()).isEqualTo("Permissions for financially related projects");
    assertThat(finance.getKeyPattern()).isEqualTo(".*\\.finance\\..*");
    assertThat(finance.getKee()).isEqualTo(templateDto.getKee());
    assertThat(finance.getCreatedAt()).isEqualTo(templateDto.getCreatedAt());
    assertThat(finance.getUpdatedAt().getTime()).isEqualTo(1440512328743L);
  }

  @Test
  public void update_with_the_same_values() {
    newRequest(templateDto.getKee(), templateDto.getName(), templateDto.getDescription(), templateDto.getKeyPattern());

    PermissionTemplateDto updatedTemplate = dbClient.permissionTemplateDao().selectByUuid(dbSession, templateDto.getKee());
    assertThat(updatedTemplate.getName()).isEqualTo(templateDto.getName());
    assertThat(updatedTemplate.getDescription()).isEqualTo(templateDto.getDescription());
    assertThat(updatedTemplate.getKeyPattern()).isEqualTo(templateDto.getKeyPattern());
  }

  @Test
  public void update_name_only() {
    newRequest(templateDto.getKee(), "Finance", null, null);

    PermissionTemplateDto finance = dbClient.permissionTemplateDao().selectByName(dbSession, "Finance");
    assertThat(finance.getName()).isEqualTo("Finance");
    assertThat(finance.getDescription()).isEqualTo(templateDto.getDescription());
    assertThat(finance.getKeyPattern()).isEqualTo(templateDto.getKeyPattern());
  }

  @Test
  public void fail_if_key_is_not_found() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with id 'unknown-key' is not found");

    newRequest("unknown-key", null, null, null);
  }

  @Test
  public void fail_if_name_already_exists_in_another_template() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A template with the name 'My Template' already exists (case insensitive).");

    insertTemplate(newPermissionTemplateDto()
      .setName("My Template")
      .setKee("my-key")
      .setCreatedAt(new Date(12345789L))
      .setUpdatedAt(new Date(12345789L)));
    commit();

    newRequest(templateDto.getKee(), "My Template", null, null);
  }

  @Test
  public void fail_if_key_is_not_provided() {
    expectedException.expect(IllegalArgumentException.class);

    newRequest(null, "Finance", null, null);
  }

  @Test
  public void fail_if_name_empty() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The template name must not be blank");

    newRequest(templateDto.getKee(), "", null, null);
  }

  @Test
  public void fail_if_name_has_just_whitespaces() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The template name must not be blank");

    newRequest(templateDto.getKee(), "  \r\n", null, null);
  }

  @Test
  public void fail_if_regexp_if_not_valid() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'projectKeyPattern' parameter must be a valid Java regular expression. '[azerty' was passed");

    newRequest(templateDto.getKee(), "Finance", null, "[azerty");
  }

  @Test
  public void fail_if_name_already_exists_in_database_case_insensitive() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A template with the name 'Finance' already exists (case insensitive).");
    insertTemplate(newPermissionTemplateDto().setName("finance"));
    commit();

    newRequest(templateDto.getKee(), "Finance", null, null);
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(templateDto.getKee(), "Finance", null, null);
  }

  @Test
  public void fail_if_not_admin() {
    expectedException.expect(ForbiddenException.class);
    userSession.setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    newRequest(templateDto.getKee(), "Finance", null, null);
  }

  private PermissionTemplateDto insertTemplate(PermissionTemplateDto template) {
    return dbClient.permissionTemplateDao().insert(dbSession, template);
  }

  private GroupDto insertGroup(GroupDto group) {
    return dbClient.groupDao().insert(db.getSession(), group);
  }

  private void commit() {
    dbSession.commit();
  }

  private TestResponse newRequest(@Nullable String key, @Nullable String name, @Nullable String description, @Nullable String projectPattern) {
    TestRequest request = ws.newRequest();
    if (key != null) {
      request.setParam(PARAM_ID, key);
    }
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
