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

import java.util.LinkedHashMap;
import java.util.Map;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.scanner.repository.settings.SettingsLoader;

public class GlobalConfigurationProvider extends ProviderAdapter {

  private GlobalConfiguration globalSettings;

  public GlobalConfiguration provide(SettingsLoader loader, GlobalProperties globalProps, PropertyDefinitions propertyDefinitions, GlobalAnalysisMode mode) {
    if (globalSettings == null) {

      Map<String, String> serverSideSettings = loader.load(null);

      Map<String, String> settings = new LinkedHashMap<>();
      settings.putAll(serverSideSettings);
      settings.putAll(globalProps.properties());

      globalSettings = new GlobalConfiguration(propertyDefinitions, globalProps.getEncryption(), mode, settings, serverSideSettings);
    }
    return globalSettings;
  }
}
