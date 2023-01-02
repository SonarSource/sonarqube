/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.predicates.DefaultFilePredicates;

import javax.annotation.Priority;
import javax.inject.Inject;

@Priority(1)
public class DefaultModuleFileSystem extends MutableFileSystem {

  @Inject
  public DefaultModuleFileSystem(ModuleInputComponentStore moduleInputFileCache, DefaultInputModule module) {
    super(module.getBaseDir(), moduleInputFileCache, new DefaultFilePredicates(module.getBaseDir()));
    initFields(module);
  }

  public DefaultModuleFileSystem(DefaultInputModule module) {
    super(module.getBaseDir());
    initFields(module);
  }

  private void initFields(DefaultInputModule module) {
    setWorkDir(module.getWorkDir());
    setEncoding(module.getEncoding());
  }

}
