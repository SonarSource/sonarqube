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

import java.util.Optional;
import org.junit.Test;
import org.sonar.api.resources.Language;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LanguageRepositoryImplTest {

  private static final String ANY_KEY = "Any_Key";
  private static final String SOME_LANGUAGE_KEY = "SoMe language_Key";
  private static final Language SOME_LANGUAGE = createLanguage(SOME_LANGUAGE_KEY, "_name");

  @Test
  public void constructor_fails_is_language_have_the_same_key() {
    assertThatThrownBy(() -> new LanguageRepositoryImpl(createLanguage(SOME_LANGUAGE_KEY, " 1"), createLanguage(SOME_LANGUAGE_KEY, " 2")))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void find_on_empty_LanguageRepository_returns_absent() {
    assertThat(new LanguageRepositoryImpl().find(ANY_KEY)).isEmpty();
  }

  @Test
  public void find_by_key_returns_the_same_object() {
    LanguageRepositoryImpl languageRepository = new LanguageRepositoryImpl(SOME_LANGUAGE);
    Optional<Language> language = languageRepository.find(SOME_LANGUAGE_KEY);
    assertThat(language)
      .isPresent()
      .containsSame(SOME_LANGUAGE);
  }

  @Test
  public void find_by_other_key_returns_absent() {
    LanguageRepositoryImpl languageRepository = new LanguageRepositoryImpl(SOME_LANGUAGE);
    Optional<Language> language = languageRepository.find(ANY_KEY);
    assertThat(language).isEmpty();
  }

  @Test
  public void find_skips_language_beans_that_fail_to_build_and_still_returns_the_others() {
    ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
    when(beanFactory.getBeanNamesForType(Language.class, false, true)).thenReturn(new String[] {"broken", "ok"});
    when(beanFactory.getBean("broken", Language.class)).thenThrow(new NoSuchBeanDefinitionException("SomeMissingMetadata"));
    when(beanFactory.getBean("ok", Language.class)).thenReturn(SOME_LANGUAGE);

    LanguageRepositoryImpl languageRepository = new LanguageRepositoryImpl(beanFactory);

    verify(beanFactory).getBean("broken", Language.class);
    assertThat(languageRepository.find(SOME_LANGUAGE_KEY)).contains(SOME_LANGUAGE);
  }

  @Test
  public void find_keeps_first_language_when_two_beans_share_the_same_language_key() {
    Language first = createLanguage(SOME_LANGUAGE_KEY, "_first");
    Language duplicate = createLanguage(SOME_LANGUAGE_KEY, "_duplicate");
    ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
    when(beanFactory.getBeanNamesForType(Language.class, false, true)).thenReturn(new String[] {"first", "duplicate"});
    when(beanFactory.getBean("first", Language.class)).thenReturn(first);
    when(beanFactory.getBean("duplicate", Language.class)).thenReturn(duplicate);

    LanguageRepositoryImpl languageRepository = new LanguageRepositoryImpl(beanFactory);

    assertThat(languageRepository.find(SOME_LANGUAGE_KEY)).contains(first);
  }

  private static Language createLanguage(final String key, final String nameSuffix) {
    return new Language() {
      @Override
      public String getKey() {
        return key;
      }

      @Override
      public String getName() {
        return key + nameSuffix;
      }

      @Override
      public String[] getFileSuffixes() {
        return new String[0];
      }

      @Override
      public boolean publishAllFiles() {
        return true;
      }
    };
  }
}
