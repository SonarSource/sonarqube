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
package org.sonar.api.batch.bootstrap;

import org.sonar.api.BatchExtension;
import org.sonar.api.batch.InstantiationStrategy;

/**
 * This extension point allows to change project structure at runtime. It is executed once during task startup.
 * Some use-cases :
 * <ul>
 *   <li>Add sub-projects which are not defined in batch bootstrapper. For example the C# plugin gets the hierarchy
 *   of sub-projects from the Visual Studio metadata file. The single root pom.xml does not contain any declarations of
 *   modules</li>
 *   <li>Change project metadata like description or source directories.</li>
 * </ul>
 *
 * @since 2.9
 */
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public abstract class ProjectBuilder implements BatchExtension {

  private ProjectReactor reactor;

  protected ProjectBuilder(final ProjectReactor reactor) {
    this.reactor = reactor;
  }

  public final void start() {
    build(reactor);
  }

  protected abstract void build(ProjectReactor reactor);
}
