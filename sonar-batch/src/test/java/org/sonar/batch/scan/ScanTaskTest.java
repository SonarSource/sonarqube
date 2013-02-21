/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan;

import org.junit.Test;
import org.mockito.InOrder;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.batch.ProjectTree;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ScanTaskTest {

  @Test
  public void should_scan_each_module() {

    final Project project = new Project("parent");
    final ProjectTree projectTree = mock(ProjectTree.class);
    Project module1 = new Project("module1");
    module1.setParent(project);
    Project module2 = new Project("module2");
    module2.setParent(project);

    when(projectTree.getRootProject()).thenReturn(project);
    ScanTask scanTask = new ScanTask(projectTree, mock(ComponentContainer.class));
    ScanTask spy = spy(scanTask);
    doNothing().when(spy).scan(any(Project.class));
    spy.execute();

    InOrder inOrder = inOrder(spy);

    inOrder.verify(spy).scan(module1);
    inOrder.verify(spy).scan(module2);
    inOrder.verify(spy).scan(project);
  }
}
