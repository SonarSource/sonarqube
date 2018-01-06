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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.config.WebhookProperties;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.async.AsyncExecution;

import static java.lang.String.format;
import static org.sonar.core.config.WebhookProperties.MAX_WEBHOOKS_PER_TYPE;

public class WebHooksImpl implements WebHooks {

  private static final Logger LOGGER = Loggers.get(WebHooksImpl.class);
  private static final String WEBHOOK_PROPERTY_FORMAT = "%s.%s";

  private final WebhookCaller caller;
  private final WebhookDeliveryStorage deliveryStorage;
  private final AsyncExecution asyncExecution;

  public WebHooksImpl(WebhookCaller caller, WebhookDeliveryStorage deliveryStorage, AsyncExecution asyncExecution) {
    this.caller = caller;
    this.deliveryStorage = deliveryStorage;
    this.asyncExecution = asyncExecution;
  }

  @Override
  public boolean isEnabled(Configuration config) {
    return readWebHooksFrom(config)
      .findAny()
      .isPresent();
  }

  private static Stream<NameUrl> readWebHooksFrom(Configuration config) {
    return Stream.concat(
      getWebhookProperties(config, WebhookProperties.GLOBAL_KEY).stream(),
      getWebhookProperties(config, WebhookProperties.PROJECT_KEY).stream())
      .map(
        webHookProperty -> {
          String name = config.get(format(WEBHOOK_PROPERTY_FORMAT, webHookProperty, WebhookProperties.NAME_FIELD)).orElse(null);
          String url = config.get(format(WEBHOOK_PROPERTY_FORMAT, webHookProperty, WebhookProperties.URL_FIELD)).orElse(null);
          if (name == null || url == null) {
            return null;
          }
          return new NameUrl(name, url);
        })
      .filter(Objects::nonNull);
  }

  private static List<String> getWebhookProperties(Configuration config, String propertyKey) {
    String[] webhookIds = config.getStringArray(propertyKey);
    return Arrays.stream(webhookIds)
      .map(webhookId -> format(WEBHOOK_PROPERTY_FORMAT, propertyKey, webhookId))
      .limit(MAX_WEBHOOKS_PER_TYPE)
      .collect(MoreCollectors.toList(webhookIds.length));
  }

  @Override
  public void sendProjectAnalysisUpdate(Configuration config, Analysis analysis, Supplier<WebhookPayload> payloadSupplier) {
    List<Webhook> webhooks = readWebHooksFrom(config)
      .map(nameUrl -> new Webhook(analysis.getProjectUuid(), analysis.getCeTaskUuid(), analysis.getAnalysisUuid(), nameUrl.getName(), nameUrl.getUrl()))
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

  private static final class NameUrl {
    private final String name;
    private final String url;

    private NameUrl(String name, String url) {
      this.name = name;
      this.url = url;
    }

    public String getName() {
      return name;
    }

    public String getUrl() {
      return url;
    }
  }
}
