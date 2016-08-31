/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
  public void loadAll(ImmutableMap.Builder<String, String> appendTo) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.propertiesDao().selectGlobalProperties(dbSession)
        .forEach(p -> appendTo.put(p.getKey(), defaultString(p.getValue())));
    }
  }

}
