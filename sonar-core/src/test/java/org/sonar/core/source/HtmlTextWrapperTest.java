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

import org.junit.Test;
import org.sonar.api.scan.source.HighlightableTextType;
import org.sonar.api.scan.source.SyntaxHighlightingRuleSet;

import static org.fest.assertions.Assertions.assertThat;

public class HtmlTextWrapperTest {

  private static final String NEW_LINE = "\n";

  @Test
  public void should_decorate_simple_character_range() throws Exception {

    String packageDeclaration = "package org.sonar.core.source;";

    SyntaxHighlightingRuleSet syntaxHighlighting = SyntaxHighlightingRuleSet.builder()
            .registerHighlightingRule(0, 7, HighlightableTextType.KEYWORD).build();

    HtmlTextWrapper htmlTextWrapper = new HtmlTextWrapper();
    String htmlOutput = htmlTextWrapper.wrapTextWithHtml(packageDeclaration, syntaxHighlighting);

    assertThat(htmlOutput).isEqualTo("<tr><td><span class=\"k\">package</span> org.sonar.core.source;");
  }

  @Test
  public void should_decorate_multiple_lines_characters_range() throws Exception {

    String firstCommentLine = "/*";
    String secondCommentLine = " * Test";
    String thirdCommentLine = " */";

    String blockComment = firstCommentLine + NEW_LINE + secondCommentLine + NEW_LINE + thirdCommentLine + NEW_LINE;

    SyntaxHighlightingRuleSet syntaxHighlighting = new SyntaxHighlightingRuleSet.Builder()
            .registerHighlightingRule(0, 14, HighlightableTextType.BLOCK_COMMENT).build();

    HtmlTextWrapper htmlTextWrapper = new HtmlTextWrapper();
    String htmlOutput = htmlTextWrapper.wrapTextWithHtml(blockComment, syntaxHighlighting);

    assertThat(htmlOutput).isEqualTo(
            "<tr><td><span class=\"cppd\">" + firstCommentLine + "</span></td></tr>" + NEW_LINE +
            "<tr><td><span class=\"cppd\">" + secondCommentLine + "</span></td></tr>" + NEW_LINE +
            "<tr><td><span class=\"cppd\">" + thirdCommentLine + "</span></td></tr>" + NEW_LINE
    );
  }

  @Test
  public void should_highlight_multiple_words_in_one_line() throws Exception {

    String classDeclaration = "public class MyClass implements MyInterface {\n";

    SyntaxHighlightingRuleSet syntaxHighlighting = SyntaxHighlightingRuleSet.builder()
            .registerHighlightingRule(0, 6, HighlightableTextType.KEYWORD)
            .registerHighlightingRule(7, 12, HighlightableTextType.KEYWORD)
            .registerHighlightingRule(21, 31, HighlightableTextType.KEYWORD)
            .build();

    HtmlTextWrapper htmlTextWrapper = new HtmlTextWrapper();
    String htmlOutput = htmlTextWrapper.wrapTextWithHtml(classDeclaration, syntaxHighlighting);

    assertThat(htmlOutput).isEqualTo(
            "<tr><td><span class=\"k\">public</span> <span class=\"k\">class</span> MyClass <span class=\"k\">implements</span> MyInterface {</td></tr>\n");
  }
}
