/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.component;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.InOrder;
import org.sonar.server.computation.context.ComputationContext;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.server.computation.measure.MeasureRepository;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;

public class ChildFirstTypeAwareVisitorTest {

  private static final String UNSUPPORTED_OPERATION_ERROR = "This node has no repository nor context";

  private static final Component FILE_4 = component(FILE, 4);
  private static final Component FILE_5 = component(FILE, 5);
  private static final Component DIRECTORY_3 = component(DIRECTORY, 3, FILE_4, FILE_5);
  private static final Component MODULE_2 = component(MODULE, 2, DIRECTORY_3);
  private static final Component COMPONENT_TREE = component(PROJECT, 1, MODULE_2);

  private final ChildFirstTypeAwareVisitor spyProjectVisitor = spy(new ChildFirstTypeAwareVisitor(PROJECT) {});
  private final ChildFirstTypeAwareVisitor spyModuleVisitor = spy(new ChildFirstTypeAwareVisitor(MODULE) {});
  private final ChildFirstTypeAwareVisitor spyDirectoryVisitor = spy(new ChildFirstTypeAwareVisitor(DIRECTORY) {});
  private final ChildFirstTypeAwareVisitor spyFileVisitor = spy(new ChildFirstTypeAwareVisitor(FILE) {});
  private final InOrder inOrder = inOrder(spyProjectVisitor, spyModuleVisitor, spyDirectoryVisitor, spyFileVisitor);

  @Test(expected = NullPointerException.class)
  public void non_null_max_depth_fast_fail() {
    new ChildFirstTypeAwareVisitor(null) {
    };
  }

  @Test(expected = NullPointerException.class)
  public void visit_null_Component_throws_NPE() {
    spyFileVisitor.visit(null);
  }

  @Test
  public void visit_file_with_depth_FILE_calls_visit_file() {
    Component component = component(FILE, 1);
    spyFileVisitor.visit(component);

    inOrder.verify(spyFileVisitor).visit(component);
    inOrder.verify(spyFileVisitor).visitFile(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_module_with_depth_FILE_calls_visit_module() {
    Component component = component(MODULE, 1);
    spyFileVisitor.visit(component);

    inOrder.verify(spyFileVisitor).visit(component);
    inOrder.verify(spyFileVisitor).visitModule(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_directory_with_depth_FILE_calls_visit_directory() {
    Component component = component(DIRECTORY, 1);
    spyFileVisitor.visit(component);

    inOrder.verify(spyFileVisitor).visit(component);
    inOrder.verify(spyFileVisitor).visitDirectory(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_project_with_depth_FILE_calls_visit_project() {
    Component component = component(PROJECT, 1);
    spyFileVisitor.visit(component);

    inOrder.verify(spyFileVisitor).visit(component);
    inOrder.verify(spyFileVisitor).visitProject(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_file_with_depth_DIRECTORY_does_not_call_visit_file() {
    Component component = component(FILE, 1);
    spyDirectoryVisitor.visit(component);

    inOrder.verify(spyDirectoryVisitor).visit(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_directory_with_depth_DIRECTORY_calls_visit_directory() {
    Component component = component(DIRECTORY, 1);
    spyDirectoryVisitor.visit(component);

    inOrder.verify(spyDirectoryVisitor).visit(component);
    inOrder.verify(spyDirectoryVisitor).visitDirectory(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_module_with_depth_DIRECTORY_calls_visit_module() {
    Component component = component(MODULE, 1);
    spyDirectoryVisitor.visit(component);

    inOrder.verify(spyDirectoryVisitor).visit(component);
    inOrder.verify(spyDirectoryVisitor).visitModule(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_project_with_depth_DIRECTORY_calls_visit_project() {
    Component component = component(PROJECT, 1);
    spyDirectoryVisitor.visit(component);

    inOrder.verify(spyDirectoryVisitor).visit(component);
    inOrder.verify(spyDirectoryVisitor).visitProject(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_file_with_depth_MODULE_does_not_call_visit_file() {
    Component component = component(FILE, 1);
    spyModuleVisitor.visit(component);

    inOrder.verify(spyModuleVisitor).visit(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_directory_with_depth_MODULE_does_not_call_visit_directory() {
    Component component = component(DIRECTORY, 1);
    spyModuleVisitor.visit(component);

    inOrder.verify(spyModuleVisitor).visit(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_module_with_depth_MODULE_calls_visit_module() {
    Component component = component(MODULE, 1);
    spyModuleVisitor.visit(component);

    inOrder.verify(spyModuleVisitor).visit(component);
    inOrder.verify(spyModuleVisitor).visitModule(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_project_with_depth_MODULE_calls_visit_project() {
    Component component = component(MODULE, 1);
    spyModuleVisitor.visit(component);

    inOrder.verify(spyModuleVisitor).visit(component);
    inOrder.verify(spyModuleVisitor).visitModule(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_file_with_depth_PROJECT_does_not_call_visit_file() {
    Component component = component(FILE, 1);
    spyProjectVisitor.visit(component);

    inOrder.verify(spyProjectVisitor).visit(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_directory_with_depth_PROJECT_does_not_call_visit_directory() {
    Component component = component(DIRECTORY, 1);
    spyProjectVisitor.visit(component);

    inOrder.verify(spyProjectVisitor).visit(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_module_with_depth_PROJECT_does_not_call_visit_module() {
    Component component = component(MODULE, 1);
    spyProjectVisitor.visit(component);

    inOrder.verify(spyProjectVisitor).visit(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_project_with_depth_PROJECT_calls_visit_project() {
    Component component = component(PROJECT, 1);
    spyProjectVisitor.visit(component);

    inOrder.verify(spyProjectVisitor).visit(component);
    inOrder.verify(spyProjectVisitor).visitProject(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_FILE() {
    spyFileVisitor.visit(COMPONENT_TREE);

    inOrder.verify(spyFileVisitor).visit(COMPONENT_TREE);
    inOrder.verify(spyFileVisitor).visit(MODULE_2);
    inOrder.verify(spyFileVisitor).visit(DIRECTORY_3);
    inOrder.verify(spyFileVisitor).visit(FILE_4);
    inOrder.verify(spyFileVisitor).visitFile(FILE_4);
    inOrder.verify(spyFileVisitor).visit(FILE_5);
    inOrder.verify(spyFileVisitor).visitFile(FILE_5);
    inOrder.verify(spyFileVisitor).visitDirectory(DIRECTORY_3);
    inOrder.verify(spyFileVisitor).visitModule(MODULE_2);
    inOrder.verify(spyFileVisitor).visitProject(COMPONENT_TREE);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_DIRECTORY() {
    spyDirectoryVisitor.visit(COMPONENT_TREE);

    inOrder.verify(spyDirectoryVisitor).visit(COMPONENT_TREE);
    inOrder.verify(spyDirectoryVisitor).visit(MODULE_2);
    inOrder.verify(spyDirectoryVisitor).visit(DIRECTORY_3);
    inOrder.verify(spyDirectoryVisitor).visitDirectory(DIRECTORY_3);
    inOrder.verify(spyDirectoryVisitor).visitModule(MODULE_2);
    inOrder.verify(spyDirectoryVisitor).visitProject(COMPONENT_TREE);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_MODULE() {
    spyModuleVisitor.visit(COMPONENT_TREE);

    inOrder.verify(spyModuleVisitor).visit(COMPONENT_TREE);
    inOrder.verify(spyModuleVisitor).visit(MODULE_2);
    inOrder.verify(spyModuleVisitor).visitModule(MODULE_2);
    inOrder.verify(spyModuleVisitor).visitProject(COMPONENT_TREE);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_PROJECT() {
    spyProjectVisitor.visit(COMPONENT_TREE);

    inOrder.verify(spyProjectVisitor).visit(COMPONENT_TREE);
    inOrder.verify(spyProjectVisitor).visitProject(COMPONENT_TREE);
    inOrder.verifyNoMoreInteractions();
  }

  private static Component component(final Component.Type type, final int ref, final Component... children) {
    return new Component() {

      @Override
      public Type getType() {
        return type;
      }

      @Override
      public int getRef() {
        return ref;
      }

      @Override
      public List<Component> getChildren() {
        return children == null ? Collections.<Component>emptyList() : ImmutableList.copyOf(Arrays.asList(children));
      }

      @Override
      public ComputationContext getContext() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_ERROR);
      }

      @Override
      public EventRepository getEventRepository() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_ERROR);
      }

      @Override
      public MeasureRepository getMeasureRepository() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_ERROR);
      }
    };
  }
}
