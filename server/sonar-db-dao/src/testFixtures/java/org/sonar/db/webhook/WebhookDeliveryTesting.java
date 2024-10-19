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

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

public class WebhookDeliveryTesting {

  private static final Random RANDOM = new SecureRandom();

  private WebhookDeliveryTesting() {
    // only statics
  }

  /**
   * Build a {@link WebhookDeliveryDto} with all mandatory fields.
   * Optional fields are kept null.
   */
  public static WebhookDeliveryDto newDto(String uuid, String webhookUuid, String projectUuid, String ceTaskUuid) {
    return newDto()
      .setUuid(uuid)
      .setWebhookUuid(webhookUuid)
      .setProjectUuid(projectUuid)
      .setCeTaskUuid(ceTaskUuid);
  }

  public static WebhookDeliveryDto newDto() {
    return new WebhookDeliveryDto()
      .setUuid(Uuids.createFast())
      .setWebhookUuid(randomAlphanumeric(40))
      .setProjectUuid(randomAlphanumeric(40))
      .setCeTaskUuid(randomAlphanumeric(40))
      .setAnalysisUuid(randomAlphanumeric(40))
      .setName(randomAlphanumeric(10))
      .setUrl(randomAlphanumeric(10))
      .setDurationMs(RANDOM.nextInt(Integer.MAX_VALUE))
      .setHttpStatus(RANDOM.nextInt(Integer.MAX_VALUE))
      .setSuccess(RANDOM.nextBoolean())
      .setPayload(randomAlphanumeric(10))
      .setCreatedAt(RANDOM.nextLong(Long.MAX_VALUE));
  }

  public static List<String> selectAllDeliveryUuids(DbTester dbTester, DbSession dbSession) {
    return dbTester.select(dbSession, "select uuid as \"uuid\" from webhook_deliveries")
      .stream()
      .map(columns -> (String) columns.get("uuid"))
      .toList();
  }
}
