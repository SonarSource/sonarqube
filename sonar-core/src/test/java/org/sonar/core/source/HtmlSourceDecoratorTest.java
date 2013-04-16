/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.core.source;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class HtmlSourceDecoratorTest extends AbstractDaoTestCase {

  @Before
  public void setUpDatasets() {
    setupData("shared");
  }

  @Test
  public void should_highlight_syntax_with_html() throws Exception {

    HtmlSourceDecorator highlighter = new HtmlSourceDecorator(getMyBatis());

    List<String> highlightedSource = (List<String>) highlighter.getDecoratedSourceAsHtml(11L);

    assertThat(highlightedSource).containsExactly(
      "<span class=\"cppd\">/*</span>",
      "<span class=\"cppd\"> * Header</span>",
      "<span class=\"cppd\"> */</span>",
      "",
      "<span class=\"k\">public </span><span class=\"k\">class </span>HelloWorld {",
      "}"
    );
  }

  @Test
  public void should_mark_symbols_with_html() throws Exception {

    HtmlSourceDecorator highlighter = new HtmlSourceDecorator(getMyBatis());

    List<String> highlightedSource = (List<String>) highlighter.getDecoratedSourceAsHtml(12L);

    assertThat(highlightedSource).containsExactly(
      "/*",
      " * Header",
      " */",
      "",
      "public class <span class=\"symbol.31\">HelloWorld</span> {",
      "}"
    );
  }

  @Test
  public void should_decorate_source_with_multiple_decoration_strategies() throws Exception {

    HtmlSourceDecorator highlighter = new HtmlSourceDecorator(getMyBatis());

    List<String> highlightedSource = (List<String>) highlighter.getDecoratedSourceAsHtml(13L);

    assertThat(highlightedSource).containsExactly(
      "<span class=\"cppd\">/*</span>",
      "<span class=\"cppd\"> * Header</span>",
      "<span class=\"cppd\"> */</span>",
      "",
      "<span class=\"k\">public </span><span class=\"k\">class </span><span class=\"symbol.31\">HelloWorld</span> {",
      "  <span class=\"k\">public</span> <span class=\"k\">void</span> <span class=\"symbol.58\">foo</span>() {",
      "  }",
      "  <span class=\"k\">public</span> <span class=\"k\">void</span> <span class=\"symbol.84\">bar</span>() {",
      "    <span class=\"symbol.58\">foo</span>();",
      "  }",
      "}"
    );
  }
}
