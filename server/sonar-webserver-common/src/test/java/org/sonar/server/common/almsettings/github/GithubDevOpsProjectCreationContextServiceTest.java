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
package org.sonar.server.common.almsettings.github;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.auth.github.client.GithubApplicationClient;
import org.sonar.auth.github.client.GithubApplicationClient.Repository;
import org.sonar.auth.github.security.UserAccessToken;
import org.sonar.db.DbClient;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.common.almsettings.DevOpsProjectCreationContext;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubDevOpsProjectCreationContextServiceTest {

  private static final DevOpsProjectDescriptor DEV_OPS_PROJECT_DESCRIPTOR = new DevOpsProjectDescriptor(ALM.GITHUB, "project-key", "repository-identifier", null);

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private UserSession userSession;
  @Mock
  private GithubApplicationClient githubApplicationClient;

  @InjectMocks
  private GithubDevOpsProjectCreationContextService githubDevOpsProjectService;

  @Test
  void create_whenUserUuidIsNull_shouldThrow() {
    AlmSettingDto almSettingDto = mock();

    assertThatNullPointerException()
      .isThrownBy(() -> githubDevOpsProjectService.create(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR))
      .withMessage("User UUID cannot be null.");
  }

  @Test
  void create_whenNoPat_shouldThrow() {
    AlmSettingDto almSettingDto = mock();

    when(userSession.getUuid()).thenReturn("user-uuid");
    when(dbClient.almPatDao().selectByUserAndAlmSetting(dbClient.openSession(false), userSession.getUuid(), almSettingDto)).thenReturn(Optional.empty());

    assertThatIllegalArgumentException()
      .isThrownBy(() -> githubDevOpsProjectService.create(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR))
      .withMessage("No personal access token found");
  }

  @Test
  void create_whenRepoNotFound_throws() {
    AlmSettingDto almSettingDto = mockAlmSettingDto();

    mockValidAccessToken(almSettingDto);

    assertThatIllegalStateException()
      .isThrownBy(() -> githubDevOpsProjectService.create(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR))
      .withMessage("Impossible to find the repository 'repository-identifier' on GitHub, using the devops config alm-config-key");
  }

  @Test
  void create_whenRepoFound_createsDevOpsProject() {
    AlmSettingDto almSettingDto = mockAlmSettingDto();

    AlmPatDto almPatDto = mockValidAccessToken(almSettingDto);

    Repository repository = mockGitHubRepository(almPatDto, almSettingDto);

    DevOpsProjectCreationContext devOpsProjectCreationContext = githubDevOpsProjectService.create(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR);

    assertThat(devOpsProjectCreationContext.name()).isEqualTo(repository.getName());
    assertThat(devOpsProjectCreationContext.fullName()).isEqualTo(repository.getFullName());
    assertThat(devOpsProjectCreationContext.devOpsPlatformIdentifier()).isEqualTo(repository.getFullName());
    assertThat(devOpsProjectCreationContext.isPublic()).isTrue();
    assertThat(devOpsProjectCreationContext.defaultBranchName()).isEqualTo(repository.getDefaultBranch());
  }

  private static AlmSettingDto mockAlmSettingDto() {
    AlmSettingDto almSettingDto = mock();
    when(almSettingDto.getUrl()).thenReturn("http://www.url.com");
    lenient().when(almSettingDto.getKey()).thenReturn("alm-config-key");
    return almSettingDto;
  }

  private AlmPatDto mockValidAccessToken(AlmSettingDto almSettingDto) {
    when(userSession.getUuid()).thenReturn("user-uuid");
    AlmPatDto almPatDto = mock();
    when(dbClient.almPatDao().selectByUserAndAlmSetting(dbClient.openSession(false), userSession.getUuid(), almSettingDto)).thenReturn(Optional.of(almPatDto));
    when(almPatDto.getPersonalAccessToken()).thenReturn("token");
    return almPatDto;
  }

  private Repository mockGitHubRepository(AlmPatDto almPatDto, AlmSettingDto almSettingDto) {
    Repository repository = mock();
    when(repository.getName()).thenReturn("name");
    when(repository.getFullName()).thenReturn("full-name");
    when(repository.isPrivate()).thenReturn(false);
    when(repository.getDefaultBranch()).thenReturn("default-branch");
    UserAccessToken accessToken = new UserAccessToken(almPatDto.getPersonalAccessToken());
    when(githubApplicationClient.getRepository(almSettingDto.getUrl(), accessToken,
      DEV_OPS_PROJECT_DESCRIPTOR.repositoryIdentifier())).thenReturn(Optional.of(repository));
    return repository;
  }

}
