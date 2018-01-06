/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.batch;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.resources.Project;

/**
 * <p>
 * Initializer are executed at the very beginning of each module analysis, prior the core listing files to be analyzed. It means {@link FileSystem} should not be accessed.
 * <p>
 * @since 2.6
 * @deprecated since 6.6 no known valid use case
 */
@ScannerSide
@ExtensionPoint
@Deprecated
public abstract class Initializer implements CheckProject {

  /**
   * @deprecated since 5.6 should no more be implemented by plugins
   */
  @Deprecated
  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  /**
   * @deprecated since 5.6 override {@link #execute()} instead
   */
  @Deprecated
  public void execute(Project project) {
    execute();
  }

  public void execute() {
    // To be implemented by client
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
