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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;

import static com.google.common.base.Preconditions.checkArgument;

public class FavoriteUpdater {
  static final String PROP_FAVORITE_KEY = "favourite";

  private final DbClient dbClient;

  public FavoriteUpdater(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Set favorite to the logged in user. If no user, no action is done
   */
  public void add(DbSession dbSession, EntityDto entity, @Nullable String userUuid, @Nullable String userLogin, boolean failIfTooManyFavorites) {
    if (userUuid == null) {
      return;
    }

    List<PropertyDto> existingFavoriteOnComponent = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey(PROP_FAVORITE_KEY)
      .setUserUuid(userUuid)
      .setEntityUuid(entity.getUuid())
      .build(), dbSession);
    checkArgument(existingFavoriteOnComponent.isEmpty(), "Component '%s' (uuid: %s) is already a favorite", entity.getKey(), entity.getUuid());

    List<PropertyDto> existingFavorites = dbClient.propertiesDao().selectEntityPropertyByKeyAndUserUuid(dbSession, PROP_FAVORITE_KEY, userUuid);
    if (existingFavorites.size() >= 100) {
      checkArgument(!failIfTooManyFavorites, "You cannot have more than 100 favorites on components with qualifier '%s'", entity.getQualifier());
      return;
    }
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
        .setKey(PROP_FAVORITE_KEY)
        .setEntityUuid(entity.getUuid())
        .setUserUuid(userUuid),
      userLogin,
      entity.getKey(), entity.getName(), entity.getQualifier());
  }

  /**
   * Remove a favorite to the user.
   *
   * @throws IllegalArgumentException if the component is not a favorite
   */
  public void remove(DbSession dbSession, EntityDto entity, @Nullable String userUuid, @Nullable String userLogin) {
    if (userUuid == null) {
      return;
    }

    int result = dbClient.propertiesDao().delete(dbSession, new PropertyDto()
        .setKey(PROP_FAVORITE_KEY)
        .setEntityUuid(entity.getUuid())
        .setUserUuid(userUuid),
      userLogin, entity.getKey(), entity.getName(), entity.getQualifier());
    checkArgument(result == 1, "Component '%s' (uuid: %s) is not a favorite", entity.getKey(), entity.getUuid());
  }
}
