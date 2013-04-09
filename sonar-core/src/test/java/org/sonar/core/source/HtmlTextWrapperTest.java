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

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.core.source.HtmlTextWrapper.CR_END_OF_LINE;
import static org.sonar.core.source.HtmlTextWrapper.LF_END_OF_LINE;

public class HtmlTextWrapperTest {

  @Test
  public void should_decorate_simple_character_range() throws Exception {

    String packageDeclaration = "package org.sonar.core.source;";

    SyntaxHighlightingRuleSet syntaxHighlighting = SyntaxHighlightingRuleSet.builder()
            .registerHighlightingRule(0, 7, "k").build();

    HtmlTextWrapper htmlTextWrapper = new HtmlTextWrapper();
    String htmlOutput = htmlTextWrapper.wrapTextWithHtml(packageDeclaration, syntaxHighlighting);

    assertThat(htmlOutput).isEqualTo("<tr><td><span class=\"k\">package</span> org.sonar.core.source;");
  }

  @Test
  public void should_decorate_multiple_lines_characters_range() throws Exception {

    String firstCommentLine = "/*";
    String secondCommentLine = " * Test";
    String thirdCommentLine = " */";

    String blockComment = firstCommentLine + LF_END_OF_LINE
            + secondCommentLine + LF_END_OF_LINE
            + thirdCommentLine + LF_END_OF_LINE;

    SyntaxHighlightingRuleSet syntaxHighlighting = SyntaxHighlightingRuleSet.builder()
            .registerHighlightingRule(0, 14, "cppd").build();

    HtmlTextWrapper htmlTextWrapper = new HtmlTextWrapper();
    String htmlOutput = htmlTextWrapper.wrapTextWithHtml(blockComment, syntaxHighlighting);

    assertThat(htmlOutput).isEqualTo(
            "<tr><td><span class=\"cppd\">" + firstCommentLine + "</span></td></tr>" + LF_END_OF_LINE +
            "<tr><td><span class=\"cppd\">" + secondCommentLine + "</span></td></tr>" + LF_END_OF_LINE +
            "<tr><td><span class=\"cppd\">" + thirdCommentLine + "</span></td></tr>" + LF_END_OF_LINE
    );
  }

  @Test
  public void should_highlight_multiple_words_in_one_line() throws Exception {

    String classDeclaration = "public class MyClass implements MyInterface {\n";

    SyntaxHighlightingRuleSet syntaxHighlighting = SyntaxHighlightingRuleSet.builder()
            .registerHighlightingRule(0, 6, "k")
            .registerHighlightingRule(7, 12, "k")
            .registerHighlightingRule(21, 31, "k")
            .build();

    HtmlTextWrapper htmlTextWrapper = new HtmlTextWrapper();
    String htmlOutput = htmlTextWrapper.wrapTextWithHtml(classDeclaration, syntaxHighlighting);

    assertThat(htmlOutput).isEqualTo(
            "<tr><td><span class=\"k\">public</span> <span class=\"k\">class</span> MyClass <span class=\"k\">implements</span> MyInterface {</td></tr>\n");
  }

  @Test
  public void should_allow_multiple_levels_highlighting() throws Exception {

    String javaDocSample =
            "/**" + LF_END_OF_LINE +
            " * Creates a FormulaDecorator" + LF_END_OF_LINE +
            " *" + LF_END_OF_LINE +
            " * @param metric the metric should have an associated formula" + LF_END_OF_LINE +
            " * " + LF_END_OF_LINE +
            " * @throws IllegalArgumentException if no formula is associated to the metric" + LF_END_OF_LINE +
            " */" + LF_END_OF_LINE;

    SyntaxHighlightingRuleSet syntaxHighlighting = SyntaxHighlightingRuleSet.builder()
            .registerHighlightingRule(0, 184, "cppd")
            .registerHighlightingRule(47, 53, "k")
            .build();

    HtmlTextWrapper htmlTextWrapper = new HtmlTextWrapper();
    String htmlOutput = htmlTextWrapper.wrapTextWithHtml(javaDocSample, syntaxHighlighting);

    assertThat(htmlOutput).isEqualTo(
            "<tr><td><span class=\"cppd\">/**</span></td></tr>" + LF_END_OF_LINE +
            "<tr><td><span class=\"cppd\"> * Creates a FormulaDecorator</span></td></tr>" + LF_END_OF_LINE +
            "<tr><td><span class=\"cppd\"> *</span></td></tr>" + LF_END_OF_LINE +
            "<tr><td><span class=\"cppd\"> * @param <span class=\"k\">metric</span> the metric should have an associated formula</span></td></tr>" + LF_END_OF_LINE +
            "<tr><td><span class=\"cppd\"> * </span></td></tr>" + LF_END_OF_LINE +
            "<tr><td><span class=\"cppd\"> * @throws IllegalArgumentException if no formula is associated to the metric</span></td></tr>" + LF_END_OF_LINE +
            "<tr><td><span class=\"cppd\"> */</span></td></tr>" + LF_END_OF_LINE
          );
  }

  @Test
  public void should_support_crlf_line_breaks() throws Exception {

    String crlfCodeSample =
            "/**" + CR_END_OF_LINE + LF_END_OF_LINE +
            "* @return metric generated by the decorator" + CR_END_OF_LINE + LF_END_OF_LINE +
            "*/" + CR_END_OF_LINE + LF_END_OF_LINE +
            "@DependedUpon" + CR_END_OF_LINE + LF_END_OF_LINE +
            "public Metric generatesMetric() {" + CR_END_OF_LINE + LF_END_OF_LINE +
            "  return metric;" + CR_END_OF_LINE + LF_END_OF_LINE +
            "}" + CR_END_OF_LINE + LF_END_OF_LINE;

    SyntaxHighlightingRuleSet syntaxHighlighting = SyntaxHighlightingRuleSet.builder()
            .registerHighlightingRule(0, 52, "cppd")
            .registerHighlightingRule(54, 67, "a")
            .registerHighlightingRule(69, 75, "k")
            .registerHighlightingRule(106, 112, "k")
            .build();

    HtmlTextWrapper htmlTextWrapper = new HtmlTextWrapper();
    String htmlOutput = htmlTextWrapper.wrapTextWithHtml(crlfCodeSample, syntaxHighlighting);

    assertThat(htmlOutput).isEqualTo(
            "<tr><td><span class=\"cppd\">/**</span></td></tr>" + CR_END_OF_LINE + LF_END_OF_LINE +
            "<tr><td><span class=\"cppd\">* @return metric generated by the decorator</span></td></tr>" + CR_END_OF_LINE + LF_END_OF_LINE +
            "<tr><td><span class=\"cppd\">*/</span></td></tr>" + CR_END_OF_LINE + LF_END_OF_LINE +
            "<tr><td><span class=\"a\">@DependedUpon</span></td></tr>" + CR_END_OF_LINE + LF_END_OF_LINE +
            "<tr><td><span class=\"k\">public</span> Metric generatesMetric() {</td></tr>" + CR_END_OF_LINE + LF_END_OF_LINE +
            "<tr><td>  <span class=\"k\">return</span> metric;</td></tr>" + CR_END_OF_LINE + LF_END_OF_LINE +
            "<tr><td>}</td></tr>" + CR_END_OF_LINE + LF_END_OF_LINE
          );
  }
}
