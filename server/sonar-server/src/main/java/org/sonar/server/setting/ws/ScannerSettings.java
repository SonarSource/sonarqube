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
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;

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
public class ScannerSettings {

  private static final Set<String> SERVER_SETTING_KEYS = ImmutableSet.of(PERMANENT_SERVER_ID, SERVER_STARTTIME, SERVER_ID);

  private final DbClient dbClient;
  private final PropertyDefinitions propertyDefinitions;

  public ScannerSettings(DbClient dbClient, PropertyDefinitions propertyDefinitions) {
    this.dbClient = dbClient;
    this.propertyDefinitions = propertyDefinitions;
  }

  Set<String> getScannerSettingKeys(DbSession dbSession) {
    return concat(concat(loadLicenseKeys(), loadLicenseHashKeys(dbSession)),
      SERVER_SETTING_KEYS.stream()).collect(toSet());
  }

  private Stream<String> loadLicenseHashKeys(DbSession dbSession) {
    return dbClient.propertiesDao().selectGlobalPropertiesByKeyQuery(dbSession, LICENSE_HASH_SUFFIX).stream().map(PropertyDto::getKey);
  }

  private Stream<String> loadLicenseKeys() {
    return propertyDefinitions.getAll()
      .stream()
      .filter(setting -> setting.type().equals(LICENSE))
      .map(PropertyDefinition::key);
  }

}
