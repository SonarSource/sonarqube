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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Test;
import org.mockito.Mock;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DelegatingDevOpsPlatformServiceTest {

  private static final DelegatingDevOpsPlatformService NO_DEVOPS_PLATFORMS = new DelegatingDevOpsPlatformService(emptySet());
  private static final DelegatingDevOpsPlatformService MULTIPLE_DEVOPS_PLATFORMS = new DelegatingDevOpsPlatformService(
    Set.of(mockGitHubDevOpsPlatformService(), mockAzureDevOpsPlatformService()));

  @Mock
  private DbSession dbSession;

  @Test
  public void getDevOpsPlatform_shouldThrow() {
    assertThatThrownBy(NO_DEVOPS_PLATFORMS::getDevOpsPlatform)
      .isInstanceOf(NotImplementedException.class);
  }

  @Test
  public void getDevOpsProjectDescriptor_whenNoDelegates_shouldReturnOptionalEmpty() {
    Optional<DevOpsProjectDescriptor> devOpsProjectDescriptor = NO_DEVOPS_PLATFORMS.getDevOpsProjectDescriptor(Map.of());

    assertThat(devOpsProjectDescriptor).isEmpty();
  }

  @Test
  public void getDevOpsProjectDescriptor_whenDelegates_shouldReturnDelegateResponse() {
    Optional<DevOpsProjectDescriptor> devOpsProjectDescriptor = MULTIPLE_DEVOPS_PLATFORMS.getDevOpsProjectDescriptor(Map.of(
      "githubUrl", "githubUrl"
    ));

    assertThat(devOpsProjectDescriptor)
      .isPresent()
      .get().usingRecursiveComparison().isEqualTo(new DevOpsProjectDescriptor(ALM.GITHUB, "githubUrl", "githubRepo"));
  }

  @Test
  public void getValidAlmSettingDto_whenNoDelegates_shouldReturnOptionalEmpty() {
    Optional<AlmSettingDto> almSettingDto = NO_DEVOPS_PLATFORMS.getValidAlmSettingDto(dbSession, mock(DevOpsProjectDescriptor.class));

    assertThat(almSettingDto).isEmpty();
  }

  @Test
  public void getValidAlmSettingDto_whenDelegates_shouldReturnDelegateResponse() {
    Optional<AlmSettingDto> almSettingDto = MULTIPLE_DEVOPS_PLATFORMS.getValidAlmSettingDto(dbSession, new DevOpsProjectDescriptor(ALM.GITHUB, "githubUrl", "githubRepo"));

    assertThat(almSettingDto)
      .isPresent()
      .get()
      .usingRecursiveComparison().isEqualTo(new AlmSettingDto().setAlm(ALM.GITHUB));
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenNoDelegates_shouldThrow() {
    DevOpsProjectDescriptor devOpsProjectDescriptor = mock();
    when(devOpsProjectDescriptor.alm()).thenReturn(ALM.GITHUB);

    assertThatIllegalStateException()
      .isThrownBy(() -> NO_DEVOPS_PLATFORMS.isScanAllowedUsingPermissionsFromDevopsPlatform(mock(), devOpsProjectDescriptor))
      .withMessage("No delegate found to handle projects on GITHUB");
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenDelegates_shouldReturnDelegateResponse() {
    DevOpsProjectDescriptor devOpsProjectDescriptor = mock();
    when(devOpsProjectDescriptor.alm()).thenReturn(ALM.GITHUB);

    boolean isScanAllowed = MULTIPLE_DEVOPS_PLATFORMS.isScanAllowedUsingPermissionsFromDevopsPlatform(mock(), devOpsProjectDescriptor);

    assertThat(isScanAllowed).isTrue();
  }

  private static DevOpsPlatformService mockGitHubDevOpsPlatformService() {
    DevOpsPlatformService mockDevOpsPlatformService = mock();
    when(mockDevOpsPlatformService.getDevOpsPlatform()).thenReturn(ALM.GITHUB);
    when(mockDevOpsPlatformService.getDevOpsProjectDescriptor(Map.of("githubUrl", "githubUrl")))
      .thenReturn(Optional.of(new DevOpsProjectDescriptor(ALM.GITHUB, "githubUrl", "githubRepo")));
    when(mockDevOpsPlatformService.getValidAlmSettingDto(any(), any()))
      .thenReturn(Optional.of(new AlmSettingDto().setAlm(ALM.GITHUB)));
    when(mockDevOpsPlatformService.isScanAllowedUsingPermissionsFromDevopsPlatform(any(), any())).thenReturn(true);
    return mockDevOpsPlatformService;
  }

  private static DevOpsPlatformService mockAzureDevOpsPlatformService() {
    DevOpsPlatformService mockDevOpsPlatformService = mock();
    when(mockDevOpsPlatformService.getDevOpsPlatform()).thenReturn(ALM.AZURE_DEVOPS);
    when(mockDevOpsPlatformService.getDevOpsProjectDescriptor(Map.of("azureUrl", "azureUrl")))
      .thenReturn(Optional.of(new DevOpsProjectDescriptor(ALM.AZURE_DEVOPS, "azureUrl", "azureProject")));
    when(mockDevOpsPlatformService.getValidAlmSettingDto(any(), any()))
      .thenReturn(Optional.of(new AlmSettingDto().setAlm(ALM.AZURE_DEVOPS)));
    return mockDevOpsPlatformService;
  }

}
