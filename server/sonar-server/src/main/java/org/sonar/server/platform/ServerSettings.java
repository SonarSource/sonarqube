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
package org.sonar.server.platform;

import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * Load settings in the following order (the last override the first) :
 * <ol>
 * <li>general settings persisted in database</li>
 * <li>file $SONAR_HOME/conf/sonar.properties</li>
 * <li>environment variables</li>
 * <li>system properties</li>
 * </ol>
 *
 * @since 2.12
 */
public class ServerSettings extends Settings {

  private final Properties properties;

  public ServerSettings(PropertyDefinitions definitions, Properties properties) {
    super(definitions);
    this.properties = properties;
    load(Collections.<String, String>emptyMap());
    // Secret key is loaded from conf/sonar.properties
    getEncryption().setPathToSecretKey(getString(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
  }

  public ServerSettings activateDatabaseSettings(Map<String, String> databaseProperties) {
    return load(databaseProperties);
  }

  private ServerSettings load(Map<String, String> databaseSettings) {
    clear();

    // order is important : the last override the first
    addProperties(databaseSettings);
    addProperties(properties);

    return this;
  }
}
