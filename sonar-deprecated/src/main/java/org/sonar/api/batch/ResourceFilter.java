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
package org.sonar.api.batch;

import org.sonar.api.BatchSide;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.resources.Resource;

/**
 * Filter resources to save. For example, ignore a resource if its path matches an exclusion pattern (defined on the project).
 * Filters are applied to files, directories and packages only.
 *
 * If the method start(), without parameters, exists, then it is executed at startup.
 *
 * @since 1.12
 * @deprecated since 4.2. Analysis is file-system oriented. See {@link org.sonar.api.scan.filesystem.InputFileFilter}
 */
@Deprecated
@BatchSide
@ExtensionPoint
public interface ResourceFilter {

  /**
   * Return true if the resource must be ignored, else it's saved into database.
   */
  boolean isIgnored(Resource resource);

}
