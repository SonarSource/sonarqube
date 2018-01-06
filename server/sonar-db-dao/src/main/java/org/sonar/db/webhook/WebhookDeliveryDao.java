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
package org.sonar.db.webhook;

import java.util.List;
import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class WebhookDeliveryDao implements Dao {

  public Optional<WebhookDeliveryDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  /**
   * All the deliveries for the specified component. Results are ordered by descending date.
   */
  public List<WebhookDeliveryLiteDto> selectOrderedByComponentUuid(DbSession dbSession, String componentUuid) {
    return mapper(dbSession).selectOrderedByComponentUuid(componentUuid);
  }

  /**
   * All the deliveries for the specified CE task. Results are ordered by descending date.
   */
  public List<WebhookDeliveryLiteDto> selectOrderedByCeTaskUuid(DbSession dbSession, String ceTaskUuid) {
    return mapper(dbSession).selectOrderedByCeTaskUuid(ceTaskUuid);
  }

  public void insert(DbSession dbSession, WebhookDeliveryDto dto) {
    mapper(dbSession).insert(dto);
  }

  public void deleteComponentBeforeDate(DbSession dbSession, String componentUuid, long beforeDate) {
    mapper(dbSession).deleteComponentBeforeDate(componentUuid, beforeDate);
  }

  private static WebhookDeliveryMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(WebhookDeliveryMapper.class);
  }
}
