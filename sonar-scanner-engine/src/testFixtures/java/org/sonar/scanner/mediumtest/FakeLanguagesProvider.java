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
package org.sonar.scanner.mediumtest;

import javax.annotation.Priority;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.springframework.context.annotation.Bean;

@Priority(1)
public class FakeLanguagesProvider {

  private Languages languages = new Languages();

  @Bean("Languages")
  public Languages provide() {
    return this.languages;
  }

  public void addLanguage(String key, String name, boolean publishAllFiles) {
    this.languages.add(new FakeLanguage(key, name, publishAllFiles));
  }

  private static class FakeLanguage implements Language {

    private final String name;
    private final String key;
    private final boolean publishAllFiles;

    public FakeLanguage(String key, String name, boolean publishAllFiles) {
      this.name = name;
      this.key = key;
      this.publishAllFiles = publishAllFiles;
    }

    @Override
    public String getKey() {
      return this.key;
    }

    @Override
    public String getName() {
      return this.name;
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[0];
    }

    @Override
    public boolean publishAllFiles() {
      return this.publishAllFiles;
    }
  }


}
