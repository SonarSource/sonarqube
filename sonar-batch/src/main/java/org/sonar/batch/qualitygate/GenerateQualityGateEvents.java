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
package org.sonar.batch.qualitygate;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.batch.protocol.Constants.EventCategory;
import org.sonar.batch.report.EventCache;

import java.util.List;

@RequiresDB
public class GenerateQualityGateEvents implements Decorator {

  private final QualityGate qualityGate;
  private final TimeMachine timeMachine;
  private final NotificationManager notificationManager;
  private final EventCache eventCache;

  public GenerateQualityGateEvents(QualityGate qualityGate, TimeMachine timeMachine, NotificationManager notificationManager, EventCache eventCache) {
    this.qualityGate = qualityGate;
    this.timeMachine = timeMachine;
    this.notificationManager = notificationManager;
    this.eventCache = eventCache;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return qualityGate.isEnabled();
  }

  @DependsUpon
  public Metric dependsUponAlertStatus() {
    return CoreMetrics.ALERT_STATUS;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (!shouldDecorateResource(resource)) {
      return;
    }
    Measure currentStatus = context.getMeasure(CoreMetrics.ALERT_STATUS);
    if (currentStatus == null) {
      return;
    }

    TimeMachineQuery query = new TimeMachineQuery(resource).setOnlyLastAnalysis(true).setMetrics(CoreMetrics.ALERT_STATUS);
    List<Measure> measures = timeMachine.getMeasures(query);

    Measure pastStatus = measures != null && measures.size() == 1 ? measures.get(0) : null;
    checkQualityGateStatusChange(resource, context, currentStatus, pastStatus);

  }

  private void checkQualityGateStatusChange(Resource resource, DecoratorContext context, Measure currentStatus, Measure pastStatus) {
    String alertText = currentStatus.getAlertText();
    Level alertLevel = currentStatus.getDataAsLevel();
    String alertName = null;
    boolean isNewAlert = true;
    if (pastStatus != null && pastStatus.getDataAsLevel() != alertLevel) {
      // The alert status has changed
      alertName = getName(pastStatus, currentStatus);
      if (pastStatus.getDataAsLevel() != Metric.Level.OK) {
        // There was already a Orange/Red alert, so this is no new alert: it has just changed
        isNewAlert = false;
      }
      createEvent(context, alertName, alertText);
      notifyUsers(resource, alertName, alertText, alertLevel, isNewAlert);

    } else if (pastStatus == null && alertLevel != Metric.Level.OK) {
      // There were no defined alerts before, so this one is a new one
      alertName = getName(currentStatus);
      createEvent(context, alertName, alertText);
      notifyUsers(resource, alertName, alertText, alertLevel, isNewAlert);
    }
  }

  protected void notifyUsers(Resource resource, String alertName, String alertText, Level alertLevel, boolean isNewAlert) {
    Notification notification = new Notification("alerts")
      .setDefaultMessage("Alert on " + resource.getLongName() + ": " + alertName)
      .setFieldValue("projectName", resource.getLongName())
      .setFieldValue("projectKey", resource.getKey())
      .setFieldValue("projectId", String.valueOf(resource.getId()))
      .setFieldValue("alertName", alertName)
      .setFieldValue("alertText", alertText)
      .setFieldValue("alertLevel", alertLevel.toString())
      .setFieldValue("isNewAlert", Boolean.toString(isNewAlert));
    notificationManager.scheduleForSending(notification);
  }

  private boolean shouldDecorateResource(Resource resource) {
    return ResourceUtils.isRootProject(resource);
  }

  private String getName(Measure pastStatus, Measure currentStatus) {
    return getName(currentStatus) + " (was " + getName(pastStatus) + ")";

  }

  private String getName(Measure currentStatus) {
    return currentStatus.getDataAsLevel().getColorName();
  }

  private void createEvent(DecoratorContext context, String name, String description) {
    eventCache.createEvent(context.getResource(), name, description, EventCategory.ALERT, null);
  }
}
