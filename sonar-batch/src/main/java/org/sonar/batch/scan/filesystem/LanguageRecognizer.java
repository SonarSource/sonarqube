/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan.filesystem;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.resources.Language;

import javax.annotation.CheckForNull;
import java.util.Map;

/**
 * Based on file extensions.
 */
public class LanguageRecognizer implements BatchComponent {

  private final Map<String, String> byExtensions = Maps.newHashMap();

  public LanguageRecognizer(Language[] languages) {
    for (Language language : languages) {
      for (String suffix : language.getFileSuffixes()) {
        String extension = StringUtils.removeStart(suffix, ".");

        String s = byExtensions.get(extension);
        if (s != null) {
          throw new IllegalStateException(String.format(
            "File extension '%s' is declared by two languages: %s and %s", extension, s, language.getKey()
          ));
        }
        byExtensions.put(extension, language.getKey());
      }
    }
  }

  // TODO what about cobol files without extension ?
  @CheckForNull
  String ofExtension(String fileExtension) {
    if (StringUtils.isNotBlank(fileExtension)) {
      return byExtensions.get(fileExtension);
    }
    return null;
  }
}
