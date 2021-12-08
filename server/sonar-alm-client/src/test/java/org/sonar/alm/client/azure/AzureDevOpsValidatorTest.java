/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import org.junit.Test;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonarqube.ws.AlmSettings;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AzureDevOpsValidatorTest {

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

  @Test(expected = Test.None.class /* no exception expected */)
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
}
