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
package org.sonar.db.favorite;

import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;

public class FavoriteDbTester {
  private static final String PROP_FAVORITE_KEY = "favourite";

  private final DbClient dbClient;
  private final DbTester db;

  public FavoriteDbTester(DbTester db) {
    this.dbClient = db.getDbClient();
    this.db = db;
  }

  public void add(EntityDto entity, String userUuid, String userLogin) {
    dbClient.propertiesDao().saveProperty(db.getSession(), new PropertyDto()
        .setKey(PROP_FAVORITE_KEY)
        .setUserUuid(userUuid)
        .setEntityUuid(entity.getUuid()),
      userLogin, entity.getKey(), entity.getName(), entity.getQualifier());
    db.commit();
  }

  public boolean hasFavorite(EntityDto entity, String userUuid) {
    List<PropertyDto> result = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey(PROP_FAVORITE_KEY)
      .setEntityUuid(entity.getUuid())
      .setUserUuid(userUuid)
      .build(), db.getSession());

    return !result.isEmpty();
  }

  public boolean hasNoFavorite(EntityDto entity) {
    List<PropertyDto> result = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey(PROP_FAVORITE_KEY)
      .setEntityUuid(entity.getUuid())
      .build(), db.getSession());
    return result.isEmpty();
  }
}
