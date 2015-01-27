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
package org.sonar.batch.scan.sensor;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;

import java.util.Iterator;

@RequiresDB
public class VersionEventsSensor implements Sensor {

  private final AnalysisMode analysisMode;

  public VersionEventsSensor(AnalysisMode analysisMode) {
    this.analysisMode = analysisMode;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return !analysisMode.isPreview();
  }

  @Override
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
