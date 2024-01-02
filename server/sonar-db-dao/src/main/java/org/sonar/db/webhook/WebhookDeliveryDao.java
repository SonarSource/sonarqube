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
package org.sonar.db.webhook;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class WebhookDeliveryDao implements Dao {

  public Optional<WebhookDeliveryDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  public int countDeliveriesByWebhookUuid(DbSession dbSession, String webhookUuid) {
    return mapper(dbSession).countByWebhookUuid(webhookUuid);
  }

  /**
   * All the deliveries for the specified webhook. Results are ordered by descending date.
   */
  public List<WebhookDeliveryLiteDto> selectByWebhookUuid(DbSession dbSession, String webhookUuid, Pagination pagination) {
    return mapper(dbSession).selectByWebhookUuid(webhookUuid, pagination);
  }

  public int countDeliveriesByProjectUuid(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).countByProjectUuid(projectUuid);
  }

  /**
   * All the deliveries for the specified project. Results are ordered by descending date.
   */
  public List<WebhookDeliveryLiteDto> selectOrderedByProjectUuid(DbSession dbSession, String projectUuid, Pagination pagination) {
    return mapper(dbSession).selectOrderedByProjectUuid(projectUuid, pagination);
  }

  public int countDeliveriesByCeTaskUuid(DbSession dbSession, String ceTaskId) {
    return mapper(dbSession).countByCeTaskUuid(ceTaskId);
  }

  /**
   * All the deliveries for the specified CE task. Results are ordered by descending date.
   */
  public List<WebhookDeliveryLiteDto> selectOrderedByCeTaskUuid(DbSession dbSession, String ceTaskUuid, Pagination pagination) {
    return mapper(dbSession).selectOrderedByCeTaskUuid(ceTaskUuid, pagination);
  }

  public void insert(DbSession dbSession, WebhookDeliveryDto dto) {
    mapper(dbSession).insert(dto);
  }

  public void deleteAllBeforeDate(DbSession dbSession, long beforeDate) {
    mapper(dbSession).deleteAllBeforeDate(beforeDate);
  }

  public Map<String, WebhookDeliveryLiteDto> selectLatestDeliveries(DbSession dbSession, List<WebhookDto> webhooks) {
    return webhooks.stream()
      .flatMap(webhook -> selectByWebhookUuid(dbSession, webhook.getUuid(), Pagination.forPage(1).andSize(1)).stream())
      .collect(toMap(WebhookDeliveryLiteDto::getWebhookUuid, identity()));
  }

  private static WebhookDeliveryMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(WebhookDeliveryMapper.class);
  }

  public void deleteByWebhook(DbSession dbSession, WebhookDto webhook) {
    mapper(dbSession).deleteByWebhookUuid(webhook.getUuid());
  }
}
