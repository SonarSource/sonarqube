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
package org.sonar.server.webhook;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.webhook.WebhookDao;
import org.sonar.db.webhook.WebhookDto;
import org.sonar.server.async.AsyncExecution;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@ServerSide
@ComputeEngineSide
public class WebHooksImpl implements WebHooks {

  private static final Logger LOGGER = Loggers.get(WebHooksImpl.class);

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
  public boolean isEnabled(ComponentDto projectDto) {
    return readWebHooksFrom(projectDto.uuid())
      .findAny()
      .isPresent();
  }

  private Stream<WebhookDto> readWebHooksFrom(String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {

      Optional<ComponentDto> optionalComponentDto = ofNullable(dbClient.componentDao().selectByUuid(dbSession, projectUuid).orElse(null));
      ComponentDto componentDto = checkStateWithOptional(optionalComponentDto, "the requested project '%s' was not found", projectUuid);

      if (componentDto.getMainBranchProjectUuid() != null && !componentDto.uuid().equals(componentDto.getMainBranchProjectUuid())) {
        Optional<ComponentDto> mainBranchComponentDto = ofNullable(dbClient.componentDao().selectByUuid(dbSession, componentDto.getMainBranchProjectUuid()).orElse(null));
        componentDto = checkStateWithOptional(mainBranchComponentDto, "the requested project '%s' was not found", projectUuid);
      }

      WebhookDao dao = dbClient.webhookDao();
      return Stream.concat(
        dao.selectByProject(dbSession, componentDto).stream(),
        dao.selectByOrganizationUuid(dbSession, componentDto.getOrganizationUuid()).stream());
    }
  }

  private static <T> T checkStateWithOptional(java.util.Optional<T> value, String message, Object... messageArguments) {
    if (!value.isPresent()) {
      throw new IllegalStateException(format(message, messageArguments));
    }

    return value.get();
  }

  @Override
  public void sendProjectAnalysisUpdate(Analysis analysis, Supplier<WebhookPayload> payloadSupplier) {
    List<Webhook> webhooks = readWebHooksFrom(analysis.getProjectUuid())
      .map(dto -> new Webhook(dto.getUuid(), analysis.getProjectUuid(), analysis.getCeTaskUuid(), analysis.getAnalysisUuid(), dto.getName(), dto.getUrl()))
      .collect(MoreCollectors.toList());
    if (webhooks.isEmpty()) {
      return;
    }

    WebhookPayload payload = payloadSupplier.get();
    webhooks.forEach(webhook -> asyncExecution.addToQueue(() -> {
      WebhookDelivery delivery = caller.call(webhook, payload);
      log(delivery);
      deliveryStorage.persist(delivery);
    }));
    asyncExecution.addToQueue(() -> deliveryStorage.purge(analysis.getProjectUuid()));
  }

  private static void log(WebhookDelivery delivery) {
    Optional<String> error = delivery.getErrorMessage();
    if (error.isPresent()) {
      LOGGER.debug("Failed to send webhook '{}' | url={} | message={}",
        delivery.getWebhook().getName(), delivery.getWebhook().getUrl(), error.get());
    } else {
      LOGGER.debug("Sent webhook '{}' | url={} | time={}ms | status={}",
        delivery.getWebhook().getName(), delivery.getWebhook().getUrl(), delivery.getDurationInMs().orElse(-1), delivery.getHttpStatus().orElse(-1));
    }
  }

}
