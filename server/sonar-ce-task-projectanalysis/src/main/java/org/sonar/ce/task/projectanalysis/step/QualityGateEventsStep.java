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
package org.sonar.ce.task.projectanalysis.step;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.event.Event;
import org.sonar.ce.task.projectanalysis.event.EventRepository;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.QualityGateStatus;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.Metric.MetricType;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.server.notification.NotificationService;
import org.sonar.server.qualitygate.notification.QGChangeNotification;

import static java.util.Collections.singleton;

/**
 * This step must be executed after computation of quality gate measure {@link QualityGateMeasuresStep}
 */
public class QualityGateEventsStep implements ComputationStep {
  private static final Logger LOGGER = LoggerFactory.getLogger(QualityGateEventsStep.class);

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final EventRepository eventRepository;
  private final NotificationService notificationService;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public QualityGateEventsStep(TreeRootHolder treeRootHolder,
    MetricRepository metricRepository, MeasureRepository measureRepository, EventRepository eventRepository,
    NotificationService notificationService, AnalysisMetadataHolder analysisMetadataHolder) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.eventRepository = eventRepository;
    this.notificationService = notificationService;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    // no notification on pull requests as there is no real Quality Gate on those
    if (analysisMetadataHolder.isPullRequest()) {
      return;
    }
    executeForProject(treeRootHolder.getRoot());
  }

  private void executeForProject(Component project) {
    Metric metric = metricRepository.getByKey(CoreMetrics.ALERT_STATUS_KEY);
    Optional<Measure> rawStatus = measureRepository.getRawMeasure(project, metric);
    if (!rawStatus.isPresent() || !rawStatus.get().hasQualityGateStatus()) {
      return;
    }

    checkQualityGateStatusChange(project, metric, rawStatus.get().getQualityGateStatus());
  }

  private void checkQualityGateStatusChange(Component project, Metric metric, QualityGateStatus rawStatus) {
    Optional<Measure> baseMeasure = measureRepository.getBaseMeasure(project, metric);
    if (!baseMeasure.isPresent()) {
      checkNewQualityGate(project, rawStatus);
      return;
    }

    if (!baseMeasure.get().hasQualityGateStatus()) {
      LOGGER.warn(String.format("Previous Quality gate status for project %s is not a supported value. Can not compute Quality Gate event", project.getKey()));
      checkNewQualityGate(project, rawStatus);
      return;
    }
    QualityGateStatus baseStatus = baseMeasure.get().getQualityGateStatus();

    if (baseStatus.getStatus() != rawStatus.getStatus()) {
      // The QualityGate status has changed
      createEvent(rawStatus.getStatus().getLabel(), rawStatus.getText());
      boolean isNewKo = rawStatus.getStatus() == Measure.Level.OK;
      notifyUsers(project, rawStatus, isNewKo);
    }
  }

  private void checkNewQualityGate(Component project, QualityGateStatus rawStatus) {
    if (rawStatus.getStatus() != Measure.Level.OK) {
      // There were no defined alerts before, so this one is a new one
      createEvent(rawStatus.getStatus().getLabel(), rawStatus.getText());
      notifyUsers(project, rawStatus, true);
    }
  }

  /**
   * @param rawStatus OK or ERROR + optional text
   */
  private void notifyUsers(Component project, QualityGateStatus rawStatus, boolean isNewAlert) {
    QGChangeNotification notification = new QGChangeNotification();
    notification
      .setDefaultMessage(String.format("Alert on %s: %s", project.getName(), rawStatus.getStatus().getLabel()))
      .setFieldValue("projectName", project.getName())
      .setFieldValue("projectKey", project.getKey())
      .setFieldValue("projectVersion", project.getProjectAttributes().getProjectVersion())
      .setFieldValue("alertName", rawStatus.getStatus().getLabel())
      .setFieldValue("alertText", rawStatus.getText())
      .setFieldValue("alertLevel", rawStatus.getStatus().toString())
      .setFieldValue("isNewAlert", Boolean.toString(isNewAlert));
    Branch branch = analysisMetadataHolder.getBranch();
    if (!branch.isMain()) {
      notification.setFieldValue("branch", branch.getName());
    }

    List<Metric> ratingMetrics = metricRepository.getMetricsByType(MetricType.RATING);
    String ratingMetricsInOneString = ratingMetrics.stream().map(Metric::getName).collect(Collectors.joining(","));
    notification.setFieldValue("ratingMetrics", ratingMetricsInOneString);
    notificationService.deliverEmails(singleton(notification));

    // compatibility with old API
    notificationService.deliver(notification);
  }

  private void createEvent(String name, @Nullable String description) {
    eventRepository.add(Event.createAlert(name, null, description));
  }

  @Override
  public String getDescription() {
    return "Generate Quality gate events";
  }

}
