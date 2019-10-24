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
package org.sonar.server.language;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.server.plugins.ServerPluginRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LanguageValidationTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void fail_if_conflicting_languages() {
    Language lang1 = mock(Language.class);
    Language lang2 = mock(Language.class);
    when(lang1.getKey()).thenReturn("key");
    when(lang2.getKey()).thenReturn("key");

    ServerPluginRepository repo = mock(ServerPluginRepository.class);
    when(repo.getPluginKey(lang1)).thenReturn("plugin1");
    when(repo.getPluginKey(lang2)).thenReturn("plugin2");

    exception.expect(IllegalStateException.class);
    exception.expectMessage("There are two languages declared with the same key 'key' declared by the plugins 'plugin1' and 'plugin2'. "
      + "Please uninstall one of the conflicting plugins.");
    LanguageValidation languageValidation = new LanguageValidation(repo, lang1, lang2);
    languageValidation.start();
  }

  @Test
  public void succeed_if_no_language() {
    ServerPluginRepository repo = mock(ServerPluginRepository.class);

    LanguageValidation languageValidation = new LanguageValidation(repo);
    languageValidation.start();
    languageValidation.stop();
  }

  @Test
  public void succeed_if_no_duplicated_language() {
    Language lang1 = mock(Language.class);
    Language lang2 = mock(Language.class);
    when(lang1.getKey()).thenReturn("key1");
    when(lang2.getKey()).thenReturn("key2");

    ServerPluginRepository repo = mock(ServerPluginRepository.class);
    when(repo.getPluginKey(lang1)).thenReturn("plugin1");
    when(repo.getPluginKey(lang2)).thenReturn("plugin2");

    LanguageValidation languageValidation = new LanguageValidation(repo);
    languageValidation.start();
    languageValidation.stop();
  }
}
