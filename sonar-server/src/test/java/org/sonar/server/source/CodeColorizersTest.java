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
package org.sonar.server.source;

import org.junit.Test;
import org.sonar.api.web.CodeColorizerFormat;
import org.sonar.colorizer.LiteralTokenizer;
import org.sonar.colorizer.Tokenizer;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class CodeColorizersTest {
  @Test
  public void colorize_source_code() throws Exception {
    CodeColorizerFormat format = new LiteralFormat("java");
    CodeColorizers colorizers = new CodeColorizers(Arrays.asList(format));

    String html = colorizers.toHtml("String s = \"foo\";", "java");
    assertThat(html).isEqualTo("String s = <span class=\"s\">\"foo\"</span>;");
  }

  @Test
  public void do_not_fail_if_unsupported_language() throws Exception {
    CodeColorizerFormat format = new LiteralFormat("java");
    CodeColorizers colorizers = new CodeColorizers(Arrays.asList(format));

    String html = colorizers.toHtml("String s = \"foo\";", "groovy");
    assertThat(html).isEqualTo("String s = \"foo\";");
  }

  /**
   * Highlights only litterals
   */
  static class LiteralFormat extends CodeColorizerFormat {
    LiteralFormat(String languageKey) {
      super(languageKey);
    }

    @Override
    public List<Tokenizer> getTokenizers() {
      return Arrays.<Tokenizer>asList(new LiteralTokenizer("<span class=\"s\">", "</span>"));
    }
  }
}
