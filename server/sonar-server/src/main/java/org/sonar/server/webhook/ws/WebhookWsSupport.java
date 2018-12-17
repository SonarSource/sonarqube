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
package org.sonar.server.webhook.ws;

import org.sonar.db.component.ComponentDto;
import org.sonar.db.webhook.WebhookDeliveryDto;
import org.sonar.db.webhook.WebhookDeliveryLiteDto;
import org.sonarqube.ws.Webhooks;

import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;

class WebhookWsSupport {
  private WebhookWsSupport() {
    // only statics
  }

  static Webhooks.Delivery.Builder copyDtoToProtobuf(ComponentDto component, WebhookDeliveryLiteDto dto, Webhooks.Delivery.Builder builder) {
    builder
      .clear()
      .setId(dto.getUuid())
      .setAt(formatDateTime(dto.getCreatedAt()))
      .setName(dto.getName())
      .setUrl(dto.getUrl())
      .setSuccess(dto.isSuccess())
      .setComponentKey(component.getDbKey());
    ofNullable(dto.getCeTaskUuid()).ifPresent(builder::setCeTaskId);
    ofNullable(dto.getHttpStatus()).ifPresent(builder::setHttpStatus);
    ofNullable(dto.getDurationMs()).ifPresent(builder::setDurationMs);
    return builder;
  }

  static Webhooks.Delivery.Builder copyDtoToProtobuf(ComponentDto component, WebhookDeliveryDto dto, Webhooks.Delivery.Builder builder) {
    copyDtoToProtobuf(component, (WebhookDeliveryLiteDto) dto, builder);
    builder.setPayload(dto.getPayload());
    ofNullable(dto.getErrorStacktrace()).ifPresent(builder::setErrorStacktrace);
    return builder;
  }
}
