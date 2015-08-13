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
package org.sonar.batch.bootstrap;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.protocol.input.GlobalRepositories;

public class GlobalSettings extends Settings {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalSettings.class);

  private static final String JDBC_SPECIFIC_MESSAGE = "There is no more DB connection to the SQ database. It will be ignored.";
  /**
   * A map of dropped properties as key and specific message to display for that property
   * (what will happen, what should the user do, ...) as a value
   */
  private static final Map<String, String> DROPPED_PROPERTIES = ImmutableMap.of(
    "sonar.jdbc.url", JDBC_SPECIFIC_MESSAGE,
    "sonar.jdbc.username", JDBC_SPECIFIC_MESSAGE,
    "sonar.jdbc.password", JDBC_SPECIFIC_MESSAGE
    );

  private final GlobalProperties bootstrapProps;
  private final GlobalRepositories globalReferentials;
  private final GlobalMode mode;

  public GlobalSettings(GlobalProperties bootstrapProps, PropertyDefinitions propertyDefinitions,
    GlobalRepositories globalReferentials, GlobalMode mode) {

    super(propertyDefinitions);
    this.mode = mode;
    getEncryption().setPathToSecretKey(bootstrapProps.property(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
    this.bootstrapProps = bootstrapProps;
    this.globalReferentials = globalReferentials;
    init();
    new DroppedPropertyChecker(this, DROPPED_PROPERTIES).checkDroppedProperties();
  }

  private void init() {
    addProperties(globalReferentials.globalSettings());
    addProperties(bootstrapProps.properties());

    LOG.info("Server id: " + getString(CoreProperties.SERVER_ID));
  }

  @Override
  protected void doOnGetProperties(String key) {
    if (mode.isIssues() && key.endsWith(".secured") && !key.contains(".license")) {
      throw MessageException.of("Access to the secured property '" + key
        + "' is not possible in issues mode. The SonarQube plugin which requires this property must be deactivated in issues mode.");
    }
  }
}
