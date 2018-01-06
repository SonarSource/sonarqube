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
package org.sonar.api.batch.fs.internal;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

/**
 * Optimized version of FilePredicate allowing to speed up query by looking at InputFile by index.
 */
public interface OptimizedFilePredicate extends FilePredicate, Comparable<OptimizedFilePredicate> {

  /**
   * Filter provided files to keep only the ones that are valid for this predicate
   */
  Iterable<InputFile> filter(Iterable<InputFile> inputFiles);

  /**
   * Get all files that are valid for this predicate.
   */
  Iterable<InputFile> get(FileSystem.Index index);

  /**
   * For optimization. FilePredicates will be applied in priority order. For example when doing
   * p.and(p1, p2, p3) then p1, p2 and p3 will be applied according to their priority value. Higher priority value
   * are applied first.
   * Assign a high priority when the predicate will likely highly reduce the set of InputFiles to filter. Also
   * {@link RelativePathPredicate} and AbsolutePathPredicate have a high priority since they are using cache index.
   */
  int priority();
}
