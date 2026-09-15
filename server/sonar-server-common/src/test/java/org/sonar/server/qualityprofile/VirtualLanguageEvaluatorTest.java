/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.qualityprofile;

import org.junit.Test;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.server.language.LanguageTesting;

import static org.assertj.core.api.Assertions.assertThat;

public class VirtualLanguageEvaluatorTest {

  @Test
  public void isVirtual_returns_true_when_language_has_no_suffixes_and_no_patterns() {
    Language secrets = LanguageTesting.newLanguage("secrets");

    assertThat(VirtualLanguageEvaluator.isVirtual(secrets)).isTrue();
  }

  @Test
  public void isVirtual_returns_false_when_language_has_both_suffixes_and_patterns() {
    Language mixed = newLanguageWithSuffixesAndPatterns("mixed", new String[] {"mx"}, new String[] {"Mixedfile"});

    assertThat(VirtualLanguageEvaluator.isVirtual(mixed)).isFalse();
  }

  @Test
  public void isVirtual_returns_false_when_language_has_file_suffixes() {
    Language java = LanguageTesting.newLanguage("java", "Java", "java");

    assertThat(VirtualLanguageEvaluator.isVirtual(java)).isFalse();
  }

  @Test
  public void isVirtual_returns_false_when_language_has_filename_patterns_but_no_suffixes() {
    Language docker = newLanguageWithPatterns("docker", "Dockerfile");

    assertThat(VirtualLanguageEvaluator.isVirtual(docker)).isFalse();
  }

  @Test
  public void isVirtual_returns_false_when_language_is_null() {
    assertThat(VirtualLanguageEvaluator.isVirtual(null)).isFalse();
  }

  @Test
  public void isVirtual_returns_false_for_cobol_even_when_it_has_no_suffixes_or_patterns() {
    Language cobol = LanguageTesting.newLanguage("cobol");

    assertThat(VirtualLanguageEvaluator.isVirtual(cobol)).isFalse();
  }

  @Test
  public void isVirtual_does_not_throw_when_language_returns_null_suffixes_or_patterns() {
    Language secrets = newLanguageWithNullArrays("secrets");

    assertThat(VirtualLanguageEvaluator.isVirtual(secrets)).isTrue();
  }

  private static AbstractLanguage newLanguageWithSuffixesAndPatterns(String key, String[] suffixes, String[] patterns) {
    return new AbstractLanguage(key) {
      @Override
      public String[] getFileSuffixes() {
        return suffixes;
      }

      @Override
      public String[] filenamePatterns() {
        return patterns;
      }
    };
  }

  private static AbstractLanguage newLanguageWithPatterns(String key, String... patterns) {
    return newLanguageWithSuffixesAndPatterns(key, new String[0], patterns);
  }

  private static AbstractLanguage newLanguageWithNullArrays(String key) {
    return new AbstractLanguage(key) {
      @Override
      public String[] getFileSuffixes() {
        return null;
      }

      @Override
      public String[] filenamePatterns() {
        return null;
      }
    };
  }
}
