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

package org.sonar.server.source;

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.db.SnapshotDataDao;
import org.sonar.core.source.db.SnapshotSourceDao;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class HtmlSourceDecoratorTest extends AbstractDaoTestCase {


  HtmlSourceDecorator sourceDecorator;

  @Before
  public void setUpDatasets() {
    setupData("shared");

    SnapshotSourceDao snapshotSourceDao = new SnapshotSourceDao(getMyBatis());
    SnapshotDataDao snapshotDataDao = new SnapshotDataDao(getMyBatis());
    sourceDecorator = new HtmlSourceDecorator(getMyBatis(), snapshotSourceDao, snapshotDataDao);
  }

  @Test
  public void highlight_syntax_with_html() throws Exception {
    List<String> decoratedSource = sourceDecorator.getDecoratedSourceAsHtml(11L);

    assertThat(decoratedSource).containsExactly(
      "<span class=\"cppd\">/*</span>",
      "<span class=\"cppd\"> * Header</span>",
      "<span class=\"cppd\"> */</span>",
      "",
      "<span class=\"k\">public </span><span class=\"k\">class </span>HelloWorld {",
      "}"
    );
  }

  @Test
  public void highlight_syntax_with_html_from_component() throws Exception {
    List<String> decoratedSource = sourceDecorator.getDecoratedSourceAsHtml("org.apache.struts:struts:Dispatcher", null, null);

    assertThat(decoratedSource).containsExactly(
      "<span class=\"cppd\">/*</span>",
      "<span class=\"cppd\"> * Header</span>",
      "<span class=\"cppd\"> */</span>",
      "",
      "<span class=\"k\">public </span><span class=\"k\">class </span>HelloWorld {",
      "}"
    );
  }

  @Test
  public void highlight_syntax_with_html_from_component_on_given_lines() throws Exception {
    assertThat(sourceDecorator.getDecoratedSourceAsHtml("org.apache.struts:struts:Dispatcher", null, 2)).hasSize(2);
    assertThat(sourceDecorator.getDecoratedSourceAsHtml("org.apache.struts:struts:Dispatcher", 2, null)).hasSize(5);
    assertThat(sourceDecorator.getDecoratedSourceAsHtml("org.apache.struts:struts:Dispatcher", 1, 2)).hasSize(2);
  }

  @Test
  public void mark_symbols_with_html() throws Exception {
    List<String> decoratedSource = sourceDecorator.getDecoratedSourceAsHtml(12L);

    assertThat(decoratedSource).containsExactly(
      "/*",
      " * Header",
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {",
      "}"
    );
  }

  @Test
  public void mark_symbols_with_html_from_component() throws Exception {
    List<String> decoratedSource = sourceDecorator.getDecoratedSourceAsHtml("org.apache.struts:struts:VelocityManager", null, null);

    assertThat(decoratedSource).containsExactly(
      "/*",
      " * Header",
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {",
      "}"
    );
  }

  @Test
  public void decorate_source_with_multiple_decoration_strategies() throws Exception {
    List<String> decoratedSource = sourceDecorator.getDecoratedSourceAsHtml(13L);

    assertThat(decoratedSource).containsExactly(
      "<span class=\"cppd\">/*</span>",
      "<span class=\"cppd\"> * Header</span>",
      "<span class=\"cppd\"> */</span>",
      "",
      "<span class=\"k\">public </span><span class=\"k\">class </span><span class=\"sym-31 sym\">HelloWorld</span> {",
      "  <span class=\"k\">public</span> <span class=\"k\">void</span> <span class=\"sym-58 sym\">foo</span>() {",
      "  }",
      "  <span class=\"k\">public</span> <span class=\"k\">void</span> <span class=\"sym-84 sym\">bar</span>() {",
      "    <span class=\"sym-58 sym\">foo</span>();",
      "  }",
      "}"
    );
  }

  @Test
  public void decorate_source_with_multiple_decoration_strategies_from_component() throws Exception {
    List<String> decoratedSource = sourceDecorator.getDecoratedSourceAsHtml("org.apache.struts:struts:DebuggingInterceptor", null, null);

    assertThat(decoratedSource).containsExactly(
      "<span class=\"cppd\">/*</span>",
      "<span class=\"cppd\"> * Header</span>",
      "<span class=\"cppd\"> */</span>",
      "",
      "<span class=\"k\">public </span><span class=\"k\">class </span><span class=\"sym-31 sym\">HelloWorld</span> {",
      "  <span class=\"k\">public</span> <span class=\"k\">void</span> <span class=\"sym-58 sym\">foo</span>() {",
      "  }",
      "  <span class=\"k\">public</span> <span class=\"k\">void</span> <span class=\"sym-84 sym\">bar</span>() {",
      "    <span class=\"sym-58 sym\">foo</span>();",
      "  }",
      "}"
    );
  }

  @Test
  public void should_not_query_sources_if_no_snapshot_data() throws Exception {
    SnapshotSourceDao snapshotSourceDao = mock(SnapshotSourceDao.class);
    SnapshotDataDao snapshotDataDao = mock(SnapshotDataDao.class);

    HtmlSourceDecorator sourceDecorator = new HtmlSourceDecorator(mock(MyBatis.class), snapshotSourceDao, snapshotDataDao);

    sourceDecorator.getDecoratedSourceAsHtml(14L);

    verify(snapshotDataDao, times(1)).selectSnapshotData(14L, Lists.newArrayList("highlight_syntax", "symbol"));
    verify(snapshotSourceDao, times(0)).selectSnapshotSource(14L);
  }

  @Test
  public void should_not_query_sources_if_no_snapshot_data_from_component() throws Exception {
    SnapshotSourceDao snapshotSourceDao = mock(SnapshotSourceDao.class);
    SnapshotDataDao snapshotDataDao = mock(SnapshotDataDao.class);

    HtmlSourceDecorator sourceDecorator = new HtmlSourceDecorator(mock(MyBatis.class), snapshotSourceDao, snapshotDataDao);

    sourceDecorator.getDecoratedSourceAsHtml("org.apache.struts:struts:DebuggingInterceptor", null, null);

    verify(snapshotDataDao, times(1)).selectSnapshotDataByComponentKey(eq("org.apache.struts:struts:DebuggingInterceptor"), eq(Lists.newArrayList("highlight_syntax", "symbol")),
      any(SqlSession.class));
    verify(snapshotSourceDao, times(0)).selectSnapshotSourceByComponentKey(eq("org.apache.struts:struts:DebuggingInterceptor"),
      any(SqlSession.class));
  }
}
