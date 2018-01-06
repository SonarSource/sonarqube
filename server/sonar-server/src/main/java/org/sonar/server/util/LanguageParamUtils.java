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
package org.sonar.server.util;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;

public class LanguageParamUtils {

  private LanguageParamUtils() {
    // Utility class
  }

  public static String getExampleValue(Languages languages) {
    Language[] languageArray = languages.all();
    if (languageArray.length > 0) {
      return languageArray[0].getKey();
    } else {
      return "";
    }
  }

  public static Collection<String> getLanguageKeys(Languages languages) {
    return Collections2.transform(Arrays.asList(languages.all()), LanguageToKeyFunction.INSTANCE);
  }

  private enum LanguageToKeyFunction implements Function<Language, java.lang.String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull Language input) {
      return input.getKey();
    }
  }
}
