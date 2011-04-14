/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.checkstyle;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.Logs;

public class CheckstyleSensor implements Sensor {

  private RulesProfile profile;
  private CheckstyleExecutor executor;

  public CheckstyleSensor(RulesProfile profile, CheckstyleExecutor executor) {
    this.profile = profile;
    this.executor = executor;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.getFileSystem().hasJavaSourceFiles() &&
        !profile.getActiveRulesByRepository(CheckstyleConstants.REPOSITORY_KEY).isEmpty();
  }

  public void analyse(Project project, SensorContext context) {
    executor.execute();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
