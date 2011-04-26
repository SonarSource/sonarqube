/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.markdown;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MarkdownEngineTest {

  @Test
  public void shouldDecorateUrl() {
    assertThat(MarkdownEngine.convertToHtml("http://google.com"), is("<a href=\"http://google.com\">http://google.com</a>"));
  }

  @Test
  public void shouldDecorateEndOfLine() {
    assertThat(MarkdownEngine.convertToHtml("1\r2\r\n3\n"), is("1<br/>2<br/>3<br/>"));
  }
  
  @Test
  public void shouldDecorateList() {
    assertThat(MarkdownEngine.convertToHtml("  * one\r* two\r\n* three\n * \n *five"), is("<ul><li>one</li>\r<li>two</li>\r\n<li>three</li>\n<li> </li>\n</ul> *five"));
    assertThat(MarkdownEngine.convertToHtml("  * one\r* two"), is("<ul><li>one</li>\r<li>two</li></ul>"));
  }

  @Test
  public void shouldDecorateCode() {
    assertThat(MarkdownEngine.convertToHtml("This is a ''line of code''"), is("This is a <code>line of code</code>"));
    assertThat(MarkdownEngine.convertToHtml("This is not a ''line of code"), is("This is not a ''line of code"));
  }

  @Test
  public void shouldEmphaseText() {
    assertThat(MarkdownEngine.convertToHtml("This is *important*"), is("This is <em>important</em>"));
    assertThat(MarkdownEngine.convertToHtml("This should not be * \n emphase"), is("This should not be * <br/> emphase"));
    assertThat(MarkdownEngine.convertToHtml("This is *very* very *important*"), is("This is <em>very</em> very <em>important</em>"));
    assertThat(MarkdownEngine.convertToHtml("Not * emphase * because of whitespaces"), is("Not * emphase * because of whitespaces"));
    assertThat(MarkdownEngine.convertToHtml("Not *emphase * because of whitespace"), is("Not *emphase * because of whitespace"));
    assertThat(MarkdownEngine.convertToHtml("Not * emphase* because of whitespace"), is("Not * emphase* because of whitespace"));
    assertThat(MarkdownEngine.convertToHtml("emphase*inside*word"), is("emphase<em>inside</em>word"));
    assertThat(MarkdownEngine.convertToHtml("*Emphase many words*"), is("<em>Emphase many words</em>"));
  }

  @Test
  public void shouldNotChangeAnythingInTheText() {
    assertThat(MarkdownEngine.convertToHtml("My text is $123 ''"), is("My text is $123 ''"));
  }

}
