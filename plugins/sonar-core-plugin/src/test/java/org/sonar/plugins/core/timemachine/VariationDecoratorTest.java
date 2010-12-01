/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine;

import org.junit.Test;
import org.sonar.api.resources.*;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class VariationDecoratorTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldNotCalculateVariationsOnFiles() {
    assertThat(VariationDecorator.shouldCalculateVariations(new Project("foo")), is(true));
    assertThat(VariationDecorator.shouldCalculateVariations(new JavaPackage("org.foo")), is(true));
    assertThat(VariationDecorator.shouldCalculateVariations(new Directory("org/foo")), is(true));

    assertThat(VariationDecorator.shouldCalculateVariations(new JavaFile("org.foo.Bar")), is(false));
    assertThat(VariationDecorator.shouldCalculateVariations(new JavaFile("org.foo.Bar", true)), is(false));
    assertThat(VariationDecorator.shouldCalculateVariations(new File("org/foo/Bar.php")), is(false));
  }

}
