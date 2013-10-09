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

import org.junit.Test;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class LanguageRecognizerTest {

  @Test
  public void test_sanitizeExtension() throws Exception {
    assertThat(LanguageRecognizer.sanitizeExtension(".cbl")).isEqualTo("cbl");
    assertThat(LanguageRecognizer.sanitizeExtension(".CBL")).isEqualTo("cbl");
    assertThat(LanguageRecognizer.sanitizeExtension("CBL")).isEqualTo("cbl");
    assertThat(LanguageRecognizer.sanitizeExtension("cbl")).isEqualTo("cbl");
  }

  @Test
  public void search_by_file_extension() throws Exception {
    Language[] languages = new Language[]{Java.INSTANCE, new Cobol()};
    LanguageRecognizer recognizer = new LanguageRecognizer(languages);

    recognizer.start();
    assertThat(recognizer.ofExtension("java")).isEqualTo(Java.KEY);
    assertThat(recognizer.ofExtension("cbl")).isEqualTo("cobol");
    assertThat(recognizer.ofExtension("CBL")).isEqualTo("cobol");
    assertThat(recognizer.ofExtension("php")).isNull();
    assertThat(recognizer.ofExtension("")).isNull();
    assertThat(recognizer.ofExtension(null)).isNull();
    recognizer.stop();
  }

  @Test
  public void fail_if_conflict_of_file_extensions() throws Exception {
    Language[] languages = new Language[]{Java.INSTANCE, new Language() {
      @Override
      public String getKey() {
        return "java2";
      }

      @Override
      public String getName() {
        return "Java2";
      }

      @Override
      public String[] getFileSuffixes() {
        return new String[]{"java2", "java"};
      }
    }};

    try {
      new LanguageRecognizer(languages).start();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("File extension 'java' is declared by two languages: java and java2");
    }
  }

  static class Cobol implements Language {
    @Override
    public String getKey() {
      return "cobol";
    }

    @Override
    public String getName() {
      return "Cobol";
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[]{"cbl", "cob"};
    }
  }
}
