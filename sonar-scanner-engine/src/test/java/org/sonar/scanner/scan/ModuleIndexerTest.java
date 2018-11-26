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
package org.sonar.scanner.scan;

import java.util.Arrays;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModuleIndexerTest {
  private ModuleIndexer indexer;
  private DefaultInputModuleHierarchy moduleHierarchy;
  private InputComponentStore componentStore;

  public void createIndexer(DefaultInputProject rootProject) {
    componentStore = new InputComponentStore(mock(BranchConfiguration.class));
    moduleHierarchy = mock(DefaultInputModuleHierarchy.class);
    indexer = new ModuleIndexer(componentStore, moduleHierarchy);
  }

  @Test
  public void testIndex() {
    ProjectDefinition rootDef = mock(ProjectDefinition.class);
    ProjectDefinition def = mock(ProjectDefinition.class);
    when(rootDef.getParent()).thenReturn(null);
    when(def.getParent()).thenReturn(rootDef);

    DefaultInputModule root = mock(DefaultInputModule.class);
    DefaultInputModule mod1 = mock(DefaultInputModule.class);
    DefaultInputModule mod2 = mock(DefaultInputModule.class);
    DefaultInputModule mod3 = mock(DefaultInputModule.class);

    when(root.key()).thenReturn("root");
    when(mod1.key()).thenReturn("mod1");
    when(mod2.key()).thenReturn("mod2");
    when(mod3.key()).thenReturn("mod3");

    when(root.getKeyWithBranch()).thenReturn("root");
    when(mod1.getKeyWithBranch()).thenReturn("mod1");
    when(mod2.getKeyWithBranch()).thenReturn("mod2");
    when(mod3.getKeyWithBranch()).thenReturn("mod3");

    when(root.definition()).thenReturn(rootDef);
    when(mod1.definition()).thenReturn(def);
    when(mod2.definition()).thenReturn(def);
    when(mod3.definition()).thenReturn(def);

    createIndexer(mock(DefaultInputProject.class));
    when(moduleHierarchy.root()).thenReturn(root);
    when(moduleHierarchy.children(root)).thenReturn(Arrays.asList(mod1, mod2, mod3));

    indexer.start();

    DefaultInputModule rootModule = moduleHierarchy.root();
    assertThat(rootModule).isNotNull();
    assertThat(moduleHierarchy.children(rootModule)).hasSize(3);
  }
}
