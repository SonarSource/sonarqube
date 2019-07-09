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
package org.sonar.scanner.scan.filesystem;

import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.predicates.DefaultFilePredicates;

public class DefaultProjectFileSystem extends DefaultFileSystem {

  public DefaultProjectFileSystem(InputComponentStore inputComponentStore, DefaultInputProject project) {
    super(project.getBaseDir(), inputComponentStore, new DefaultFilePredicates(project.getBaseDir()));
    setFields(project);
  }

  public DefaultProjectFileSystem(DefaultInputProject project) {
    super(project.getBaseDir());
    setFields(project);
  }

  private void setFields(DefaultInputProject project) {
    setWorkDir(project.getWorkDir());
    setEncoding(project.getEncoding());
  }

}
