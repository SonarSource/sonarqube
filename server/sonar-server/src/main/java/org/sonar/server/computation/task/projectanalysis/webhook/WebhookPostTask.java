/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.config.WebhookProperties;
import org.sonar.core.util.stream.Collectors;
import org.sonar.server.computation.task.projectanalysis.component.SettingsRepository;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;

import static com.google.common.base.Throwables.getRootCause;
import static java.lang.String.format;

public class WebhookPostTask implements PostProjectAnalysisTask {

  private static final Logger LOGGER = Loggers.get(WebhookPostTask.class);

  private final TreeRootHolder rootHolder;
  private final SettingsRepository settingsRepository;
  private final WebhookCaller caller;

  public WebhookPostTask(TreeRootHolder rootHolder, SettingsRepository settingsRepository, WebhookCaller caller) {
    this.rootHolder = rootHolder;
    this.settingsRepository = settingsRepository;
    this.caller = caller;
  }

  @Override
  public void finished(ProjectAnalysis analysis) {
    Settings settings = settingsRepository.getSettings(rootHolder.getRoot());

    Iterable<String> webhookProps = Iterables.concat(
      getWebhookProperties(settings, WebhookProperties.GLOBAL_KEY),
      getWebhookProperties(settings, WebhookProperties.PROJECT_KEY)
    );
    if (!Iterables.isEmpty(webhookProps)) {
      process(settings, analysis, webhookProps);
    }
  }

  private static List<String> getWebhookProperties(Settings settings, String propertyKey) {
    String[] webhookIds = settings.getStringArray(propertyKey);
    return Arrays.stream(webhookIds)
      .map(webhookId -> format("%s.%s", propertyKey, webhookId))
      .collect(Collectors.toList(webhookIds.length));
  }

  private void process(Settings settings, ProjectAnalysis analysis, Iterable<String> webhookProperties) {
    WebhookPayload payload = WebhookPayload.from(analysis);
    for (String webhookProp : webhookProperties) {
      String name = settings.getString(format("%s.%s", webhookProp, WebhookProperties.NAME_FIELD));
      String url = settings.getString(format("%s.%s", webhookProp, WebhookProperties.URL_FIELD));
      // as webhooks are defined as property sets, we can't ensure validity of fields on creation.
      if (name != null && url != null) {
        Webhook webhook = new Webhook(name, url);
        WebhookDelivery delivery = caller.call(webhook, payload);
        log(delivery);
      }
    }
  }

  private static void log(WebhookDelivery delivery) {
    Optional<Throwable> throwable = delivery.getThrowable();
    if (throwable.isPresent()) {
      LOGGER.debug("Failed to send webhook '{}' | url={} | message={}",
        delivery.getWebhook().getName(), delivery.getWebhook().getUrl(), getRootCause(throwable.get()).getMessage());
    } else {
      LOGGER.debug("Sent webhook '{}' | url={} | time={}ms | status={}",
        delivery.getWebhook().getName(), delivery.getWebhook().getUrl(), delivery.getDurationInMs().orElse(-1L), delivery.getHttpStatus().orElse(-1));
    }
  }
}
