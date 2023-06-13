/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Map;
import java.util.Optional;
import org.sonar.api.CoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.repository.settings.GlobalSettingsLoader;
import org.springframework.context.annotation.Bean;

public class GlobalServerSettingsProvider {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalServerSettingsProvider.class);

  @Bean("GlobalServerSettings")
  public GlobalServerSettings provide(GlobalSettingsLoader loader) {
    Map<String, String> serverSideSettings = loader.loadGlobalSettings();
    Optional.ofNullable(serverSideSettings.get(CoreProperties.SERVER_ID)).ifPresent(v -> LOG.info("Server id: {}", v));
    return new GlobalServerSettings(serverSideSettings);
  }
}
