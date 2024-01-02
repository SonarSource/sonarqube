/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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


  private MapSettings settings;

  @Before
  public void setUp() {
    settings = new MapSettings();
  }

  @Test
  public void test_sanitizeExtension() {
    assertThat(LanguageDetection.sanitizeExtension(".cbl")).isEqualTo("cbl");
    assertThat(LanguageDetection.sanitizeExtension(".CBL")).isEqualTo("cbl");
    assertThat(LanguageDetection.sanitizeExtension("CBL")).isEqualTo("cbl");
    assertThat(LanguageDetection.sanitizeExtension("cbl")).isEqualTo("cbl");
  }

  @Test
  public void search_by_file_extension() {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("java", "java", "jav"), new MockLanguage("cobol", "cbl", "cob")));
    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);

    assertThat(detectLanguageKey(detection, "Foo.java")).isEqualTo("java");
    assertThat(detectLanguageKey(detection, "src/Foo.java")).isEqualTo("java");
    assertThat(detectLanguageKey(detection, "Foo.JAVA")).isEqualTo("java");
    assertThat(detectLanguageKey(detection, "Foo.jav")).isEqualTo("java");
    assertThat(detectLanguageKey(detection, "Foo.Jav")).isEqualTo("java");

    assertThat(detectLanguageKey(detection, "abc.cbl")).isEqualTo("cobol");
    assertThat(detectLanguageKey(detection, "abc.CBL")).isEqualTo("cobol");

    assertThat(detectLanguageKey(detection, "abc.php")).isNull();
    assertThat(detectLanguageKey(detection, "abc")).isNull();
  }

  @Test
  public void should_not_fail_if_no_language() {
    LanguageDetection detection = spy(new LanguageDetection(settings.asConfig(), new DefaultLanguagesRepository(new Languages())));
    assertThat(detectLanguageKey(detection, "Foo.java")).isNull();
  }

  @Test
  public void plugin_can_declare_a_file_extension_twice_for_case_sensitivity() {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("abap", "abap", "ABAP")));

    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);
    assertThat(detectLanguageKey(detection, "abc.abap")).isEqualTo("abap");
  }

  @Test
  public void fail_if_conflicting_language_suffix() {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("xml", "xhtml"), new MockLanguage("web", "xhtml")));
    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);
    try {
      detectLanguageKey(detection, "abc.xhtml");
      fail();
    } catch (MessageException e) {
      assertThat(e.getMessage())
        .contains("Language of file 'abc.xhtml' can not be decided as the file matches patterns of both ")
        .contains("sonar.lang.patterns.web : **/*.xhtml")
        .contains("sonar.lang.patterns.xml : **/*.xhtml");
    }
  }

  @Test
  public void solve_conflict_using_filepattern() {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("xml", "xhtml"), new MockLanguage("web", "xhtml")));

    settings.setProperty("sonar.lang.patterns.xml", "xml/**");
    settings.setProperty("sonar.lang.patterns.web", "web/**");
    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);
    assertThat(detectLanguageKey(detection, "xml/abc.xhtml")).isEqualTo("xml");
    assertThat(detectLanguageKey(detection, "web/abc.xhtml")).isEqualTo("web");
  }

  @Test
  public void fail_if_conflicting_filepattern() {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("abap", "abap"), new MockLanguage("cobol", "cobol")));
    settings.setProperty("sonar.lang.patterns.abap", "*.abap,*.txt");
    settings.setProperty("sonar.lang.patterns.cobol", "*.cobol,*.txt");

    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);

    assertThat(detectLanguageKey(detection, "abc.abap")).isEqualTo("abap");
    assertThat(detectLanguageKey(detection, "abc.cobol")).isEqualTo("cobol");
    try {
      detectLanguageKey(detection, "abc.txt");
      fail();
    } catch (MessageException e) {
      assertThat(e.getMessage())
        .contains("Language of file 'abc.txt' can not be decided as the file matches patterns of both ")
        .contains("sonar.lang.patterns.abap : *.abap,*.txt")
        .contains("sonar.lang.patterns.cobol : *.cobol,*.txt");
    }
  }

  private String detectLanguageKey(LanguageDetection detection, String path) {
    org.sonar.scanner.repository.language.Language language = detection.language(new File(temp.getRoot(), path).toPath(), Paths.get(path));
    return language != null ? language.key() : null;
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

    @Override
    public boolean publishAllFiles() {
      return true;
    }
  }
}
