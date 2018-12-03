/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan;

import java.lang.reflect.Method;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.batch.bootstrap.internal.ProjectBuilderContext;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;

public class ProjectBuildersExecutor {

  private static final Logger LOG = Loggers.get(ProjectBuildersExecutor.class);

  private final ProjectBuilder[] projectBuilders;

  public ProjectBuildersExecutor(ProjectBuilder... projectBuilders) {
    this.projectBuilders = projectBuilders;
  }

  public ProjectBuildersExecutor() {
    this(new ProjectBuilder[0]);
  }

  public void execute(ProjectReactor reactor) {
    if (projectBuilders.length > 0) {
      Profiler profiler = Profiler.create(LOG).startInfo("Execute project builders");
      ProjectBuilderContext context = new ProjectBuilderContext(reactor);

      for (ProjectBuilder projectBuilder : projectBuilders) {
        try {
          LOG.debug("Execute project builder: {}", projectBuilder.getClass().getName());
          projectBuilder.build(context);
        } catch (Exception e) {
          throw MessageException.of("Failed to execute project builder: " + getDescription(projectBuilder), e);
        }
      }
      profiler.stopInfo();
    }
  }

  private static String getDescription(ProjectBuilder projectBuilder) {
    Method toString;
    try {
      toString = projectBuilder.getClass().getMethod("toString");
    } catch (Exception e) {
      // should never happen as every class has toString
      return projectBuilder.toString();
    }
    if (toString.getDeclaringClass() != Object.class) {
      return projectBuilder.toString();
    } else {
      return projectBuilder.getClass().getName();
    }
  }
}
