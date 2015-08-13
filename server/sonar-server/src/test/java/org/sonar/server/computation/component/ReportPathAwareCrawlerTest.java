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

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Test;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

public class ReportPathAwareCrawlerTest {

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
    TestPathAwareCrawler underTest = new TestPathAwareCrawler(FILE, PRE_ORDER);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<CallRecord> expected = of(
      newCallRecord("visitAny", 1, null, of(1)),
      newCallRecord("visitProject", 1, null, of(1)),
      newCallRecord("visitAny", 11, 1, of(11, 1)),
      newCallRecord("visitModule", 11, 1, of(11, 1)),
      newCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      newCallRecord("visitDirectory", 111, 11, of(111, 11, 1)),
      newCallRecord("visitAny", 1111, 111, of(1111, 111, 11, 1)),
      newCallRecord("visitFile", 1111, 111, of(1111, 111, 11, 1)),
      newCallRecord("visitAny", 1112, 111, of(1112, 111, 11, 1)),
      newCallRecord("visitFile", 1112, 111, of(1112, 111, 11, 1)),
      newCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      newCallRecord("visitDirectory", 112, 11, of(112, 11, 1)),
      newCallRecord("visitAny", 1121, 112, of(1121, 112, 11, 1)),
      newCallRecord("visitFile", 1121, 112, of(1121, 112, 11, 1)),
      newCallRecord("visitAny", 12, 1, of(12, 1)),
      newCallRecord("visitModule", 12, 1, of(12, 1)),
      newCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      newCallRecord("visitModule", 121, 12, of(121, 12, 1)),
      newCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      newCallRecord("visitDirectory", 1211, 121, of(1211, 121, 12, 1)),
      newCallRecord("visitAny", 12111, 1211, of(12111, 1211, 121, 12, 1)),
      newCallRecord("visitFile", 12111, 1211, of(12111, 1211, 121, 12, 1))
      ).iterator();
    verifyCallRecords(expected, underTest.callsRecords.iterator());
  }

  @Test
  public void verify_preOrder_visit_call_when_visit_tree_with_depth_DIRECTORY() {
    TestPathAwareCrawler underTest = new TestPathAwareCrawler(DIRECTORY, PRE_ORDER);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<CallRecord> expected = of(
      newCallRecord("visitAny", 1, null, of(1)),
      newCallRecord("visitProject", 1, null, of(1)),
      newCallRecord("visitAny", 11, 1, of(11, 1)),
      newCallRecord("visitModule", 11, 1, of(11, 1)),
      newCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      newCallRecord("visitDirectory", 111, 11, of(111, 11, 1)),
      newCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      newCallRecord("visitDirectory", 112, 11, of(112, 11, 1)),
      newCallRecord("visitAny", 12, 1, of(12, 1)),
      newCallRecord("visitModule", 12, 1, of(12, 1)),
      newCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      newCallRecord("visitModule", 121, 12, of(121, 12, 1)),
      newCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      newCallRecord("visitDirectory", 1211, 121, of(1211, 121, 12, 1))
      ).iterator();
    verifyCallRecords(expected, underTest.callsRecords.iterator());
  }

  @Test
  public void verify_preOrder_visit_call_when_visit_tree_with_depth_MODULE() {
    TestPathAwareCrawler underTest = new TestPathAwareCrawler(MODULE, PRE_ORDER);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<CallRecord> expected = of(
      newCallRecord("visitAny", 1, null, of(1)),
      newCallRecord("visitProject", 1, null, of(1)),
      newCallRecord("visitAny", 11, 1, of(11, 1)),
      newCallRecord("visitModule", 11, 1, of(11, 1)),
      newCallRecord("visitAny", 12, 1, of(12, 1)),
      newCallRecord("visitModule", 12, 1, of(12, 1)),
      newCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      newCallRecord("visitModule", 121, 12, of(121, 12, 1))
      ).iterator();
    verifyCallRecords(expected, underTest.callsRecords.iterator());
  }

  @Test
  public void verify_preOrder_visit_call_when_visit_tree_with_depth_PROJECT() {
    TestPathAwareCrawler underTest = new TestPathAwareCrawler(PROJECT, PRE_ORDER);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<CallRecord> expected = of(
      newCallRecord("visitAny", 1, null, of(1)),
      newCallRecord("visitProject", 1, null, of(1))
      ).iterator();
    verifyCallRecords(expected, underTest.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder_visit_call_when_visit_tree_with_depth_FILE() {
    TestPathAwareCrawler underTest = new TestPathAwareCrawler(FILE, POST_ORDER);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<CallRecord> expected = of(
      newCallRecord("visitAny", 1111, 111, of(1111, 111, 11, 1)),
      newCallRecord("visitFile", 1111, 111, of(1111, 111, 11, 1)),
      newCallRecord("visitAny", 1112, 111, of(1112, 111, 11, 1)),
      newCallRecord("visitFile", 1112, 111, of(1112, 111, 11, 1)),
      newCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      newCallRecord("visitDirectory", 111, 11, of(111, 11, 1)),
      newCallRecord("visitAny", 1121, 112, of(1121, 112, 11, 1)),
      newCallRecord("visitFile", 1121, 112, of(1121, 112, 11, 1)),
      newCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      newCallRecord("visitDirectory", 112, 11, of(112, 11, 1)),
      newCallRecord("visitAny", 11, 1, of(11, 1)),
      newCallRecord("visitModule", 11, 1, of(11, 1)),
      newCallRecord("visitAny", 12111, 1211, of(12111, 1211, 121, 12, 1)),
      newCallRecord("visitFile", 12111, 1211, of(12111, 1211, 121, 12, 1)),
      newCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      newCallRecord("visitDirectory", 1211, 121, of(1211, 121, 12, 1)),
      newCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      newCallRecord("visitModule", 121, 12, of(121, 12, 1)),
      newCallRecord("visitAny", 12, 1, of(12, 1)),
      newCallRecord("visitModule", 12, 1, of(12, 1)),
      newCallRecord("visitAny", 1, null, of(1)),
      newCallRecord("visitProject", 1, null, of(1))
      ).iterator();
    verifyCallRecords(expected, underTest.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder_visit_call_when_visit_tree_with_depth_DIRECTORY() {
    TestPathAwareCrawler underTest = new TestPathAwareCrawler(DIRECTORY, POST_ORDER);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<CallRecord> expected = of(
      newCallRecord("visitAny", 111, 11, of(111, 11, 1)),
      newCallRecord("visitDirectory", 111, 11, of(111, 11, 1)),
      newCallRecord("visitAny", 112, 11, of(112, 11, 1)),
      newCallRecord("visitDirectory", 112, 11, of(112, 11, 1)),
      newCallRecord("visitAny", 11, 1, of(11, 1)),
      newCallRecord("visitModule", 11, 1, of(11, 1)),
      newCallRecord("visitAny", 1211, 121, of(1211, 121, 12, 1)),
      newCallRecord("visitDirectory", 1211, 121, of(1211, 121, 12, 1)),
      newCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      newCallRecord("visitModule", 121, 12, of(121, 12, 1)),
      newCallRecord("visitAny", 12, 1, of(12, 1)),
      newCallRecord("visitModule", 12, 1, of(12, 1)),
      newCallRecord("visitAny", 1, null, of(1)),
      newCallRecord("visitProject", 1, null, of(1))
      ).iterator();
    verifyCallRecords(expected, underTest.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder_visit_call_when_visit_tree_with_depth_MODULE() {
    TestPathAwareCrawler underTest = new TestPathAwareCrawler(MODULE, POST_ORDER);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<CallRecord> expected = of(
      newCallRecord("visitAny", 11, 1, of(11, 1)),
      newCallRecord("visitModule", 11, 1, of(11, 1)),
      newCallRecord("visitAny", 121, 12, of(121, 12, 1)),
      newCallRecord("visitModule", 121, 12, of(121, 12, 1)),
      newCallRecord("visitAny", 12, 1, of(12, 1)),
      newCallRecord("visitModule", 12, 1, of(12, 1)),
      newCallRecord("visitAny", 1, null, of(1)),
      newCallRecord("visitProject", 1, null, of(1))
      ).iterator();
    verifyCallRecords(expected, underTest.callsRecords.iterator());
  }

  @Test
  public void verify_postOrder_visit_call_when_visit_tree_with_depth_PROJECT() {
    TestPathAwareCrawler underTest = new TestPathAwareCrawler(PROJECT, POST_ORDER);
    underTest.visit(SOME_TREE_ROOT);

    Iterator<CallRecord> expected = of(
      newCallRecord("visitAny", 1, null, of(1)),
      newCallRecord("visitProject", 1, null, of(1))
      ).iterator();
    verifyCallRecords(expected, underTest.callsRecords.iterator());
  }

  private static void verifyCallRecords(Iterator<CallRecord> expected, Iterator<CallRecord> actual) {
    while (expected.hasNext()) {
      assertThat(actual.next()).isEqualTo(expected.next());
    }
    assertThat(expected.hasNext()).isEqualTo(actual.hasNext());
  }

  private static CallRecord newCallRecord(String method, int currentRef, @Nullable Integer parentRef, List<Integer> path) {
    return CallRecord.reportCallRecord(method, currentRef, currentRef, parentRef, ROOT_REF, path);
  }

}
