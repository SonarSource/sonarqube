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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.web.CodeColorizerFormat;
import org.sonar.batch.highlighting.SyntaxHighlightingData;
import org.sonar.colorizer.CDocTokenizer;
import org.sonar.colorizer.CppDocTokenizer;
import org.sonar.colorizer.JavadocTokenizer;
import org.sonar.colorizer.KeywordsTokenizer;
import org.sonar.colorizer.StringTokenizer;
import org.sonar.colorizer.Tokenizer;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CodeColorizersTest {

  private static final String HIGHLIGHTING_JS = "0,4,cppd;5,11,cppd;12,15,cppd;16,19,k;29,37,k;65,69,k;85,93,cd;98,102,k;112,114,s;120,124,k";
  private static final String HIGHLIGHTING_JAVA = "0,4,j;5,11,j;12,15,j;16,22,k;23,28,k;43,50,k;51,54,k;67,78,a;81,87,k;88,92,k;97,100,k;142,146,k;162,170,cd";
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testConvertToHighlighting() throws Exception {
    CodeColorizers codeColorizers = new CodeColorizers(Arrays.<CodeColorizerFormat>asList(new JavaScriptColorizerFormat()));

    File jsFile = new File(this.getClass().getResource("CodeColorizersTest/Person.js").toURI());

    SyntaxHighlightingData syntaxHighlighting = codeColorizers.toSyntaxHighlighting(jsFile, Charsets.UTF_8, "js");

    assertThat(syntaxHighlighting.writeString()).isEqualTo(HIGHLIGHTING_JS);

  }

  @Test
  public void testConvertToHighlightingIgnoreBOM() throws Exception {
    CodeColorizers codeColorizers = new CodeColorizers(Arrays.<CodeColorizerFormat>asList(new JavaScriptColorizerFormat()));

    File fileWithBom = temp.newFile();
    FileUtils.write(fileWithBom, "\uFEFF", "UTF-8");
    File jsFile = new File(this.getClass().getResource("CodeColorizersTest/Person.js").toURI());
    FileUtils.write(fileWithBom, FileUtils.readFileToString(jsFile), "UTF-8", true);

    SyntaxHighlightingData syntaxHighlighting = codeColorizers.toSyntaxHighlighting(fileWithBom, Charsets.UTF_8, "js");

    assertThat(syntaxHighlighting.writeString()).isEqualTo(HIGHLIGHTING_JS);
  }

  @Test
  public void shouldSupportJavaIfNotProvidedByJavaPluginForBackwardCompatibility() throws Exception {
    CodeColorizers codeColorizers = new CodeColorizers(Arrays.<CodeColorizerFormat>asList());

    File javaFile = new File(this.getClass().getResource("CodeColorizersTest/Person.java").toURI());

    SyntaxHighlightingData syntaxHighlighting = codeColorizers.toSyntaxHighlighting(javaFile, Charsets.UTF_8, "java");

    assertThat(syntaxHighlighting.writeString()).isEqualTo(HIGHLIGHTING_JAVA);

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
