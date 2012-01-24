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
package org.sonar.plugins.dbcleaner.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.core.NotDryRun;
import org.sonar.plugins.dbcleaner.purges.ProjectPurgeContext;
import org.sonar.plugins.dbcleaner.purges.ProjectPurgeTask;

import java.util.Date;

@NotDryRun
public class ProjectPurgePostJob implements PostJob {

  private ProjectPurgeTask task;

  public ProjectPurgePostJob(ProjectPurgeTask task) {
    this.task = task;
  }

  public void executeOn(final Project project, SensorContext context) {
    final Date beforeBuildDate = new Date();

    ProjectPurgeContext purgeContext = new ProjectPurgeContext() {
      public Long getRootProjectId() {
        return new Long(project.getId());
      }

      public Date getBeforeBuildDate() {
        return beforeBuildDate;
      }
    };

    Logger logger = LoggerFactory.getLogger(getClass());
    logger.info("Optimizing project");
    task.execute(purgeContext);
  }
}
