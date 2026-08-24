/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.alm.client.azure;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.db.alm.setting.AlmSettingDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AzureDevOpsValidatorTest {

  @Rule
  public LogTester logTester = new LogTester();

  private final AzureDevOpsHttpClient azureDevOpsHttpClient = mock(AzureDevOpsHttpClient.class);
  private final Settings settings = mock(Settings.class);
  private final AzureDevOpsValidator underTest = new AzureDevOpsValidator(azureDevOpsHttpClient, settings);

  @Test
  public void validate_givenHttpClientThrowingException_throwException() {
    AlmSettingDto dto = createMockDto();

    doThrow(new IllegalArgumentException()).when(azureDevOpsHttpClient).checkPAT(any(), any());

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validate(dto))
      .withMessage("Invalid Azure URL or Personal Access Token");

  }

  @Test
  public void validate_givenHttpClientNotThrowingException_doesNotThrowException() {
    AlmSettingDto dto = createMockDto();

    underTest.validate(dto);
  }

  private AlmSettingDto createMockDto() {
    AlmSettingDto dto = mock(AlmSettingDto.class);
    when(dto.getUrl()).thenReturn("http://azure-devops-url.url");
    when(dto.getDecryptedPersonalAccessToken(any())).thenReturn("decrypted-token");
    return dto;
  }

  @Test
  public void checkPatIsNotGlobal_givenServerUrl_doesNotProbe() {
    underTest.checkPatIsNotGlobal("https://ado.sonarqube.com/", "token");

    verifyNoInteractions(azureDevOpsHttpClient);
  }

  @Test
  public void checkPatIsNotGlobal_givenCloudUrlAndScopedPat_doesNotThrow() {
    when(azureDevOpsHttpClient.isGlobalPat("token")).thenReturn(false);

    underTest.checkPatIsNotGlobal("https://dev.azure.com/myorg", "token");
  }

  @Test
  public void checkPatIsNotGlobal_givenCloudUrlAndGlobalPat_throws() {
    when(azureDevOpsHttpClient.isGlobalPat("token")).thenReturn(true);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.checkPatIsNotGlobal("https://dev.azure.com/myorg", "token"))
      .withMessage(AzureDevOpsValidator.GLOBAL_PAT_ERROR_MESSAGE);
  }

  @Test
  public void checkPatIsNotGlobal_givenVisualStudioUrlAndGlobalPat_throws() {
    when(azureDevOpsHttpClient.isGlobalPat("token")).thenReturn(true);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.checkPatIsNotGlobal("https://myorg.visualstudio.com", "token"))
      .withMessage(AzureDevOpsValidator.GLOBAL_PAT_ERROR_MESSAGE);
  }

  @Test
  public void checkPatIsNotGlobal_givenProbeFailure_failsOpenAndLogsWarning() {
    doThrow(new IllegalArgumentException("boom")).when(azureDevOpsHttpClient).isGlobalPat("token");

    underTest.checkPatIsNotGlobal("https://dev.azure.com/myorg", "token");

    assertThat(logTester.logs(Level.WARN)).isNotEmpty();
  }

  @Test
  public void checkPatIsNotGlobal_givenProbeServerError_failsOpenAndLogsWarning() {
    doThrow(new AzureDevopsServerException(500, "boom")).when(azureDevOpsHttpClient).isGlobalPat("token");

    underTest.checkPatIsNotGlobal("https://dev.azure.com/myorg", "token");

    assertThat(logTester.logs(Level.WARN)).isNotEmpty();
  }
}
