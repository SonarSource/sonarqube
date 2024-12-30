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

import com.google.common.net.InternetDomainName;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_HOST;

public class EmailConfigHostTelemetryProvider extends AbstractTelemetryDataProvider<String> {
  private final DbClient dbClient;

  public EmailConfigHostTelemetryProvider(DbClient dbClient) {
    super("email_conf_host", Dimension.INSTALLATION, Granularity.WEEKLY, TelemetryDataType.STRING);
    this.dbClient = dbClient;
  }

  @Override
  public Optional<String> getValue() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return Optional.of(dbClient.internalPropertiesDao()
        .selectByKey(dbSession, EMAIL_CONFIG_SMTP_HOST)
        .map(EmailConfigHostTelemetryProvider::extractDomain)
        .orElse("NOT_SET"));
    }
  }

  private static String extractDomain(String host) {
    if (StringUtils.isEmpty(host)) {
      return "EMPTY_DOMAIN";
    }
    try {
      host = host.contains("://") ? StringUtils.substringBefore(StringUtils.substringAfter(host, "://"), "/") : StringUtils.substringBefore(host, "/");
      return InternetDomainName.from(host).topPrivateDomain().toString();
    } catch (RuntimeException e) {
      return switch (StringUtils.substringBefore(e.getMessage(), ":")) {
        case "Not a valid domain name" -> "NOT_VALID_DOMAIN_NAME";
        case "Not under a public suffix" -> "DOMAIN_NOT_UNDER_PUBLIC_SUFFIX";
        default -> "NOT_VALID_DOMAIN";
      };
    }
  }
}
