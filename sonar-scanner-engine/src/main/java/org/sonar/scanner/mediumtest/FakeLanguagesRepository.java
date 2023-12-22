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
package org.sonar.scanner.mediumtest;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Priority;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.resources.Languages;
import org.sonar.scanner.repository.language.Language;
import org.sonar.scanner.repository.language.LanguagesRepository;
import org.sonar.scanner.repository.language.SupportedLanguageDto;

@Priority(1)
public class FakeLanguagesRepository implements LanguagesRepository {

  private final Map<String, Language> languageMap = new HashMap<>();

  public FakeLanguagesRepository() {
    languageMap.put("xoo", new Language(new FakeLanguage("xoo", "xoo", new String[] { ".xoo" }, new String[0], true)));
  }

  public FakeLanguagesRepository(Languages languages) {
    for (org.sonar.api.resources.Language language : languages.all()) {
      languageMap.put(language.getKey(), new Language(new FakeLanguage(language.getKey(), language.getName(), language.getFileSuffixes(), language.filenamePatterns(), true)));
    }
  }

  @Nullable
  @Override
  public Language get(String languageKey) {
    return languageMap.get(languageKey);
  }

  @Override
  public Collection<Language> all() {
    return languageMap.values().stream()
      // sorted for test consistency
      .sorted(Comparator.comparing(Language::key)).toList();
  }

  public void addLanguage(String key, String name, String[] suffixes, String[] filenamePatterns) {
    languageMap.put(key, new Language(new FakeLanguage(key, name, suffixes, filenamePatterns, true)));
  }

  public void addLanguage(String key, String name, String[] suffixes, String[] filenamePatterns, boolean publishAllFiles) {
    languageMap.put(key, new Language(new FakeLanguage(key, name, suffixes, filenamePatterns, publishAllFiles)));
  }

  private static class FakeLanguage extends SupportedLanguageDto {

    private final boolean publishAllFiles;

    public FakeLanguage(String key, String name, String[] fileSuffixes, String[] filenamePatterns, boolean publishAllFiles) {
      super(key, name, fileSuffixes, filenamePatterns);
      this.publishAllFiles = publishAllFiles;
    }

    @Override
    public boolean publishAllFiles() {
      return publishAllFiles;
    }
  }
}
