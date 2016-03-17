/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.phases;

import org.sonar.api.batch.BatchSide;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.MessageException;

/**
 * Should be dropped when org.sonar.api.resources.Project is fully refactored.
 */
@BatchSide
public class ProjectInitializer {

  private Languages languages;
  private Settings settings;

  public ProjectInitializer(Settings settings, Languages languages) {
    this.settings = settings;
    this.languages = languages;
  }

  public void execute(Project project) {
    if (project.getLanguage() == null) {
      initDeprecatedLanguage(project);
    }
  }

  private void initDeprecatedLanguage(Project project) {
    String languageKey = settings.getString(CoreProperties.PROJECT_LANGUAGE_PROPERTY);
    if (StringUtils.isNotBlank(languageKey)) {
      Language language = languages.get(languageKey);
      if (language == null) {
        throw MessageException.of("Language with key '" + languageKey + "' not found");
      }
      project.setLanguage(language);
    } else {
      project.setLanguage(Project.NONE_LANGUAGE);
    }
  }
}
