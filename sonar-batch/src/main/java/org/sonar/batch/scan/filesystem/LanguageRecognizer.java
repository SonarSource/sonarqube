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

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.utils.SonarException;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detect language of source files.
 */
public class LanguageRecognizer implements BatchComponent, Startable {

  private static final Logger LOG = LoggerFactory.getLogger(LanguageRecognizer.class);

  private final Languages languages;

  /**
   * Lower-case extension -> languages
   */
  private Map<String, PathPattern[]> patternByLanguage = Maps.newLinkedHashMap();

  private Settings settings;

  public LanguageRecognizer(Settings settings, Languages languages) {
    this.settings = settings;
    this.languages = languages;
  }

  @Override
  public void start() {
    for (Language language : languages.all()) {
      String[] filePatterns = settings.getStringArray(getFileLangPatternPropKey(language.getKey()));
      PathPattern[] pathPatterns = PathPattern.create(filePatterns);
      if (pathPatterns.length > 0) {
        patternByLanguage.put(language.getKey(), pathPatterns);
      } else if (language.getFileSuffixes().length > 0) {
        // If no custom language pattern is defined then fallback to suffixes declared by language
        String[] patterns = language.getFileSuffixes();
        for (int i = 0; i < patterns.length; i++) {
          String suffix = patterns[i];
          String extension = sanitizeExtension(suffix);
          patterns[i] = "**/*." + extension;
        }
        PathPattern[] defaultLanguagePatterns = PathPattern.create(patterns);
        patternByLanguage.put(language.getKey(), defaultLanguagePatterns);
        LOG.debug("Declared extensions of language " + language + " were converted to " + getDetails(language.getKey()));
      }
    }
  }

  private String getFileLangPatternPropKey(String languageKey) {
    return "sonar.lang.patterns." + languageKey;
  }

  @Override
  public void stop() {
    // do nothing
  }

  @CheckForNull
  String of(InputFile inputFile) {
    String deprecatedLanguageParam = settings.getString(CoreProperties.PROJECT_LANGUAGE_PROPERTY);

    // First try with lang patterns
    List<String> languagesToConsider = new ArrayList<String>();
    if (!StringUtils.isBlank(deprecatedLanguageParam)) {
      languagesToConsider.add(deprecatedLanguageParam);
    } else {
      languagesToConsider.addAll(patternByLanguage.keySet());
    }
    String detectedLanguage = null;
    for (String languageKey : languagesToConsider) {
      PathPattern[] patterns = patternByLanguage.get(languageKey);
      if (patterns != null) {
        for (PathPattern pathPattern : patterns) {
          if (pathPattern.match(inputFile, false)) {
            if (detectedLanguage == null) {
              detectedLanguage = languageKey;
              break;
            } else {
              // Language was already forced by another pattern
              throw new SonarException("Language of file '" + inputFile.path() + "' can not be decided as the file matches patterns of both " + getDetails(detectedLanguage)
                + " and " + getDetails(languageKey));
            }
          }
        }
      }
    }
    if (detectedLanguage != null) {
      LOG.debug("Language of file '" + inputFile.path() + "' was detected to be '" + detectedLanguage + "'");
      return detectedLanguage;
    }

    // Check if deprecated sonar.language is used and we are on a language without declared extensions
    if (StringUtils.isNotBlank(deprecatedLanguageParam)) {
      Language language = languages.get(deprecatedLanguageParam);
      if (language == null) {
        throw new SonarException("No language is installed with key '" + deprecatedLanguageParam + "'. Please update property '" + CoreProperties.PROJECT_LANGUAGE_PROPERTY + "'");
      }
      // Languages without declared suffixes match everything
      String[] fileSuffixes = language.getFileSuffixes();
      if (fileSuffixes.length == 0) {
        return deprecatedLanguageParam;
      }
      return null;
    }

    return null;
  }

  private String getDetails(String detectedLanguage) {
    return getFileLangPatternPropKey(detectedLanguage) + " : " + Joiner.on(",").join(patternByLanguage.get(detectedLanguage));
  }

  static String sanitizeExtension(String suffix) {
    return StringUtils.lowerCase(StringUtils.removeStart(suffix, "."));
  }
}
