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

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.config.GithubAppConfiguration;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.AlmSettingDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDto;

public class GithubGlobalSettingsValidatorTest {
  private static final Encryption encryption = mock(Encryption.class);
  private static final Settings settings = mock(Settings.class);

  private final GithubApplicationClientImpl appClient = mock(GithubApplicationClientImpl.class);
  private final GithubGlobalSettingsValidator underTest = new GithubGlobalSettingsValidator(appClient, settings);

  @BeforeClass
  public static void setUp() {
    when(settings.getEncryption()).thenReturn(encryption);
  }

  @Test
  public void github_global_settings_validation() {
    AlmSettingDto almSettingDto = newGithubAlmSettingDto()
      .setClientId("clientId")
      .setClientSecret("clientSecret");
    when(encryption.isEncrypted(any())).thenReturn(false);

    GithubAppConfiguration configuration = underTest.validate(almSettingDto);

    ArgumentCaptor<GithubAppConfiguration> configurationArgumentCaptor = ArgumentCaptor.forClass(GithubAppConfiguration.class);
    verify(appClient).checkApiEndpoint(configurationArgumentCaptor.capture());
    verify(appClient).checkAppPermissions(configurationArgumentCaptor.capture());
    assertThat(configuration.getId()).isEqualTo(configurationArgumentCaptor.getAllValues().get(0).getId());
    assertThat(configuration.getId()).isEqualTo(configurationArgumentCaptor.getAllValues().get(1).getId());
  }

  @Test
  public void github_global_settings_validation_with_encrypted_key() {
    String encryptedKey = "encrypted-key";
    String decryptedKey = "decrypted-key";
    AlmSettingDto almSettingDto = newGithubAlmSettingDto()
      .setClientId("clientId")
      .setPrivateKey(encryptedKey)
      .setClientSecret("clientSecret");
    when(encryption.isEncrypted(encryptedKey)).thenReturn(true);
    when(encryption.decrypt(encryptedKey)).thenReturn(decryptedKey);

    GithubAppConfiguration configuration = underTest.validate(almSettingDto);

    ArgumentCaptor<GithubAppConfiguration> configurationArgumentCaptor = ArgumentCaptor.forClass(GithubAppConfiguration.class);
    verify(appClient).checkApiEndpoint(configurationArgumentCaptor.capture());
    verify(appClient).checkAppPermissions(configurationArgumentCaptor.capture());
    assertThat(configuration.getId()).isEqualTo(configurationArgumentCaptor.getAllValues().get(0).getId());
    assertThat(decryptedKey).isEqualTo(configurationArgumentCaptor.getAllValues().get(0).getPrivateKey());
    assertThat(configuration.getId()).isEqualTo(configurationArgumentCaptor.getAllValues().get(1).getId());
    assertThat(decryptedKey).isEqualTo(configurationArgumentCaptor.getAllValues().get(1).getPrivateKey());
  }

  @Test
  public void github_validation_checks_invalid_appId() {
    AlmSettingDto almSettingDto = newGithubAlmSettingDto()
      .setAppId("abc")
      .setClientId("clientId")
      .setClientSecret("clientSecret");

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid appId; For input string: \"abc\"");
  }

  @Test
  public void github_validation_checks_missing_appId() {
    AlmSettingDto almSettingDto = newGithubAlmSettingDto().setAppId(null);

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing appId");
  }

  @Test
  public void github_validation_checks_missing_clientId() {
    AlmSettingDto almSettingDto = newGithubAlmSettingDto().setClientId(null);

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing Client Id");
  }

  @Test
  public void github_validation_checks_missing_clientSecret() {
    AlmSettingDto almSettingDto = newGithubAlmSettingDto().setClientSecret(null);

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing Client Secret");

  }
}
