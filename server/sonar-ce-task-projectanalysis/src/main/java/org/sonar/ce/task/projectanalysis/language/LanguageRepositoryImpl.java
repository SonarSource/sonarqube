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
package org.sonar.ce.task.projectanalysis.language;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Language;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Implementation of {@link LanguageRepository} which find {@link Language} instances available in the container.
 */
public class LanguageRepositoryImpl implements LanguageRepository {

  private static final Logger LOG = LoggerFactory.getLogger(LanguageRepositoryImpl.class);

  private final Map<String, Language> languagesByKey;

  @Autowired
  public LanguageRepositoryImpl(ConfigurableListableBeanFactory beanFactory) {
    Map<String, Language> map = new HashMap<>();
    for (String beanName : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Language.class, false, true)) {
      try {
        Language language = beanFactory.getBean(beanName, Language.class);
        Language previous = map.putIfAbsent(language.getKey(), language);
        if (previous != null) {
          LOG.warn("Duplicate language key '{}': keeping the first bean registered, ignoring '{}'", language.getKey(), beanName);
        }
      } catch (BeansException e) {
        // Set to debug to avoid noisy program stack trace with errors
        LOG.debug("Skipping language bean '{}': unable to build in this container", beanName, e);
      }
    }
    this.languagesByKey = map;
  }

  public LanguageRepositoryImpl(Language... languages) {
    this.languagesByKey = Arrays.stream(languages).filter(Objects::nonNull).collect(Collectors.toMap(Language::getKey, Function.identity()));
  }

  @Override
  public Optional<Language> find(String languageKey) {
    return Optional.ofNullable(languagesByKey.get(languageKey));
  }
}
