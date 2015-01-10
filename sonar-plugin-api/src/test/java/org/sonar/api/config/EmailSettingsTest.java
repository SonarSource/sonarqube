/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.config;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.config.Settings;

import static org.assertj.core.api.Assertions.assertThat;

public class EmailSettingsTest {
  EmailSettings emailSettings;

  @Before
  public void setUp() {
    emailSettings = new EmailSettings(new Settings());
  }

  @Test
  public void should_return_default_values() {
    assertThat(emailSettings.getSmtpHost()).isEqualTo("");
    assertThat(emailSettings.getSmtpPort()).isEqualTo(25);
    assertThat(emailSettings.getSmtpUsername()).isEmpty();
    assertThat(emailSettings.getSmtpPassword()).isEmpty();
    assertThat(emailSettings.getSecureConnection()).isEmpty();
    assertThat(emailSettings.getFrom()).isEqualTo("noreply@nowhere");
    assertThat(emailSettings.getPrefix()).isEqualTo("[SONARQUBE]");
    assertThat(emailSettings.getServerBaseURL()).isEqualTo(CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE);
  }
}
