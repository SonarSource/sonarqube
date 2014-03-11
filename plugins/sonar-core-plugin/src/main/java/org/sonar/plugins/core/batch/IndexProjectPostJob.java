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
package org.sonar.plugins.core.batch;

import org.sonar.core.DryRunIncompatible;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.core.resource.ResourceIndexerDao;

/**
 * @since 2.13
 */
@DryRunIncompatible
public class IndexProjectPostJob implements PostJob {
  private ResourceIndexerDao indexer;

  public IndexProjectPostJob(ResourceIndexerDao indexer) {
    this.indexer = indexer;
  }

  public void executeOn(Project project, SensorContext context) {
    if (project.getId() != null) {
      indexer.indexProject(project.getId());
    }
  }
}
