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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LanguageTest {
  @Test
  public void hashCode_and_equals_depends_on_key() {
    Language lang1 = new Language(mockApiLanguage("key1", "name1", true, new String[] {"f1"}, new String[0]));
    Language lang2 = new Language(mockApiLanguage("key1", "name2", false, new String[] {"f2"}, new String[0]));
    Language lang3 = new Language(mockApiLanguage("key2", "name1", true, new String[] {"f1"}, new String[0]));
    assertThat(lang1)
      .hasSameHashCodeAs(lang2)
      .doesNotHaveSameHashCodeAs(lang3);
    assertThat(lang2).doesNotHaveSameHashCodeAs(lang3);

    assertThat(lang1)
      .isEqualTo(lang2)
      .isNotEqualTo(lang3);
    assertThat(lang2).isNotEqualTo(lang3);
  }

  @Test
  public void getters_match_constructor() {
    Language lang1 = new Language(mockApiLanguage("key1", "name1", true, new String[] {"f1"}, new String[] {"p1"}));
    assertThat(lang1.key()).isEqualTo("key1");
    assertThat(lang1.name()).isEqualTo("name1");
    assertThat(lang1.isPublishAllFiles()).isTrue();
    assertThat(lang1.fileSuffixes()).containsOnly("f1");
    assertThat(lang1.filenamePatterns()).containsOnly("p1");
  }

  private org.sonar.api.resources.Language mockApiLanguage(String key, String name, boolean publishAllFiles, String[] fileSuffixes, String[] filenamePatterns) {
    org.sonar.api.resources.Language mock = mock(org.sonar.api.resources.Language.class);
    when(mock.getKey()).thenReturn(key);
    when(mock.getName()).thenReturn(name);
    when(mock.publishAllFiles()).thenReturn(publishAllFiles);
    when(mock.getFileSuffixes()).thenReturn(fileSuffixes);
    when(mock.filenamePatterns()).thenReturn(filenamePatterns);
    return mock;
  }
}
