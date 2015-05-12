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
package org.sonar.batch.source;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.web.CodeColorizerFormat;
import org.sonar.colorizer.CDocTokenizer;
import org.sonar.colorizer.CppDocTokenizer;
import org.sonar.colorizer.JavadocTokenizer;
import org.sonar.colorizer.KeywordsTokenizer;
import org.sonar.colorizer.StringTokenizer;
import org.sonar.colorizer.Tokenizer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CodeColorizersTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testConvertToHighlighting() throws Exception {
    CodeColorizers codeColorizers = new CodeColorizers(Arrays.<CodeColorizerFormat>asList(new JavaScriptColorizerFormat()));
    File jsFile = new File(this.getClass().getResource("CodeColorizersTest/Person.js").toURI());
    NewHighlighting highlighting = mock(NewHighlighting.class);

    codeColorizers.toSyntaxHighlighting(jsFile, StandardCharsets.UTF_8, "js", highlighting);

    verifyForJs(highlighting);
  }

  private void verifyForJs(NewHighlighting highlighting) {
    verify(highlighting).highlight(0, 4, TypeOfText.CPP_DOC);
    verify(highlighting).highlight(5, 11, TypeOfText.CPP_DOC);
    verify(highlighting).highlight(12, 15, TypeOfText.CPP_DOC);
    verify(highlighting).highlight(16, 19, TypeOfText.KEYWORD);
    verify(highlighting).highlight(29, 37, TypeOfText.KEYWORD);
    verify(highlighting).highlight(65, 69, TypeOfText.KEYWORD);
    verify(highlighting).highlight(85, 93, TypeOfText.COMMENT);
    verify(highlighting).highlight(98, 102, TypeOfText.KEYWORD);
    verify(highlighting).highlight(112, 114, TypeOfText.STRING);
    verify(highlighting).highlight(120, 124, TypeOfText.KEYWORD);
  }

  @Test
  public void testConvertToHighlightingIgnoreBOM() throws Exception {
    CodeColorizers codeColorizers = new CodeColorizers(Arrays.<CodeColorizerFormat>asList(new JavaScriptColorizerFormat()));

    File fileWithBom = temp.newFile();
    FileUtils.write(fileWithBom, "\uFEFF", "UTF-8");
    File jsFile = new File(this.getClass().getResource("CodeColorizersTest/Person.js").toURI());
    FileUtils.write(fileWithBom, FileUtils.readFileToString(jsFile), "UTF-8", true);

    NewHighlighting highlighting = mock(NewHighlighting.class);
    codeColorizers.toSyntaxHighlighting(fileWithBom, StandardCharsets.UTF_8, "js", highlighting);

    verifyForJs(highlighting);
  }

  @Test
  public void shouldSupportJavaIfNotProvidedByJavaPluginForBackwardCompatibility() throws Exception {
    CodeColorizers codeColorizers = new CodeColorizers(Arrays.<CodeColorizerFormat>asList());

    File javaFile = new File(this.getClass().getResource("CodeColorizersTest/Person.java").toURI());

    NewHighlighting highlighting = mock(NewHighlighting.class);
    codeColorizers.toSyntaxHighlighting(javaFile, StandardCharsets.UTF_8, "java", highlighting);

    verify(highlighting).highlight(0, 4, TypeOfText.STRUCTURED_COMMENT);
    verify(highlighting).highlight(5, 11, TypeOfText.STRUCTURED_COMMENT);
    verify(highlighting).highlight(12, 15, TypeOfText.STRUCTURED_COMMENT);
    verify(highlighting).highlight(16, 22, TypeOfText.KEYWORD);
    verify(highlighting).highlight(23, 28, TypeOfText.KEYWORD);
    verify(highlighting).highlight(43, 50, TypeOfText.KEYWORD);
    verify(highlighting).highlight(51, 54, TypeOfText.KEYWORD);
    verify(highlighting).highlight(67, 78, TypeOfText.ANNOTATION);
    verify(highlighting).highlight(81, 87, TypeOfText.KEYWORD);
    verify(highlighting).highlight(88, 92, TypeOfText.KEYWORD);
    verify(highlighting).highlight(97, 100, TypeOfText.KEYWORD);
    verify(highlighting).highlight(142, 146, TypeOfText.KEYWORD);
    verify(highlighting).highlight(162, 170, TypeOfText.COMMENT);

  }

  public static class JavaScriptColorizerFormat extends CodeColorizerFormat {

    public JavaScriptColorizerFormat() {
      super("js");
    }

    @Override
    public List<Tokenizer> getTokenizers() {
      return ImmutableList.<Tokenizer>of(
        new StringTokenizer("<span class=\"s\">", "</span>"),
        new CDocTokenizer("<span class=\"cd\">", "</span>"),
        new JavadocTokenizer("<span class=\"cppd\">", "</span>"),
        new CppDocTokenizer("<span class=\"cppd\">", "</span>"),
        new KeywordsTokenizer("<span class=\"k\">", "</span>", "null",
          "true",
          "false",
          "break",
          "case",
          "catch",
          "class",
          "continue",
          "debugger",
          "default",
          "delete",
          "do",
          "extends",
          "else",
          "finally",
          "for",
          "function",
          "if",
          "import",
          "in",
          "instanceof",
          "new",
          "return",
          "super",
          "switch",
          "this",
          "throw",
          "try",
          "typeof",
          "var",
          "void",
          "while",
          "with",
          "yield",
          "const",
          "enum",
          "export"));
    }

  }

}
