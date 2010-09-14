/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.checks.templates;

import org.sonar.api.ServerExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public class CheckTemplateRepositories implements ServerExtension {

  private Map<String, CheckTemplateRepository> repositoriesByKey = new HashMap<String, CheckTemplateRepository>();

  public CheckTemplateRepositories(CheckTemplateRepository[] repositories) {
    if (repositories != null) {
      for (CheckTemplateRepository templateRepository : repositories) {
        repositoriesByKey.put(templateRepository.getKey(), templateRepository);
      }
    }
  }

  public CheckTemplateRepositories() {
    // DO NOT REMOVE THIS CONSTRUCTOR. It is used by Picocontainer when no repositories are available.
  }

  public CheckTemplateRepository getRepository(String key) {
    return repositoriesByKey.get(key);
  }

  public Collection<CheckTemplateRepository> getRepositories() {
    return repositoriesByKey.values();
  }

  public CheckTemplate getTemplate(String repositoryKey, String templateKey) {
    CheckTemplateRepository repo = getRepository(repositoryKey);
    if (repo != null) {
      return repo.getTemplate(templateKey);
    }
    return null;
  }
}
