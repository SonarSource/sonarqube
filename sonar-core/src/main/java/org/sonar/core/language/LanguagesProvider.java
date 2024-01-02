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
package org.sonar.core.language;

import java.util.List;
import java.util.Optional;

import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.springframework.context.annotation.Bean;

public class LanguagesProvider {
  @Bean("Languages")
  public Languages provide(Optional<List<Language>> languages) {
    if (languages.isPresent()) {
      return new Languages(languages.get().toArray(new Language[0]));
    } else {
      return new Languages();
    }
  }
}
