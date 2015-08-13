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

import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

public class ViewsPostOrderDepthTraversalTypeAwareCrawlerTest {

  private static final Component PROJECT_VIEW_5 = component(PROJECT_VIEW, 5);
  private static final Component PROJECT_VIEW_6 = component(PROJECT_VIEW, 6);
  private static final Component SUBVIEW_4 = component(SUBVIEW, 4, PROJECT_VIEW_5, PROJECT_VIEW_6);
  private static final Component SUBVIEW_3 = component(SUBVIEW, 3, SUBVIEW_4);
  private static final Component SUBVIEW_2 = component(SUBVIEW, 2, SUBVIEW_3);
  private static final Component COMPONENT_TREE = component(VIEW, 1, SUBVIEW_2);

  private final DepthTraversalTypeAwareCrawler spyViewVisitor = spy(new DepthTraversalTypeAwareCrawler(VIEW, POST_ORDER) {
  });
  private final DepthTraversalTypeAwareCrawler spySubViewVisitor = spy(new DepthTraversalTypeAwareCrawler(SUBVIEW, POST_ORDER) {
  });
  private final DepthTraversalTypeAwareCrawler spyProjectViewVisitor = spy(new DepthTraversalTypeAwareCrawler(PROJECT_VIEW, POST_ORDER) {
  });
  private final InOrder inOrder = inOrder(spyViewVisitor, spySubViewVisitor, spyProjectViewVisitor);

  @Test(expected = NullPointerException.class)
  public void non_null_max_depth_fast_fail() {
    new DepthTraversalTypeAwareCrawler(null, POST_ORDER) {
    };
  }

  @Test(expected = NullPointerException.class)
  public void visit_null_Component_throws_NPE() {
    spyProjectViewVisitor.visit(null);
  }

  @Test
  public void visit_viewView_with_depth_PROJECT_VIEW_calls_visit_viewView() {
    Component component = component(PROJECT_VIEW, 1);
    spyProjectViewVisitor.visit(component);

    inOrder.verify(spyProjectViewVisitor).visit(component);
    inOrder.verify(spyProjectViewVisitor).visitAny(component);
    inOrder.verify(spyProjectViewVisitor).visitProjectView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_subView_with_depth_PROJECT_VIEW_calls_visit_subView() {
    Component component = component(SUBVIEW, 1);
    spyProjectViewVisitor.visit(component);

    inOrder.verify(spyProjectViewVisitor).visit(component);
    inOrder.verify(spyProjectViewVisitor).visitAny(component);
    inOrder.verify(spyProjectViewVisitor).visitSubView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_view_with_depth_PROJECT_VIEW_calls_visit_view() {
    Component component = component(VIEW, 1);
    spyProjectViewVisitor.visit(component);

    inOrder.verify(spyProjectViewVisitor).visit(component);
    inOrder.verify(spyProjectViewVisitor).visitAny(component);
    inOrder.verify(spyProjectViewVisitor).visitView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_viewView_with_depth_SUBVIEW_does_not_call_visit_viewView_nor_visitAny() {
    Component component = component(PROJECT_VIEW, 1);
    spySubViewVisitor.visit(component);

    inOrder.verify(spySubViewVisitor).visit(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_subView_with_depth_SUBVIEW_calls_visit_subView() {
    Component component = component(SUBVIEW, 1);
    spySubViewVisitor.visit(component);

    inOrder.verify(spySubViewVisitor).visit(component);
    inOrder.verify(spySubViewVisitor).visitAny(component);
    inOrder.verify(spySubViewVisitor).visitSubView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_view_with_depth_SUBVIEW_calls_visit_view() {
    Component component = component(VIEW, 1);
    spySubViewVisitor.visit(component);

    inOrder.verify(spySubViewVisitor).visit(component);
    inOrder.verify(spySubViewVisitor).visitAny(component);
    inOrder.verify(spySubViewVisitor).visitView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_viewView_with_depth_VIEW_does_not_call_visit_viewView_nor_visitAny() {
    Component component = component(PROJECT_VIEW, 1);
    spyViewVisitor.visit(component);

    inOrder.verify(spyViewVisitor).visit(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_subView_with_depth_VIEW_does_not_call_visit_subView_nor_visitAny() {
    Component component = component(SUBVIEW, 1);
    spyViewVisitor.visit(component);

    inOrder.verify(spyViewVisitor).visit(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void visit_view_with_depth_VIEW_calls_visit_view() {
    Component component = component(VIEW, 1);
    spyViewVisitor.visit(component);

    inOrder.verify(spyViewVisitor).visit(component);
    inOrder.verify(spyViewVisitor).visitAny(component);
    inOrder.verify(spyViewVisitor).visitView(component);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_PROJECT_VIEW() {
    spyProjectViewVisitor.visit(COMPONENT_TREE);

    inOrder.verify(spyProjectViewVisitor).visit(COMPONENT_TREE);
    inOrder.verify(spyProjectViewVisitor).visit(SUBVIEW_2);
    inOrder.verify(spyProjectViewVisitor).visit(SUBVIEW_3);
    inOrder.verify(spyProjectViewVisitor).visit(SUBVIEW_4);
    inOrder.verify(spyProjectViewVisitor).visit(PROJECT_VIEW_5);
    inOrder.verify(spyProjectViewVisitor).visitAny(PROJECT_VIEW_5);
    inOrder.verify(spyProjectViewVisitor).visitProjectView(PROJECT_VIEW_5);
    inOrder.verify(spyProjectViewVisitor).visit(PROJECT_VIEW_6);
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
    spySubViewVisitor.visit(COMPONENT_TREE);

    inOrder.verify(spySubViewVisitor).visit(COMPONENT_TREE);
    inOrder.verify(spySubViewVisitor).visit(SUBVIEW_2);
    inOrder.verify(spySubViewVisitor).visit(SUBVIEW_3);
    inOrder.verify(spySubViewVisitor).visit(SUBVIEW_4);
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
    spyViewVisitor.visit(COMPONENT_TREE);

    inOrder.verify(spyViewVisitor).visit(COMPONENT_TREE);
    inOrder.verify(spyViewVisitor).visitAny(COMPONENT_TREE);
    inOrder.verify(spyViewVisitor).visitView(COMPONENT_TREE);
    inOrder.verifyNoMoreInteractions();
  }

  private static Component component(Component.Type type, int ref, Component... children) {
    return ViewsComponent.builder(type, ref).addChildren(children).build();
  }

}
