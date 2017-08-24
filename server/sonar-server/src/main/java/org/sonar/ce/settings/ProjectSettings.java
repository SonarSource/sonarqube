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
package org.sonar.ce.settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Settings;

import static java.util.Objects.requireNonNull;

@ComputeEngineSide
public class ProjectSettings extends Settings {

  private final Map<String, String> projectProps = new HashMap<>();
  private final Settings globalSettings;

  public ProjectSettings(Settings globalSettings) {
    super(globalSettings.getDefinitions(), globalSettings.getEncryption());
    this.globalSettings = globalSettings;
  }

  @Override
  protected Optional<String> get(String key) {
    String value = projectProps.get(key);
    if (value != null) {
      return Optional.of(value);
    }
    return globalSettings.getRawString(key);
  }

  @Override
  protected void set(String key, String value) {
    projectProps.put(
      requireNonNull(key, "key can't be null"),
      requireNonNull(value, "value can't be null").trim());
  }

  @Override
  protected void remove(String key) {
    projectProps.remove(key);
  }

  @Override
  public Map<String, String> getProperties() {
    // order is important. Project properties override global properties.
    Map<String, String> result = new HashMap<>();
    result.putAll(globalSettings.getProperties());
    result.putAll(projectProps);
    return result;
  }
}
