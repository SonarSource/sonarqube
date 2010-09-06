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
package org.sonar.api.platform;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EnvironmentTest {

  @Test
  public void testMaven2() {
    assertThat(Environment.MAVEN2.isBatch(), is(true));
    assertThat(Environment.MAVEN2.isMaven2Batch(), is(true));
    assertThat(Environment.MAVEN2.isMaven3Batch(), is(false));
    assertThat(Environment.MAVEN2.isServer(), is(false));
  }

  @Test
  public void testMaven3() {
    assertThat(Environment.MAVEN3.isBatch(), is(true));
    assertThat(Environment.MAVEN3.isMaven2Batch(), is(false));
    assertThat(Environment.MAVEN3.isMaven3Batch(), is(true));
    assertThat(Environment.MAVEN3.isServer(), is(false));
  }

  @Test
  public void testServer() {
    assertThat(Environment.SERVER.isBatch(), is(false));
    assertThat(Environment.SERVER.isMaven2Batch(), is(false));
    assertThat(Environment.SERVER.isMaven3Batch(), is(false));
    assertThat(Environment.SERVER.isServer(), is(true));
  }
}
