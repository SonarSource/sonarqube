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
import org.sonar.api.impl.ws.SimpleGetRequest;
import org.sonar.api.server.ws.Request;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.ProjectTesting;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.almintegration.ws.ImportHelper.PARAM_ALM_SETTING;
import static org.sonarqube.ws.Projects.CreateWsResponse;

public class ImportHelperIT {

  // We use a GitHub ALM just because we have to use one. Tests are not specific to GitHub.
  private static final ALM GITHUB_ALM = ALM.GITHUB;
  private final System2 system2 = System2.INSTANCE;
  private final ProjectDto projectDto = ProjectTesting.newPublicProjectDto();
  private final Request emptyRequest = new SimpleGetRequest();

  @Rule
  public final DbTester db = DbTester.create(system2);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();


  private final ImportHelper underTest = new ImportHelper(db.getDbClient(), userSession);

  @Test
  public void checkProvisionProjectPermission_whenNoPermissions_shouldThrow() {
    assertThatThrownBy(underTest::checkProvisionProjectPermission)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void getAlmConfig_whenNoAlmSettingKeyAndNoConfig_shouldThrow() {
    assertThatThrownBy(() -> underTest.getAlmSettingDtoForAlm(emptyRequest, GITHUB_ALM))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("There is no GITHUB configuration for DevOps Platform. Please add one.");
  }

  @Test
  public void getAlmConfig_whenNoAlmSettingKeyAndOnlyOneConfig_shouldReturnConfig() {
    AlmSettingDto githubAlmSettingDto = db.almSettings().insertGitHubAlmSetting();

    AlmSettingDto almSettingDto = underTest.getAlmSettingDtoForAlm(emptyRequest, GITHUB_ALM);

    assertThat(almSettingDto).usingRecursiveComparison().isEqualTo(githubAlmSettingDto);
  }

  @Test
  public void getAlmConfig_whenNoAlmSettingKeyAndMultipleConfigs_shouldThrow() {
    db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubAlmSetting();

    assertThatThrownBy(() -> underTest.getAlmSettingDtoForAlm(emptyRequest, GITHUB_ALM))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter almSetting is required as there are multiple DevOps Platform configurations.");
  }

  @Test
  public void getAlmConfig_whenAlmSettingKeyProvidedButDoesNotExist_shouldThrow() {
    Request request = new SimpleGetRequest()
      .setParam(PARAM_ALM_SETTING, "key");

    assertThatThrownBy(() -> underTest.getAlmSettingDtoForAlm(request, GITHUB_ALM))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform configuration 'key' not found.");
  }

  @Test
  public void getAlmConfig_whenConfigExists_shouldReturnConfig(){
    AlmSettingDto almSettingDto = db.almSettings().insertAzureAlmSetting();
    Request request = new SimpleGetRequest()
      .setParam(PARAM_ALM_SETTING, almSettingDto.getKey());

    AlmSettingDto result = underTest.getAlmSettingDtoForAlm(request, GITHUB_ALM);

    assertThat(result.getUuid()).isEqualTo(almSettingDto.getUuid());
  }

  @Test
  public void getUserUuid_whenUserUuidNull_shouldThrow() {
    assertThatThrownBy(underTest::getUserUuid)
      .isInstanceOf(NullPointerException.class)
      .hasMessage("User UUID cannot be null.");
  }

  @Test
  public void toCreateResponse_shouldReturnProjectResponse() {
    CreateWsResponse response = ImportHelper.toCreateResponse(projectDto);
    CreateWsResponse.Project project = response.getProject();

    assertThat(project).extracting(CreateWsResponse.Project::getKey, CreateWsResponse.Project::getName,
        CreateWsResponse.Project::getQualifier)
      .containsExactly(projectDto.getKey(), projectDto.getName(), projectDto.getQualifier());
  }
}
