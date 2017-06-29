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
package org.sonar.core.config;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.ConfigurationBridge;

public class ConfigurationProvider extends ProviderAdapter {

  private Configuration configuration;

  public Configuration provide(Settings settings) {
    if (configuration == null) {
      configuration = new ConfigurationBridge(settings);
    }
    return configuration;
  }

}
