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
package org.sonar.server.config;

import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang.ArrayUtils;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;

import static java.util.function.Function.identity;
import static org.sonar.core.config.MultivalueProperty.parseAsCsv;

public class ConfigurationProvider extends ProviderAdapter {

  private Configuration configuration;

  public Configuration provide(Settings settings) {
    if (configuration == null) {
      configuration = new ServerConfigurationAdapter(settings);
    }
    return configuration;
  }

  private static class ServerConfigurationAdapter implements Configuration {
    private static final Function<String, String> REPLACE_ENCODED_COMMAS = value -> value.replace("%2C", ",");

    private final Settings settings;

    private ServerConfigurationAdapter(Settings settings) {
      this.settings = settings;
    }

    @Override
    public Optional<String> get(String key) {
      return Optional.ofNullable(settings.getString(key));
    }

    @Override
    public boolean hasKey(String key) {
      return settings.hasKey(key);
    }

    @Override
    public String[] getStringArray(String key) {
      boolean multiValue = settings.getDefinition(key)
        .map(PropertyDefinition::multiValues)
        .orElse(false);
      return get(key)
        .map(v -> parseAsCsv(key, v, multiValue ? REPLACE_ENCODED_COMMAS : identity()))
        .orElse(ArrayUtils.EMPTY_STRING_ARRAY);
    }

  }
}
