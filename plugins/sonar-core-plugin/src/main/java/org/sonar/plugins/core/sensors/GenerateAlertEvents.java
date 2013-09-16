/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.*;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import java.util.List;

public class GenerateAlertEvents implements Decorator {

  private final RulesProfile profile;
  private final TimeMachine timeMachine;
  private NotificationManager notificationManager;

  public GenerateAlertEvents(RulesProfile profile, TimeMachine timeMachine, NotificationManager notificationManager) {
    this.profile = profile;
    this.timeMachine = timeMachine;
    this.notificationManager = notificationManager;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return profile != null && profile.getAlerts() != null && profile.getAlerts().size() > 0;
  }

  @DependsUpon
  public Metric dependsUponAlertStatus() {
    return CoreMetrics.ALERT_STATUS;
  }

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
    context.createEvent(name, description, Event.CATEGORY_ALERT, null);
  }
}
