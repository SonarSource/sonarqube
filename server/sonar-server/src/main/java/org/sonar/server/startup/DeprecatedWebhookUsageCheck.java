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
package org.sonar.server.startup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Consumer;
import okhttp3.HttpUrl;
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.webhook.WebhookDto;

public class DeprecatedWebhookUsageCheck implements Startable {
  private static final Logger LOG = Loggers.get(DeprecatedWebhookUsageCheck.class);
  private final DbClient dbClient;

  public DeprecatedWebhookUsageCheck(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.webhookDao().scrollAll(dbSession, new WebhookConsumer(dbClient, dbSession).consumer);
    }
  }

  @Override
  public void stop() {
    // nothing to do here
  }

  static class WebhookConsumer {
    private DbClient dbClient;
    private DbSession session;

    final Consumer<WebhookDto> consumer = webhookDto -> {
      HttpUrl url = HttpUrl.parse(webhookDto.getUrl());
      if (url != null) {
        try {
          InetAddress address = InetAddress.getByName(url.host());
          if (address.isLoopbackAddress() || address.isAnyLocalAddress()) {
            if (webhookDto.getProjectUuid() != null) {
              ComponentDto project = dbClient.componentDao().selectOrFailByUuid(session, webhookDto.getProjectUuid());
              LOG.warn("Webhook '{}' for project '{}' uses an invalid, unsafe URL and will be automatically removed in a " +
                "future version of SonarQube. You should update the URL of that webhook or ask a project administrator to do it.",
                webhookDto.getName(), project.name());
            } else {
              LOG.warn("Global webhook '{}' uses an invalid, unsafe URL and will be automatically removed in a future version of SonarQube. " +
                "You should update the URL of that webhook.", webhookDto.getName());
            }
          }
        } catch (UnknownHostException e) {
          // do nothing
        }
      }
    };

    WebhookConsumer(DbClient dbClient, DbSession session) {
      this.dbClient = dbClient;
      this.session = session;
    }
  }
}
