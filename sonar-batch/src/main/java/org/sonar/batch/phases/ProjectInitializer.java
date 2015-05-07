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
package org.sonar.batch.phases;

import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

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
    if (languageKey != null) {
      Language language = languages.get(languageKey);
      if (language == null) {
        throw new SonarException("Language with key '" + languageKey + "' not found");
      }
      project.setLanguage(language);
    } else {
      project.setLanguage(Project.NONE_LANGUAGE);
    }
  }
}
