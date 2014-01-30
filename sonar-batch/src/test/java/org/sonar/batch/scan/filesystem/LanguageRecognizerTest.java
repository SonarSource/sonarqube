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
package org.sonar.batch.scan.filesystem;

import com.google.common.base.Charsets;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.internal.InputFileBuilder;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.spy;

public class LanguageRecognizerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_sanitizeExtension() throws Exception {
    assertThat(LanguageRecognizer.sanitizeExtension(".cbl")).isEqualTo("cbl");
    assertThat(LanguageRecognizer.sanitizeExtension(".CBL")).isEqualTo("cbl");
    assertThat(LanguageRecognizer.sanitizeExtension("CBL")).isEqualTo("cbl");
    assertThat(LanguageRecognizer.sanitizeExtension("cbl")).isEqualTo("cbl");
  }

  @Test
  public void search_by_file_extension() throws Exception {
    Languages languages = new Languages(new MockLanguage("java", "java", "jav"), new MockLanguage("cobol", "cbl", "cob"));
    LanguageRecognizer recognizer = new LanguageRecognizer(new Settings(), languages);

    recognizer.start();
    assertThat(recognizer.of(newInputFile("Foo.java"))).isEqualTo("java");
    assertThat(recognizer.of(newInputFile("Foo.JAVA"))).isEqualTo("java");
    assertThat(recognizer.of(newInputFile("Foo.jav"))).isEqualTo("java");
    assertThat(recognizer.of(newInputFile("Foo.Jav"))).isEqualTo("java");

    assertThat(recognizer.of(newInputFile("abc.cbl"))).isEqualTo("cobol");
    assertThat(recognizer.of(newInputFile("abc.CBL"))).isEqualTo("cobol");

    assertThat(recognizer.of(newInputFile("abc.php"))).isNull();
    assertThat(recognizer.of(newInputFile("abc"))).isNull();
    recognizer.stop();
  }

  @Test
  public void should_not_fail_if_no_language() throws Exception {
    LanguageRecognizer recognizer = spy(new LanguageRecognizer(new Settings(), new Languages()));
    recognizer.start();
    assertThat(recognizer.of(newInputFile("Foo.java"))).isNull();
  }

  @Test
  public void plugin_can_declare_a_file_extension_twice_for_case_sensitivity() throws Exception {
    Languages languages = new Languages(new MockLanguage("abap", "abap", "ABAP"));

    LanguageRecognizer recognizer = new LanguageRecognizer(new Settings(), languages);
    recognizer.start();
    assertThat(recognizer.of(newInputFile("abc.abap"))).isEqualTo("abap");
  }

  @Test
  public void language_with_no_extension() throws Exception {
    // abap does not declare any file extensions.
    // When analyzing an ABAP project, then all source files must be parsed.
    Languages languages = new Languages(new MockLanguage("java", "java"), new MockLanguage("abap"));

    // No side-effect on non-ABAP projects
    LanguageRecognizer recognizer = new LanguageRecognizer(new Settings(), languages);
    recognizer.start();
    assertThat(recognizer.of(newInputFile("abc"))).isNull();
    assertThat(recognizer.of(newInputFile("abc.abap"))).isNull();
    assertThat(recognizer.of(newInputFile("abc.java"))).isEqualTo("java");
    recognizer.stop();

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_LANGUAGE_PROPERTY, "abap");
    recognizer = new LanguageRecognizer(settings, languages);
    recognizer.start();
    assertThat(recognizer.of(newInputFile("abc"))).isEqualTo("abap");
    assertThat(recognizer.of(newInputFile("abc.txt"))).isEqualTo("abap");
    assertThat(recognizer.of(newInputFile("abc.java"))).isEqualTo("abap");
    recognizer.stop();
  }

  @Test
  public void force_language_using_deprecated_property() throws Exception {
    Languages languages = new Languages(new MockLanguage("java", "java"), new MockLanguage("php", "php"));

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_LANGUAGE_PROPERTY, "java");
    LanguageRecognizer recognizer = new LanguageRecognizer(settings, languages);
    recognizer.start();
    assertThat(recognizer.of(newInputFile("abc"))).isNull();
    assertThat(recognizer.of(newInputFile("abc.php"))).isNull();
    assertThat(recognizer.of(newInputFile("abc.java"))).isEqualTo("java");
    recognizer.stop();
  }

  @Test
  public void fail_if_invalid_language() throws Exception {
    Languages languages = new Languages(new MockLanguage("java", "java"), new MockLanguage("php", "php"));

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_LANGUAGE_PROPERTY, "unknow");
    LanguageRecognizer recognizer = new LanguageRecognizer(settings, languages);
    recognizer.start();
    thrown.expect(SonarException.class);
    thrown.expectMessage("No language is installed with key 'unknow'. Please update property 'sonar.language'");
    recognizer.of(newInputFile("abc"));
    recognizer.stop();
  }

  @Test
  public void fail_if_conflicting_language_suffix() throws Exception {
    Languages languages = new Languages(new MockLanguage("xml", "xhtml"), new MockLanguage("web", "xhtml"));

    Settings settings = new Settings();
    LanguageRecognizer recognizer = new LanguageRecognizer(settings, languages);
    recognizer.start();
    thrown.expect(SonarException.class);
    thrown.expectMessage(new BaseMatcher<String>() {
      @Override
      public void describeTo(Description arg0) {
      }

      @Override
      public boolean matches(Object arg0) {
        // Need custom matcher because order of language in the exception is not deterministic (hashmap)
        return arg0.toString().contains("Language of file 'abc.xhtml' can not be decided as the file matches patterns of both ")
          && arg0.toString().contains("sonar.lang.patterns.web : **/*.xhtml")
          && arg0.toString().contains("sonar.lang.patterns.xml : **/*.xhtml");
      }
    });
    recognizer.of(newInputFile("abc.xhtml"));
    recognizer.stop();
  }

  @Test
  public void solve_conflict_using_filepattern() throws Exception {
    Languages languages = new Languages(new MockLanguage("xml", "xhtml"), new MockLanguage("web", "xhtml"));

    Settings settings = new Settings();
    settings.setProperty("sonar.lang.patterns.xml", "xml/**");
    settings.setProperty("sonar.lang.patterns.web", "web/**");
    LanguageRecognizer recognizer = new LanguageRecognizer(settings, languages);
    recognizer.start();
    assertThat(recognizer.of(newInputFile("xml/abc.xhtml"))).isEqualTo("xml");
    assertThat(recognizer.of(newInputFile("web/abc.xhtml"))).isEqualTo("web");
    recognizer.stop();
  }

  @Test
  public void fail_if_conflicting_filepattern() throws Exception {
    Languages languages = new Languages(new MockLanguage("abap", "abap"), new MockLanguage("cobol", "cobol"));

    Settings settings = new Settings();
    settings.setProperty("sonar.lang.patterns.abap", "*.abap,*.txt");
    settings.setProperty("sonar.lang.patterns.cobol", "*.cobol,*.txt");
    LanguageRecognizer recognizer = new LanguageRecognizer(settings, languages);
    recognizer.start();
    assertThat(recognizer.of(newInputFile("abc.abap"))).isEqualTo("abap");
    assertThat(recognizer.of(newInputFile("abc.cobol"))).isEqualTo("cobol");
    thrown.expect(SonarException.class);
    thrown.expectMessage(new BaseMatcher<String>() {
      @Override
      public void describeTo(Description arg0) {
      }

      @Override
      public boolean matches(Object arg0) {
        // Need custom matcher because order of language in the exception is not deterministic (hashmap)
        return arg0.toString().contains("Language of file 'abc.txt' can not be decided as the file matches patterns of both ")
          && arg0.toString().contains("sonar.lang.patterns.abap : *.abap,*.txt")
          && arg0.toString().contains("sonar.lang.patterns.cobol : *.cobol,*.txt");
      }
    });
    recognizer.of(newInputFile("abc.txt"));
    recognizer.stop();
  }

  private InputFile newInputFile(String path) throws IOException {
    File basedir = temp.newFolder();
    return new InputFileBuilder(new File(basedir, path), Charsets.UTF_8, path).build();
  }

  static class MockLanguage implements Language {
    private final String key;
    private final String[] extensions;

    MockLanguage(String key, String... extensions) {
      this.key = key;
      this.extensions = extensions;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public String getName() {
      return key;
    }

    @Override
    public String[] getFileSuffixes() {
      return extensions;
    }
  }
}
