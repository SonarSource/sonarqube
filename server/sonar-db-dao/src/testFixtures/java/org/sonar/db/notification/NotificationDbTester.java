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
package org.sonar.db.notification;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationDbTester {
  private static final String PROP_NOTIFICATION_PREFIX = "notification";

  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public NotificationDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public void assertExists(String channel, String dispatcher, int userId, @Nullable ComponentDto component) {
    List<PropertyDto> result = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey(String.join(".", PROP_NOTIFICATION_PREFIX, dispatcher, channel))
      .setComponentId(component == null ? null : component.getId())
      .setUserId(userId)
      .build(), dbSession).stream()
      .filter(prop -> component == null ? prop.getResourceId() == null : prop.getResourceId() != null)
      .collect(MoreCollectors.toList());
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getValue()).isEqualTo("true");
  }

  public void assertDoesNotExist(String channel, String dispatcher, int userId, @Nullable ComponentDto component) {
    List<PropertyDto> result = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey(String.join(".", PROP_NOTIFICATION_PREFIX, dispatcher, channel))
      .setComponentId(component == null ? null : component.getId())
      .setUserId(userId)
      .build(), dbSession);
    assertThat(result).isEmpty();
  }
}
