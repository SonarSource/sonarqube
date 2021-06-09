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
package org.sonar.server.almintegration.validator;

import org.junit.Test;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.db.alm.setting.AlmSettingDto;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.db.almsettings.AlmSettingsTesting.newBitbucketAlmSettingDto;

public class BitbucketServerSettingsValidatorTest {

  private final BitbucketServerRestClient bitbucketServerRestClient = mock(BitbucketServerRestClient.class);
  private final BitbucketServerSettingsValidator underTest = new BitbucketServerSettingsValidator(bitbucketServerRestClient);

  @Test
  public void validate_success() {
    AlmSettingDto almSettingDto = newBitbucketAlmSettingDto()
      .setUrl("http://abc.com")
      .setPersonalAccessToken("abc");

    underTest.validate(almSettingDto);

    verify(bitbucketServerRestClient, times(1)).validateUrl("http://abc.com");
    verify(bitbucketServerRestClient, times(1)).validateToken("http://abc.com", "abc");
    verify(bitbucketServerRestClient, times(1)).validateReadPermission("http://abc.com", "abc");
  }

  @Test
  public void validate_failure_on_incomplete_configuration() {
    AlmSettingDto almSettingDto = newBitbucketAlmSettingDto()
      .setUrl(null)
      .setPersonalAccessToken("abc");

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void validate_failure_on_bitbucket_server_api_error() {
    doThrow(new IllegalArgumentException("error")).when(bitbucketServerRestClient).validateUrl(anyString());
    AlmSettingDto almSettingDto = newBitbucketAlmSettingDto()
      .setUrl("http://abc.com")
      .setPersonalAccessToken("abc");

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class);
  }
}
