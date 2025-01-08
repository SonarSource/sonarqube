/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.Startable;
import org.sonar.api.resources.Languages;

/**
 * Languages repository using {@link Languages}
 * @since 4.4
 */
@Immutable
public class DefaultLanguagesRepository implements LanguagesRepository, Startable {

  private final Map<String, Language> languages = new HashMap<>();
  private final LanguagesLoader languagesLoader;

  public DefaultLanguagesRepository(LanguagesLoader languagesLoader) {
    this.languagesLoader = languagesLoader;
  }

  @Override
  public void start() {
    languages.putAll(languagesLoader.load());
  }

  /**
   * Get language.
   */
  @Override
  @CheckForNull
  public Language get(String languageKey) {
    return languages.get(languageKey);
  }

  /**
   * Get list of all supported languages.
   */
  @Override
  public Collection<Language> all() {
    return languages.values();
  }

  @Override
  public void stop() {
    // nothing to do
  }


}
