/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.exceptions.BadRequestException;

import static org.sonar.server.ws.WsUtils.checkRequest;

public class FavoriteUpdater {
  static final String PROP_FAVORITE_KEY = "favourite";

  private final DbClient dbClient;

  public FavoriteUpdater(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Set favorite to the logged in user. If no user, no action is done
   */
  public void add(DbSession dbSession, ComponentDto componentDto, @Nullable Integer userId) {
    if (userId == null) {
      return;
    }

    List<PropertyDto> existingFavoriteOnComponent = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey(PROP_FAVORITE_KEY)
      .setUserId(userId)
      .setComponentId(componentDto.getId())
      .build(), dbSession);
    checkRequest(existingFavoriteOnComponent.isEmpty(), "Component '%s' is already a favorite", componentDto.getDbKey());
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setKey(PROP_FAVORITE_KEY)
      .setResourceId(componentDto.getId())
      .setUserId(userId));
  }

  /**
   * Remove a favorite to the user.
   * @throws BadRequestException if the component is not a favorite
   */
  public void remove(DbSession dbSession, ComponentDto component, @Nullable Integer userId) {
    if (userId == null) {
      return;
    }

    int result = dbClient.propertiesDao().delete(dbSession, new PropertyDto()
      .setKey(PROP_FAVORITE_KEY)
      .setResourceId(component.getId())
      .setUserId(userId));
    checkRequest(result == 1, "Component '%s' is not a favorite", component.getDbKey());
  }
}
