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
package org.sonar.batch.scan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.batch.ProfileProvider;
import org.sonar.batch.ResourceFilters;
import org.sonar.batch.ViolationFilters;
import org.sonar.batch.bootstrap.ModuleContainer;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.phases.Phases;

public class ScanContainer extends ModuleContainer {
  private static final Logger LOG = LoggerFactory.getLogger(ScanContainer.class);
  private Project project;

  public ScanContainer(Project project, ProjectDefinition projectDefinition, Snapshot snapshot) {
    super(project, projectDefinition, snapshot);
    this.project = project;
  }

  @Override
  protected void configure() {
    super.configure();
    container.addPicoAdapter(new ProfileProvider());
  }

  private void logSettings() {
    LOG.info("-------------  Inspecting {}", project.getName());
  }

  /**
   * Analyze project
   */
  @Override
  protected void doStart() {
    DefaultIndex index = container.getComponentByType(DefaultIndex.class);
    index.setCurrentProject(project,
        container.getComponentByType(ResourceFilters.class),
        container.getComponentByType(ViolationFilters.class),
        container.getComponentByType(RulesProfile.class));

    logSettings();
    container.getComponentByType(Phases.class).execute(project);
  }
}
