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

import java.util.Map;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;

import static org.apache.commons.lang.StringUtils.defaultString;

public class DatabaseSettingLoader implements SettingLoader {

  private final DbClient dbClient;

  public DatabaseSettingLoader(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public String load(String key) {
    PropertyDto dto = dbClient.propertiesDao().selectGlobalProperty(key);
    if (dto != null) {
      return defaultString(dto.getValue());
    }
    return null;
  }

  @Override
  public Map<String, String> loadAll() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.propertiesDao().selectGlobalProperties(dbSession)
        .stream()
        .collect(MoreCollectors.uniqueIndex(PropertyDto::getKey, p -> defaultString(p.getValue())));
    }
  }

}
