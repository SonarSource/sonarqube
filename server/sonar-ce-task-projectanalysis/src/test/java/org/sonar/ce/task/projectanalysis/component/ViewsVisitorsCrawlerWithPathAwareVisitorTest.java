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
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Test;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class ViewsVisitorsCrawlerWithPathAwareVisitorTest {

  private static final int ROOT_REF = 1;
  private static final ViewsComponent SOME_TREE_ROOT = ViewsComponent.builder(VIEW, ROOT_REF)
    .addChildren(
      ViewsComponent.builder(SUBVIEW, 11)
        .addChildren(
          ViewsComponent.builder(SUBVIEW, 111)
            .addChildren(
              ViewsComponent.builder(PROJECT_VIEW, 1111).build(),
              ViewsComponent.builder(PROJECT_VIEW, 1112).build())
            .build(),
          ViewsComponent.builder(SUBVIEW, 112)
            .addChildren(
              ViewsComponent.builder(PROJECT_VIEW, 1121).build())
            .build())
        .build(),
      ViewsComponent.builder(SUBVIEW, 12)
        .addChildren(
          ViewsComponent.builder(SUBVIEW, 121)
            .addChildren(
              ViewsComponent.builder(SUBVIEW, 1211)
                .addChildren(
                  ViewsComponent.builder(PROJECT_VIEW, 12111).build())
                .build())
            .build())
        .build())
    .build();

  @Test
  public void verify_preOrder_visit_call_when_visit_tree_with_depth_PROJECT_VIEW() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.PROJECT_VIEW, PRE_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      viewsCallRecord("visitAny", 1, null, of(1)),
      viewsCallRecord("visitView", 1, null, of(1)),
      viewsCallRecord("visitAny", 11, 1, of(11, 1)),
      viewsCallRecord("visitSubView", 11, 1, of(11, 1)),
      viewsCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      viewsCallRecord("visitSubView", 111, 11, of(111, 11, 1)),
      viewsCallRecord("visitAny", 1111, 111, of(1111, 111, 11, 1)),
      viewsCallRecord("visitProjectView", 1111, 111, of(1111, 111, 11, 1)),
      viewsCallRecord("visitAny", 1112, 111, of(1112, 111, 11, 1)),
      viewsCallRecord("visitProjectView", 1112, 111, of(1112, 111, 11, 1)),
      viewsCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      viewsCallRecord("visitSubView", 112, 11, of(112, 11, 1)),
      viewsCallRecord("visitAny", 1121, 112, of(1121, 112, 11, 1)),
      viewsCallRecord("visitProjectView", 1121, 112, of(1121, 112, 11, 1)),
      viewsCallRecord("visitAny", 12, 1, of(12, 1)),
      viewsCallRecord("visitSubView", 12, 1, of(12, 1)),
      viewsCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      viewsCallRecord("visitSubView", 121, 12, of(121, 12, 1)),
      viewsCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      viewsCallRecord("visitSubView", 1211, 121, of(1211, 121, 12, 1)),
      viewsCallRecord("visitAny", 12111, 1211, of(12111, 1211, 121, 12, 1)),
      viewsCallRecord("visitProjectView", 12111, 1211, of(12111, 1211, 121, 12, 1))).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_preOrder_visit_call_when_visit_tree_with_depth_SUBVIEW() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.SUBVIEW, PRE_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      viewsCallRecord("visitAny", 1, null, of(1)),
      viewsCallRecord("visitView", 1, null, of(1)),
      viewsCallRecord("visitAny", 11, 1, of(11, 1)),
      viewsCallRecord("visitSubView", 11, 1, of(11, 1)),
      viewsCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      viewsCallRecord("visitSubView", 111, 11, of(111, 11, 1)),
      viewsCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      viewsCallRecord("visitSubView", 112, 11, of(112, 11, 1)),
      viewsCallRecord("visitAny", 12, 1, of(12, 1)),
      viewsCallRecord("visitSubView", 12, 1, of(12, 1)),
      viewsCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      viewsCallRecord("visitSubView", 121, 12, of(121, 12, 1)),
      viewsCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      viewsCallRecord("visitSubView", 1211, 121, of(1211, 121, 12, 1))).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_preOrder_visit_call_when_visit_tree_with_depth_VIEW() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.VIEW, PRE_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      viewsCallRecord("visitAny", 1, null, of(1)),
      viewsCallRecord("visitView", 1, null, of(1))).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder_visit_call_when_visit_tree_with_depth_PROJECT_VIEW() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.PROJECT_VIEW, POST_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      viewsCallRecord("visitAny", 1111, 111, of(1111, 111, 11, 1)),
      viewsCallRecord("visitProjectView", 1111, 111, of(1111, 111, 11, 1)),
      viewsCallRecord("visitAny", 1112, 111, of(1112, 111, 11, 1)),
      viewsCallRecord("visitProjectView", 1112, 111, of(1112, 111, 11, 1)),
      viewsCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      viewsCallRecord("visitSubView", 111, 11, of(111, 11, 1)),
      viewsCallRecord("visitAny", 1121, 112, of(1121, 112, 11, 1)),
      viewsCallRecord("visitProjectView", 1121, 112, of(1121, 112, 11, 1)),
      viewsCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      viewsCallRecord("visitSubView", 112, 11, of(112, 11, 1)),
      viewsCallRecord("visitAny", 11, 1, of(11, 1)),
      viewsCallRecord("visitSubView", 11, 1, of(11, 1)),
      viewsCallRecord("visitAny", 12111, 1211, of(12111, 1211, 121, 12, 1)),
      viewsCallRecord("visitProjectView", 12111, 1211, of(12111, 1211, 121, 12, 1)),
      viewsCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      viewsCallRecord("visitSubView", 1211, 121, of(1211, 121, 12, 1)),
      viewsCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      viewsCallRecord("visitSubView", 121, 12, of(121, 12, 1)),
      viewsCallRecord("visitAny", 12, 1, of(12, 1)),
      viewsCallRecord("visitSubView", 12, 1, of(12, 1)),
      viewsCallRecord("visitAny", 1, null, of(1)),
      viewsCallRecord("visitView", 1, null, of(1))).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder_visit_call_when_visit_tree_with_depth_SUBVIEW() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.SUBVIEW, POST_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      viewsCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      viewsCallRecord("visitSubView", 111, 11, of(111, 11, 1)),
      viewsCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      viewsCallRecord("visitSubView", 112, 11, of(112, 11, 1)),
      viewsCallRecord("visitAny", 11, 1, of(11, 1)),
      viewsCallRecord("visitSubView", 11, 1, of(11, 1)),
      viewsCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      viewsCallRecord("visitSubView", 1211, 121, of(1211, 121, 12, 1)),
      viewsCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      viewsCallRecord("visitSubView", 121, 12, of(121, 12, 1)),
      viewsCallRecord("visitAny", 12, 1, of(12, 1)),
      viewsCallRecord("visitSubView", 12, 1, of(12, 1)),
      viewsCallRecord("visitAny", 1, null, of(1)),
      viewsCallRecord("visitView", 1, null, of(1))).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder_visit_call_when_visit_tree_with_depth_VIEW() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.VIEW, POST_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      viewsCallRecord("visitAny", 1, null, of(1)),
      viewsCallRecord("visitView", 1, null, of(1))).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  private static void verifyCallRecords(Iterator<PathAwareCallRecord> expected, Iterator<PathAwareCallRecord> actual) {
    while (expected.hasNext()) {
      assertThat(actual.next()).isEqualTo(expected.next());
    }
  }

  private static PathAwareCallRecord viewsCallRecord(String method, int currentRef, @Nullable Integer parentRef, List<Integer> path) {
    return PathAwareCallRecord.viewsCallRecord(method, String.valueOf(currentRef), currentRef, parentRef, ROOT_REF, path);
  }

  private static VisitorsCrawler newVisitorsCrawler(ComponentVisitor componentVisitor) {
    return new VisitorsCrawler(Arrays.asList(componentVisitor));
  }

}
