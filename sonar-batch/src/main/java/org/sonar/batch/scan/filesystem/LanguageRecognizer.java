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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.scan.filesystem.internal.InputFile;
import org.sonar.api.utils.SonarException;

import javax.annotation.CheckForNull;

import java.util.Map;
import java.util.Set;

/**
 * Detect language of source files.
 */
public class LanguageRecognizer implements BatchComponent, Startable {

  private static final Logger LOG = LoggerFactory.getLogger(LanguageRecognizer.class);

  private final Languages languages;

  /**
   * Lower-case extension -> languages
   */
  private SetMultimap<String, String> langsByExtension = HashMultimap.create();
  private Map<String, PathPattern[]> patternByLanguage = Maps.newLinkedHashMap();

  private Settings settings;

  public LanguageRecognizer(Settings settings, Languages languages) {
    this.settings = settings;
    this.languages = languages;
  }

  @Override
  public void start() {
    for (Language language : languages.all()) {
      for (String suffix : language.getFileSuffixes()) {
        String extension = sanitizeExtension(suffix);
        langsByExtension.put(extension, language.getKey());
      }
      String[] filePatterns = settings.getStringArray(getFilePatternPropKey(language.getKey()));
      PathPattern[] pathPatterns = PathPattern.create(filePatterns);
      if (pathPatterns.length > 0) {
        patternByLanguage.put(language.getKey(), pathPatterns);
      }
    }
  }

  private String getFilePatternPropKey(String languageKey) {
    return "sonar." + languageKey + ".filePatterns";
  }

  @Override
  public void stop() {
    // do nothing
  }

  @CheckForNull
  String of(InputFile inputFile) {
    // First try with patterns
    String forcedLanguage = null;
    for (Map.Entry<String, PathPattern[]> languagePattern : patternByLanguage.entrySet()) {
      PathPattern[] patterns = languagePattern.getValue();
      for (PathPattern pathPattern : patterns) {
        if (pathPattern.match(inputFile)) {
          if (forcedLanguage == null) {
            forcedLanguage = languagePattern.getKey();
            break;
          } else {
            // Language was already forced by another pattern
            throw new SonarException("Language of file '" + inputFile.path() + "' can not be decided as the file matches patterns of both " + getFilePatternPropKey(forcedLanguage)
              + " and "
              + getFilePatternPropKey(languagePattern.getKey()));
          }
        }
      }
    }
    if (forcedLanguage != null) {
      LOG.debug("Language of file '" + inputFile.path() + "' was forced to '" + forcedLanguage + "'");
      return forcedLanguage;
    }

    String extension = sanitizeExtension(FilenameUtils.getExtension(inputFile.file().getName()));

    // Check if deprecated sonar.language is used
    String languageKey = settings.getString(CoreProperties.PROJECT_LANGUAGE_PROPERTY);
    if (StringUtils.isNotBlank(languageKey)) {
      Language language = languages.get(languageKey);
      if (language == null) {
        throw new SonarException("No language is installed with key '" + languageKey + "'. Please update property '" + CoreProperties.PROJECT_LANGUAGE_PROPERTY + "'");
      }
      // Languages without declared suffixes match everything
      String[] fileSuffixes = language.getFileSuffixes();
      if (fileSuffixes.length == 0) {
        return languageKey;
      }
      for (String fileSuffix : fileSuffixes) {
        if (sanitizeExtension(fileSuffix).equals(extension)) {
          return languageKey;
        }
      }
      return null;
    }

    // At this point use extension to detect language
    Set<String> langs = langsByExtension.get(extension);
    if (langs.size() > 1) {
      throw new SonarException("Language of file '" + inputFile.path() + "' can not be decided as the file extension '" + extension + "' is declared by several languages: "
        + StringUtils.join(langs, ", "));
    }
    return langs.isEmpty() ? null : langs.iterator().next();
  }

  static String sanitizeExtension(String suffix) {
    return StringUtils.lowerCase(StringUtils.removeStart(suffix, "."));
  }
}
