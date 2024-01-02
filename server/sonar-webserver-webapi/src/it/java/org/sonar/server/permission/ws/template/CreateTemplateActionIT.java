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
package org.sonar.server.permission.ws.template;

import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.permission.ws.BasePermissionWsIT;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_DESCRIPTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY_PATTERN;

public class CreateTemplateActionIT extends BasePermissionWsIT<CreateTemplateAction> {

  private static final long NOW = 1_440_512_328_743L;
  private final System2 system = new TestSystem2().setNow(NOW);

  @Override
  protected CreateTemplateAction buildWsAction() {
    return new CreateTemplateAction(db.getDbClient(), userSession, system);
  }

  @Test
  public void create_full_permission_template() {
    loginAsAdmin();

    TestResponse result = newRequest("Finance", "Permissions for financially related projects", ".*\\.finance\\..*");

    assertJson(result.getInput())
      .ignoreFields("id")
      .isSimilarTo(getClass().getResource("create_template-example.json"));
    PermissionTemplateDto finance = selectPermissionTemplate("Finance");
    assertThat(finance.getName()).isEqualTo("Finance");
    assertThat(finance.getDescription()).isEqualTo("Permissions for financially related projects");
    assertThat(finance.getKeyPattern()).isEqualTo(".*\\.finance\\..*");
    assertThat(finance.getUuid()).isNotEmpty();
    assertThat(finance.getCreatedAt().getTime()).isEqualTo(NOW);
    assertThat(finance.getUpdatedAt().getTime()).isEqualTo(NOW);
  }

  @Test
  public void create_minimalist_permission_template() {
    loginAsAdmin();

    newRequest("Finance", null, null);

    PermissionTemplateDto finance = selectPermissionTemplate("Finance");
    assertThat(finance.getName()).isEqualTo("Finance");
    assertThat(finance.getDescription()).isNullOrEmpty();
    assertThat(finance.getKeyPattern()).isNullOrEmpty();
    assertThat(finance.getUuid()).isNotEmpty();
    assertThat(finance.getCreatedAt().getTime()).isEqualTo(NOW);
    assertThat(finance.getUpdatedAt().getTime()).isEqualTo(NOW);
  }

  @Test
  public void fail_if_name_not_provided() {
    loginAsAdmin();

    assertThatThrownBy(() -> newRequest(null, null, null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_name_empty() {
    loginAsAdmin();

    assertThatThrownBy(() -> newRequest("", null, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'name' parameter is missing");
  }

  @Test
  public void fail_if_regexp_if_not_valid() {
    loginAsAdmin();

    assertThatThrownBy(() -> newRequest("Finance", null, "[azerty"))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The 'projectKeyPattern' parameter must be a valid Java regular expression. '[azerty' was passed");
  }

  @Test
  public void fail_if_name_already_exists_in_database_case_insensitive() {
    loginAsAdmin();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate();

    assertThatThrownBy(() -> newRequest(template.getName(), null, null))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("A template with the name '" + template.getName() + "' already exists (case insensitive).");
  }

  @Test
  public void fail_if_not_admin() {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> newRequest("Finance", null, null))
      .isInstanceOf(ForbiddenException.class);
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
