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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ComponentVisitor;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.event.Event;
import org.sonar.ce.task.projectanalysis.event.EventRepository;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.QualityGateStatus;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.server.notification.NotificationService;
import org.sonar.server.qualitygate.notification.QGChangeNotification;

import static java.util.Collections.singleton;

/**
 * This step must be executed after computation of quality gate measure {@link QualityGateMeasuresStep}
 */
public class QualityGateEventsStep implements ComputationStep {
  private static final Logger LOGGER = Loggers.get(QualityGateEventsStep.class);

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
    // no notification on short living branch and pull request as there is no real Quality Gate on those
    if (analysisMetadataHolder.isSLBorPR()) {
      return;
    }
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.PROJECT, ComponentVisitor.Order.PRE_ORDER) {
        @Override
        public void visitProject(Component project) {
          executeForProject(project);
        }
      }).visit(treeRootHolder.getRoot());
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
      LOGGER.warn(String.format("Previous Quality gate status for project %s is not a supported value. Can not compute Quality Gate event", project.getDbKey()));
      checkNewQualityGate(project, rawStatus);
      return;
    }
    QualityGateStatus baseStatus = baseMeasure.get().getQualityGateStatus();

    if (baseStatus.getStatus() != rawStatus.getStatus()) {
      // The QualityGate status has changed
      String label = String.format("%s (was %s)", rawStatus.getStatus().getColorName(), baseStatus.getStatus().getColorName());
      createEvent(project, label, rawStatus.getText());
      boolean isNewKo = rawStatus.getStatus() == Measure.Level.OK;
      notifyUsers(project, label, rawStatus, isNewKo);
    }
  }

  private void checkNewQualityGate(Component project, QualityGateStatus rawStatus) {
    if (rawStatus.getStatus() != Measure.Level.OK) {
      // There were no defined alerts before, so this one is a new one
      createEvent(project, rawStatus.getStatus().getColorName(), rawStatus.getText());
      notifyUsers(project, rawStatus.getStatus().getColorName(), rawStatus, true);
    }
  }

  /**
   * @param label "Red (was Green)"
   * @param rawStatus OK or ERROR + optional text
   */
  private void notifyUsers(Component project, String label, QualityGateStatus rawStatus, boolean isNewAlert) {
    QGChangeNotification notification = new QGChangeNotification();
    notification
      .setDefaultMessage(String.format("Alert on %s: %s", project.getName(), label))
      .setFieldValue("projectName", project.getName())
      .setFieldValue("projectKey", project.getKey())
      .setFieldValue("projectVersion", project.getProjectAttributes().getProjectVersion())
      .setFieldValue("alertName", label)
      .setFieldValue("alertText", rawStatus.getText())
      .setFieldValue("alertLevel", rawStatus.getStatus().toString())
      .setFieldValue("isNewAlert", Boolean.toString(isNewAlert));
    Branch branch = analysisMetadataHolder.getBranch();
    if (!branch.isMain()) {
      notification.setFieldValue("branch", branch.getName());
    }
    notificationService.deliverEmails(singleton(notification));

    // compatibility with old API
    notificationService.deliver(notification);
  }

  private void createEvent(Component project, String name, @Nullable String description) {
    eventRepository.add(project, Event.createAlert(name, null, description));
  }

  @Override
  public String getDescription() {
    return "Generate Quality gate events";
  }

}
