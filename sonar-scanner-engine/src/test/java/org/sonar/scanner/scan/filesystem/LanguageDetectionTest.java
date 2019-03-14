/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
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

  private MapSettings settings;

  @Before
  public void setUp() throws IOException {
    settings = new MapSettings();
  }

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
    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);

    assertThat(detectLanguage(detection, "Foo.java")).isEqualTo("java");
    assertThat(detectLanguage(detection, "src/Foo.java")).isEqualTo("java");
    assertThat(detectLanguage(detection, "Foo.JAVA")).isEqualTo("java");
    assertThat(detectLanguage(detection, "Foo.jav")).isEqualTo("java");
    assertThat(detectLanguage(detection, "Foo.Jav")).isEqualTo("java");

    assertThat(detectLanguage(detection, "abc.cbl")).isEqualTo("cobol");
    assertThat(detectLanguage(detection, "abc.CBL")).isEqualTo("cobol");

    assertThat(detectLanguage(detection, "abc.php")).isNull();
    assertThat(detectLanguage(detection, "abc")).isNull();
  }

  @Test
  public void should_not_fail_if_no_language() throws Exception {
    LanguageDetection detection = spy(new LanguageDetection(settings.asConfig(), new DefaultLanguagesRepository(new Languages())));
    assertThat(detectLanguage(detection, "Foo.java")).isNull();
  }

  @Test
  public void plugin_can_declare_a_file_extension_twice_for_case_sensitivity() throws Exception {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("abap", "abap", "ABAP")));

    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);
    assertThat(detectLanguage(detection, "abc.abap")).isEqualTo("abap");
  }

  @Test
  public void fail_if_conflicting_language_suffix() throws Exception {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("xml", "xhtml"), new MockLanguage("web", "xhtml")));
    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);
    try {
      detectLanguage(detection, "abc.xhtml");
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

    settings.setProperty("sonar.lang.patterns.xml", "xml/**");
    settings.setProperty("sonar.lang.patterns.web", "web/**");
    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);
    assertThat(detectLanguage(detection, "xml/abc.xhtml")).isEqualTo("xml");
    assertThat(detectLanguage(detection, "web/abc.xhtml")).isEqualTo("web");
  }

  @Test
  public void fail_if_conflicting_filepattern() throws Exception {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("abap", "abap"), new MockLanguage("cobol", "cobol")));
    settings.setProperty("sonar.lang.patterns.abap", "*.abap,*.txt");
    settings.setProperty("sonar.lang.patterns.cobol", "*.cobol,*.txt");

    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);

    assertThat(detectLanguage(detection, "abc.abap")).isEqualTo("abap");
    assertThat(detectLanguage(detection, "abc.cobol")).isEqualTo("cobol");
    try {
      detectLanguage(detection, "abc.txt");
      fail();
    } catch (MessageException e) {
      assertThat(e.getMessage())
        .contains("Language of file 'abc.txt' can not be decided as the file matches patterns of both ")
        .contains("sonar.lang.patterns.abap : *.abap,*.txt")
        .contains("sonar.lang.patterns.cobol : *.cobol,*.txt");
    }
  }

  private String detectLanguage(LanguageDetection detection, String path) {
    return detection.language(new File(temp.getRoot(), path).toPath(), Paths.get(path));
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
