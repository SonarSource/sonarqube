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

package org.sonar.ce.cluster;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.sonar.api.config.Encryption;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;

public class TestSettings extends Settings {

  private final Properties properties;

  public TestSettings(Properties properties) {
    super(new PropertyDefinitions(), new Encryption(null));
    this.properties = properties;
  }

  @Override
  protected Optional<String> get(String key) {
    if (properties.getProperty(key) != null) {
      return Optional.of(properties.getProperty(key));
    } else {
      return Optional.empty();
    }
  }

  @Override
  protected void set(String key, String value) {
    properties.setProperty(key, value);
  }

  @Override
  protected void remove(String key) {
    properties.remove(key);
  }

  @Override
  public Map<String, String> getProperties() {
    return properties.entrySet().stream().collect(
      Collectors.toMap(
        e -> e.getKey().toString(),
        e -> e.getValue().toString()
      )
    );
  }
}
