/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.repository.language.DefaultLanguagesRepository;
import org.sonar.scanner.repository.language.LanguagesRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;

@RunWith(DataProviderRunner.class)
public class LanguageDetectionTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MapSettings settings;

  @Before
  public void setUp() {
    settings = new MapSettings();
  }

  @Test
  @UseDataProvider("extensionsForSanitization")
  public void sanitizeExtension_shouldRemoveObsoleteCharacters(String extension) {
    assertThat(LanguageDetection.sanitizeExtension(extension)).isEqualTo("cbl");
  }

  @DataProvider
  public static Object[][] extensionsForSanitization() {
    return new Object[][] {
      {".cbl"},
      {".CBL"},
      {"CBL"},
      {"cbl"},
    };
  }

  @Test
  public void detectLanguageKey_shouldDetectByFileExtension() {
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
  @UseDataProvider("filenamePatterns")
  public void detectLanguageKey_shouldDetectByFileNamePattern(String fileName, String expectedLanguageKey) {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(
      new MockLanguage("docker", new String[0], new String[] {"*.dockerfile", "*.Dockerfile", "Dockerfile", "Dockerfile.*"}),
      new MockLanguage("terraform", new String[] {"tf"}, new String[] {".tf"}),
      new MockLanguage("java", new String[0], new String[] {"**/*Test.java"})));
    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);
    assertThat(detectLanguageKey(detection, fileName)).isEqualTo(expectedLanguageKey);
  }

  @DataProvider
  public static Object[][] filenamePatterns() {
    return new Object[][] {
      {"Dockerfile", "docker"},
      {"src/Dockerfile", "docker"},
      {"my.Dockerfile", "docker"},
      {"my.dockerfile", "docker"},
      {"Dockerfile.old", "docker"},
      {"Dockerfile.OLD", "docker"},
      {"DOCKERFILE", null},
      {"infra.tf", "terraform"},
      {"FooTest.java", "java"},
      {"FooTest.JAVA", "java"},
      {"FooTEST.java", null}
    };
  }

  @Test
  public void detectLanguageKey_shouldNotFailIfNoLanguage() {
    LanguageDetection detection = spy(new LanguageDetection(settings.asConfig(), new DefaultLanguagesRepository(new Languages())));
    assertThat(detectLanguageKey(detection, "Foo.java")).isNull();
  }

  @Test
  public void detectLanguageKey_shouldAllowPluginsToDeclareFileExtensionTwiceForCaseSensitivity() {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("abap", "abap", "ABAP")));

    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);
    assertThat(detectLanguageKey(detection, "abc.abap")).isEqualTo("abap");
  }

  @Test
  public void detectLanguageKey_shouldFailIfConflictingLanguageSuffix() {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("xml", "xhtml"), new MockLanguage("web", "xhtml")));
    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);
    assertThatThrownBy(() -> detectLanguageKey(detection, "abc.xhtml"))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("Language of file 'abc.xhtml' can not be decided as the file matches patterns of both ")
      .hasMessageContaining("sonar.lang.patterns.web : **/*.xhtml")
      .hasMessageContaining("sonar.lang.patterns.xml : **/*.xhtml");
  }

  @Test
  public void detectLanguageKey_shouldSolveConflictUsingFilePattern() {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("xml", "xhtml"), new MockLanguage("web", "xhtml")));

    settings.setProperty("sonar.lang.patterns.xml", "xml/**");
    settings.setProperty("sonar.lang.patterns.web", "web/**");
    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);
    assertThat(detectLanguageKey(detection, "xml/abc.xhtml")).isEqualTo("xml");
    assertThat(detectLanguageKey(detection, "web/abc.xhtml")).isEqualTo("web");
  }

  @Test
  public void detectLanguageKey_shouldFailIfConflictingFilePattern() {
    LanguagesRepository languages = new DefaultLanguagesRepository(new Languages(new MockLanguage("abap", "abap"), new MockLanguage("cobol", "cobol")));
    settings.setProperty("sonar.lang.patterns.abap", "*.abap,*.txt");
    settings.setProperty("sonar.lang.patterns.cobol", "*.cobol,*.txt");

    LanguageDetection detection = new LanguageDetection(settings.asConfig(), languages);

    assertThat(detectLanguageKey(detection, "abc.abap")).isEqualTo("abap");
    assertThat(detectLanguageKey(detection, "abc.cobol")).isEqualTo("cobol");

    assertThatThrownBy(() -> detectLanguageKey(detection, "abc.txt"))
      .hasMessageContaining("Language of file 'abc.txt' can not be decided as the file matches patterns of both ")
      .hasMessageContaining("sonar.lang.patterns.abap : *.abap,*.txt")
      .hasMessageContaining("sonar.lang.patterns.cobol : *.cobol,*.txt");
  }

  private String detectLanguageKey(LanguageDetection detection, String path) {
    org.sonar.scanner.repository.language.Language language = detection.language(new File(temp.getRoot(), path).toPath(), Paths.get(path));
    return language != null ? language.key() : null;
  }

  static class MockLanguage implements Language {
    private final String key;
    private final String[] extensions;
    private final String[] filenamePatterns;

    MockLanguage(String key, String... extensions) {
      this.key = key;
      this.extensions = extensions;
      this.filenamePatterns = new String[0];
    }

    MockLanguage(String key, String[] extensions, String[] filenamePatterns) {
      this.key = key;
      this.extensions = extensions;
      this.filenamePatterns = filenamePatterns;
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
    public String[] filenamePatterns() {
      return filenamePatterns;
    }

    @Override
    public boolean publishAllFiles() {
      return true;
    }
  }
}
