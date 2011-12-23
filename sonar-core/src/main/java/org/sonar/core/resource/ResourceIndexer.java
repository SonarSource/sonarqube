/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.core.resource;

import com.google.common.annotations.Beta;
import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;

/**
 * This component will be automatically called in v3.0 when a resource is created or updated.
 * It means that it will not be exposed to plugin API.
 *
 * @since 2.13
 */
@Beta
public class ResourceIndexer implements BatchComponent, ServerComponent {
  private ResourceIndexerDao dao;

  /**
   * Hardcoded list of qualifiers to index. Need to be configurable.
   * Directories and packages are explicitly excluded.
   */
  static final String[] INDEXABLE_QUALIFIERS = {
    Qualifiers.VIEW,
    Qualifiers.SUBVIEW,
    Qualifiers.PROJECT,
    Qualifiers.MODULE,
    Qualifiers.FILE,
    Qualifiers.CLASS,
    Qualifiers.UNIT_TEST_FILE
  };

  public ResourceIndexer(ResourceIndexerDao dao) {
    this.dao = dao;
  }

  public ResourceIndexer index(String resourceName, String qualifier, int resourceId, int rootProjectId) {
    if (ArrayUtils.contains(INDEXABLE_QUALIFIERS, qualifier)) {
      dao.index(resourceName, qualifier, resourceId, rootProjectId);
    }
    return this;
  }

  /**
   * Used only for the migration from a version less than 2.13.
   */
  public ResourceIndexer indexAll() {
    ResourceIndexerFilter filter = ResourceIndexerFilter.create()
      .setScopes(new String[]{Scopes.PROJECT, Scopes.FILE})
      .setQualifiers(INDEXABLE_QUALIFIERS);
    dao.index(filter);
    return this;
  }
}
