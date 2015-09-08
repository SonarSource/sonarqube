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
package org.sonar.batch.repository;

import org.apache.commons.lang.mutable.MutableBoolean;

import javax.annotation.Nullable;

import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.sonar.batch.protocol.input.ProjectRepositories;

public class DefaultProjectSettingsLoader implements ProjectSettingsLoader {
  private ProjectRepositoriesFactory projectRepositoryFactory;

  public DefaultProjectSettingsLoader(ProjectRepositoriesFactory projectRepositoryFactory) {
    this.projectRepositoryFactory = projectRepositoryFactory;
  }

  @Override
  public ProjectSettingsRepo load(String projectKey, @Nullable MutableBoolean fromCache) {
    ProjectRepositories pr = projectRepositoryFactory.create();
    return new ProjectSettingsRepo(toTable(pr.settings()), toTable(pr.fileDataByModuleAndPath()), pr.lastAnalysisDate());
  }

  private static <T, U, V> Table<T, U, V> toTable(Map<T, Map<U, V>> map) {
    Table<T, U, V> table = HashBasedTable.create();

    for (Map.Entry<T, Map<U, V>> e1 : map.entrySet()) {
      for (Map.Entry<U, V> e2 : e1.getValue().entrySet()) {
        table.put(e1.getKey(), e2.getKey(), e2.getValue());
      }
    }

    return table;
  }
}
