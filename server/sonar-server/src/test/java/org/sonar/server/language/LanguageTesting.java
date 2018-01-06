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
package org.sonar.server.language;

import com.google.common.collect.Collections2;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.core.util.NonNullInputFunction;

import java.util.Arrays;

public class LanguageTesting {

  public static Language newLanguage(String key, String name, final String... prefixes) {
    return new AbstractLanguage(key, name) {
      @Override
      public String[] getFileSuffixes() {
        return prefixes;
      }
    };
  }

  public static Language newLanguage(String key) {
    return newLanguage(key, StringUtils.capitalize(key));
  }

  public static Languages newLanguages(String... languageKeys) {
    return new Languages(Collections2.transform(Arrays.asList(languageKeys), new NonNullInputFunction<String, Language>() {
      @Override
      protected Language doApply(String languageKey) {
        return newLanguage(languageKey);
      }

    }).toArray(new Language[0]));
  }
}
