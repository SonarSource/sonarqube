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
package org.sonar.server.webhook;

import com.google.common.base.Throwables;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.webhook.WebhookDeliveryDao;
import org.sonar.db.webhook.WebhookDeliveryDto;

/**
 * Persist and purge {@link WebhookDelivery} into database
 */
@ComputeEngineSide
public class WebhookDeliveryStorage {

  private static final long ALIVE_DELAY_MS = 30L * 24 * 60 * 60 * 1000;

  private final DbClient dbClient;
  private final System2 system;
  private final UuidFactory uuidFactory;

  public WebhookDeliveryStorage(DbClient dbClient, System2 system, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.system = system;
    this.uuidFactory = uuidFactory;
  }

  public void persist(WebhookDelivery delivery) {
    WebhookDeliveryDao dao = dbClient.webhookDeliveryDao();
    try (DbSession dbSession = dbClient.openSession(false)) {
      dao.insert(dbSession, toDto(delivery));
      dbSession.commit();
    }
  }

  public void purge(String componentUuid) {
    long beforeDate = system.now() - ALIVE_DELAY_MS;
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.webhookDeliveryDao().deleteComponentBeforeDate(dbSession, componentUuid, beforeDate);
      dbSession.commit();
    }
  }

  private WebhookDeliveryDto toDto(WebhookDelivery delivery) {
    WebhookDeliveryDto dto = new WebhookDeliveryDto();
    dto.setUuid(uuidFactory.create());
    dto.setComponentUuid(delivery.getWebhook().getComponentUuid());
    delivery.getWebhook().getCeTaskUuid().ifPresent(dto::setCeTaskUuid);
    delivery.getWebhook().getAnalysisUuid().ifPresent(dto::setAnalysisUuid);
    dto.setName(delivery.getWebhook().getName());
    dto.setUrl(delivery.getWebhook().getUrl());
    dto.setSuccess(delivery.isSuccess());
    dto.setHttpStatus(delivery.getHttpStatus().orElse(null));
    dto.setDurationMs(delivery.getDurationInMs().orElse(null));
    dto.setErrorStacktrace(delivery.getError().map(Throwables::getStackTraceAsString).orElse(null));
    dto.setPayload(delivery.getPayload().getJson());
    dto.setCreatedAt(delivery.getAt());
    return dto;
  }
}
