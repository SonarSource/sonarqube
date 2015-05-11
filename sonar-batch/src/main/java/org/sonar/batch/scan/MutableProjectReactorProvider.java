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
package org.sonar.batch.scan;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectBootstrapper;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.AnalysisProperties;

import javax.annotation.Nullable;

public class MutableProjectReactorProvider extends ProviderAdapter {

  private final ProjectBootstrapper projectBootstrapper;
  private ProjectReactor reactor = null;

  public MutableProjectReactorProvider(@Nullable ProjectBootstrapper projectBootstrapper) {
    this.projectBootstrapper = projectBootstrapper;
  }

  public ProjectReactor provide(ProjectReactorBuilder builder, AnalysisProperties settings) {
    if (reactor == null) {
      // Look for a deprecated custom ProjectBootstrapper for old versions of SQ Runner
      if (projectBootstrapper == null
        // Starting from Maven plugin 2.3 then only DefaultProjectBootstrapper should be used.
        || "true".equals(settings.property("sonar.mojoUseRunner"))) {
        // Use default SonarRunner project bootstrapper
        reactor = builder.execute();
      } else {
        reactor = projectBootstrapper.bootstrap();
      }
      if (reactor == null) {
        throw new SonarException(projectBootstrapper + " has returned null as ProjectReactor");
      }
    }
    return reactor;
  }
}
