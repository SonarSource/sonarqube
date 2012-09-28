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
package org.sonar.batch.bootstrap;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.resources.*;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.config.ProjectSettings;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

/**
 * Should be dropped when org.sonar.api.resources.Project is fully refactored.
 */
public class ProjectInitializer implements BatchComponent {

  private ResourceDao resourceDao;
  private DryRun dryRun;
  private ProjectFileSystem fileSystem;
  private Languages languages;

  public ProjectInitializer(ResourceDao resourceDao, DryRun dryRun, ProjectFileSystem fileSystem, Languages languages) {
    this.resourceDao = resourceDao;
    this.dryRun = dryRun;
    this.fileSystem = fileSystem;
    this.languages = languages;
  }

  public void execute(Project project, ProjectSettings settings) {
    initLanguage(project, settings);
    initFileSystem(project);
  }

  private void initLanguage(Project project, ProjectSettings settings) {
    project.setLanguageKey(StringUtils.defaultIfBlank(settings.getString("sonar.language"), Java.KEY));
    Language language = languages.get(project.getLanguageKey());
    if (language == null) {
      throw new SonarException("Language with key '" + project.getLanguageKey() + "' not found");
    }
    project.setLanguage(language);
    if (!dryRun.isEnabled() && project.getId() != null) {
      ResourceDto dto = resourceDao.getResource(project.getId());
      dto.setLanguage(project.getLanguageKey());
      resourceDao.insertOrUpdate(dto);
    }

  }

  private void initFileSystem(Project project) {
    // TODO See http://jira.codehaus.org/browse/SONAR-2126
    // previously MavenProjectBuilder was responsible for creation of ProjectFileSystem
    project.setFileSystem(fileSystem);

  }
}
