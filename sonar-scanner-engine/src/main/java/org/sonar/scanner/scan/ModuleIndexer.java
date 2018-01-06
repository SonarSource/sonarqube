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
package org.sonar.scanner.scan;

import org.picocontainer.Startable;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

/**
 * Indexes all modules into {@link DefaultComponentTree}, {@link DefaultInputModuleHierarchy) and {@link InputComponentStore}, using the 
 * project definitions provided by the {@link ImmutableProjectReactor}.
 */
public class ModuleIndexer implements Startable {
  private final DefaultComponentTree componentTree;
  private final InputModuleHierarchy moduleHierarchy;
  private final InputComponentStore componentStore;

  public ModuleIndexer(DefaultComponentTree componentTree, InputComponentStore componentStore, InputModuleHierarchy moduleHierarchy) {
    this.componentTree = componentTree;
    this.componentStore = componentStore;
    this.moduleHierarchy = moduleHierarchy;
  }

  @Override
  public void start() {
    DefaultInputModule root = moduleHierarchy.root();
    indexChildren(root);
  }

  private void indexChildren(DefaultInputModule parent) {
    for (DefaultInputModule module : moduleHierarchy.children(parent)) {
      componentTree.index(module, parent);
      componentStore.put(module);
      indexChildren(module);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
