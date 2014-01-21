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
package org.sonar.batch.scan.language;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.SonarException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Give access to all languages detected on the current module
 * @since 4.2
 */
public class ModuleLanguages implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(ModuleLanguages.class);

  private Map<String, Language> moduleLanguages = new HashMap<String, Language>();

  private Languages languages;

  public ModuleLanguages(Settings settings, Languages languages) {
    this.languages = languages;
    if (settings.hasKey(CoreProperties.PROJECT_LANGUAGE_PROPERTY)) {
      String languageKey = settings.getString(CoreProperties.PROJECT_LANGUAGE_PROPERTY);
      LOG.info("Language is forced to {}", languageKey);
      addLanguage(languageKey);
    }
  }

  public void addLanguage(String languageKey) {
    Language language = languages.get(languageKey);
    if (language == null) {
      throw new SonarException("Unknow language " + languageKey);
    }
    moduleLanguages.put(languageKey, language);
  }

  public Collection<String> getModuleLanguageKeys() {
    return moduleLanguages.keySet();
  }

  public Collection<Language> getModuleLanguages() {
    return moduleLanguages.values();
  }
}
