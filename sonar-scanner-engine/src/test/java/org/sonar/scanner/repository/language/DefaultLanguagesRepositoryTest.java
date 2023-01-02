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
package org.sonar.scanner.repository.language;

import org.junit.Test;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultLanguagesRepositoryTest {
  private final Languages languages = mock(Languages.class);
  private final DefaultLanguagesRepository underTest = new DefaultLanguagesRepository(languages);

  @Test
  public void returns_all_languages() {
    when(languages.all()).thenReturn(new Language[] {new TestLanguage("k1", true), new TestLanguage("k2", false)});
    assertThat(underTest.all())
      .extracting("key", "name", "fileSuffixes", "publishAllFiles")
      .containsOnly(
        tuple("k1", "name k1", new String[] {"k1"}, true),
        tuple("k2", "name k2", new String[] {"k2"}, false)
      );
  }

  @Test
  public void publishAllFiles_by_default() {
    when(languages.all()).thenReturn(new Language[] {new TestLanguage2("k1"), new TestLanguage2("k2")});
    assertThat(underTest.all())
      .extracting("key", "name", "fileSuffixes", "publishAllFiles")
      .containsOnly(
        tuple("k1", "name k1", new String[] {"k1"}, true),
        tuple("k2", "name k2", new String[] {"k2"}, true)
      );
  }

  @Test
  public void get_find_language_by_key() {
    when(languages.get("k1")).thenReturn(new TestLanguage2("k1"));
    assertThat(underTest.get("k1"))
      .extracting("key", "name", "fileSuffixes", "publishAllFiles")
      .containsOnly("k1", "name k1", new String[] {"k1"}, true);
  }

  private static class TestLanguage implements Language {
    private final String key;
    private final boolean publishAllFiles;

    public TestLanguage(String key, boolean publishAllFiles) {
      this.key = key;
      this.publishAllFiles = publishAllFiles;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public String getName() {
      return "name " + key;
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[] {key};
    }

    @Override
    public boolean publishAllFiles() {
      return publishAllFiles;
    }
  }

  private static class TestLanguage2 implements Language {
    private final String key;

    public TestLanguage2(String key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public String getName() {
      return "name " + key;
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[] {key};
    }
  }

}
