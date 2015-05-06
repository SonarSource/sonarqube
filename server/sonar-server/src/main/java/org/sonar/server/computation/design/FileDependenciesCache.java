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

package org.sonar.server.computation.design;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;

/**
 * Cache of all the file dependencies involved in the analysis.
 * TODO Use a cache on disk
 */
public class FileDependenciesCache {

  private Multimap<Integer, FileDependency> fileDependenciesByRef;

  public FileDependenciesCache() {
    this.fileDependenciesByRef = ArrayListMultimap.create();
  }

  public void addFileDependency(int ref, FileDependency fileDependency) {
    fileDependenciesByRef.put(ref, fileDependency);
  }

  public Collection<FileDependency> getFileDependencies(int ref) {
    return fileDependenciesByRef.get(ref);
  }

}
