/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Encryption;
import org.sonar.api.config.ImmutableSettings;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.repository.settings.SettingsLoader;

@Immutable
public class GlobalSettings extends ImmutableSettings {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalSettings.class);

  private static final String JDBC_SPECIFIC_MESSAGE = "It will be ignored. There is no longer any DB connection to the SQ database.";
  /**
   * A map of dropped properties as key and specific message to display for that property
   * (what will happen, what should the user do, ...) as a value
   */
  private static final Map<String, String> DROPPED_PROPERTIES = ImmutableMap.of(
    "sonar.jdbc.url", JDBC_SPECIFIC_MESSAGE,
    "sonar.jdbc.username", JDBC_SPECIFIC_MESSAGE,
    "sonar.jdbc.password", JDBC_SPECIFIC_MESSAGE);

  private final Map<String, String> serverSideSettings;

  private final GlobalProperties bootstrapProps;
  private final GlobalMode mode;
  private final Map<String, String> properties;

  public GlobalSettings(GlobalProperties bootstrapProps, PropertyDefinitions propertyDefinitions,
    SettingsLoader settingsLoader, GlobalMode mode) {

    super(propertyDefinitions, new Encryption(bootstrapProps.property(CoreProperties.ENCRYPTION_SECRET_KEY_PATH)));
    this.mode = mode;
    this.bootstrapProps = bootstrapProps;
    this.serverSideSettings = ImmutableMap.copyOf(settingsLoader.load(null));
    Map<String, String> props = init();
    new DroppedPropertyChecker(props, DROPPED_PROPERTIES).checkDroppedProperties();
    this.properties = Collections.unmodifiableMap(props);
  }

  private Map<String, String> init() {
    Map<String, String> props = new HashMap<>();
    addProperties(serverSideSettings, props);
    addProperties(bootstrapProps.properties(), props);

    if (hasKey(CoreProperties.PERMANENT_SERVER_ID)) {
      LOG.info("Server id: " + getString(CoreProperties.PERMANENT_SERVER_ID));
    }
    return props;
  }

  public Map<String, String> getServerSideSettings() {
    return serverSideSettings;
  }

  @Override
  protected Optional<String> get(String key) {
    if (mode.isIssues() && key.endsWith(".secured") && !key.contains(".license")) {
      throw MessageException.of("Access to the secured property '" + key
        + "' is not possible in issues mode. The SonarQube plugin which requires this property must be deactivated in issues mode.");
    }
    return Optional.ofNullable(properties.get(key));
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }
}
