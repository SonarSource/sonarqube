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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Test;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.MODULE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class ReportVisitorsCrawlerWithPathAwareVisitorTest {

  private static final int ROOT_REF = 1;
  private static final ReportComponent SOME_TREE_ROOT = ReportComponent.builder(PROJECT, ROOT_REF)
    .addChildren(
      ReportComponent.builder(MODULE, 11)
        .addChildren(
          ReportComponent.builder(DIRECTORY, 111)
            .addChildren(
              ReportComponent.builder(FILE, 1111).build(),
              ReportComponent.builder(FILE, 1112).build()
            )
            .build(),
          ReportComponent.builder(DIRECTORY, 112)
            .addChildren(
              ReportComponent.builder(FILE, 1121).build()
            )
            .build())
        .build(),
      ReportComponent.builder(MODULE, 12)
        .addChildren(
          ReportComponent.builder(MODULE, 121)
            .addChildren(
              ReportComponent.builder(DIRECTORY, 1211)
                .addChildren(
                  ReportComponent.builder(FILE, 12111).build()
                )
                .build()
            ).build()
        ).build()
    ).build();

  @Test
  public void verify_preOrder_visit_call_when_visit_tree_with_depth_FILE() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.FILE, PRE_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      reportCallRecord("visitAny", 1, null, of(1)),
      reportCallRecord("visitProject", 1, null, of(1)),
      reportCallRecord("visitAny", 11, 1, of(11, 1)),
      reportCallRecord("visitModule", 11, 1, of(11, 1)),
      reportCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      reportCallRecord("visitDirectory", 111, 11, of(111, 11, 1)),
      reportCallRecord("visitAny", 1111, 111, of(1111, 111, 11, 1)),
      reportCallRecord("visitFile", 1111, 111, of(1111, 111, 11, 1)),
      reportCallRecord("visitAny", 1112, 111, of(1112, 111, 11, 1)),
      reportCallRecord("visitFile", 1112, 111, of(1112, 111, 11, 1)),
      reportCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      reportCallRecord("visitDirectory", 112, 11, of(112, 11, 1)),
      reportCallRecord("visitAny", 1121, 112, of(1121, 112, 11, 1)),
      reportCallRecord("visitFile", 1121, 112, of(1121, 112, 11, 1)),
      reportCallRecord("visitAny", 12, 1, of(12, 1)),
      reportCallRecord("visitModule", 12, 1, of(12, 1)),
      reportCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      reportCallRecord("visitModule", 121, 12, of(121, 12, 1)),
      reportCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      reportCallRecord("visitDirectory", 1211, 121, of(1211, 121, 12, 1)),
      reportCallRecord("visitAny", 12111, 1211, of(12111, 1211, 121, 12, 1)),
      reportCallRecord("visitFile", 12111, 1211, of(12111, 1211, 121, 12, 1))
      ).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_preOrder_visit_call_when_visit_tree_with_depth_DIRECTORY() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.DIRECTORY, PRE_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      reportCallRecord("visitAny", 1, null, of(1)),
      reportCallRecord("visitProject", 1, null, of(1)),
      reportCallRecord("visitAny", 11, 1, of(11, 1)),
      reportCallRecord("visitModule", 11, 1, of(11, 1)),
      reportCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      reportCallRecord("visitDirectory", 111, 11, of(111, 11, 1)),
      reportCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      reportCallRecord("visitDirectory", 112, 11, of(112, 11, 1)),
      reportCallRecord("visitAny", 12, 1, of(12, 1)),
      reportCallRecord("visitModule", 12, 1, of(12, 1)),
      reportCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      reportCallRecord("visitModule", 121, 12, of(121, 12, 1)),
      reportCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      reportCallRecord("visitDirectory", 1211, 121, of(1211, 121, 12, 1))
      ).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_preOrder_visit_call_when_visit_tree_with_depth_MODULE() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.MODULE, PRE_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      reportCallRecord("visitAny", 1, null, of(1)),
      reportCallRecord("visitProject", 1, null, of(1)),
      reportCallRecord("visitAny", 11, 1, of(11, 1)),
      reportCallRecord("visitModule", 11, 1, of(11, 1)),
      reportCallRecord("visitAny", 12, 1, of(12, 1)),
      reportCallRecord("visitModule", 12, 1, of(12, 1)),
      reportCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      reportCallRecord("visitModule", 121, 12, of(121, 12, 1))
      ).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_preOrder_visit_call_when_visit_tree_with_depth_PROJECT() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.PROJECT, PRE_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      reportCallRecord("visitAny", 1, null, of(1)),
      reportCallRecord("visitProject", 1, null, of(1))
      ).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder_visit_call_when_visit_tree_with_depth_FILE() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.FILE, POST_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      reportCallRecord("visitAny", 1111, 111, of(1111, 111, 11, 1)),
      reportCallRecord("visitFile", 1111, 111, of(1111, 111, 11, 1)),
      reportCallRecord("visitAny", 1112, 111, of(1112, 111, 11, 1)),
      reportCallRecord("visitFile", 1112, 111, of(1112, 111, 11, 1)),
      reportCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      reportCallRecord("visitDirectory", 111, 11, of(111, 11, 1)),
      reportCallRecord("visitAny", 1121, 112, of(1121, 112, 11, 1)),
      reportCallRecord("visitFile", 1121, 112, of(1121, 112, 11, 1)),
      reportCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      reportCallRecord("visitDirectory", 112, 11, of(112, 11, 1)),
      reportCallRecord("visitAny", 11, 1, of(11, 1)),
      reportCallRecord("visitModule", 11, 1, of(11, 1)),
      reportCallRecord("visitAny", 12111, 1211, of(12111, 1211, 121, 12, 1)),
      reportCallRecord("visitFile", 12111, 1211, of(12111, 1211, 121, 12, 1)),
      reportCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      reportCallRecord("visitDirectory", 1211, 121, of(1211, 121, 12, 1)),
      reportCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      reportCallRecord("visitModule", 121, 12, of(121, 12, 1)),
      reportCallRecord("visitAny", 12, 1, of(12, 1)),
      reportCallRecord("visitModule", 12, 1, of(12, 1)),
      reportCallRecord("visitAny", 1, null, of(1)),
      reportCallRecord("visitProject", 1, null, of(1))
      ).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder_visit_call_when_visit_tree_with_depth_DIRECTORY() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.DIRECTORY, POST_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      reportCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      reportCallRecord("visitDirectory", 111, 11, of(111, 11, 1)),
      reportCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      reportCallRecord("visitDirectory", 112, 11, of(112, 11, 1)),
      reportCallRecord("visitAny", 11, 1, of(11, 1)),
      reportCallRecord("visitModule", 11, 1, of(11, 1)),
      reportCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      reportCallRecord("visitDirectory", 1211, 121, of(1211, 121, 12, 1)),
      reportCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      reportCallRecord("visitModule", 121, 12, of(121, 12, 1)),
      reportCallRecord("visitAny", 12, 1, of(12, 1)),
      reportCallRecord("visitModule", 12, 1, of(12, 1)),
      reportCallRecord("visitAny", 1, null, of(1)),
      reportCallRecord("visitProject", 1, null, of(1))
      ).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder_visit_call_when_visit_tree_with_depth_MODULE() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.MODULE, POST_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      reportCallRecord("visitAny", 11, 1, of(11, 1)),
      reportCallRecord("visitModule", 11, 1, of(11, 1)),
      reportCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      reportCallRecord("visitModule", 121, 12, of(121, 12, 1)),
      reportCallRecord("visitAny", 12, 1, of(12, 1)),
      reportCallRecord("visitModule", 12, 1, of(12, 1)),
      reportCallRecord("visitAny", 1, null, of(1)),
      reportCallRecord("visitProject", 1, null, of(1))
      ).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder_visit_call_when_visit_tree_with_depth_PROJECT() {
    CallRecorderPathAwareVisitor visitor = new CallRecorderPathAwareVisitor(CrawlerDepthLimit.PROJECT, POST_ORDER);
    VisitorsCrawler underTest = newVisitorsCrawler(visitor);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<PathAwareCallRecord> expected = of(
      reportCallRecord("visitAny", 1, null, of(1)),
      reportCallRecord("visitProject", 1, null, of(1))
      ).iterator();
    verifyCallRecords(expected, visitor.callsRecords.iterator());
  }

  private static void verifyCallRecords(Iterator<PathAwareCallRecord> expected, Iterator<PathAwareCallRecord> actual) {
    while (expected.hasNext()) {
      assertThat(actual.next()).isEqualTo(expected.next());
    }
  }

  private static PathAwareCallRecord reportCallRecord(String method, int currentRef, @Nullable Integer parentRef, List<Integer> path) {
    return PathAwareCallRecord.reportCallRecord(method, currentRef, currentRef, parentRef, ROOT_REF, path);
  }

  private static VisitorsCrawler newVisitorsCrawler(ComponentVisitor componentVisitor) {
    return new VisitorsCrawler(Arrays.asList(componentVisitor));
  }

}
