/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.source;

import java.util.List;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.source.SourceLinesHashRepositoryImpl.LineHashesComputer;

public interface SourceLinesHashRepository {
  /**
   * Get line hashes from the report matching the version of the line hashes existing in the report, if possible.
   * The line hashes are cached.
   */
  List<String> getMatchingDB(Component component);

  /**
   * The line computer will compute line hashes taking into account significant code (if it was provided by a code analyzer).
   * It will use a cached value, if possible. If it's generated, it's not cached since it's assumed that it won't be 
   * needed again after it is persisted.
   */
  LineHashesComputer getLineProcessorToPersist(Component component);

  /**
   * Get the version of line hashes in the report
   */
  Integer getLineHashesVersion(Component component);
}
