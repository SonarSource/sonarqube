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
package org.sonar.server.almintegration.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonarqube.ws.Projects.CreateWsResponse;

public class ImportHelperTest {

  private final System2 system2 = System2.INSTANCE;
  private final ComponentDto componentDto = ComponentTesting.newPublicProjectDto();
  private final Request request = mock(Request.class);

  @Rule
  public final DbTester db = DbTester.create(system2);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final ImportHelper underTest = new ImportHelper(db.getDbClient(), userSession);

  @Test
  public void it_throws_exception_when_provisioning_project_without_permission() {
    assertThatThrownBy(() -> underTest.checkProvisionProjectPermission())
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void it_throws_exception_on_get_alm_setting_when_key_is_empty() {
    assertThatThrownBy(() -> underTest.getAlmSetting(request))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void it_throws_exception_on_get_alm_setting_when_key_is_not_found() {
    when(request.mandatoryParam("almSetting")).thenReturn("key");
    assertThatThrownBy(() -> underTest.getAlmSetting(request))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform Setting 'key' not found");
  }

  @Test
  public void it_throws_exception_when_user_uuid_is_null() {
    assertThatThrownBy(() -> underTest.getUserUuid())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("User UUID cannot be null");
  }

  @Test
  public void it_returns_create_response() {
    CreateWsResponse response = ImportHelper.toCreateResponse(componentDto);
    CreateWsResponse.Project project = response.getProject();

    assertThat(project).extracting(CreateWsResponse.Project::getKey, CreateWsResponse.Project::getName,
        CreateWsResponse.Project::getQualifier)
      .containsExactly(componentDto.getKey(), componentDto.name(), componentDto.qualifier());
  }
}
