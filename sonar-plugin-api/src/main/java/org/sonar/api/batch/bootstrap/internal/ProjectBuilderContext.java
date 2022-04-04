/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.batch.bootstrap.internal;


import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Configuration;

/**
 * Context that is passed to {@link org.sonar.api.batch.bootstrap.ProjectBuilder} as parameter.
 * Important - plugins must use this class only for unit test needs.
 *
 * @deprecated since 6.5
 * @since 3.7
 */
@Deprecated
public class ProjectBuilderContext implements ProjectBuilder.Context {

  private final ProjectReactor reactor;
  private final Configuration configuration;

  public ProjectBuilderContext(ProjectReactor reactor, Configuration configuration) {
    this.reactor = reactor;
    this.configuration = configuration;
  }

  @Override
  public ProjectReactor projectReactor() {
    return reactor;
  }

  @Override
  public Configuration config() {
    return configuration;
  }
}
