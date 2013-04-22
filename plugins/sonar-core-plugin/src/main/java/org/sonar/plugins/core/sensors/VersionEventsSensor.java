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

import org.apache.commons.lang.StringUtils;
import org.sonar.core.DryRunIncompatible;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;

import java.util.Iterator;

@DryRunIncompatible
public class VersionEventsSensor implements Sensor {

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void analyse(Project project, SensorContext context) {
    if (StringUtils.isBlank(project.getAnalysisVersion())) {
      return;
    }
    deleteDeprecatedEvents(project, context);
    context.createEvent(project, project.getAnalysisVersion(), null, Event.CATEGORY_VERSION, null);
  }

  private void deleteDeprecatedEvents(Project project, SensorContext context) {
    String version = project.getAnalysisVersion();
    for (Iterator<Event> it = context.getEvents(project).iterator(); it.hasNext();) {
      Event event = it.next();
      if (event.isVersionCategory() && version.equals(event.getName())) {
        it.remove();
        context.deleteEvent(event);
      }
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
