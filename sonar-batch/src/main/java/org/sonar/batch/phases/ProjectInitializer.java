/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.phases;

import org.sonar.api.BatchComponent;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

/**
 * Should be dropped when org.sonar.api.resources.Project is fully refactored.
 */
public class ProjectInitializer implements BatchComponent {

  private ResourceDao resourceDao;
  private Languages languages;

  public ProjectInitializer(ResourceDao resourceDao, Languages languages) {
    this.resourceDao = resourceDao;
    this.languages = languages;
  }

  public void execute(Project project) {
    if (project.getLanguage() == null) {
      initLanguage(project);
    }
  }

  private void initLanguage(Project project) {
    Language language = languages.get(project.getLanguageKey());
    if (language == null) {
      throw new SonarException("Language with key '" + project.getLanguageKey() + "' not found");
    }
    project.setLanguage(language);
    if (project.getId() != null) {
      ResourceDto dto = resourceDao.getResource(project.getId());
      dto.setLanguage(project.getLanguageKey());
      resourceDao.insertOrUpdate(dto);
    }

  }
}
