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
package org.sonar.colorizer;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class HtmlRendererTest {

  private KeywordsTokenizer javaKeywordTokenizer = new KeywordsTokenizer("<span class='k'>", "</span>", JavaKeywords.get());

  @Test
  public void renderJavaSyntax() {
    HtmlRenderer htmlRenderer = new HtmlRenderer(HtmlOptions.ONLY_SYNTAX);

    String html = htmlRenderer.render(new StringReader("public class Hello {"), Arrays.asList(javaKeywordTokenizer));

    assertThat(html).isEqualTo("<span class='k'>public</span> <span class='k'>class</span> Hello {");
  }

  @Test
  public void supportHtmlSpecialCharacters() {
    HtmlRenderer htmlRenderer = new HtmlRenderer(HtmlOptions.ONLY_SYNTAX);

    String html = htmlRenderer.render(new StringReader("foo(\"<html>\");"), Arrays.asList(new LiteralTokenizer("<s>", "</s>")));

    assertThat(html).isEqualTo("foo(<s>\"&lt;html&gt;\"</s>);");
  }

  @Test
  public void renderJavaFile() throws IOException {
    File java = FileUtils.toFile(getClass().getResource("/org/sonar/colorizer/HtmlRendererTest/Sample.java"));

    String html = new HtmlRenderer().render(new FileReader(java), Arrays.asList(javaKeywordTokenizer));

    assertThat(html).contains("<html>");
    assertThat(html).contains("<style");
    assertThat(html).contains("<table class=\"code\"");
    assertThat(html).contains("public");
    assertThat(html).contains("class");
    assertThat(html).contains("Sample");
    assertThat(html).contains("</html>");
  }

}
