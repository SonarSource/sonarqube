/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationDbTester {
  private static final String PROP_NOTIFICATION_PREFIX = "notification";

  private final DbClient dbClient;
  private final DbTester db;

  public NotificationDbTester(DbTester db) {
    this.dbClient = db.getDbClient();
    this.db = db;
  }

  public void assertExists(String channel, String dispatcher, String userUuid, @Nullable ProjectDto project, boolean enabled) {
    List<PropertyDto> result = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey(String.join(".", PROP_NOTIFICATION_PREFIX, dispatcher, channel))
      .setEntityUuid(project == null ? null : project.getUuid())
      .setUserUuid(userUuid)
      .build(), db.getSession()).stream()
      .filter(prop -> project == null ? prop.getEntityUuid() == null : prop.getEntityUuid() != null)
      .toList();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getValue()).isEqualTo(Boolean.toString(enabled));
  }

  public void assertDoesNotExist(String channel, String dispatcher, String userUuid, @Nullable ProjectDto project) {
    List<PropertyDto> result = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setKey(String.join(".", PROP_NOTIFICATION_PREFIX, dispatcher, channel))
      .setEntityUuid(project == null ? null : project.getUuid())
      .setUserUuid(userUuid)
      .build(), db.getSession());
    assertThat(result).isEmpty();
  }
}
