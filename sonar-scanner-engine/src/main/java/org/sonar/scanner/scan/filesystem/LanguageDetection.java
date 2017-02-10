/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import com.google.common.base.Joiner;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.PathPattern;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.repository.language.Language;
import org.sonar.scanner.repository.language.LanguagesRepository;

/**
 * Detect language of a source file based on its suffix and configured patterns.
 */
@ScannerSide
public class LanguageDetection {

  private static final Logger LOG = LoggerFactory.getLogger(LanguageDetection.class);

  /**
   * Lower-case extension -> languages
   */
  private final Map<String, PathPattern[]> patternsByLanguage = new LinkedHashMap<>();
  private final List<String> languagesToConsider = new ArrayList<>();
  private final String forcedLanguage;

  public LanguageDetection(Settings settings, LanguagesRepository languages) {
    for (Language language : languages.all()) {
      String[] filePatterns = settings.getStringArray(getFileLangPatternPropKey(language.key()));
      PathPattern[] pathPatterns = PathPattern.create(filePatterns);
      if (pathPatterns.length > 0) {
        patternsByLanguage.put(language.key(), pathPatterns);
      } else {
        // If no custom language pattern is defined then fallback to suffixes declared by language
        String[] patterns = language.fileSuffixes().toArray(new String[language.fileSuffixes().size()]);
        for (int i = 0; i < patterns.length; i++) {
          String suffix = patterns[i];
          String extension = sanitizeExtension(suffix);
          patterns[i] = new StringBuilder().append("**/*.").append(extension).toString();
        }
        PathPattern[] defaultLanguagePatterns = PathPattern.create(patterns);
        patternsByLanguage.put(language.key(), defaultLanguagePatterns);
        LOG.debug("Declared extensions of language {} were converted to {}", language, getDetails(language.key()));
      }
    }

    forcedLanguage = StringUtils.defaultIfBlank(settings.getString(CoreProperties.PROJECT_LANGUAGE_PROPERTY), null);
    // First try with lang patterns
    if (forcedLanguage != null) {
      if (!patternsByLanguage.containsKey(forcedLanguage)) {
        throw MessageException.of("You must install a plugin that supports the language '" + forcedLanguage + "'");
      }
      LOG.info("Language is forced to {}", forcedLanguage);
      languagesToConsider.add(forcedLanguage);
    } else {
      languagesToConsider.addAll(patternsByLanguage.keySet());
    }
  }

  public String forcedLanguage() {
    return forcedLanguage;
  }

  Map<String, PathPattern[]> patternsByLanguage() {
    return patternsByLanguage;
  }

  @CheckForNull
  String language(DefaultIndexedFile inputFile) {
    String detectedLanguage = null;
    for (String languageKey : languagesToConsider) {
      if (isCandidateForLanguage(inputFile, languageKey)) {
        if (detectedLanguage == null) {
          detectedLanguage = languageKey;
        } else {
          // Language was already forced by another pattern
          throw MessageException.of(MessageFormat.format("Language of file ''{0}'' can not be decided as the file matches patterns of both {1} and {2}",
            inputFile.relativePath(), getDetails(detectedLanguage), getDetails(languageKey)));
        }
      }
    }
    if (detectedLanguage != null) {
      return detectedLanguage;
    }

    // Check if deprecated sonar.language is used and we are on a language without declared extensions.
    // Languages without declared suffixes match everything.
    if (forcedLanguage != null && patternsByLanguage.get(forcedLanguage).length == 0) {
      return forcedLanguage;
    }
    return null;
  }

  private boolean isCandidateForLanguage(DefaultIndexedFile inputFile, String languageKey) {
    PathPattern[] patterns = patternsByLanguage.get(languageKey);
    if (patterns != null) {
      for (PathPattern pathPattern : patterns) {
        if (pathPattern.match(inputFile, false)) {
          return true;
        }
      }
    }
    return false;
  }

  private static String getFileLangPatternPropKey(String languageKey) {
    return "sonar.lang.patterns." + languageKey;
  }

  private String getDetails(String detectedLanguage) {
    return getFileLangPatternPropKey(detectedLanguage) + " : " + Joiner.on(",").join(patternsByLanguage.get(detectedLanguage));
  }

  static String sanitizeExtension(String suffix) {
    return StringUtils.lowerCase(StringUtils.removeStart(suffix, "."));
  }
}
