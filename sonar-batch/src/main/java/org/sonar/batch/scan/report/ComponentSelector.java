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
package org.sonar.batch.scan.report;

import org.sonar.api.issue.Issue;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.batch.scan.filesystem.InputFileCache;

import java.util.Set;

abstract class ComponentSelector {

  private final InputFileCache cache;

  ComponentSelector(InputFileCache cache) {
    this.cache = cache;
  }

  public InputFileCache getCache() {
    return cache;
  }

  abstract void init();

  abstract boolean register(Issue issue);

  abstract Set<String> componentKeys();

  InputFile component(String componentKey) {
    String moduleKey = org.apache.commons.lang.StringUtils.substringBeforeLast(componentKey, ":");
    String path = org.apache.commons.lang.StringUtils.substringAfterLast(componentKey, ":");
    return cache.byPath(moduleKey, path);
  }
}
