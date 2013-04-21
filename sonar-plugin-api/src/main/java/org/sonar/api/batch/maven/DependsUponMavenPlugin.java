/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch.maven;

import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.resources.Project;

/**
 * Can be used only for {@link org.sonar.api.batch.Initializer Initializers}, {@link org.sonar.api.batch.Sensor Sensors} and
 * {@link org.sonar.api.batch.PostJob PostJobs}.
 * 
 * <p>
 * If extension implements this interface, then it would be available only when Sonar executed from Maven. So we recommend to use this
 * interface for Initializers instead of Sensors.
 * </p>
 * 
 * @since 1.10
 */
@SupportedEnvironment("maven")
public interface DependsUponMavenPlugin extends BatchExtension {

  MavenPluginHandler getMavenPluginHandler(Project project);

}
