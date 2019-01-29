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
package org.sonar.server.permission.ws.template;

import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_DESCRIPTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY_PATTERN;

public class CreateTemplateActionTest extends BasePermissionWsTest<CreateTemplateAction> {

  private static final long NOW = 1_440_512_328_743L;
  private System2 system = new TestSystem2().setNow(NOW);

  @Override
  protected CreateTemplateAction buildWsAction() {
    return new CreateTemplateAction(db.getDbClient(), userSession, system, newPermissionWsSupport());
  }

  @Test
  public void create_full_permission_template() {
    loginAsAdmin(db.getDefaultOrganization());

    TestResponse result = newRequest("Finance", "Permissions for financially related projects", ".*\\.finance\\..*");

    assertJson(result.getInput())
      .ignoreFields("id")
      .isSimilarTo(getClass().getResource("create_template-example.json"));
    PermissionTemplateDto finance = selectTemplateInDefaultOrganization("Finance");
    assertThat(finance.getName()).isEqualTo("Finance");
    assertThat(finance.getDescription()).isEqualTo("Permissions for financially related projects");
    assertThat(finance.getKeyPattern()).isEqualTo(".*\\.finance\\..*");
    assertThat(finance.getUuid()).isNotEmpty();
    assertThat(finance.getCreatedAt().getTime()).isEqualTo(NOW);
    assertThat(finance.getUpdatedAt().getTime()).isEqualTo(NOW);
  }

  @Test
  public void create_minimalist_permission_template() {
    loginAsAdmin(db.getDefaultOrganization());

    newRequest("Finance", null, null);

    PermissionTemplateDto finance = selectTemplateInDefaultOrganization("Finance");
    assertThat(finance.getName()).isEqualTo("Finance");
    assertThat(finance.getDescription()).isNullOrEmpty();
    assertThat(finance.getKeyPattern()).isNullOrEmpty();
    assertThat(finance.getUuid()).isNotEmpty();
    assertThat(finance.getCreatedAt().getTime()).isEqualTo(NOW);
    assertThat(finance.getUpdatedAt().getTime()).isEqualTo(NOW);
  }

  @Test
  public void fail_if_name_not_provided() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest(null, null, null);
  }

  @Test
  public void fail_if_name_empty() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    newRequest("", null, null);
  }

  @Test
  public void fail_if_regexp_if_not_valid() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'projectKeyPattern' parameter must be a valid Java regular expression. '[azerty' was passed");

    newRequest("Finance", null, "[azerty");
  }

  @Test
  public void fail_if_name_already_exists_in_database_case_insensitive() {
    loginAsAdmin(db.getDefaultOrganization());
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A template with the name '" + template.getName() + "' already exists (case insensitive).");

    newRequest(template.getName(), null, null);
  }

  @Test
  public void fail_if_not_admin() {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, db.getDefaultOrganization());

    expectedException.expect(ForbiddenException.class);

    newRequest("Finance", null, null);
  }

  private TestResponse newRequest(@Nullable String name, @Nullable String description, @Nullable String projectPattern) {
    TestRequest request = newRequest();
    if (name != null) {
      request.setParam(PARAM_NAME, name);
    }
    if (description != null) {
      request.setParam(PARAM_DESCRIPTION, description);
    }
    if (projectPattern != null) {
      request.setParam(PARAM_PROJECT_KEY_PATTERN, projectPattern);
    }

    return request.execute();
  }
}
