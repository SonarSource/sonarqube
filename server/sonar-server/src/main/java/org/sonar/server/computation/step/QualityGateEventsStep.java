/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.notifications.Notification;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.notification.NotificationManager;

public class QualityGateEventsStep implements ComputationStep {
  public static final Logger LOGGER = Loggers.get(QualityGateEventsStep.class);
  private final TreeRootHolder treeRootHolder;
  private final EventRepository eventRepository;
  private final MeasureRepository measureRepository;
  private final NotificationManager notificationManager;

  public QualityGateEventsStep(TreeRootHolder treeRootHolder, EventRepository eventRepository,
    MeasureRepository measureRepository, NotificationManager notificationManager) {
    this.eventRepository = eventRepository;
    this.measureRepository = measureRepository;
    this.treeRootHolder = treeRootHolder;
    this.notificationManager = notificationManager;
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareVisitor(Component.Type.PROJECT, DepthTraversalTypeAwareVisitor.Order.PRE_ORDER) {
      @Override
      public void visitProject(Component project) {
        executeForProject(project);
      }
    }.visit(treeRootHolder.getRoot());
  }

  private void executeForProject(Component project) {
    Optional<BatchReport.Measure> statusMeasure = measureRepository.findCurrent(project, CoreMetrics.ALERT_STATUS);
    if (!statusMeasure.isPresent()) {
      return;
    }
    Optional<GateStatus> status = parse(statusMeasure.get().getAlertStatus());
    if (!status.isPresent()) {
      return;
    }

    checkStatusChange(project, status.get(), statusMeasure.get().getAlertText());
  }

  private void checkStatusChange(Component project, GateStatus status, String description) {
    Optional<MeasureDto> baseMeasure = measureRepository.findPrevious(project, CoreMetrics.ALERT_STATUS);
    if (!baseMeasure.isPresent()) {
      checkStatus(project, status, description);
      return;
    }

    Optional<GateStatus> baseStatus = parse(baseMeasure.get().getAlertStatus());
    if (!baseStatus.isPresent()) {
      LOGGER.warn(String.format("Base status for project %s is not a supported value. Can not compute Quality Gate event", project.getKey()));
      checkStatus(project, status, description);
      return;
    }

    if (baseStatus.get() != status) {
      // The status has changed
      String label = String.format("%s (was %s)", status.getColorName(), baseStatus.get().getColorName());
      createEvent(project, label, description);
      boolean isNewKo = (baseStatus.get() == GateStatus.OK);
      notifyUsers(project, label, description, status, isNewKo);
    }
  }

  private void checkStatus(Component project, GateStatus status, String description) {
    if (status != GateStatus.OK) {
      // There were no defined alerts before, so this one is a new one
      createEvent(project, status.getColorName(), description);
      notifyUsers(project, status.getColorName(), description, status, true);
    }
  }

  private static Optional<GateStatus> parse(@Nullable String alertStatus) {
    if (alertStatus == null) {
      return Optional.absent();
    }

    try {
      return Optional.of(GateStatus.valueOf(alertStatus));
    } catch (IllegalArgumentException e) {
      LOGGER.error(String.format("Unsupported alertStatus value '%s' can not be parsed to AlertStatus", alertStatus));
      return Optional.absent();
    }
  }

  private enum GateStatus {
    OK("Green"), WARN("Orange"), ERROR("Red");

    private String colorName;

    GateStatus(String colorName) {
      this.colorName = colorName;
    }

    public String getColorName() {
      return colorName;
    }
  }

  /**
   * @param label "Red (was Orange)"
   * @param description text detail, for example "Coverage < 80%"
   * @param status OK, WARN or ERROR
   */
  private void notifyUsers(Component project, String label, String description, GateStatus status, boolean isNewAlert) {
    Notification notification = new Notification("alerts")
      .setDefaultMessage(String.format("Alert on %s: %s", project.getName(), label))
      .setFieldValue("projectName", project.getName())
      .setFieldValue("projectKey", project.getKey())
      .setFieldValue("projectUuid", project.getUuid())
      .setFieldValue("alertName", label)
      .setFieldValue("alertText", description)
      .setFieldValue("alertLevel", status.toString())
      .setFieldValue("isNewAlert", Boolean.toString(isNewAlert));
    notificationManager.scheduleForSending(notification);
  }

  private void createEvent(Component project, String name, String description) {
    eventRepository.add(project, Event.createAlert(name, null, description));
  }

  @Override
  public String getDescription() {
    return "Generate Quality Gate Events";
  }
}
