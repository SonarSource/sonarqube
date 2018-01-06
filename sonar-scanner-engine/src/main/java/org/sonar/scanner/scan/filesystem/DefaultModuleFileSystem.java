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
package org.sonar.scanner.scan.filesystem;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.analysis.DefaultAnalysisMode;

public class DefaultModuleFileSystem extends DefaultFileSystem {

  public DefaultModuleFileSystem(ModuleInputComponentStore moduleInputFileCache, DefaultInputModule module, ModuleFileSystemInitializer initializer, DefaultAnalysisMode mode,
    StatusDetection statusDetection) {
    super(module.getBaseDir(), moduleInputFileCache);
    setFields(module, initializer, mode, statusDetection);
  }

  @VisibleForTesting
  public DefaultModuleFileSystem(DefaultInputModule module, ModuleFileSystemInitializer initializer, DefaultAnalysisMode mode, StatusDetection statusDetection) {
    super(module.getBaseDir());
    setFields(module, initializer, mode, statusDetection);
  }

  private void setFields(DefaultInputModule module, ModuleFileSystemInitializer initializer, DefaultAnalysisMode mode, StatusDetection statusDetection) {
    setWorkDir(module.getWorkDir());
    setEncoding(initializer.defaultEncoding());

    // filter the files sensors have access to
    if (!mode.scanAllFiles()) {
      setDefaultPredicate(p -> new SameInputFilePredicate(p, statusDetection, module.definition().getKeyWithBranch()));
    }
  }

}
