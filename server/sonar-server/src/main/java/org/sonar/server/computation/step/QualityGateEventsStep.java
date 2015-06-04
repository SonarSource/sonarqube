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
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;

public class QualityGateEventsStep implements ComputationStep {
  private static final Logger LOGGER = Loggers.get(QualityGateEventsStep.class);

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
    Optional<Measure> currentStatus = measureRepository.findCurrent(project, CoreMetrics.ALERT_STATUS);
    if (!currentStatus.isPresent()) {
      return;
    }
    Measure measure = currentStatus.get();
    if (!measure.hasAlertStatus()) {
      return;
    }

    checkQualityGateStatusChange(project, measure.getAlertStatus(), measure.hasAlertText() ? measure.getAlertText() : null);
  }

  private void checkQualityGateStatusChange(Component project, Measure.AlertStatus currentStatus, @Nullable String alertText) {
    Optional<Measure> previousMeasure = measureRepository.findPrevious(project, CoreMetrics.ALERT_STATUS);
    if (!previousMeasure.isPresent()) {
      checkNewQualityGate(project, currentStatus, alertText);
      return;
    }

    if (!previousMeasure.get().hasAlertStatus()) {
      LOGGER.warn(String.format("Previous alterStatus for project %s is not a supported value. Can not compute Quality Gate event", project.getKey()));
      checkNewQualityGate(project, currentStatus, alertText);
      return;
    }
    Measure.AlertStatus previousStatus = previousMeasure.get().getAlertStatus();

    if (previousStatus != currentStatus) {
      // The alert status has changed
      String alertName = String.format("%s (was %s)", currentStatus.getColorName(), previousStatus.getColorName());
      createEvent(project, alertName, alertText);
      // FIXME @Simon uncomment and/or rewrite code below when implementing notifications in CE
      // There was already a Orange/Red alert, so this is no new alert: it has just changed
      // boolean isNewAlert = previousQGStatus == AlertStatus.OK;
      // notifyUsers(project, alertName, alertText, alertLevel, isNewAlert);
    }
  }

  private void checkNewQualityGate(Component project, Measure.AlertStatus currentStatus, @Nullable String alertText) {
    if (currentStatus != Measure.AlertStatus.OK) {
      // There were no defined alerts before, so this one is a new one
      createEvent(project, currentStatus.getColorName(), alertText);
      // notifyUsers(project, alertName, alertText, alertLevel, true);
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

  private void createEvent(Component project, String name, @Nullable String description) {
    eventRepository.add(project, Event.createAlert(name, null, description));
  }

  @Override
  public String getDescription() {
    return "Generate Quality Gate Events";
  }
}
