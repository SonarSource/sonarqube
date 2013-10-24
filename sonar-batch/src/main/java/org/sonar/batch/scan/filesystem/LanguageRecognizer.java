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

  private final Project project;
  private final Language[] languages;

  /**
   * Lower-case extension -> languages
   */
  private SetMultimap<String, String> langsByExtension = HashMultimap.create();

  /**
   * Some plugins, like web and cobol, can analyze all the source files, whatever
   * their file extension. This behavior is kept for backward-compatibility,
   * but it should be fixed with future multi-language support.
   */
  private boolean ignoreFileExtension = false;

  public LanguageRecognizer(Project project, Language[] languages) {
    this.project = project;
    this.languages = languages;
  }

  /**
   * When no language plugin is installed
   */
  public LanguageRecognizer(Project project) {
    this(project, new Language[0]);
  }

  @Override
  public void start() {
    for (Language language : languages) {
      if (language.getFileSuffixes().length == 0 && language.getKey().equals(project.getLanguageKey())) {
        ignoreFileExtension = true;

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
    if (ignoreFileExtension) {
      return project.getLanguageKey();
    }
    // multi-language is not supported yet. Filter on project language
    String extension = sanitizeExtension(FilenameUtils.getExtension(file.getName()));
    Set<String> langs = langsByExtension.get(extension);
    return langs.contains(project.getLanguageKey()) ? project.getLanguageKey() : null;
  }

  static String sanitizeExtension(String suffix) {
    return StringUtils.lowerCase(StringUtils.removeStart(suffix, "."));
  }
}
