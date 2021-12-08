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
package org.sonar.alm.client.bitbucket.bitbucketcloud;

import org.junit.Test;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.AlmSettingDto;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BitbucketCloudValidatorTest {

  private final BitbucketCloudRestClient bitbucketCloudRestClient = mock(BitbucketCloudRestClient.class);
  private final Settings settings = mock(Settings.class);

  private final BitbucketCloudValidator underTest = new BitbucketCloudValidator(bitbucketCloudRestClient, settings);

  private static final String EXAMPLE_APP_ID = "123";

  @Test
  public void validate_forwardsExceptionFromRestClient() {
    AlmSettingDto dto = mock(AlmSettingDto.class);
    when(dto.getAppId()).thenReturn(EXAMPLE_APP_ID);
    when(dto.getClientId()).thenReturn("clientId");
    when(dto.getDecryptedClientSecret(any())).thenReturn("secret");

    doThrow(new IllegalArgumentException("Exception from bitbucket cloud rest client"))
      .when(bitbucketCloudRestClient).validate(any(), any(), any());

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validate(dto))
      .withMessage("Exception from bitbucket cloud rest client");
  }

  @Test
  public void validate_callsValidate() {
    AlmSettingDto dto = mock(AlmSettingDto.class);
    when(dto.getAppId()).thenReturn(EXAMPLE_APP_ID);
    when(dto.getClientId()).thenReturn("clientId");
    when(dto.getDecryptedClientSecret(any())).thenReturn("secret");

    underTest.validate(dto);

    verify(bitbucketCloudRestClient, times(1)).validate("clientId", "secret", EXAMPLE_APP_ID);
  }
}
