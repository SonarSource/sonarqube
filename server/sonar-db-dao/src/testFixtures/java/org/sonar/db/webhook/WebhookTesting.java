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

import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Consumer;
import org.sonar.db.project.ProjectDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;

public class WebhookTesting {

  private WebhookTesting() {
    // only statics
  }

  public static WebhookDto newWebhook(ProjectDto project) {
    return getWebhookDto()
      .setProjectUuid(project.getUuid());
  }

  public static WebhookDto newProjectWebhook(String projectUuid) {
    return getWebhookDto()
      .setProjectUuid(projectUuid);
  }

  public static WebhookDto newGlobalWebhook() {
    return getWebhookDto();
  }

  @SafeVarargs
  public static WebhookDto newGlobalWebhook(String name, Consumer<WebhookDto>... consumers) {
    return getWebhookDto(consumers)
      .setName(name);
  }

  @SafeVarargs
  private static WebhookDto getWebhookDto(Consumer<WebhookDto>... consumers) {
    WebhookDto res = new WebhookDto()
      .setUuid(secure().nextAlphanumeric(40))
      .setName(secure().nextAlphanumeric(64))
      .setUrl("https://www.random-site/" + secure().nextAlphanumeric(256))
      .setSecret(secure().nextAlphanumeric(10))
      .setCreatedAt(Calendar.getInstance().getTimeInMillis());
    Arrays.stream(consumers).forEach(consumer -> consumer.accept(res));
    return res;
  }
}
