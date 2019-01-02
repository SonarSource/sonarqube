/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.api.batch.fs.InputFile;

class OptimizedFilePredicateAdapter extends AbstractFilePredicate {

  private FilePredicate unoptimizedPredicate;

  private OptimizedFilePredicateAdapter(FilePredicate unoptimizedPredicate) {
    this.unoptimizedPredicate = unoptimizedPredicate;
  }

  @Override
  public boolean apply(InputFile inputFile) {
    return unoptimizedPredicate.apply(inputFile);
  }

  public static OptimizedFilePredicate create(FilePredicate predicate) {
    if (predicate instanceof OptimizedFilePredicate) {
      return (OptimizedFilePredicate) predicate;
    } else {
      return new OptimizedFilePredicateAdapter(predicate);
    }
  }

}
