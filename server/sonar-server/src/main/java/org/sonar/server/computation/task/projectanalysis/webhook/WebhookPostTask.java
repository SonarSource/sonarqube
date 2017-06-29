/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.webhook;

import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.config.WebhookProperties;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;

import static java.lang.String.format;
import static org.sonar.core.config.WebhookProperties.MAX_WEBHOOKS_PER_TYPE;

public class WebhookPostTask implements PostProjectAnalysisTask {

  private static final Logger LOGGER = Loggers.get(WebhookPostTask.class);

  private final TreeRootHolder rootHolder;
  private final ConfigurationRepository configRepository;
  private final WebhookPayloadFactory payloadFactory;
  private final WebhookCaller caller;
  private final WebhookDeliveryStorage deliveryStorage;

  public WebhookPostTask(TreeRootHolder rootHolder, ConfigurationRepository settingsRepository, WebhookPayloadFactory payloadFactory,
    WebhookCaller caller, WebhookDeliveryStorage deliveryStorage) {
    this.rootHolder = rootHolder;
    this.configRepository = settingsRepository;
    this.payloadFactory = payloadFactory;
    this.caller = caller;
    this.deliveryStorage = deliveryStorage;
  }

  @Override
  public void finished(ProjectAnalysis analysis) {
    Configuration config = configRepository.getConfiguration(rootHolder.getRoot());

    Iterable<String> webhookProps = Iterables.concat(
      getWebhookProperties(config, WebhookProperties.GLOBAL_KEY),
      getWebhookProperties(config, WebhookProperties.PROJECT_KEY));
    if (!Iterables.isEmpty(webhookProps)) {
      process(config, analysis, webhookProps);
      deliveryStorage.purge(analysis.getProject().getUuid());
    }
  }

  private static List<String> getWebhookProperties(Configuration config, String propertyKey) {
    String[] webhookIds = config.getStringArray(propertyKey);
    return Arrays.stream(webhookIds)
      .map(webhookId -> format("%s.%s", propertyKey, webhookId))
      .limit(MAX_WEBHOOKS_PER_TYPE)
      .collect(MoreCollectors.toList(webhookIds.length));
  }

  private void process(Configuration config, ProjectAnalysis analysis, Iterable<String> webhookProperties) {
    WebhookPayload payload = payloadFactory.create(analysis);
    for (String webhookProp : webhookProperties) {
      String name = config.get(format("%s.%s", webhookProp, WebhookProperties.NAME_FIELD)).orElse(null);
      String url = config.get(format("%s.%s", webhookProp, WebhookProperties.URL_FIELD)).orElse(null);
      // as webhooks are defined as property sets, we can't ensure validity of fields on creation.
      if (name != null && url != null) {
        Webhook webhook = new Webhook(analysis.getProject().getUuid(), analysis.getCeTask().getId(), name, url);
        WebhookDelivery delivery = caller.call(webhook, payload);
        log(delivery);
        deliveryStorage.persist(delivery);
      }
    }
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
