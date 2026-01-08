/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.qualitygate.notification;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.notification.NotificationService;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListener;

import static java.util.Collections.singleton;

public class EmailQGChangeEventListener implements QGChangeEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(EmailQGChangeEventListener.class);

  private final NotificationService notificationService;
  private final DbClient dbClient;
  private final QualityGateConditionFormatter conditionFormatter;

  public EmailQGChangeEventListener(NotificationService notificationService, DbClient dbClient,
    QualityGateConditionFormatter conditionFormatter) {
    this.notificationService = notificationService;
    this.dbClient = dbClient;
    this.conditionFormatter = conditionFormatter;
  }

  @Override
  public void onIssueChanges(QGChangeEvent event, Set<ChangedIssue> changedIssues) {
    Optional<EvaluatedQualityGate> evaluatedQG = event.getQualityGateSupplier().get();
    if (evaluatedQG.isEmpty()) {
      return;
    }

    Metric.Level newStatus = evaluatedQG.get().getStatus();
    Optional<Metric.Level> previousStatus = event.getPreviousStatus();

    if (previousStatus.isPresent() && previousStatus.get() == newStatus) {
      return;
    }

    try {
      notifyUsers(event, newStatus, previousStatus.orElse(null));
    } catch (Exception e) {
      LOGGER.warn("Failed to send quality gate change email notification for project {}", event.getProject().getKey(), e);
    }
  }

  private void notifyUsers(QGChangeEvent event, Metric.Level newStatus, @Nullable Metric.Level previousStatus) {
    ProjectDto project = event.getProject();
    BranchDto branch = event.getBranch();
    EvaluatedQualityGate evaluatedQG = event.getQualityGateSupplier().get().orElseThrow();

    String statusLabel = toStatusLabel(newStatus);
    boolean isNewAlert = previousStatus == null;

    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, MetricDto> metricsByKey = dbClient.metricDao().selectAll(dbSession).stream()
        .collect(Collectors.toMap(MetricDto::getKey, Function.identity()));

      String alertText = conditionFormatter.buildAlertText(evaluatedQG, metricKey -> {
        MetricDto metric = metricsByKey.get(metricKey);
        return new QualityGateConditionFormatter.MetricInfo(
          metric != null ? metric.getShortName() : null,
          metric != null ? metric.getValueType() : null
        );
      });

      QGChangeNotification notification = new QGChangeNotification();
      notification
        .setDefaultMessage(String.format("Alert on %s: %s", project.getName(), statusLabel))
        .setFieldValue(QGChangeNotification.FIELD_PROJECT_NAME, project.getName())
        .setFieldValue(QGChangeNotification.FIELD_PROJECT_KEY, project.getKey())
        .setFieldValue(QGChangeNotification.FIELD_PROJECT_ID, project.getUuid())
        .setFieldValue(QGChangeNotification.FIELD_ALERT_NAME, statusLabel)
        .setFieldValue(QGChangeNotification.FIELD_ALERT_TEXT, alertText)
        .setFieldValue(QGChangeNotification.FIELD_ALERT_LEVEL, newStatus.name())
        .setFieldValue(QGChangeNotification.FIELD_IS_NEW_ALERT, Boolean.toString(isNewAlert));

      notification.setFieldValue(QGChangeNotification.FIELD_IS_MAIN_BRANCH, Boolean.toString(branch.isMain()));
      notification.setFieldValue(QGChangeNotification.FIELD_BRANCH, branch.getKey());

      if (previousStatus != null) {
        notification.setFieldValue(QGChangeNotification.FIELD_PREVIOUS_ALERT_LEVEL, toStatusLabel(previousStatus));
      }

      String ratingMetrics = metricsByKey.values().stream()
        .filter(m -> m.getValueType().equals(Metric.ValueType.RATING.name()))
        .map(MetricDto::getShortName)
        .collect(Collectors.joining(","));
      notification.setFieldValue(QGChangeNotification.FIELD_RATING_METRICS, ratingMetrics);

      notificationService.deliverEmails(singleton(notification));

      // compatibility with old API
      notificationService.deliver(notification);
    }
  }

  private static String toStatusLabel(Metric.Level status) {
    return status == Metric.Level.OK ? "Passed" : "Failed";
  }

}
