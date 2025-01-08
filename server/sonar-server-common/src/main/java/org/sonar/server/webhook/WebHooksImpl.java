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
package org.sonar.server.webhook;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.webhook.WebhookDao;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.async.AsyncExecution;

import static java.lang.String.format;

@ServerSide
@ComputeEngineSide
public class WebHooksImpl implements WebHooks {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebHooksImpl.class);

  private final WebhookCaller caller;
  private final WebhookDeliveryStorage deliveryStorage;
  private final AsyncExecution asyncExecution;
  private final DbClient dbClient;

  public WebHooksImpl(WebhookCaller caller, WebhookDeliveryStorage deliveryStorage, AsyncExecution asyncExecution, DbClient dbClient) {
    this.caller = caller;
    this.deliveryStorage = deliveryStorage;
    this.asyncExecution = asyncExecution;
    this.dbClient = dbClient;
  }

  @Override
  public boolean isEnabled(ProjectDto projectDto) {
    return readWebHooksFrom(projectDto.getUuid(), null)
      .findAny()
      .isPresent();
  }

  private Stream<WebhookDto> readWebHooksFrom(String projectUuid, @CheckForNull PostProjectAnalysisTask.LogStatistics taskLogStatistics) {
    try (DbSession dbSession = dbClient.openSession(false)) {

      Optional<ProjectDto> optionalProjectDto = dbClient.projectDao().selectByUuid(dbSession, projectUuid);
      ProjectDto projectDto = checkStateWithOptional(optionalProjectDto, "the requested project '%s' was not found", projectUuid);

      WebhookDao dao = dbClient.webhookDao();
      List<WebhookDto> projectWebhooks = dao.selectByProject(dbSession, projectDto);
      List<WebhookDto> globalWebhooks = dao.selectGlobalWebhooks(dbSession);

      if (taskLogStatistics != null) {
        taskLogStatistics.add("globalWebhooks", globalWebhooks.size());
        taskLogStatistics.add("projectWebhooks", projectWebhooks.size());
      }
      return Stream.concat(projectWebhooks.stream(), globalWebhooks.stream());
    }
  }

  private static <T> T checkStateWithOptional(Optional<T> value, String message, Object... messageArguments) {
    if (!value.isPresent()) {
      throw new IllegalStateException(format(message, messageArguments));
    }

    return value.get();
  }

  @Override
  public void sendProjectAnalysisUpdate(Analysis analysis, Supplier<WebhookPayload> payloadSupplier) {
    sendProjectAnalysisUpdateImpl(analysis, payloadSupplier, null);
  }

  @Override
  public void sendProjectAnalysisUpdate(Analysis analysis, Supplier<WebhookPayload> payloadSupplier, PostProjectAnalysisTask.LogStatistics taskLogStatistics) {
    sendProjectAnalysisUpdateImpl(analysis, payloadSupplier, taskLogStatistics);
  }

  private void sendProjectAnalysisUpdateImpl(Analysis analysis, Supplier<WebhookPayload> payloadSupplier,
    @Nullable PostProjectAnalysisTask.LogStatistics taskLogStatistics) {
    List<Webhook> webhooks = readWebHooksFrom(analysis.projectUuid(), taskLogStatistics)
      .map(dto -> new Webhook(dto.getUuid(), analysis.projectUuid(), analysis.ceTaskUuid(), analysis.analysisUuid(),
        dto.getName(), dto.getUrl(), dto.getSecret()))
      .toList();
    if (webhooks.isEmpty()) {
      return;
    }

    WebhookPayload payload = payloadSupplier.get();
    webhooks.forEach(webhook -> asyncExecution.addToQueue(() -> {
      WebhookDelivery delivery = caller.call(webhook, payload);
      log(delivery);
      deliveryStorage.persist(delivery);
    }));
    asyncExecution.addToQueue(deliveryStorage::purge);
  }

  private static void log(WebhookDelivery delivery) {
    Optional<String> error = delivery.getErrorMessage();
    if (error.isPresent()) {
      LOGGER.warn("Failed to send webhook '{}' | url={} | message={}",
        delivery.getWebhook().getName(), delivery.getWebhook().getUrl(), error.get());
    } else {
      LOGGER.debug("Sent webhook '{}' | url={} | time={}ms | status={}",
        delivery.getWebhook().getName(), delivery.getWebhook().getUrl(), delivery.getDurationInMs().orElse(-1), delivery.getHttpStatus().orElse(-1));
    }
  }

}
