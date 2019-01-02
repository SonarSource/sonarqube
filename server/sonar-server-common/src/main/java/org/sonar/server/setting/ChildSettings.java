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
package org.sonar.server.setting;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.ConfigurationBridge;

import static java.util.Objects.requireNonNull;

public class ChildSettings extends Settings {

  private final Settings parentSettings;
  private final Map<String, String> localProperties = new HashMap<>();

  public ChildSettings(Settings parentSettings) {
    super(parentSettings.getDefinitions(), parentSettings.getEncryption());
    this.parentSettings = parentSettings;
  }

  @Override
  protected Optional<String> get(String key) {
    String value = localProperties.get(key);
    if (value != null) {
      return Optional.of(value);
    }
    return parentSettings.getRawString(key);
  }

  @Override
  protected void set(String key, String value) {
    localProperties.put(
      requireNonNull(key, "key can't be null"),
      requireNonNull(value, "value can't be null").trim());
  }

  @Override
  protected void remove(String key) {
    localProperties.remove(key);
  }

  /**
   * Only returns the currently loaded properties.
   *
   * <p>
   * On the Web Server, global properties are loaded lazily when requested by name. Therefor,
   * this will return only global properties which have been requested using
   * {@link #get(String)} at least once prior to this call.
   */
  @Override
  public Map<String, String> getProperties() {
    // order is important. local properties override parent properties.
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    builder.putAll(parentSettings.getProperties());
    builder.putAll(localProperties);
    return builder.build();
  }

  public Configuration asConfiguration() {
    return new ConfigurationBridge(this);
  }
}
