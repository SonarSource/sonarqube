/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

package org.sonar.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;
import org.sonar.api.utils.WildcardPattern;

public class PatternUtilsTest {

  @Test
  public void shouldConvertJavaPackagesToPatterns() {
    WildcardPattern[] patterns = PatternUtils.createPatterns("org.sonar.Foo,javax.**");

    assertThat(patterns.length, is(2));
    assertThat(patterns[0].match("org/sonar/Foo"), is(true));
    assertThat(patterns[1].match("javax.Bar"), is(true));
  }
}
