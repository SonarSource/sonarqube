/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.api.batch.bootstrap.ImmutableProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.scan.filesystem.BatchIdGenerator;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

/**
 * Indexes all modules into {@link DefaultComponentTree}, {@link DefaultInputModuleHierarchy) and {@link InputComponentStore}, using the 
 * project definitions provided by the {@link ImmutableProjectReactor}.
 */
public class ModuleIndexer implements Startable {
  private final ImmutableProjectReactor projectReactor;
  private final DefaultComponentTree componentTree;
  private final DefaultInputModuleHierarchy moduleHierarchy;
  private final BatchIdGenerator batchIdGenerator;
  private final InputComponentStore componentStore;

  public ModuleIndexer(ImmutableProjectReactor projectReactor, DefaultComponentTree componentTree,
    InputComponentStore componentStore, BatchIdGenerator batchIdGenerator, DefaultInputModuleHierarchy moduleHierarchy) {
    this.projectReactor = projectReactor;
    this.componentTree = componentTree;
    this.componentStore = componentStore;
    this.moduleHierarchy = moduleHierarchy;
    this.batchIdGenerator = batchIdGenerator;
  }

  @Override
  public void start() {
    DefaultInputModule root = new DefaultInputModule(projectReactor.getRoot(), batchIdGenerator.get());
    moduleHierarchy.setRoot(root);
    componentStore.put(root);
    createChildren(root);
  }

  private void createChildren(DefaultInputModule parent) {
    for (ImmutableProjectDefinition def : parent.definition().getSubProjects()) {
      DefaultInputModule child = new DefaultInputModule(def, batchIdGenerator.get());
      moduleHierarchy.index(child, parent);
      componentTree.index(child, parent);
      componentStore.put(child);
      createChildren(child);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
