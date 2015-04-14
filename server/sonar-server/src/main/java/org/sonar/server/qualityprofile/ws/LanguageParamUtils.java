/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile.ws;

import com.google.common.collect.Collections2;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.core.util.NonNullInputFunction;

import java.util.Arrays;
import java.util.Collection;

class LanguageParamUtils {

  private LanguageParamUtils() {
    // Utility class
  }

  static String getExampleValue(Languages languages) {
    Language[] languageArray = languages.all();
    if (languageArray.length > 0) {
      return languageArray[0].getKey();
    } else {
      return "";
    }
  }

  static Collection<String> getLanguageKeys(Languages languages) {
    return Collections2.transform(Arrays.asList(languages.all()), new NonNullInputFunction<Language, String>() {
      @Override
      public String doApply(Language input) {
        return input.getKey();
      }
    });
  }
}
