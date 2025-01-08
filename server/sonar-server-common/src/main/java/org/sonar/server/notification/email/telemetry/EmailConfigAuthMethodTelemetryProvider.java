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
package org.sonar.server.notification.email.telemetry;

import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_AUTH_METHOD;

public class EmailConfigAuthMethodTelemetryProvider extends AbstractTelemetryDataProvider<String> {
  private final DbClient dbClient;

  public EmailConfigAuthMethodTelemetryProvider(DbClient dbClient) {
    super("email_conf_auth_method", Dimension.INSTALLATION, Granularity.WEEKLY, TelemetryDataType.STRING);
    this.dbClient = dbClient;
  }

  @Override
  public Optional<String> getValue() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return Optional.of(dbClient.internalPropertiesDao().selectByKey(dbSession, EMAIL_CONFIG_SMTP_AUTH_METHOD).orElse("NOT_SET"));
    }
  }
}
