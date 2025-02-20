/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.email;


import java.io.File;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.DbClient;
import org.sonar.db.property.InternalPropertiesDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.CoreProperties.ENCRYPTION_SECRET_KEY_PATH;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_FROM;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_PASSWORD;

class EmailSmtpConfigurationTest {

  DbClient dbClientMock = mock(DbClient.class);
  InternalPropertiesDao internalPropertiesDaoMock = mock(InternalPropertiesDao.class);
  Settings settings = mock(Settings.class);
  EmailSmtpConfiguration smtpConfiguration;

  @BeforeEach
  void beforeEach() {
    when(dbClientMock.internalPropertiesDao()).thenReturn(internalPropertiesDaoMock);
    when(settings.getRawString(ENCRYPTION_SECRET_KEY_PATH)).thenReturn(Optional.of(pathToSecretKey()));
    smtpConfiguration = new EmailSmtpConfiguration(dbClientMock, settings);
  }

  @Test
  void getValue_whenNoEncryption_shouldReturnOriginalValue() {
    when(internalPropertiesDaoMock.selectByKey(any(), eq(EMAIL_CONFIG_FROM))).thenReturn(Optional.of("email-from-value"));
    var value = smtpConfiguration.getFrom();

    assertThat(value).isEqualTo("email-from-value");
  }

  @Test
  void getValue_whenStoredInEncryptedFormat_shouldReturnDecryptedValue() {
    var encryptedValue = new Encryption(pathToSecretKey()).encrypt("password123");

    when(internalPropertiesDaoMock.selectByKey(any(), eq(EMAIL_CONFIG_SMTP_PASSWORD))).thenReturn(Optional.of(encryptedValue));
    var value = smtpConfiguration.getSmtpPassword();

    assertThat(encryptedValue).startsWith("{aes-gcm}");
    assertThat(value).isEqualTo("password123");
  }

  String pathToSecretKey() {
    try {
      var resource = getClass().getResource("/org/sonar/encryption/aes_secret_key.txt");
      return new File(resource.toURI()).getCanonicalPath();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
