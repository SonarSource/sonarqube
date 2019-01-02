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

import java.util.Arrays;
import java.util.function.Consumer;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;

import java.util.Calendar;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

public class WebhookTesting {

  private WebhookTesting() {
    // only statics
  }

  public static WebhookDto newWebhook(ComponentDto project) {
    return getWebhookDto()
      .setProjectUuid(project.uuid());
  }

  public static WebhookDto newProjectWebhook(String projectUuid) {
    return getWebhookDto()
      .setProjectUuid(projectUuid);
  }

  public static WebhookDto newWebhook(OrganizationDto organizationDto) {
    return getWebhookDto()
      .setOrganizationUuid(organizationDto.getUuid());
  }

  @SafeVarargs
  public static WebhookDto newOrganizationWebhook(String name, String organizationUuid, Consumer<WebhookDto>... consumers) {
    return getWebhookDto(consumers)
            .setName(name)
            .setOrganizationUuid(organizationUuid);
  }

  @SafeVarargs
  private static WebhookDto getWebhookDto(Consumer<WebhookDto>... consumers) {
    WebhookDto res = new WebhookDto()
      .setUuid(randomAlphanumeric(40))
      .setName(randomAlphanumeric(64))
      .setUrl("https://www.random-site/" + randomAlphanumeric(256))
      .setCreatedAt(Calendar.getInstance().getTimeInMillis());
    Arrays.stream(consumers).forEach(consumer -> consumer.accept(res));
    return res;
  }
}
