/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Date;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.ws.BasePermissionWsIT;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_DESCRIPTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY_PATTERN;

public class UpdateTemplateActionIT extends BasePermissionWsIT<UpdateTemplateAction> {

  private final System2 system = spy(System2.INSTANCE);
  private PermissionTemplateDto template;

  @Override
  protected UpdateTemplateAction buildWsAction() {
    return new UpdateTemplateAction(db.getDbClient(), userSession, system, newPermissionWsSupport());
  }

  @Before
  public void setUp() {
    when(system.now()).thenReturn(1_440_512_328_743L);
    template = db.getDbClient().permissionTemplateDao().insert(db.getSession(), newPermissionTemplateDto()
      .setName("Permission Template Name")
      .setDescription("Permission Template Description")
      .setKeyPattern(".*\\.pattern\\..*")
      .setCreatedAt(new Date(1_000_000_000_000L))
      .setUpdatedAt(new Date(1_000_000_000_000L)));
    db.commit();
  }

  @Test
  public void update_all_permission_template_fields() {
    loginAsAdmin();

    String result = call(template.getUuid(), "Finance", "Permissions for financially related projects", ".*\\.finance\\..*");

    assertJson(result)
      .ignoreFields("id")
      .isSimilarTo(getClass().getResource("update_template-example.json"));
    PermissionTemplateDto finance = selectPermissionTemplate("Finance");
    assertThat(finance.getName()).isEqualTo("Finance");
    assertThat(finance.getDescription()).isEqualTo("Permissions for financially related projects");
    assertThat(finance.getKeyPattern()).isEqualTo(".*\\.finance\\..*");
    assertThat(finance.getUuid()).isEqualTo(template.getUuid());
    assertThat(finance.getCreatedAt()).isEqualTo(template.getCreatedAt());
    assertThat(finance.getUpdatedAt().getTime()).isEqualTo(1440512328743L);
  }

  @Test
  public void update_with_the_same_values() {
    loginAsAdmin();

    call(template.getUuid(), template.getName(), template.getDescription(), template.getKeyPattern());

    PermissionTemplateDto reloaded = db.getDbClient().permissionTemplateDao().selectByUuid(db.getSession(), template.getUuid());
    assertThat(reloaded.getName()).isEqualTo(template.getName());
    assertThat(reloaded.getDescription()).isEqualTo(template.getDescription());
    assertThat(reloaded.getKeyPattern()).isEqualTo(template.getKeyPattern());
  }

  @Test
  public void update_with_null_values() {
    template = db.getDbClient().permissionTemplateDao().insert(db.getSession(), newPermissionTemplateDto()
      .setName("Test")
      .setDescription(null)
      .setKeyPattern(null)
      .setCreatedAt(new Date(1_000_000_000_000L))
      .setUpdatedAt(new Date(1_000_000_000_000L)));
    db.commit();

    loginAsAdmin();

    call(template.getUuid(), template.getName(), null, null);

    PermissionTemplateDto reloaded = db.getDbClient().permissionTemplateDao().selectByUuid(db.getSession(), template.getUuid());
    assertThat(reloaded.getName()).isEqualTo(template.getName());
    assertThat(reloaded.getDescription()).isNull();
    assertThat(reloaded.getKeyPattern()).isNull();
  }

  @Test
  public void update_name_only() {
    loginAsAdmin();

    call(template.getUuid(), "Finance", null, null);

    PermissionTemplateDto finance = selectPermissionTemplate("Finance");
    assertThat(finance.getName()).isEqualTo("Finance");
    assertThat(finance.getDescription()).isEqualTo(template.getDescription());
    assertThat(finance.getKeyPattern()).isEqualTo(template.getKeyPattern());
  }

  @Test
  public void fail_if_key_is_not_found() {
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      call("unknown-key", null, null, null);
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Permission template with id 'unknown-key' is not found");
  }

  @Test
  public void fail_if_name_already_exists_in_another_template() {
    loginAsAdmin();
    PermissionTemplateDto anotherTemplate = addTemplate();

    assertThatThrownBy(() ->  {
      call(this.template.getUuid(), anotherTemplate.getName(), null, null);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("A template with the name '" + anotherTemplate.getName() + "' already exists (case insensitive).");
  }

  @Test
  public void fail_if_key_is_not_provided() {
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      call(null, "Finance", null, null);
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_name_empty() {
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      call(template.getUuid(), "", null, null);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The template name must not be blank");
  }

  @Test
  public void fail_if_name_has_just_whitespaces() {
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      call(template.getUuid(), "  \r\n", null, null);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The template name must not be blank");
  }

  @Test
  public void fail_if_regexp_if_not_valid() {
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      call(template.getUuid(), "Finance", null, "[azerty");
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The 'projectKeyPattern' parameter must be a valid Java regular expression. '[azerty' was passed");
  }

  @Test
  public void fail_if_name_already_exists_in_database_case_insensitive() {
    loginAsAdmin();
    PermissionTemplateDto anotherTemplate = addTemplate();

    String nameCaseInsensitive = anotherTemplate.getName().toUpperCase();

    assertThatThrownBy(() ->  {
      call(this.template.getUuid(), nameCaseInsensitive, null, null);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("A template with the name '" + nameCaseInsensitive + "' already exists (case insensitive).");
  }

  @Test
  public void fail_if_not_logged_in() {
    assertThatThrownBy(() ->  {
      userSession.anonymous();

      call(template.getUuid(), "Finance", null, null);
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_if_not_admin() {
    userSession.logIn().addPermission(SCAN);

    assertThatThrownBy(() ->  {
      call(template.getUuid(), "Finance", null, null);
    })
      .isInstanceOf(ForbiddenException.class);
  }

  private String call(@Nullable String key, @Nullable String name, @Nullable String description, @Nullable String projectPattern) {
    TestRequest request = newRequest();
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
      request.setParam(PARAM_PROJECT_KEY_PATTERN, projectPattern);
    }

    return request.execute().getInput();
  }
}
