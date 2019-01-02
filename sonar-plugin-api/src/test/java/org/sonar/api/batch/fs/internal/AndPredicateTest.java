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

import org.junit.Test;
import org.sonar.api.batch.fs.FilePredicate;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class AndPredicateTest {

  @Test
  public void flattenNestedAnd() {
    PathPatternPredicate pathPatternPredicate1 = new PathPatternPredicate(PathPattern.create("foo1/**"));
    PathPatternPredicate pathPatternPredicate2 = new PathPatternPredicate(PathPattern.create("foo2/**"));
    PathPatternPredicate pathPatternPredicate3 = new PathPatternPredicate(PathPattern.create("foo3/**"));
    FilePredicate andPredicate = AndPredicate.create(Arrays.<FilePredicate>asList(pathPatternPredicate1,
      AndPredicate.create(Arrays.<FilePredicate>asList(pathPatternPredicate2, pathPatternPredicate3))));
    assertThat(((AndPredicate) andPredicate).predicates()).containsExactly(pathPatternPredicate1, pathPatternPredicate2, pathPatternPredicate3);
  }

  @Test
  public void sortPredicatesByPriority() {
    PathPatternPredicate pathPatternPredicate1 = new PathPatternPredicate(PathPattern.create("foo1/**"));
    PathPatternPredicate pathPatternPredicate2 = new PathPatternPredicate(PathPattern.create("foo2/**"));
    RelativePathPredicate relativePathPredicate = new RelativePathPredicate("foo");
    FilePredicate andPredicate = AndPredicate.create(Arrays.<FilePredicate>asList(pathPatternPredicate1,
      relativePathPredicate, pathPatternPredicate2));
    assertThat(((AndPredicate) andPredicate).predicates()).containsExactly(relativePathPredicate, pathPatternPredicate1, pathPatternPredicate2);
  }

  @Test
  public void simplifyAndExpressionsWhenEmpty() {
    FilePredicate andPredicate = AndPredicate.create(Arrays.<FilePredicate>asList());
    assertThat(andPredicate).isEqualTo(TruePredicate.TRUE);
  }

  @Test
  public void simplifyAndExpressionsWhenTrue() {
    PathPatternPredicate pathPatternPredicate1 = new PathPatternPredicate(PathPattern.create("foo1/**"));
    PathPatternPredicate pathPatternPredicate2 = new PathPatternPredicate(PathPattern.create("foo2/**"));
    FilePredicate andPredicate = AndPredicate.create(Arrays.<FilePredicate>asList(pathPatternPredicate1,
      TruePredicate.TRUE, pathPatternPredicate2));
    assertThat(((AndPredicate) andPredicate).predicates()).containsExactly(pathPatternPredicate1, pathPatternPredicate2);
  }

  @Test
  public void simplifyAndExpressionsWhenFalse() {
    PathPatternPredicate pathPatternPredicate1 = new PathPatternPredicate(PathPattern.create("foo1/**"));
    PathPatternPredicate pathPatternPredicate2 = new PathPatternPredicate(PathPattern.create("foo2/**"));
    FilePredicate andPredicate = AndPredicate.create(Arrays.<FilePredicate>asList(pathPatternPredicate1,
      FalsePredicate.FALSE, pathPatternPredicate2));
    assertThat(andPredicate).isEqualTo(FalsePredicate.FALSE);
  }

}
