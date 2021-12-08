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
package org.sonar.alm.client.bitbucketserver;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BitbucketServerSettingsValidatorTest {
  private static final Encryption encryption = mock(Encryption.class);
  private static final Settings settings = mock(Settings.class);

  private final BitbucketServerRestClient bitbucketServerRestClient = mock(BitbucketServerRestClient.class);
  private final BitbucketServerSettingsValidator underTest = new BitbucketServerSettingsValidator(bitbucketServerRestClient, settings);

  @BeforeClass
  public static void setUp() {
    when(settings.getEncryption()).thenReturn(encryption);
  }

  @Test
  public void validate_success() {
    AlmSettingDto almSettingDto = createNewBitbucketDto("http://abc.com", "abc");
    when(encryption.isEncrypted(any())).thenReturn(false);

    underTest.validate(almSettingDto);

    verify(bitbucketServerRestClient, times(1)).validateUrl("http://abc.com");
    verify(bitbucketServerRestClient, times(1)).validateToken("http://abc.com", "abc");
    verify(bitbucketServerRestClient, times(1)).validateReadPermission("http://abc.com", "abc");
  }

  @Test
  public void validate_success_with_encrypted_token() {
    String encryptedToken = "abc";
    String decryptedToken = "decrypted-token";
    AlmSettingDto almSettingDto = createNewBitbucketDto("http://abc.com", encryptedToken);
    when(encryption.isEncrypted(encryptedToken)).thenReturn(true);
    when(encryption.decrypt(encryptedToken)).thenReturn(decryptedToken);

    underTest.validate(almSettingDto);

    verify(bitbucketServerRestClient, times(1)).validateUrl("http://abc.com");
    verify(bitbucketServerRestClient, times(1)).validateToken("http://abc.com", decryptedToken);
    verify(bitbucketServerRestClient, times(1)).validateReadPermission("http://abc.com", decryptedToken);
  }

  @Test
  public void validate_failure_on_incomplete_configuration() {
    AlmSettingDto almSettingDto = createNewBitbucketDto(null, "abc");

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void validate_failure_on_bitbucket_server_api_error() {
    doThrow(new IllegalArgumentException("error")).when(bitbucketServerRestClient).validateUrl(anyString());
    AlmSettingDto almSettingDto = createNewBitbucketDto("http://abc.com", "abc");

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private AlmSettingDto createNewBitbucketDto(String url, String pat) {
    AlmSettingDto dto = new AlmSettingDto();
    dto.setAlm(ALM.BITBUCKET);
    dto.setUrl(url);
    dto.setPersonalAccessToken(pat);
    return dto;
  }
}
