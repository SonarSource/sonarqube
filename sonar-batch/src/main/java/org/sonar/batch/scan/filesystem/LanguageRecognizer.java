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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.Set;

/**
 * Detect language of source files. Simplistic, based on file extensions.
 */
public class LanguageRecognizer implements BatchComponent, Startable {

  static final String NO_EXTENSION = "<UNSET>";

  private final Project project;
  private final Language[] languages;

  /**
   * Lower-case extension -> languages
   */
  private SetMultimap<String, String> langsByExtension = HashMultimap.create();

  public LanguageRecognizer(Project project, Language[] languages) {
    this.project = project;
    this.languages = languages;
  }

  @Override
  public void start() {
    for (Language language : languages) {
      if (language.getFileSuffixes().length == 0) {
        langsByExtension.put(NO_EXTENSION, language.getKey());

      } else {
        for (String suffix : language.getFileSuffixes()) {
          String extension = sanitizeExtension(suffix);
          langsByExtension.put(extension, language.getKey());
        }
      }
    }

    for (String extension : langsByExtension.keySet()) {
      Set<String> langs = langsByExtension.get(extension);
      if (langs.size() > 1) {
        warnConflict(extension, langs);
      }
    }
  }

  @VisibleForTesting
  void warnConflict(String extension, Set<String> langs) {
    LoggerFactory.getLogger(LanguageRecognizer.class).warn(String.format(
      "File extension '%s' is declared by several plugins: %s", extension, StringUtils.join(langs, ", ")
    ));
  }

  @Override
  public void stop() {
    // do nothing
  }

  @CheckForNull
  String of(File file) {
    // multi-language is not supported yet. Filter on project language
    String extension = sanitizeExtension(FilenameUtils.getExtension(file.getName()));
    extension = StringUtils.defaultIfBlank(extension, NO_EXTENSION);
    Set<String> langs = langsByExtension.get(extension);
    return langs.contains(project.getLanguageKey()) ? project.getLanguageKey() : null;
  }

  static String sanitizeExtension(String suffix) {
    return StringUtils.lowerCase(StringUtils.removeStart(suffix, "."));
  }
}
