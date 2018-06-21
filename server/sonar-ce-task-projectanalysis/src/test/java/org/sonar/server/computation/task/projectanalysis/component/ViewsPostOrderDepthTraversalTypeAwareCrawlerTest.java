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
package org.sonar.server.computation.task.projectanalysis.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class ViewsPostOrderDepthTraversalTypeAwareCrawlerTest {

  private static final Component PROJECT_VIEW_5 = component(PROJECT_VIEW, 5);
  private static final Component PROJECT_VIEW_6 = component(PROJECT_VIEW, 6);
  private static final Component SUBVIEW_4 = component(SUBVIEW, 4, PROJECT_VIEW_5, PROJECT_VIEW_6);
  private static final Component SUBVIEW_3 = component(SUBVIEW, 3, SUBVIEW_4);
  private static final Component SUBVIEW_2 = component(SUBVIEW, 2, SUBVIEW_3);
  private static final Component COMPONENT_TREE = component(VIEW, 1, SUBVIEW_2);

  private final CallRecorderTypeAwareVisitor viewVisitor = new CallRecorderTypeAwareVisitor(CrawlerDepthLimit.VIEW, POST_ORDER);
  private final CallRecorderTypeAwareVisitor subViewVisitor = new CallRecorderTypeAwareVisitor(CrawlerDepthLimit.SUBVIEW, POST_ORDER);
  private final CallRecorderTypeAwareVisitor projectViewVisitor = new CallRecorderTypeAwareVisitor(CrawlerDepthLimit.PROJECT_VIEW, POST_ORDER);

  private final DepthTraversalTypeAwareCrawler viewCrawler = new DepthTraversalTypeAwareCrawler(viewVisitor);
  private final DepthTraversalTypeAwareCrawler subViewCrawler = new DepthTraversalTypeAwareCrawler(subViewVisitor);
  private final DepthTraversalTypeAwareCrawler projectViewCrawler = new DepthTraversalTypeAwareCrawler(projectViewVisitor);

  @Test(expected = NullPointerException.class)
  public void visit_null_Component_throws_NPE() {
    projectViewCrawler.visit(null);
  }

  @Test
  public void visit_viewView_with_depth_PROJECT_VIEW_calls_visit_viewView() {
    Component component = component(PROJECT_VIEW, 1);
    projectViewCrawler.visit(component);

    assertThat(projectViewVisitor.callsRecords).containsExactly(
        viewsCallRecord("visitAny", component),
        viewsCallRecord("visitProjectView", component));
  }

  @Test
  public void visit_subView_with_depth_PROJECT_VIEW_calls_visit_subView() {
    Component component = component(SUBVIEW, 1);
    projectViewCrawler.visit(component);

    assertThat(projectViewVisitor.callsRecords).containsExactly(
        viewsCallRecord("visitAny", component),
        viewsCallRecord("visitSubView", component));
  }

  @Test
  public void visit_view_with_depth_PROJECT_VIEW_calls_visit_view() {
    Component component = component(VIEW, 1);
    projectViewCrawler.visit(component);

    assertThat(projectViewVisitor.callsRecords).containsExactly(
        viewsCallRecord("visitAny", component),
        viewsCallRecord("visitView", component));
  }

  @Test
  public void visit_viewView_with_depth_SUBVIEW_does_not_call_visit_viewView_nor_visitAny() {
    Component component = component(PROJECT_VIEW, 1);
    subViewCrawler.visit(component);

    assertThat(subViewVisitor.callsRecords).isEmpty();
  }

  @Test
  public void visit_subView_with_depth_SUBVIEW_calls_visit_subView() {
    Component component = component(SUBVIEW, 1);
    subViewCrawler.visit(component);

    assertThat(subViewVisitor.callsRecords).containsExactly(
        viewsCallRecord("visitAny", component),
        viewsCallRecord("visitSubView", component));
  }

  @Test
  public void visit_view_with_depth_SUBVIEW_calls_visit_view() {
    Component component = component(VIEW, 1);
    subViewCrawler.visit(component);

    assertThat(subViewVisitor.callsRecords).containsExactly(
        viewsCallRecord("visitAny", component),
        viewsCallRecord("visitView", component));
  }

  @Test
  public void visit_viewView_with_depth_VIEW_does_not_call_visit_viewView_nor_visitAny() {
    Component component = component(PROJECT_VIEW, 1);
    viewCrawler.visit(component);

    assertThat(viewVisitor.callsRecords).isEmpty();
  }

  @Test
  public void visit_subView_with_depth_VIEW_does_not_call_visit_subView_nor_visitAny() {
    Component component = component(SUBVIEW, 1);
    viewCrawler.visit(component);

    assertThat(viewVisitor.callsRecords).isEmpty();
  }

  @Test
  public void visit_view_with_depth_VIEW_calls_visit_view() {
    Component component = component(VIEW, 1);
    viewCrawler.visit(component);

    assertThat(viewVisitor.callsRecords).containsExactly(
        viewsCallRecord("visitAny", component),
        viewsCallRecord("visitView", component));
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_PROJECT_VIEW() {
    projectViewCrawler.visit(COMPONENT_TREE);

    assertThat(projectViewVisitor.callsRecords).containsExactly(
        viewsCallRecord("visitAny", PROJECT_VIEW_5),
        viewsCallRecord("visitProjectView", PROJECT_VIEW_5),
        viewsCallRecord("visitAny", PROJECT_VIEW_6),
        viewsCallRecord("visitProjectView", PROJECT_VIEW_6),
        viewsCallRecord("visitAny", SUBVIEW_4),
        viewsCallRecord("visitSubView", SUBVIEW_4),
        viewsCallRecord("visitAny", SUBVIEW_3),
        viewsCallRecord("visitSubView", SUBVIEW_3),
        viewsCallRecord("visitAny", SUBVIEW_2),
        viewsCallRecord("visitSubView", SUBVIEW_2),
        viewsCallRecord("visitAny", COMPONENT_TREE),
        viewsCallRecord("visitView", COMPONENT_TREE));
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_SUBVIEW() {
    subViewCrawler.visit(COMPONENT_TREE);

    assertThat(subViewVisitor.callsRecords).containsExactly(
        viewsCallRecord("visitAny", SUBVIEW_4),
        viewsCallRecord("visitSubView", SUBVIEW_4),
        viewsCallRecord("visitAny", SUBVIEW_3),
        viewsCallRecord("visitSubView", SUBVIEW_3),
        viewsCallRecord("visitAny", SUBVIEW_2),
        viewsCallRecord("visitSubView", SUBVIEW_2),
        viewsCallRecord("visitAny", COMPONENT_TREE),
        viewsCallRecord("visitView", COMPONENT_TREE));
  }

  @Test
  public void verify_visit_call_when_visit_tree_with_depth_VIEW() {
    viewCrawler.visit(COMPONENT_TREE);

    assertThat(viewVisitor.callsRecords).containsExactly(
        viewsCallRecord("visitAny", COMPONENT_TREE),
        viewsCallRecord("visitView", COMPONENT_TREE));
  }

  private static Component component(Component.Type type, int ref, Component... children) {
    return ViewsComponent.builder(type, ref).addChildren(children).build();
  }

  private static CallRecord viewsCallRecord(String methodName, Component component) {
    return CallRecord.viewsCallRecord(methodName, component.getKey());
  }

}
