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
package org.sonar.markdown;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkdownTest {

  @Test
  public void shouldDecorateUrl() {
    assertThat(Markdown.convertToHtml("http://google.com"))
        .isEqualTo("<a href=\"http://google.com\" target=\"_blank\">http://google.com</a>");
  }

  @Test
  public void shouldDecorateDocumentedLink() {
    assertThat(Markdown.convertToHtml("For more details, please [check online documentation](http://docs.sonarqube.org/display/SONAR)."))
        .isEqualTo("For more details, please <a href=\"http://docs.sonarqube.org/display/SONAR\" target=\"_blank\">check online documentation</a>.");
  }


  @Test
  public void shouldDecorateEndOfLine() {
    assertThat(Markdown.convertToHtml("1\r2\r\n3\n")).isEqualTo("1<br/>2<br/>3<br/>");
  }

  @Test
  public void shouldDecorateUnorderedList() {
    assertThat(Markdown.convertToHtml("  * one\r* two\r\n* three\n * \n *five"))
        .isEqualTo("<ul><li>one</li>\r<li>two</li>\r\n<li>three</li>\n<li> </li>\n</ul> *five");
    assertThat(Markdown.convertToHtml("  * one\r* two")).isEqualTo("<ul><li>one</li>\r<li>two</li></ul>");
    assertThat(Markdown.convertToHtml("* \r*")).isEqualTo("<ul><li> </li>\r</ul>*");
  }

  @Test
  public void shouldDecorateOrderedList() {
    assertThat(Markdown.convertToHtml("  1. one\r1. two\r\n1. three\n 1. \n 1.five"))
        .isEqualTo("<ol><li>one</li>\r<li>two</li>\r\n<li>three</li>\n<li> </li>\n</ol> 1.five");
    assertThat(Markdown.convertToHtml("  1. one\r1. two")).isEqualTo("<ol><li>one</li>\r<li>two</li></ol>");
    assertThat(Markdown.convertToHtml("1. \r1.")).isEqualTo("<ol><li> </li>\r</ol>1.");
  }

  @Test
  public void shouldDecorateHeadings() {
    assertThat(Markdown.convertToHtml("  = Top\r== Sub\r\n=== Subsub\n ==== \n 1.five"))
        .isEqualTo("<h1>Top\r</h1><h2>Sub\r\n</h2><h3>Subsub\n</h3><h4></h4> 1.five");
  }

  @Test
  public void shouldDecorateBlockquote() {
    assertThat(Markdown.convertToHtml("> Yesterday <br/> it worked\n> Today it is not working\r\n> Software is like that\r"))
        .isEqualTo("<blockquote>Yesterday &lt;br/&gt; it worked<br/>\nToday it is not working<br/>\r\nSoftware is like that<br/>\r</blockquote>");
    assertThat(Markdown.convertToHtml("HTML elements should <em>not</em> be quoted!"))
        .isEqualTo("HTML elements should &lt;em&gt;not&lt;/em&gt; be quoted!");
  }

  @Test
  public void shouldDecorateMixedOrderedAndUnorderedList() {
    assertThat(Markdown.convertToHtml("  1. one\r* two\r\n1. three\n * \n 1.five"))
        .isEqualTo("<ol><li>one</li>\r</ol><ul><li>two</li>\r\n</ul><ol><li>three</li>\n</ol><ul><li> </li>\n</ul> 1.five");
  }

  @Test
  public void shouldDecorateCode() {
    assertThat(Markdown.convertToHtml("This is a ``line of code``")).isEqualTo("This is a <code>line of code</code>");
    assertThat(Markdown.convertToHtml("This is not a ``line of code")).isEqualTo("This is not a ``line of code");
  }

  @Test
  public void shouldDecorateMultipleLineCode() {
    assertThat(Markdown.convertToHtml("This is a ``\nline of code\nOn multiple lines\n``")).isEqualTo("This is a <pre><code>line of code\nOn multiple lines</code></pre>");
    assertThat(Markdown.convertToHtml("This is not a ``line of code\nOn multiple lines``")).isEqualTo("This is not a ``line of code<br/>On multiple lines``");
    assertThat(Markdown.convertToHtml("This is not a ``line of code\nOn multiple lines")).isEqualTo("This is not a ``line of code<br/>On multiple lines");
  }

  @Test
  public void shouldDecorateMultipleLineCodeWithLanguageSpecified() {
    assertThat(Markdown.convertToHtml("This is a ``java\nline of code\nOn multiple lines\n``")).isEqualTo("This is a <pre lang=\"java\"><code>line of code\nOn multiple lines</code></pre>");
    assertThat(Markdown.convertToHtml("This is not a ``java line of code\nOn multiple lines``")).isEqualTo("This is not a ``java line of code<br/>On multiple lines``");
    assertThat(Markdown.convertToHtml("This is not a ``java \nline of code\nOn multiple lines``")).isEqualTo("This is not a ``java <br/>line of code<br/>On multiple lines``");
  }

  @Test
  public void shouldEmphasisText() {
    assertThat(Markdown.convertToHtml("This is *Sparta !!!*")).isEqualTo("This is <strong>Sparta !!!</strong>");
    assertThat(Markdown.convertToHtml("This is *A*")).isEqualTo("This is <strong>A</strong>");
    assertThat(Markdown.convertToHtml("This should not be * \n emphasized")).isEqualTo("This should not be * <br/> emphasized");
    assertThat(Markdown.convertToHtml("This is *very* very *important*")).isEqualTo("This is <strong>very</strong> very <strong>important</strong>");
    assertThat(Markdown.convertToHtml("Not * emphasized * because of whitespaces")).isEqualTo("Not * emphasized * because of whitespaces");
    assertThat(Markdown.convertToHtml("Not *emphasized * because of whitespace")).isEqualTo("Not *emphasized * because of whitespace");
    assertThat(Markdown.convertToHtml("Not * emphasized* because of whitespace")).isEqualTo("Not * emphasized* because of whitespace");
    assertThat(Markdown.convertToHtml("emphasized*inside*word")).isEqualTo("emphasized<strong>inside</strong>word");
    assertThat(Markdown.convertToHtml("*Emphasize many words*")).isEqualTo("<strong>Emphasize many words</strong>");
  }

  @Test
  public void shouldNotChangeAnythingInTheText() {
    assertThat(Markdown.convertToHtml("My text is $123 ''")).isEqualTo("My text is $123 ''");
  }

}
