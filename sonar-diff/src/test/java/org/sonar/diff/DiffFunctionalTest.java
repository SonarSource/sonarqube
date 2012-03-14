/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.diff;

import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DiffFunctionalTest {

  @Test
  public void example0() throws Exception {
    CodeChurn r = diff("example0");
    assertThat(r.getAdded(), is(5));
    assertThat(r.getDeleted(), is(0));
    assertThat(r.getDiff().size(), is(8));
  }

  @Test
  public void example1() throws Exception {
    CodeChurn r = diff("example1");
    assertThat(r.getAdded(), is(2));
    assertThat(r.getDeleted(), is(1));
    assertThat(r.getDiff().size(), is(4));
  }

  private CodeChurn diff(String name) throws IOException {
    return diff("examples/" + name + "/v1.java", "examples/" + name + "/v2.java");
  }

  private CodeChurn diff(String resourceA, String resourceB) throws IOException {
    Text a = new Text(Resources.toByteArray(Resources.getResource(resourceA)));
    Text b = new Text(Resources.toByteArray(Resources.getResource(resourceB)));
    return new CodeChurn(a, b, TextComparator.IGNORE_WHITESPACE);
  }

}
