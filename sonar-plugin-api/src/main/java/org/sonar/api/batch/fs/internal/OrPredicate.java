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
package org.sonar.api.batch.fs.internal;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;

import java.util.Arrays;
import java.util.Collection;

/**
 * @since 4.2
 */
class OrPredicate implements FilePredicate {

  private final Collection<FilePredicate> predicates;

  OrPredicate(Collection<FilePredicate> predicates) {
    if (predicates.isEmpty()) {
      this.predicates = Arrays.asList(TruePredicate.TRUE);
    } else {
      this.predicates = predicates;
    }
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

}
