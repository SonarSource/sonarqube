/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Arrays;
import org.junit.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;
public class ReportVisitorsCrawlerWithPreOrderTypeAwareVisitorTest {

  private static final Component FILE_5 = component(FILE, 5);
  private static final Component FILE_6 = component(FILE, 6);
  private static final Component DIRECTORY_4 = component(DIRECTORY, 4, FILE_5, FILE_6);
  private static final Component COMPONENT_TREE = component(PROJECT, 1, DIRECTORY_4);

  private final TypeAwareVisitor spyProjectVisitor = spy(new TypeAwareVisitorAdapter(CrawlerDepthLimit.PROJECT, PRE_ORDER) {
  });
  private final TypeAwareVisitor spyDirectoryVisitor = spy(new TypeAwareVisitorAdapter(CrawlerDepthLimit.DIRECTORY, PRE_ORDER) {
  });
  private final TypeAwareVisitor spyFileVisitor = spy(new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, PRE_ORDER) {
  });
  private final InOrder inOrder = inOrder(spyProjectVisitor, spyDirectoryVisitor, spyFileVisitor);

  @Test
  public void visit_null_Component_throws_NPE() {
    VisitorsCrawler underTest = newVisitorsCrawler(spyFileVisitor);

    assertThatThrownBy(() -> underTest.visit(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void visit_file_with_depth_FILE_calls_visit_file() {
    Component component = component(FILE, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyFileVisitor);
    underTest.visit(component);

    inOrder.verify(spyFileVisitor).visitAny(component);
    inOrder.verify(spyFileVisitor).visitFile(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_directory_with_depth_FILE_calls_visit_directory() {
    Component component = component(DIRECTORY, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyFileVisitor);
    underTest.visit(component);

    inOrder.verify(spyFileVisitor).visitAny(component);
    inOrder.verify(spyFileVisitor).visitDirectory(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_project_with_depth_FILE_calls_visit_project() {
    Component component = component(PROJECT, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyFileVisitor);
    underTest.visit(component);

    inOrder.verify(spyFileVisitor).visitProject(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_file_with_depth_DIRECTORY_does_not_call_visit_file_nor_visitAny() {
    Component component = component(FILE, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyDirectoryVisitor);
    underTest.visit(component);

    inOrder.verify(spyDirectoryVisitor, never()).visitFile(component);
    inOrder.verify(spyDirectoryVisitor, never()).visitAny(component);
  }

  @Test
  public void visit_directory_with_depth_DIRECTORY_calls_visit_directory() {
    Component component = component(DIRECTORY, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyDirectoryVisitor);
    underTest.visit(component);

    inOrder.verify(spyDirectoryVisitor).visitAny(component);
    inOrder.verify(spyDirectoryVisitor).visitDirectory(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_project_with_depth_DIRECTORY_calls_visit_project() {
    Component component = component(PROJECT, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyDirectoryVisitor);
    underTest.visit(component);

    inOrder.verify(spyDirectoryVisitor).visitAny(component);
    inOrder.verify(spyDirectoryVisitor).visitProject(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_file_with_depth_PROJECT_does_not_call_visit_file_nor_visitAny() {
    Component component = component(FILE, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyProjectVisitor);
    underTest.visit(component);

    inOrder.verify(spyProjectVisitor, never()).visitFile(component);
    inOrder.verify(spyProjectVisitor, never()).visitAny(component);
  }

  @Test
  public void visit_directory_with_depth_PROJECT_does_not_call_visit_directory_nor_visitAny() {
    Component component = component(DIRECTORY, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyProjectVisitor);
    underTest.visit(component);

    inOrder.verify(spyProjectVisitor, never()).visitFile(component);
    inOrder.verify(spyProjectVisitor, never()).visitAny(component);
  }

  @Test
  public void visit_project_with_depth_PROJECT_calls_visit_project_nor_visitAny() {
    Component component = component(PROJECT, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyProjectVisitor);
    underTest.visit(component);

    inOrder.verify(spyProjectVisitor).visitAny(component);
    inOrder.verify(spyProjectVisitor).visitProject(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_FILE() {
    VisitorsCrawler underTest = newVisitorsCrawler(spyFileVisitor);
    underTest.visit(COMPONENT_TREE);

    inOrder.verify(spyFileVisitor).visitAny(COMPONENT_TREE);
    inOrder.verify(spyFileVisitor).visitProject(COMPONENT_TREE);
    inOrder.verify(spyFileVisitor).visitAny(DIRECTORY_4);
    inOrder.verify(spyFileVisitor).visitDirectory(DIRECTORY_4);
    inOrder.verify(spyFileVisitor).visitAny(FILE_5);
    inOrder.verify(spyFileVisitor).visitFile(FILE_5);
    inOrder.verify(spyFileVisitor).visitAny(FILE_6);
    inOrder.verify(spyFileVisitor).visitFile(FILE_6);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_DIRECTORY() {
    VisitorsCrawler underTest = newVisitorsCrawler(spyDirectoryVisitor);
    underTest.visit(COMPONENT_TREE);

    inOrder.verify(spyDirectoryVisitor).visitProject(COMPONENT_TREE);
    inOrder.verify(spyDirectoryVisitor).visitDirectory(DIRECTORY_4);
    inOrder.verify(spyProjectVisitor, never()).visitFile(FILE_5);
    inOrder.verify(spyProjectVisitor, never()).visitFile(FILE_6);
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_PROJECT() {
    VisitorsCrawler underTest = newVisitorsCrawler(spyProjectVisitor);
    underTest.visit(COMPONENT_TREE);

    inOrder.verify(spyProjectVisitor).visitAny(COMPONENT_TREE);
    inOrder.verify(spyProjectVisitor).visitProject(COMPONENT_TREE);
  }

  private static Component component(final Component.Type type, final int ref, final Component... children) {
    return ReportComponent.builder(type, ref).addChildren(children).build();
  }

  private static VisitorsCrawler newVisitorsCrawler(ComponentVisitor componentVisitor) {
    return new VisitorsCrawler(Arrays.asList(componentVisitor));
  }

}
