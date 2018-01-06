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
package org.sonar.server.property;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A cache-less implementation of {@link InternalProperties} reading and writing to DB table INTERNAL_PROPERTIES.
 */
public class InternalPropertiesImpl implements InternalProperties {
  private final DbClient dbClient;

  public InternalPropertiesImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public Optional<String> read(String propertyKey) {
    checkPropertyKey(propertyKey);

    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.internalPropertiesDao().selectByKey(dbSession, propertyKey);
    }
  }

  @Override
  public void write(String propertyKey, @Nullable String value) {
    checkPropertyKey(propertyKey);

    try (DbSession dbSession = dbClient.openSession(false)) {
      if (value == null || value.isEmpty()) {
        dbClient.internalPropertiesDao().saveAsEmpty(dbSession, propertyKey);
      } else {
        dbClient.internalPropertiesDao().save(dbSession, propertyKey, value);
      }
      dbSession.commit();
    }
  }

  private static void checkPropertyKey(@Nullable String propertyKey) {
    checkArgument(propertyKey != null && !propertyKey.isEmpty(), "property key can't be null nor empty");
  }

}
