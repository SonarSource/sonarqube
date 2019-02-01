/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

public class GlobalConfigurationProvider extends ProviderAdapter {

  private GlobalConfiguration globalConfig;

  public GlobalConfiguration provide(GlobalServerSettings globalServerSettings, RawScannerProperties scannerProps,
    PropertyDefinitions propertyDefinitions) {
    if (globalConfig == null) {
      Map<String, String> mergedSettings = new LinkedHashMap<>();
      mergedSettings.putAll(globalServerSettings.properties());
      mergedSettings.putAll(scannerProps.properties());

      globalConfig = new GlobalConfiguration(propertyDefinitions, scannerProps.getEncryption(), mergedSettings);
    }
    return globalConfig;
  }
}
