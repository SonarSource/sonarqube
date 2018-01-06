/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.language;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.api.resources.Language;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Maps.uniqueIndex;
import static java.util.Arrays.asList;

/**
 * Implementation of {@link LanguageRepository} which find {@link Language} instances available in the container.
 */
public class LanguageRepositoryImpl implements LanguageRepository {

  private final Map<String, Language> languagesByKey;

  public LanguageRepositoryImpl() {
    this.languagesByKey = Collections.emptyMap();
  }

  public LanguageRepositoryImpl(Language... languages) {
    this.languagesByKey = uniqueIndex(filter(asList(languages), notNull()), LanguageToKey.INSTANCE);
  }

  @Override
  public Optional<Language> find(String languageKey) {
    return Optional.fromNullable(languagesByKey.get(languageKey));
  }

  private enum LanguageToKey implements Function<Language, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull Language input) {
      return input.getKey();
    }
  }
}
