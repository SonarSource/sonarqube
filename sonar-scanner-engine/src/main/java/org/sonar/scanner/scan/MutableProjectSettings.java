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
package org.sonar.scanner.scan;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.config.Settings;
import org.sonar.scanner.bootstrap.GlobalConfiguration;

import static java.util.Objects.requireNonNull;

/**
 * @deprecated since 6.5 {@link ProjectConfiguration} used to be mutable, so keep a mutable copy for backward compatibility.
 */
@Deprecated
public class MutableProjectSettings extends Settings {

  private final Map<String, String> properties = new HashMap<>();

  public MutableProjectSettings(GlobalConfiguration globalConfig) {
    super(globalConfig.getDefinitions(), globalConfig.getEncryption());
    addProperties(globalConfig.getProperties());
  }

  public void complete(ProjectConfiguration projectConfig) {
    addProperties(projectConfig.getProperties());
  }

  @Override
  protected Optional<String> get(String key) {
    return Optional.ofNullable(properties.get(key));
  }

  @Override
  protected void set(String key, String value) {
    properties.put(
      requireNonNull(key, "key can't be null"),
      requireNonNull(value, "value can't be null").trim());
  }

  @Override
  protected void remove(String key) {
    properties.remove(key);
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }
}
