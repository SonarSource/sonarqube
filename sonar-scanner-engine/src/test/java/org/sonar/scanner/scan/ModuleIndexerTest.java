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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

public class ModuleIndexerTest {
  private ModuleIndexer indexer;
  private DefaultComponentTree tree;
  private DefaultInputModuleHierarchy moduleHierarchy;
  private InputComponentStore componentStore;

  @Before
  public void setUp() {
    componentStore = new InputComponentStore(new PathResolver());
    tree = new DefaultComponentTree();
    moduleHierarchy = mock(DefaultInputModuleHierarchy.class);
    indexer = new ModuleIndexer(tree, componentStore, moduleHierarchy);
  }

  @Test
  public void testIndex() {
    DefaultInputModule root = mock(DefaultInputModule.class);
    DefaultInputModule mod1 = mock(DefaultInputModule.class);
    DefaultInputModule mod2 = mock(DefaultInputModule.class);
    DefaultInputModule mod3 = mock(DefaultInputModule.class);

    when(root.key()).thenReturn("root");
    when(mod1.key()).thenReturn("mod1");
    when(mod2.key()).thenReturn("mod2");
    when(mod3.key()).thenReturn("mod3");

    when(moduleHierarchy.children(root)).thenReturn(Arrays.asList(mod1, mod2, mod3));
    when(moduleHierarchy.root()).thenReturn(root);

    indexer.start();

    InputModule rootModule = moduleHierarchy.root();
    assertThat(rootModule).isNotNull();
    assertThat(moduleHierarchy.children(rootModule)).hasSize(3);
    assertThat(tree.getChildren(rootModule)).hasSize(3);
  }
}
