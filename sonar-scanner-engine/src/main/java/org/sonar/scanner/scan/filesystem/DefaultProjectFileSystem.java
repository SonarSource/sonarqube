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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.scanner.analysis.DefaultAnalysisMode;

public class DefaultProjectFileSystem extends DefaultFileSystem {

  public DefaultProjectFileSystem(InputComponentStore inputComponentStore, DefaultInputProject project, DefaultAnalysisMode mode,
    StatusDetection statusDetection) {
    super(project.getBaseDir(), inputComponentStore);
    setFields(project, mode, statusDetection);
  }

  @VisibleForTesting
  public DefaultProjectFileSystem(DefaultInputProject project, DefaultAnalysisMode mode, StatusDetection statusDetection) {
    super(project.getBaseDir());
    setFields(project, mode, statusDetection);
  }

  private void setFields(DefaultInputProject project, DefaultAnalysisMode mode, StatusDetection statusDetection) {
    setWorkDir(project.getWorkDir());
    setEncoding(project.getEncoding());

    // filter the files sensors have access to
    if (!mode.scanAllFiles()) {
      setDefaultPredicate(p -> new SameInputFilePredicate(p, statusDetection, project.definition().getKeyWithBranch()));
    }
  }

}
