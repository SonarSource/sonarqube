/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.*;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import java.util.List;

public class GenerateAlertEvents implements Decorator {

  private final RulesProfile profile;
  private final TimeMachine timeMachine;

  public GenerateAlertEvents(RulesProfile profile, TimeMachine timeMachine) {
    this.profile = profile;
    this.timeMachine = timeMachine;
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

    Measure pastStatus = (measures != null && measures.size() == 1 ? measures.get(0) : null);
    if (pastStatus != null && pastStatus.getDataAsLevel() != currentStatus.getDataAsLevel()) {
      createEvent(context, getName(pastStatus, currentStatus), currentStatus.getAlertText());

    } else if (pastStatus == null && currentStatus.getDataAsLevel() != Metric.Level.OK) {
      createEvent(context, getName(currentStatus), currentStatus.getAlertText());
    }

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
