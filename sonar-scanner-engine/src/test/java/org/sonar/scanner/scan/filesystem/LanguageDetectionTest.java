/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.config.Settings;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.repository.language.DefaultLanguagesRepository;
import org.sonar.scanner.repository.language.LanguagesRepository;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

public class LanguageDetectionTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_sanitizeExtension() throws Exception {
    assertThat(LanguageDetection.sanitizeExtension(".cbl")).isEqualTo("cbl");
    assertThat(LanguageDetection.sanitizeExtension(".CBL")).isEqualTo("cbl");
    assertThat(LanguageDetection.sanitizeExtension("CBL")).isEqualTo("cbl");
    assertThat(LanguageDetection.sanitizeExtension("cbl")).isEqualTo("cbl");
  }

  @Test
  public void search_by_file_extension() throws Exception {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("java", "java", "jav"), new MockLanguage("cobol", "cbl", "cob")));
    LanguageDetection detection = new LanguageDetection(new MapSettings(), languages);

    assertThat(detection.language(newIndexedFile("Foo.java"))).isEqualTo("java");
    assertThat(detection.language(newIndexedFile("src/Foo.java"))).isEqualTo("java");
    assertThat(detection.language(newIndexedFile("Foo.JAVA"))).isEqualTo("java");
    assertThat(detection.language(newIndexedFile("Foo.jav"))).isEqualTo("java");
    assertThat(detection.language(newIndexedFile("Foo.Jav"))).isEqualTo("java");

    assertThat(detection.language(newIndexedFile("abc.cbl"))).isEqualTo("cobol");
    assertThat(detection.language(newIndexedFile("abc.CBL"))).isEqualTo("cobol");

    assertThat(detection.language(newIndexedFile("abc.php"))).isNull();
    assertThat(detection.language(newIndexedFile("abc"))).isNull();
  }

  @Test
  public void should_not_fail_if_no_language() throws Exception {
    LanguageDetection detection = spy(new LanguageDetection(new MapSettings(), new DefaultLanguagesRepository(new Languages())));
    assertThat(detection.language(newIndexedFile("Foo.java"))).isNull();
  }

  @Test
  public void plugin_can_declare_a_file_extension_twice_for_case_sensitivity() throws Exception {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("abap", "abap", "ABAP")));

    LanguageDetection detection = new LanguageDetection(new MapSettings(), languages);
    assertThat(detection.language(newIndexedFile("abc.abap"))).isEqualTo("abap");
  }

  @Test
  public void language_with_no_extension() throws Exception {
    // abap does not declare any file extensions.
    // When analyzing an ABAP project, then all source files must be parsed.
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("java", "java"), new MockLanguage("abap")));

    // No side-effect on non-ABAP projects
    LanguageDetection detection = new LanguageDetection(new MapSettings(), languages);
    assertThat(detection.language(newIndexedFile("abc"))).isNull();
    assertThat(detection.language(newIndexedFile("abc.abap"))).isNull();
    assertThat(detection.language(newIndexedFile("abc.java"))).isEqualTo("java");

    Settings settings = new MapSettings();
    settings.setProperty(CoreProperties.PROJECT_LANGUAGE_PROPERTY, "abap");
    detection = new LanguageDetection(settings, languages);
    assertThat(detection.language(newIndexedFile("abc"))).isEqualTo("abap");
    assertThat(detection.language(newIndexedFile("abc.txt"))).isEqualTo("abap");
    assertThat(detection.language(newIndexedFile("abc.java"))).isEqualTo("abap");
  }

  @Test
  public void force_language_using_deprecated_property() throws Exception {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("java", "java"), new MockLanguage("php", "php")));

    Settings settings = new MapSettings();
    settings.setProperty(CoreProperties.PROJECT_LANGUAGE_PROPERTY, "java");
    LanguageDetection detection = new LanguageDetection(settings, languages);
    assertThat(detection.language(newIndexedFile("abc"))).isNull();
    assertThat(detection.language(newIndexedFile("abc.php"))).isNull();
    assertThat(detection.language(newIndexedFile("abc.java"))).isEqualTo("java");
    assertThat(detection.language(newIndexedFile("src/abc.java"))).isEqualTo("java");
  }

  @Test
  public void fail_if_invalid_language() {
    thrown.expect(MessageException.class);
    thrown.expectMessage("You must install a plugin that supports the language 'unknown'");

    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("java", "java"), new MockLanguage("php", "php")));
    Settings settings = new MapSettings();
    settings.setProperty(CoreProperties.PROJECT_LANGUAGE_PROPERTY, "unknown");
    new LanguageDetection(settings, languages);
  }

  @Test
  public void fail_if_conflicting_language_suffix() throws Exception {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("xml", "xhtml"), new MockLanguage("web", "xhtml")));
    LanguageDetection detection = new LanguageDetection(new MapSettings(), languages);
    try {
      detection.language(newIndexedFile("abc.xhtml"));
      fail();
    } catch (MessageException e) {
      assertThat(e.getMessage())
        .contains("Language of file 'abc.xhtml' can not be decided as the file matches patterns of both ")
        .contains("sonar.lang.patterns.web : **/*.xhtml")
        .contains("sonar.lang.patterns.xml : **/*.xhtml");
    }
  }

  @Test
  public void solve_conflict_using_filepattern() throws Exception {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("xml", "xhtml"), new MockLanguage("web", "xhtml")));

    Settings settings = new MapSettings();
    settings.setProperty("sonar.lang.patterns.xml", "xml/**");
    settings.setProperty("sonar.lang.patterns.web", "web/**");
    LanguageDetection detection = new LanguageDetection(settings, languages);
    assertThat(detection.language(newIndexedFile("xml/abc.xhtml"))).isEqualTo("xml");
    assertThat(detection.language(newIndexedFile("web/abc.xhtml"))).isEqualTo("web");
  }

  @Test
  public void fail_if_conflicting_filepattern() throws Exception {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("abap", "abap"), new MockLanguage("cobol", "cobol")));
    Settings settings = new MapSettings();
    settings.setProperty("sonar.lang.patterns.abap", "*.abap,*.txt");
    settings.setProperty("sonar.lang.patterns.cobol", "*.cobol,*.txt");

    LanguageDetection detection = new LanguageDetection(settings, languages);

    assertThat(detection.language(newIndexedFile("abc.abap"))).isEqualTo("abap");
    assertThat(detection.language(newIndexedFile("abc.cobol"))).isEqualTo("cobol");
    try {
      detection.language(newIndexedFile("abc.txt"));
      fail();
    } catch (MessageException e) {
      assertThat(e.getMessage())
        .contains("Language of file 'abc.txt' can not be decided as the file matches patterns of both ")
        .contains("sonar.lang.patterns.abap : *.abap,*.txt")
        .contains("sonar.lang.patterns.cobol : *.cobol,*.txt");
    }
  }

  private DefaultIndexedFile newIndexedFile(String path) throws IOException {
    File basedir = temp.newFolder();
    return new DefaultIndexedFile("foo", basedir.toPath(), path);
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
