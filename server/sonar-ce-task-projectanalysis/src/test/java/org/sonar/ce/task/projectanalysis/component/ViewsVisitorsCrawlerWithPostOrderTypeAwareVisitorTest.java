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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Arrays;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class ViewsVisitorsCrawlerWithPostOrderTypeAwareVisitorTest {

  private static final Component PROJECT_VIEW_5 = component(PROJECT_VIEW, 5);
  private static final Component PROJECT_VIEW_6 = component(PROJECT_VIEW, 6);
  private static final Component SUBVIEW_4 = component(SUBVIEW, 4, PROJECT_VIEW_5, PROJECT_VIEW_6);
  private static final Component SUBVIEW_3 = component(SUBVIEW, 3, SUBVIEW_4);
  private static final Component SUBVIEW_2 = component(SUBVIEW, 2, SUBVIEW_3);
  private static final Component COMPONENT_TREE = component(VIEW, 1, SUBVIEW_2);

  private final TypeAwareVisitor spyViewVisitor = spy(new TypeAwareVisitorAdapter(CrawlerDepthLimit.VIEW, POST_ORDER) {
  });
  private final TypeAwareVisitor spySubViewVisitor = spy(new TypeAwareVisitorAdapter(CrawlerDepthLimit.SUBVIEW, POST_ORDER) {
  });
  private final TypeAwareVisitor spyProjectViewVisitor = spy(new TypeAwareVisitorAdapter(CrawlerDepthLimit.PROJECT_VIEW, POST_ORDER) {
  });
  private final InOrder inOrder = inOrder(spyViewVisitor, spySubViewVisitor, spyProjectViewVisitor);

  @Test(expected = NullPointerException.class)
  public void visit_null_Component_throws_NPE() {
    VisitorsCrawler underTest = newVisitorsCrawler(spyProjectViewVisitor);
    underTest.visit(null);
  }

  @Test
  public void visit_projectView_with_depth_PROJECT_VIEW_calls_visit_projectView() {
    Component component = component(PROJECT_VIEW, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyProjectViewVisitor);
    underTest.visit(component);

    inOrder.verify(spyProjectViewVisitor).visitAny(component);
    inOrder.verify(spyProjectViewVisitor).visitProjectView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_subview_with_depth_PROJECT_VIEW_calls_visit_subview() {
    Component component = component(SUBVIEW, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyProjectViewVisitor);
    underTest.visit(component);

    inOrder.verify(spyProjectViewVisitor).visitAny(component);
    inOrder.verify(spyProjectViewVisitor).visitSubView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_view_with_depth_PROJECT_VIEW_calls_visit_view() {
    Component component = component(VIEW, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyProjectViewVisitor);
    underTest.visit(component);

    inOrder.verify(spyProjectViewVisitor).visitAny(component);
    inOrder.verify(spyProjectViewVisitor).visitView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_projectView_with_depth_SUBVIEW_does_not_call_visit_projectView_nor_visitAny() {
    Component component = component(PROJECT_VIEW, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spySubViewVisitor);
    underTest.visit(component);

    inOrder.verify(spySubViewVisitor, never()).visitProjectView(component);
    inOrder.verify(spySubViewVisitor, never()).visitAny(component);
  }

  @Test
  public void visit_subview_with_depth_SUBVIEW_calls_visit_subview() {
    Component component = component(SUBVIEW, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spySubViewVisitor);
    underTest.visit(component);

    inOrder.verify(spySubViewVisitor).visitAny(component);
    inOrder.verify(spySubViewVisitor).visitSubView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_view_with_depth_SUBVIEW_calls_visit_view() {
    Component component = component(VIEW, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spySubViewVisitor);
    underTest.visit(component);

    inOrder.verify(spySubViewVisitor).visitAny(component);
    inOrder.verify(spySubViewVisitor).visitView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_projectView_with_depth_VIEW_does_not_call_visit_projectView_nor_visitAny() {
    Component component = component(PROJECT_VIEW, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyViewVisitor);
    underTest.visit(component);

    inOrder.verify(spyViewVisitor, never()).visitProjectView(component);
    inOrder.verify(spyViewVisitor, never()).visitAny(component);
  }

  @Test
  public void visit_subview_with_depth_VIEW_does_not_call_visit_subview_nor_visitAny() {
    Component component = component(SUBVIEW, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyViewVisitor);
    underTest.visit(component);

    inOrder.verify(spyViewVisitor, never()).visitSubView(component);
    inOrder.verify(spyViewVisitor, never()).visitProjectView(component);
    inOrder.verify(spyViewVisitor, never()).visitAny(component);
  }

  @Test
  public void visit_view_with_depth_VIEW_calls_visit_view() {
    Component component = component(VIEW, 1);
    VisitorsCrawler underTest = newVisitorsCrawler(spyViewVisitor);
    underTest.visit(component);

    inOrder.verify(spyViewVisitor).visitAny(component);
    inOrder.verify(spyViewVisitor).visitView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_PROJECT_VIEW() {
    VisitorsCrawler underTest = newVisitorsCrawler(spyProjectViewVisitor);
    underTest.visit(COMPONENT_TREE);

    inOrder.verify(spyProjectViewVisitor).visitAny(PROJECT_VIEW_5);
    inOrder.verify(spyProjectViewVisitor).visitProjectView(PROJECT_VIEW_5);
    inOrder.verify(spyProjectViewVisitor).visitAny(PROJECT_VIEW_6);
    inOrder.verify(spyProjectViewVisitor).visitProjectView(PROJECT_VIEW_6);
    inOrder.verify(spyProjectViewVisitor).visitAny(SUBVIEW_4);
    inOrder.verify(spyProjectViewVisitor).visitSubView(SUBVIEW_4);
    inOrder.verify(spyProjectViewVisitor).visitAny(SUBVIEW_3);
    inOrder.verify(spyProjectViewVisitor).visitSubView(SUBVIEW_3);
    inOrder.verify(spyProjectViewVisitor).visitAny(SUBVIEW_2);
    inOrder.verify(spyProjectViewVisitor).visitSubView(SUBVIEW_2);
    inOrder.verify(spyProjectViewVisitor).visitAny(COMPONENT_TREE);
    inOrder.verify(spyProjectViewVisitor).visitView(COMPONENT_TREE);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_SUBVIEW() {
    VisitorsCrawler underTest = newVisitorsCrawler(spySubViewVisitor);
    underTest.visit(COMPONENT_TREE);

    inOrder.verify(spySubViewVisitor).visitAny(SUBVIEW_4);
    inOrder.verify(spySubViewVisitor).visitSubView(SUBVIEW_4);
    inOrder.verify(spySubViewVisitor).visitAny(SUBVIEW_3);
    inOrder.verify(spySubViewVisitor).visitSubView(SUBVIEW_3);
    inOrder.verify(spySubViewVisitor).visitAny(SUBVIEW_2);
    inOrder.verify(spySubViewVisitor).visitSubView(SUBVIEW_2);
    inOrder.verify(spySubViewVisitor).visitAny(COMPONENT_TREE);
    inOrder.verify(spySubViewVisitor).visitView(COMPONENT_TREE);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_VIEW() {
    VisitorsCrawler underTest = newVisitorsCrawler(spyViewVisitor);
    underTest.visit(COMPONENT_TREE);

    inOrder.verify(spyViewVisitor).visitAny(COMPONENT_TREE);
    inOrder.verify(spyViewVisitor).visitView(COMPONENT_TREE);
    inOrder.verifyNoMoreInteractions();
  }

  private static Component component(final Component.Type type, final int ref, final Component... children) {
    return ViewsComponent.builder(type, ref).addChildren(children).build();
  }

  private static VisitorsCrawler newVisitorsCrawler(ComponentVisitor componentVisitor) {
    return new VisitorsCrawler(Arrays.asList(componentVisitor));
  }

}
