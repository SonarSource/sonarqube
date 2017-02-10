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
package org.sonar.server.setting.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.api.Startable;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.sonar.api.CoreProperties.PERMANENT_SERVER_ID;
import static org.sonar.api.CoreProperties.SERVER_ID;
import static org.sonar.api.CoreProperties.SERVER_STARTTIME;
import static org.sonar.api.PropertyType.LICENSE;
import static org.sonar.server.setting.ws.SettingsWsSupport.LICENSE_HASH_SUFFIX;

/**
 * This class returns the list of settings required on scanner side (licenses, license hashes, server ids, etc.)
 */
public class ScannerSettings implements Startable {

  private static final String SONAR_PREFIX = "sonar.";
  private static final Set<String> SERVER_SETTING_KEYS = ImmutableSet.of(PERMANENT_SERVER_ID, SERVER_STARTTIME, SERVER_ID);

  private final PropertyDefinitions propertyDefinitions;
  private final PluginRepository pluginRepository;

  private Set<String> scannerSettingKeys;

  public ScannerSettings(PropertyDefinitions propertyDefinitions, PluginRepository pluginRepository) {
    this.propertyDefinitions = propertyDefinitions;
    this.pluginRepository = pluginRepository;
  }

  @Override
  public void start() {
    this.scannerSettingKeys = concat(concat(loadLicenseKeys(), loadLicenseHashKeys()),
      SERVER_SETTING_KEYS.stream()).collect(toSet());
  }

  private Stream<String> loadLicenseHashKeys() {
    return pluginRepository.getPluginInfos().stream()
      .map(PluginInfo::getKey)
      .map(key -> SONAR_PREFIX + key + LICENSE_HASH_SUFFIX);
  }

  private Stream<String> loadLicenseKeys() {
    return propertyDefinitions.getAll()
      .stream()
      .filter(setting -> setting.type().equals(LICENSE))
      .map(PropertyDefinition::key);
  }

  Set<String> getScannerSettingKeys() {
    return scannerSettingKeys;
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
