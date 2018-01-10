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
package org.sonar.server.source;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HtmlSourceDecoratorTest {

  HtmlSourceDecorator sourceDecorator;

  @Before
  public void setUpDatasets() {
    sourceDecorator = new HtmlSourceDecorator();
  }

  @Test
  public void should_decorate_single_line() {
    String sourceLine = "package org.polop;";
    String highlighting = "0,7,k";
    String symbols = "8,17,42";
    assertThat(sourceDecorator.getDecoratedSourceAsHtml(sourceLine, highlighting, symbols)).isEqualTo(
      "<span class=\"k\">package</span> <span class=\"sym-42 sym\">org.polop</span>;");
  }

  @Test
  public void should_handle_highlighting_too_long() {
    String sourceLine = "abc";
    String highlighting = "0,5,c";
    String symbols = "";
    assertThat(sourceDecorator.getDecoratedSourceAsHtml(sourceLine, highlighting, symbols)).isEqualTo("<span class=\"c\">abc</span>");
  }

  @Test
  public void should_ignore_missing_highlighting() {
    String sourceLine = "    if (toto < 42) {";
    assertThat(sourceDecorator.getDecoratedSourceAsHtml(sourceLine, null, null)).isEqualTo("    if (toto &lt; 42) {");
    assertThat(sourceDecorator.getDecoratedSourceAsHtml(sourceLine, "", null)).isEqualTo("    if (toto &lt; 42) {");
  }

  @Test
  public void should_ignore_null_source() {
    assertThat(sourceDecorator.getDecoratedSourceAsHtml(null, null, null)).isNull();
  }

  @Test
  public void should_ignore_empty_source() {
    assertThat(sourceDecorator.getDecoratedSourceAsHtml("", "0,1,cppd", "")).isEqualTo("");
  }

  @Test
  public void should_ignore_empty_rule() {
    String sourceLine = "@Deprecated";
    String highlighting = "0,0,a;0,11,a";
    String symbols = "1,11,1";
    assertThat(sourceDecorator.getDecoratedSourceAsHtml(sourceLine, highlighting, symbols)).isEqualTo("<span class=\"a\">@<span class=\"sym-1 sym\">Deprecated</span></span>");
  }

}
