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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.BatchComponent;
import org.sonar.api.resources.Language;

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.Map;

/**
 * Detect language of source files. Simplistic, based on file extensions.
 */
public class LanguageRecognizer implements BatchComponent, Startable {

  /**
   * Lower-case extension -> language
   */
  private Map<String, String> byExtensions = Maps.newHashMap();

  private final Language[] languages;

  public LanguageRecognizer(Language[] languages) {
    this.languages = languages;
  }

  @Override
  public void start() {
    for (Language language : languages) {
      for (String suffix : language.getFileSuffixes()) {
        String extension = sanitizeExtension(suffix);

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

  @Override
  public void stop() {
    // do nothing
  }

  // TODO what about cobol files without extension ?
  @CheckForNull
  String of(File file) {
    String extension = FilenameUtils.getExtension(file.getName());
    if (StringUtils.isNotBlank(extension)) {
      return byExtensions.get(StringUtils.lowerCase(extension));
    }
    return null;
  }

  static String sanitizeExtension(String suffix) {
    return StringUtils.lowerCase(StringUtils.removeStart(suffix, "."));
  }
}
