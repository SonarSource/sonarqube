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
package org.sonar.core.test;

import com.tinkerpop.blueprints.Direction;
import org.sonar.api.test.CoverageBlock;
import org.sonar.api.test.TestCase;
import org.sonar.api.test.Testable;
import org.sonar.core.graph.BeanEdge;

import java.util.List;

public class DefaultCoverageBlock extends BeanEdge implements CoverageBlock {

  @Override
  public TestCase testCase() {
    return getVertex(DefaultTestCase.class, Direction.OUT);
  }

  @Override
  public Testable testable() {
    return getVertex(DefaultTestable.class, Direction.IN);
  }

  @Override
  public List<Integer> lines() {
    return (List<Integer>) getProperty("lines");
  }
}
