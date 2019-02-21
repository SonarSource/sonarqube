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
import org.sonar.api.batch.fs.internal.DefaultInputModule;

public class DefaultModuleFileSystem extends DefaultFileSystem {

  public DefaultModuleFileSystem(ModuleInputComponentStore moduleInputFileCache, DefaultInputModule module) {
    super(module.getBaseDir(), moduleInputFileCache);
    initFields(module);
  }

  @VisibleForTesting
  public DefaultModuleFileSystem(DefaultInputModule module) {
    super(module.getBaseDir());
    initFields(module);
  }

  private void initFields(DefaultInputModule module) {
    setWorkDir(module.getWorkDir());
    setEncoding(module.getEncoding());
  }

}
