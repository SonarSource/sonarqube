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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class ReportVisitorsCrawlerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final Component FILE_5 = component(FILE, 5);
  private static final Component DIRECTORY_4 = component(DIRECTORY, 4, FILE_5);
  private static final Component COMPONENT_TREE = component(PROJECT, 1, DIRECTORY_4);

  private final TypeAwareVisitor spyPreOrderTypeAwareVisitor = spy(new TestTypeAwareVisitor(CrawlerDepthLimit.FILE, PRE_ORDER));
  private final TypeAwareVisitor spyPostOrderTypeAwareVisitor = spy(new TestTypeAwareVisitor(CrawlerDepthLimit.FILE, POST_ORDER));
  private final TestPathAwareVisitor spyPathAwareVisitor = spy(new TestPathAwareVisitor(CrawlerDepthLimit.FILE, POST_ORDER));

  @Test
  public void execute_each_visitor_on_each_level() {
    InOrder inOrder = inOrder(spyPostOrderTypeAwareVisitor, spyPathAwareVisitor);
    VisitorsCrawler underTest = new VisitorsCrawler(Arrays.asList(spyPostOrderTypeAwareVisitor, spyPathAwareVisitor));
    underTest.visit(COMPONENT_TREE);

    inOrder.verify(spyPostOrderTypeAwareVisitor).visitAny(FILE_5);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitFile(FILE_5);
    inOrder.verify(spyPathAwareVisitor).visitAny(eq(FILE_5), any(PathAwareVisitor.Path.class));
    inOrder.verify(spyPathAwareVisitor).visitFile(eq(FILE_5), any(PathAwareVisitor.Path.class));

    inOrder.verify(spyPostOrderTypeAwareVisitor).visitAny(DIRECTORY_4);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitDirectory(DIRECTORY_4);
    inOrder.verify(spyPathAwareVisitor).visitAny(eq(DIRECTORY_4), any(PathAwareVisitor.Path.class));
    inOrder.verify(spyPathAwareVisitor).visitDirectory(eq(DIRECTORY_4), any(PathAwareVisitor.Path.class));

    inOrder.verify(spyPostOrderTypeAwareVisitor).visitAny(COMPONENT_TREE);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitProject(COMPONENT_TREE);
    inOrder.verify(spyPathAwareVisitor).visitAny(eq(COMPONENT_TREE), any(PathAwareVisitor.Path.class));
    inOrder.verify(spyPathAwareVisitor).visitProject(eq(COMPONENT_TREE), any(PathAwareVisitor.Path.class));
  }

  @Test
  public void execute_pre_visitor_before_post_visitor() {
    InOrder inOrder = inOrder(spyPreOrderTypeAwareVisitor, spyPostOrderTypeAwareVisitor);
    VisitorsCrawler underTest = new VisitorsCrawler(Arrays.asList(spyPreOrderTypeAwareVisitor, spyPostOrderTypeAwareVisitor));
    underTest.visit(COMPONENT_TREE);

    inOrder.verify(spyPreOrderTypeAwareVisitor).visitProject(COMPONENT_TREE);
    inOrder.verify(spyPreOrderTypeAwareVisitor).visitDirectory(DIRECTORY_4);
    inOrder.verify(spyPreOrderTypeAwareVisitor).visitFile(FILE_5);

    inOrder.verify(spyPostOrderTypeAwareVisitor).visitFile(FILE_5);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitDirectory(DIRECTORY_4);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitProject(COMPONENT_TREE);
  }

  @Test
  public void getCumulativeDurations_returns_an_empty_map_when_computation_is_disabled_in_constructor() {
    VisitorsCrawler underTest = new VisitorsCrawler(Arrays.asList(spyPreOrderTypeAwareVisitor, spyPostOrderTypeAwareVisitor), false);
    underTest.visit(COMPONENT_TREE);

    assertThat(underTest.getCumulativeDurations()).isEmpty();
  }

  @Test
  public void getCumulativeDurations_returns_an_non_empty_map_when_computation_is_enabled_in_constructor() {
    VisitorsCrawler underTest = new VisitorsCrawler(Arrays.asList(spyPreOrderTypeAwareVisitor, spyPostOrderTypeAwareVisitor), true);
    underTest.visit(COMPONENT_TREE);

    assertThat(underTest.getCumulativeDurations()).hasSize(2);
  }

  @Test
  public void fail_with_IAE_when_visitor_is_not_path_aware_or_type_aware() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Only TypeAwareVisitor and PathAwareVisitor can be used");

    ComponentVisitor componentVisitor = new ComponentVisitor() {
      @Override
      public Order getOrder() {
        return PRE_ORDER;
      }

      @Override
      public CrawlerDepthLimit getMaxDepth() {
        return CrawlerDepthLimit.FILE;
      }
    };
    new VisitorsCrawler(Arrays.asList(componentVisitor));
  }

  private static Component component(final Component.Type type, final int ref, final Component... children) {
    return ReportComponent.builder(type, ref).addChildren(children).build();
  }

  private static class TestTypeAwareVisitor extends TypeAwareVisitorAdapter {

    public TestTypeAwareVisitor(CrawlerDepthLimit maxDepth, ComponentVisitor.Order order) {
      super(maxDepth, order);
    }
  }

  private static class TestPathAwareVisitor extends PathAwareVisitorAdapter<Integer> {

    public TestPathAwareVisitor(CrawlerDepthLimit maxDepth, ComponentVisitor.Order order) {
      super(maxDepth, order, new SimpleStackElementFactory<Integer>() {
        @Override
        public Integer createForAny(Component component) {
          return component.getReportAttributes().getRef();
        }
      });
    }
  }

}
