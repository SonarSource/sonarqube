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
package org.sonar.db.webhook;

import java.util.Objects;
import java.util.function.Consumer;
import org.sonar.db.DbTester;

import static java.util.Arrays.stream;
import static org.sonar.db.webhook.WebhookDeliveryTesting.newDto;

public class WebhookDeliveryDbTester {

  private final DbTester dbTester;

  public WebhookDeliveryDbTester(DbTester dbTester) {
    this.dbTester = dbTester;
  }

  public WebhookDeliveryLiteDto insert(WebhookDeliveryDto dto) {
    dbTester.getDbClient().webhookDeliveryDao().insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();
    return dto;
  }

  @SafeVarargs
  public final WebhookDeliveryLiteDto insert(Consumer<WebhookDeliveryDto>... dtoPopulators) {
    WebhookDeliveryDto dto = newDto();
    stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(dto));
    dbTester.getDbClient().webhookDeliveryDao().insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();
    return dto;
  }

  @SafeVarargs
  public final WebhookDeliveryLiteDto insert(WebhookDto webhook, Consumer<WebhookDeliveryDto>... dtoPopulators) {
    WebhookDeliveryDto dto = newDto();
    stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(dto));
    String projectUuid = webhook.getProjectUuid();
    dto.setComponentUuid(Objects.requireNonNull(projectUuid, "Project uuid of webhook cannot be null"));
    dto.setWebhookUuid(webhook.getUuid());
    dbTester.getDbClient().webhookDeliveryDao().insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();
    return dto;
  }

}
