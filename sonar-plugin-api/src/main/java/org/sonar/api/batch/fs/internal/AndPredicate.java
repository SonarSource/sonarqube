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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem.Index;
import org.sonar.api.batch.fs.InputFile;

import static java.util.stream.Collectors.toList;

/**
 * @since 4.2
 */
class AndPredicate extends AbstractFilePredicate implements OperatorPredicate {

  private final List<OptimizedFilePredicate> predicates = new ArrayList<>();

  private AndPredicate() {
  }

  public static FilePredicate create(Collection<FilePredicate> predicates) {
    if (predicates.isEmpty()) {
      return TruePredicate.TRUE;
    }
    AndPredicate result = new AndPredicate();
    for (FilePredicate filePredicate : predicates) {
      if (filePredicate == TruePredicate.TRUE) {
        continue;
      } else if (filePredicate == FalsePredicate.FALSE) {
        return FalsePredicate.FALSE;
      } else if (filePredicate instanceof AndPredicate) {
        result.predicates.addAll(((AndPredicate) filePredicate).predicates);
      } else {
        result.predicates.add(OptimizedFilePredicateAdapter.create(filePredicate));
      }
    }
    Collections.sort(result.predicates);
    return result;
  }

  @Override
  public boolean apply(InputFile f) {
    for (OptimizedFilePredicate predicate : predicates) {
      if (!predicate.apply(f)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Iterable<InputFile> filter(Iterable<InputFile> target) {
    Iterable<InputFile> result = target;
    for (OptimizedFilePredicate predicate : predicates) {
      result = predicate.filter(result);
    }
    return result;
  }

  @Override
  public Iterable<InputFile> get(Index index) {
    if (predicates.isEmpty()) {
      return index.inputFiles();
    }
    // Optimization, use get on first predicate then filter with next predicates
    Iterable<InputFile> result = predicates.get(0).get(index);
    for (int i = 1; i < predicates.size(); i++) {
      result = predicates.get(i).filter(result);
    }
    return result;
  }

  Collection<OptimizedFilePredicate> predicates() {
    return predicates;
  }

  @Override
  public List<FilePredicate> operands() {
    return predicates.stream().map(p -> (FilePredicate) p).collect(toList());
  }

}
