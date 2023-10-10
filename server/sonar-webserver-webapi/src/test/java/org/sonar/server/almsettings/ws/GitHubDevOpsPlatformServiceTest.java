/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.almsettings.ws;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.github.config.GithubAppConfiguration;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.almsettings.ws.GitHubDevOpsPlatformService.DEVOPS_PLATFORM_PROJECT_IDENTIFIER;
import static org.sonar.server.almsettings.ws.GitHubDevOpsPlatformService.DEVOPS_PLATFORM_URL;


@RunWith(MockitoJUnitRunner.class)
public class GitHubDevOpsPlatformServiceTest {

  private static final DevOpsProjectDescriptor GITHUB_PROJECT_DESCRIPTOR = new DevOpsProjectDescriptor(ALM.GITHUB, "url", "repo");

  @Mock
  private DbSession dbSession;
  @Mock
  private AlmSettingDao almSettingDao;
  @Mock
  private GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  @Mock
  private GithubApplicationClient githubApplicationClient;
  @InjectMocks
  private GitHubDevOpsPlatformService gitHubDevOpsPlatformService;

  @Test
  public void getDevOpsPlatform_shouldReturnGitHub() {
    assertThat(gitHubDevOpsPlatformService.getDevOpsPlatform())
      .isEqualTo(ALM.GITHUB);
  }

  @Test
  public void getDevOpsProjectDescriptor_whenNoCharacteristics_shouldReturnEmpty() {
    Optional<DevOpsProjectDescriptor> devOpsProjectDescriptor = gitHubDevOpsPlatformService.getDevOpsProjectDescriptor(Map.of());

    assertThat(devOpsProjectDescriptor).isEmpty();
  }

  @Test
  public void getDevOpsProjectDescriptor_whenValidCharacteristics_shouldReturn() {
    Optional<DevOpsProjectDescriptor> devOpsProjectDescriptor = gitHubDevOpsPlatformService.getDevOpsProjectDescriptor(Map.of(
      DEVOPS_PLATFORM_URL, GITHUB_PROJECT_DESCRIPTOR.url(),
      DEVOPS_PLATFORM_PROJECT_IDENTIFIER, GITHUB_PROJECT_DESCRIPTOR.projectIdentifier()
    ));

    assertThat(devOpsProjectDescriptor)
      .isPresent()
      .get().usingRecursiveComparison().isEqualTo(GITHUB_PROJECT_DESCRIPTOR);
  }

  @Test
  public void getValidAlmSettingDto_whenNoAlmSetting_shouldReturnEmpty() {
    when(almSettingDao.selectByAlm(dbSession, ALM.GITHUB)).thenReturn(emptyList());

    Optional<AlmSettingDto> almSettingDto = gitHubDevOpsPlatformService.getValidAlmSettingDto(dbSession, GITHUB_PROJECT_DESCRIPTOR);

    assertThat(almSettingDto).isEmpty();
  }

  @Test
  public void getValidAlmSettingDto_whenMultipleAlmSetting_shouldReturnTheRightOne() {
    AlmSettingDto mockGitHubAlmSettingDtoNoAccess = mockGitHubAlmSettingDto(false);
    AlmSettingDto mockGitHubAlmSettingDtoAccess = mockGitHubAlmSettingDto(true);
    when(almSettingDao.selectByAlm(dbSession, ALM.GITHUB)).thenReturn(List.of(mockGitHubAlmSettingDtoNoAccess, mockGitHubAlmSettingDtoAccess));

    Optional<AlmSettingDto> almSettingDto = gitHubDevOpsPlatformService.getValidAlmSettingDto(dbSession, GITHUB_PROJECT_DESCRIPTOR);

    assertThat(almSettingDto)
      .isPresent()
      .get().isEqualTo(mockGitHubAlmSettingDtoAccess);
  }

  private AlmSettingDto mockGitHubAlmSettingDto(boolean repoAccess) {
    AlmSettingDto mockAlmSettingDto = mock();
    when(mockAlmSettingDto.getUrl()).thenReturn(GITHUB_PROJECT_DESCRIPTOR.url());
    GithubAppConfiguration mockGithubAppConfiguration = mock(GithubAppConfiguration.class);
    when(githubGlobalSettingsValidator.validate(mockAlmSettingDto)).thenReturn(mockGithubAppConfiguration);
    when(githubApplicationClient.getInstallationId(eq(mockGithubAppConfiguration), any())).thenReturn(repoAccess ? Optional.of(1L) : Optional.empty());
    return mockAlmSettingDto;
  }

}
