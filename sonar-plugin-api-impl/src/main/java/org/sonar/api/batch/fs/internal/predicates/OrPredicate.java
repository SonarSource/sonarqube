/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.api.batch.fs.internal.predicates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;

/**
 * @since 4.2
 */
class OrPredicate extends AbstractFilePredicate implements OperatorPredicate {

  private final List<FilePredicate> predicates = new ArrayList<>();

  private OrPredicate() {
  }

  public static FilePredicate create(Collection<FilePredicate> predicates) {
    if (predicates.isEmpty()) {
      return TruePredicate.TRUE;
    }
    OrPredicate result = new OrPredicate();
    for (FilePredicate filePredicate : predicates) {
      if (filePredicate == TruePredicate.TRUE) {
        return TruePredicate.TRUE;
      } else if (filePredicate == FalsePredicate.FALSE) {
        continue;
      } else if (filePredicate instanceof OrPredicate orPredicate) {
        result.predicates.addAll(orPredicate.predicates);
      } else {
        result.predicates.add(filePredicate);
      }
    }
    return result;
  }

  @Override
  public boolean apply(InputFile f) {
    for (FilePredicate predicate : predicates) {
      if (predicate.apply(f)) {
        return true;
      }
    }
    return false;
  }

  Collection<FilePredicate> predicates() {
    return predicates;
  }

  @Override
  public List<FilePredicate> operands() {
    return predicates;
  }

}
