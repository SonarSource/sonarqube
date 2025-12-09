/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.core.language;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class LanguagesProviderTest {

  @Test
  public void should_provide_default_instance_when_no_language() {
    LanguagesProvider provider = new LanguagesProvider();
    Languages languages = provider.provide(Optional.empty());

    assertThat(languages).isNotNull();
    assertThat(languages.all()).isEmpty();
  }

  @Test
  public void should_provide_instance_when_languages() {
    Language A = mock(Language.class);
    when(A.getKey()).thenReturn("a");
    Language B = mock(Language.class);
    when(B.getKey()).thenReturn("b");

    LanguagesProvider provider = new LanguagesProvider();
    List<Language> languageList = Arrays.asList(A, B);
    Languages languages = provider.provide(Optional.of(languageList));

    assertThat(languages).isNotNull();
    assertThat(languages.all())
      .hasSize(2)
      .contains(A, B);
  }

}
