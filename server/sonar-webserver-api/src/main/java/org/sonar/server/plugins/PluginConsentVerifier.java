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
package org.sonar.server.plugins;

import java.util.Optional;

import org.sonar.api.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.extension.PluginRiskConsent;
import org.sonar.core.plugin.PluginType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;

import static org.sonar.core.config.CorePropertyDefinitions.PLUGINS_RISK_CONSENT;
import static org.sonar.core.extension.PluginRiskConsent.NOT_ACCEPTED;
import static org.sonar.core.extension.PluginRiskConsent.REQUIRED;
import static org.sonar.server.log.ServerProcessLogging.STARTUP_LOGGER_NAME;

public class PluginConsentVerifier implements Startable {
  private static final Logger LOGGER = LoggerFactory.getLogger(STARTUP_LOGGER_NAME);

  private final ServerPluginRepository pluginRepository;
  private final DbClient dbClient;

  public PluginConsentVerifier(ServerPluginRepository pluginRepository, DbClient dbClient) {
    this.pluginRepository = pluginRepository;
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    boolean hasExternalPlugins = pluginRepository.getPlugins().stream().anyMatch(plugin -> plugin.getType().equals(PluginType.EXTERNAL));
    try (DbSession session = dbClient.openSession(false)) {
      PropertyDto property = Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(session, PLUGINS_RISK_CONSENT))
        .orElse(defaultPluginRiskConsentProperty());
      if (hasExternalPlugins && NOT_ACCEPTED == PluginRiskConsent.valueOf(property.getValue())) {
        addWarningInSonarDotLog();
        property.setValue(REQUIRED.name());
        dbClient.propertiesDao().saveProperty(session, property);
        session.commit();
      } else if (!hasExternalPlugins && REQUIRED == PluginRiskConsent.valueOf(property.getValue())) {
        dbClient.propertiesDao().deleteGlobalProperty(PLUGINS_RISK_CONSENT, session);
        session.commit();
      }
    }
  }

  private static PropertyDto defaultPluginRiskConsentProperty() {
    PropertyDto property = new PropertyDto();
    property.setKey(PLUGINS_RISK_CONSENT);
    property.setValue(NOT_ACCEPTED.name());
    return property;
  }

  private static void addWarningInSonarDotLog() {
    String highlighter = "####################################################################################################################";
    String msg = "Plugin(s) detected. Plugins are not provided by SonarSource and are therefore installed at your own risk."
        + " A SonarQube administrator needs to acknowledge this risk once logged in.";

    LOGGER.warn(highlighter);
    LOGGER.warn(msg);
    LOGGER.warn(highlighter);
  }

  @Override
  public void stop() {
    // Nothing to do
  }

}
