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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.core.NotDryRun;

import java.util.Iterator;

@NotDryRun
public class VersionEventsSensor implements Sensor {

  private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

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
    boolean isReleaseVersion = !version.endsWith(SNAPSHOT_SUFFIX);
    String snapshotVersionToDelete = isReleaseVersion ? version + SNAPSHOT_SUFFIX : "";
    for (Iterator<Event> it = context.getEvents(project).iterator(); it.hasNext();) {
      Event event = it.next();
      if (event.isVersionCategory()) {
        if (snapshotVersionToDelete.equals(event.getName()) || (version.equals(event.getName()) && !isReleaseVersion)) {
          it.remove();
          context.deleteEvent(event);
        } else if (version.equals(event.getName()) && isReleaseVersion) {
          // we try to delete a released version that already exists in the project history => this shouldn't happen
          throw new IllegalStateException("A Sonar analysis can't delete a released version that already exists in the project history (version "
            + version + "). Please change the version of the project or clean its history first.");
        }
      }
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
