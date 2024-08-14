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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_HOST;

class EmailConfigHostTelemetryProviderTest {
  DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  private final DbSession dbSession = mock(DbSession.class);

  @Test
  void emailConfigHostTelemetryProvider_returnCorrectFields() {
    EmailConfigHostTelemetryProvider emailConfigHostTelemetryProvider = new EmailConfigHostTelemetryProvider(dbClient);

    String host = "smtp.office365.com";
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.internalPropertiesDao().selectByKey(dbSession, EMAIL_CONFIG_SMTP_HOST)).thenReturn(Optional.of(host));

    assertThat(emailConfigHostTelemetryProvider.getMetricKey()).isEqualTo("email_conf_host");
    assertThat(emailConfigHostTelemetryProvider.getGranularity()).isEqualTo(Granularity.WEEKLY);
    assertThat(emailConfigHostTelemetryProvider.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(emailConfigHostTelemetryProvider.getValue()).isEqualTo(Optional.of("office365.com"));
  }

  @ParameterizedTest
  @MethodSource("hostAndExpectedDomainValues")
  void getValue_returnsTheCorrectlyExtractedTopLevelDomain(String host, String domain) {
    EmailConfigHostTelemetryProvider emailConfigHostTelemetryProvider = new EmailConfigHostTelemetryProvider(dbClient);

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.internalPropertiesDao().selectByKey(dbSession, EMAIL_CONFIG_SMTP_HOST)).thenReturn(Optional.of(host));

    assertThat(emailConfigHostTelemetryProvider.getValue()).isEqualTo(Optional.of(domain));
  }

  private static Object[][] hostAndExpectedDomainValues() {
    return new Object[][]{
      {"", "EMPTY_DOMAIN"},
      {"smtpasad.org.sdf", "DOMAIN_NOT_UNDER_PUBLIC_SUFFIX"},
      {"127.0.0.0", "NOT_VALID_DOMAIN_NAME"},
      {"smtp.office365.com", "office365.com"},
      {"outlook.office365.com/smthng/extra", "office365.com"},
      {"my.domain.org/", "domain.org"},
      {"http://smtp.gmail.com", "gmail.com"},
      {"https://smtp.office365.com/smthng/extra", "office365.com"}
    };
  }
}
