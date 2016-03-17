/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.source;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.web.CodeColorizerFormat;
import org.sonar.colorizer.CDocTokenizer;
import org.sonar.colorizer.CppDocTokenizer;
import org.sonar.colorizer.JavadocTokenizer;
import org.sonar.colorizer.KeywordsTokenizer;
import org.sonar.colorizer.MultilinesDocTokenizer;
import org.sonar.colorizer.RegexpTokenizer;
import org.sonar.colorizer.StringTokenizer;
import org.sonar.colorizer.Tokenizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CodeColorizersTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testConvertToHighlighting() throws Exception {
    CodeColorizers codeColorizers = new CodeColorizers(Arrays.<CodeColorizerFormat>asList(new JavaScriptColorizerFormat(), new WebCodeColorizerFormat()));
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
    CodeColorizers codeColorizers = new CodeColorizers(Arrays.<CodeColorizerFormat>asList(new JavaScriptColorizerFormat(), new WebCodeColorizerFormat()));

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

  @Test
  public void testConvertHtmlToHighlightingWithMacEoL() throws Exception {
    CodeColorizers codeColorizers = new CodeColorizers(Arrays.<CodeColorizerFormat>asList(new JavaScriptColorizerFormat(), new WebCodeColorizerFormat()));
    File htmlFile = new File(this.getClass().getResource("CodeColorizersTest/package.html").toURI());
    SensorStorage sensorStorage = mock(SensorStorage.class);
    DefaultHighlighting highlighting = new DefaultHighlighting(sensorStorage);
    highlighting.onFile(new DefaultInputFile("FOO", "package.html")
      .initMetadata(new FileMetadata().readMetadata(htmlFile, StandardCharsets.UTF_8)));

    codeColorizers.toSyntaxHighlighting(htmlFile, StandardCharsets.UTF_8, "web", highlighting);

    assertThat(highlighting.getSyntaxHighlightingRuleSet()).extracting("range.start.line", "range.start.lineOffset", "range.end.line", "range.end.lineOffset", "textType")
      .containsExactly(
        tuple(1, 0, 1, 132, TypeOfText.STRUCTURED_COMMENT),
        tuple(2, 0, 2, 6, TypeOfText.KEYWORD),
        tuple(3, 0, 3, 3, TypeOfText.KEYWORD),
        tuple(4, 0, 4, 3, TypeOfText.KEYWORD),
        // SONARWEB-26
        tuple(5, 42, 12, 0, TypeOfText.STRING));
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

  public class WebCodeColorizerFormat extends CodeColorizerFormat {

    private final List<Tokenizer> tokenizers = new ArrayList<>();

    public WebCodeColorizerFormat() {
      super("web");
      String tagAfter = "</span>";

      // == tags ==
      tokenizers.add(new RegexpTokenizer("<span class=\"k\">", tagAfter, "</?[:\\w]+>?"));
      tokenizers.add(new RegexpTokenizer("<span class=\"k\">", tagAfter, ">"));

      // == doctype ==
      tokenizers.add(new RegexpTokenizer("<span class=\"j\">", tagAfter, "<!DOCTYPE.*>"));

      // == comments ==
      tokenizers.add(new MultilinesDocTokenizer("<!--", "-->", "<span class=\"j\">", tagAfter));
      tokenizers.add(new MultilinesDocTokenizer("<%--", "--%>", "<span class=\"j\">", tagAfter));

      // == expressions ==
      tokenizers.add(new MultilinesDocTokenizer("<%@", "%>", "<span class=\"a\">", tagAfter));
      tokenizers.add(new MultilinesDocTokenizer("<%", "%>", "<span class=\"a\">", tagAfter));

      // == tag properties ==
      tokenizers.add(new StringTokenizer("<span class=\"s\">", tagAfter));
    }

    @Override
    public List<Tokenizer> getTokenizers() {
      return tokenizers;
    }

  }

}
