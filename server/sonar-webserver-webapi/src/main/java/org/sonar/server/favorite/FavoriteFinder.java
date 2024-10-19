/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.favorite;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.user.UserSession;

import static java.util.Collections.emptyList;
import static org.sonar.server.favorite.FavoriteUpdater.PROP_FAVORITE_KEY;

public class FavoriteFinder {
  private final DbClient dbClient;
  private final UserSession userSession;

  public FavoriteFinder(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  /**
   * @return the list of favorite components of the authenticated user. Empty list if the user is not authenticated
   */
  public List<EntityDto> list() {
    if (!userSession.isLoggedIn()) {
      return emptyList();
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      PropertyQuery dbQuery = PropertyQuery.builder()
        .setKey(PROP_FAVORITE_KEY)
        .setUserUuid(userSession.getUuid())
        .build();
      Set<String> entitiesUuids = dbClient.propertiesDao().selectByQuery(dbQuery, dbSession).stream().map(PropertyDto::getEntityUuid).collect(Collectors.toSet());

      List<EntityDto> entities = dbClient.entityDao().selectByUuids(dbSession, entitiesUuids);

      return entities.stream()
        .sorted(Comparator.comparing(EntityDto::getName))
        .toList();
    }
  }
}
