/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;

public class UuidFactory {
  private final Map<String, String> uuidsByKey = new HashMap<>();

  public UuidFactory(DbClient dbClient, String rootKey) {
    DbSession session = dbClient.openSession(false);
    try {
      List<ComponentDto> components = dbClient.componentDao().selectAllComponentsFromProjectKey(session, rootKey);
      for (ComponentDto componentDto : components) {
        uuidsByKey.put(componentDto.getKey(), componentDto.uuid());
      }
    } finally {
      dbClient.closeSession(session);
    }
  }

  /**
   * Get UUID from database if it exists, else generate a new one
   */
  public String getOrCreateForKey(String key) {
    String uuid = uuidsByKey.get(key);
    return (uuid == null) ? Uuids.create() : uuid;
  }
}
