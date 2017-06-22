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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.scanner.scan.filesystem.BatchIdGenerator;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModuleIndexerTest {
  private ModuleIndexer indexer;
  private DefaultComponentTree tree;
  private DefaultInputModuleHierarchy moduleHierarchy;
  private ImmutableProjectReactor reactor;
  private InputComponentStore componentStore;

  @Before
  public void setUp() {
    reactor = mock(ImmutableProjectReactor.class);
    componentStore = new InputComponentStore(new PathResolver());
    tree = new DefaultComponentTree();
    moduleHierarchy = new DefaultInputModuleHierarchy();
    indexer = new ModuleIndexer(reactor, tree, componentStore, new BatchIdGenerator(), moduleHierarchy);
  }

  @Test
  public void testIndex() {
    ProjectDefinition root = ProjectDefinition.create().setKey("root");
    ProjectDefinition mod1 = ProjectDefinition.create().setKey("mod1");
    ProjectDefinition mod2 = ProjectDefinition.create().setKey("mod2");
    ProjectDefinition mod3 = ProjectDefinition.create().setKey("mod3");
    ProjectDefinition mod4 = ProjectDefinition.create().setKey("mod4");

    root.addSubProject(mod1);
    mod1.addSubProject(mod2);
    root.addSubProject(mod3);
    root.addSubProject(mod4);

    when(reactor.getRoot()).thenReturn(root.build());
    indexer.start();

    InputModule rootModule = moduleHierarchy.root();
    assertThat(rootModule).isNotNull();
    assertThat(moduleHierarchy.children(rootModule)).hasSize(3);
    assertThat(tree.getChildren(rootModule)).hasSize(3);
  }
}
