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
package org.sonar.core.components;

import org.junit.Before;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class DefaultMetricFinderTest extends AbstractDbUnitTestCase {

  private DefaultMetricFinder finder;

  @Before
  public void setUp() {
    setupData("shared");
    finder = new DefaultMetricFinder(getSessionFactory());
  }

  @Test
  public void shouldFindAll() {
    assertThat(finder.findAll().size(), is(2));
  }

  @Test
  public void shouldFindByKeys() {
    assertThat(finder.findAll(Arrays.<String> asList("ncloc", "foo", "coverage")).size(), is(2));
  }

  @Test
  public void shouldFindById() {
    assertThat(finder.findById(1).getKey(), is("ncloc"));
    assertThat(finder.findById(3), nullValue());
  }

  @Test
  public void shouldFindByKey() {
    assertThat(finder.findByKey("ncloc").getKey(), is("ncloc"));
    assertThat(finder.findByKey("disabled"), nullValue());
  }
}
