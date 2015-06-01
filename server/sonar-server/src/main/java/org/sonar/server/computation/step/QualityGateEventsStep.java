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

public class QualityGateEventsStep implements ComputationStep {
  public static final Logger LOGGER = Loggers.get(QualityGateEventsStep.class);
  private final TreeRootHolder treeRootHolder;
  private final EventRepository eventRepository;
  private final MeasureRepository measureRepository;

  public QualityGateEventsStep(TreeRootHolder treeRootHolder, EventRepository eventRepository, MeasureRepository measureRepository) {
    this.eventRepository = eventRepository;
    this.measureRepository = measureRepository;
    this.treeRootHolder = treeRootHolder;
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
    Optional<BatchReport.Measure> currentStatus = measureRepository.findCurrent(project, CoreMetrics.ALERT_STATUS);
    if (!currentStatus.isPresent()) {
      return;
    }
    Optional<AlertStatus> alertLevel = parse(currentStatus.get().getAlertStatus());
    if (!alertLevel.isPresent()) {
      return;
    }

    checkQualityGateStatusChange(project, alertLevel.get(), currentStatus.get().getAlertText());
  }

  private void checkQualityGateStatusChange(Component project, AlertStatus currentStatus, String alertText) {
    Optional<MeasureDto> previousMeasure = measureRepository.findPrevious(project, CoreMetrics.ALERT_STATUS);
    if (!previousMeasure.isPresent()) {
      checkNewQualityGate(project, currentStatus, alertText);
      return;
    }

    Optional<AlertStatus> previousQGStatus = parse(previousMeasure.get().getAlertStatus());
    if (!previousQGStatus.isPresent()) {
      LOGGER.warn("Previous alterStatus for project %s is not a supported value. Can not compute Quality Gate event");
      checkNewQualityGate(project, currentStatus, alertText);
      return;
    }

    if (previousQGStatus.get() != currentStatus) {
      // The alert status has changed
      String alertName = String.format("%s (was %s)", currentStatus.getColorName(), previousQGStatus.get().getColorName());
      createEvent(project, alertName, alertText);
      // FIXME @Simon uncomment and/or rewrite code below when implementing notifications in CE
      // There was already a Orange/Red alert, so this is no new alert: it has just changed
      // boolean isNewAlert = previousQGStatus == AlertStatus.OK;
      // notifyUsers(project, alertName, alertText, alertLevel, isNewAlert);
    }
  }

  private void checkNewQualityGate(Component project, AlertStatus currentStatus, String alertText) {
    if (currentStatus != AlertStatus.OK) {
      // There were no defined alerts before, so this one is a new one
      createEvent(project, currentStatus.getColorName(), alertText);
      // notifyUsers(project, alertName, alertText, alertLevel, true);
    }
  }

  private static Optional<AlertStatus> parse(@Nullable String alertStatus) {
    if (alertStatus == null) {
      return Optional.absent();
    }

    try {
      return Optional.of(AlertStatus.valueOf(alertStatus));
    } catch (IllegalArgumentException e) {
      LOGGER.error(String.format("Unsupported alertStatus value '%s' can not be parsed to AlertStatus", alertStatus));
      return Optional.absent();
    }
  }

  private enum AlertStatus {
    OK("Green"), WARN("Orange"), ERROR("Red");

    private String colorName;

    AlertStatus(String colorName) {
      this.colorName = colorName;
    }

    public String getColorName() {
      return colorName;
    }
  }

  // FIXME @Simon uncomment and/or rewrite code below when implementing notifications in CE
  // protected void notifyUsers(Component project, String alertName, String alertText, AlertStatus alertLevel, boolean isNewAlert) {
  // Notification notification = new Notification("alerts")
  // .setDefaultMessage("Alert on " + project.getName() + ": " + alertName)
  // .setFieldValue("projectName", project.getName())
  // .setFieldValue("projectKey", project.getKey())
  // .setFieldValue("projectId", String.valueOf(project.getId()))
  // .setFieldValue("alertName", alertName)
  // .setFieldValue("alertText", alertText)
  // .setFieldValue("alertLevel", alertLevel.toString())
  // .setFieldValue("isNewAlert", Boolean.toString(isNewAlert));
  // notificationManager.scheduleForSending(notification);
  // }

  private void createEvent(Component project, String name, String description) {
    eventRepository.add(project, Event.createAlert(name, null, description));
  }

  @Override
  public String getDescription() {
    return "Generate Quality Gate Events";
  }
}
