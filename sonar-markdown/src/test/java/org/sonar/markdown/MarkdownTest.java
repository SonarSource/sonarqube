/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.markdown;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MarkdownTest {

  @Test
  public void shouldDecorateUrl() {
    assertThat(Markdown.convertToHtml("http://google.com"),
        is("<a href=\"http://google.com\" target=\"_blank\">http://google.com</a>"));
  }

  @Test
  public void shouldDecorateEndOfLine() {
    assertThat(Markdown.convertToHtml("1\r2\r\n3\n"), is("1<br/>2<br/>3<br/>"));
  }

  @Test
  public void shouldDecorateList() {
    assertThat(Markdown.convertToHtml("  * one\r* two\r\n* three\n * \n *five"),
        is("<ul><li>one</li>\r<li>two</li>\r\n<li>three</li>\n<li> </li>\n</ul> *five"));
    assertThat(Markdown.convertToHtml("  * one\r* two"), is("<ul><li>one</li>\r<li>two</li></ul>"));
  }

  @Test
  public void shouldDecorateCode() {
    assertThat(Markdown.convertToHtml("This is a ``line of code``"), is("This is a <code>line of code</code>"));
    assertThat(Markdown.convertToHtml("This is not a ``line of code"), is("This is not a ``line of code"));
  }

  @Test
  public void shouldDecorateMultipleLineCode() {
    assertThat(Markdown.convertToHtml("This is a ``\nline of code\nOn multiple lines\n``"), is("This is a <pre><code>line of code\nOn multiple lines</code></pre>"));
    assertThat(Markdown.convertToHtml("This is not a ``line of code\nOn multiple lines``"), is("This is not a ``line of code<br/>On multiple lines``"));
    assertThat(Markdown.convertToHtml("This is not a ``line of code\nOn multiple lines"), is("This is not a ``line of code<br/>On multiple lines"));
  }

  @Test
  public void shouldDecorateMultipleLineCodeWithLanguageSpecified() {
    assertThat(Markdown.convertToHtml("This is a ``java\nline of code\nOn multiple lines\n``"), is("This is a <pre lang=\"java\"><code>line of code\nOn multiple lines</code></pre>"));
    assertThat(Markdown.convertToHtml("This is not a ``java line of code\nOn multiple lines``"), is("This is not a ``java line of code<br/>On multiple lines``"));
    assertThat(Markdown.convertToHtml("This is not a ``java \nline of code\nOn multiple lines``"), is("This is not a ``java <br/>line of code<br/>On multiple lines``"));
  }

  @Test
  public void shouldEmphaseText() {
    assertThat(Markdown.convertToHtml("This is *important*"), is("This is <em>important</em>"));
    assertThat(Markdown.convertToHtml("This should not be * \n emphase"), is("This should not be * <br/> emphase"));
    assertThat(Markdown.convertToHtml("This is *very* very *important*"), is("This is <em>very</em> very <em>important</em>"));
    assertThat(Markdown.convertToHtml("Not * emphase * because of whitespaces"), is("Not * emphase * because of whitespaces"));
    assertThat(Markdown.convertToHtml("Not *emphase * because of whitespace"), is("Not *emphase * because of whitespace"));
    assertThat(Markdown.convertToHtml("Not * emphase* because of whitespace"), is("Not * emphase* because of whitespace"));
    assertThat(Markdown.convertToHtml("emphase*inside*word"), is("emphase<em>inside</em>word"));
    assertThat(Markdown.convertToHtml("*Emphase many words*"), is("<em>Emphase many words</em>"));
  }

  @Test
  public void shouldNotChangeAnythingInTheText() {
    assertThat(Markdown.convertToHtml("My text is $123 ''"), is("My text is $123 ''"));
  }

}
