/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Encryption;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.scanner.config.DefaultConfiguration;

@Immutable
public class GlobalConfiguration extends DefaultConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalConfiguration.class);

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

  public GlobalConfiguration(PropertyDefinitions propertyDefinitions, Encryption encryption, GlobalAnalysisMode mode,
    Map<String, String> settings, Map<String, String> serverSideSettings) {
    super(propertyDefinitions, encryption, mode, settings);
    this.serverSideSettings = unmodifiableMapWithTrimmedValues(propertyDefinitions, serverSideSettings);

    get(CoreProperties.SERVER_ID).ifPresent(v -> LOG.info("Server id: {}", v));
    new DroppedPropertyChecker(getProperties(), DROPPED_PROPERTIES).checkDroppedProperties();
  }

  public Map<String, String> getServerSideSettings() {
    return serverSideSettings;
  }

}
