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
package org.sonar.server.notification.email.telemetry;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_AUTH_METHOD;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_AUTH_METHOD_DEFAULT;

import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;

class EmailConfigAuthMethodTelemetryProviderTest {
  DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  private final DbSession dbSession = mock(DbSession.class);

  @Test
  void getValue_returnsTheRightEmailConfAuthMethod() {
    EmailConfigAuthMethodTelemetryProvider emailConfigAuthMethodTelemetryProvider = new EmailConfigAuthMethodTelemetryProvider(dbClient);

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.internalPropertiesDao().selectByKey(dbSession, EMAIL_CONFIG_SMTP_AUTH_METHOD)).thenReturn(Optional.of(EMAIL_CONFIG_SMTP_AUTH_METHOD_DEFAULT));

    assertThat(emailConfigAuthMethodTelemetryProvider.getMetricKey()).isEqualTo("email_conf_auth_method");
    assertThat(emailConfigAuthMethodTelemetryProvider.getGranularity()).isEqualTo(Granularity.WEEKLY);
    assertThat(emailConfigAuthMethodTelemetryProvider.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(emailConfigAuthMethodTelemetryProvider.getValue()).isEqualTo(Optional.of("BASIC"));
  }

  @Test
  void getValue_returnsNotSetWhenEmpty() {
    EmailConfigAuthMethodTelemetryProvider emailConfigAuthMethodTelemetryProvider = new EmailConfigAuthMethodTelemetryProvider(dbClient);

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.internalPropertiesDao().selectByKey(dbSession, EMAIL_CONFIG_SMTP_AUTH_METHOD)).thenReturn(Optional.empty());

    assertThat(emailConfigAuthMethodTelemetryProvider.getValue()).isEqualTo(Optional.of("NOT_SET"));
  }
}
