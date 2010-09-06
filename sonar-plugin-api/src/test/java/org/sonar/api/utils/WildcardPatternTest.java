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
package org.sonar.api.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class WildcardPatternTest {
  private boolean match(String pattern, String value, String separator) {
    return new WildcardPattern(pattern, separator).match(value);
  }

  private boolean match(String pattern, String value) {
    return new WildcardPattern(pattern, "/").match(value);
  }

  @Test
  public void javaResourcesShouldMatchWildcards() {
    assertTrue(match("Foo", "Foo", "."));
    assertFalse(match("Foo", "Bar", "."));

    assertTrue(match("org/sonar/**", "org.sonar.commons.Foo", "."));
    assertTrue(match("org/sonar/**", "org.sonar.Foo", "."));
    assertFalse(match("xxx/org/sonar/**", "org.sonar.Foo", "."));

    assertTrue(match("org/sonar/**/**", "org.sonar.commons.Foo", "."));
    assertTrue(match("org/sonar/**/**", "org.sonar.commons.sub.Foo", "."));
    assertTrue(match("org/sonar/**/Foo", "org.sonar.commons.sub.Foo", "."));
    assertTrue(match("org/sonar/**/Foo", "org.sonar.Foo", "."));

    assertTrue(match("*/foo/*", "org.foo.Bar", "."));
    assertFalse(match("*/foo/*", "foo.Bar", "."));
    assertFalse(match("*/foo/*", "foo", "."));
    assertFalse(match("*/foo/*", "org.foo.bar.Hello", "."));

    assertTrue(match("hell?", "hello", "."));
    assertFalse(match("hell?", "helloworld", "."));
    assertFalse(match("hell?", "hell", "."));

    assertTrue(match("a.b.c", "a.b.c", "."));
    assertTrue(match("*/a.b.c", "foo.a.b.c", "."));
    assertFalse(match("*/a.b.c", "foo/aabbc", "."));
  }

  @Test
  public void directoriesShouldMatchWildcards() {
    assertTrue(match("Foo", "Foo"));
    assertFalse(match("Foo", "Bar"));

    assertTrue(match("org/sonar/**", "org/sonar/commons/Foo"));
    assertTrue(match("org/sonar/**", "org/sonar/Foo.java"));
    assertFalse(match("xxx/org/sonar/**", "org/sonar/Foo"));

    assertTrue(match("org/sonar/**/**", "org/sonar/commons/Foo"));
    assertTrue(match("org/sonar/**/**", "org/sonar/commons/sub/Foo.java"));
    assertTrue(match("org/sonar/**/Foo", "org/sonar/commons/sub/Foo"));
    assertTrue(match("org/sonar/**/Foo", "org/sonar/Foo"));

    assertTrue(match("*/foo/*", "org/foo/Bar"));
    assertFalse(match("*/foo/*", "foo/Bar"));
    assertFalse(match("*/foo/*", "foo"));
    assertFalse(match("*/foo/*", "org/foo/bar/Hello"));

    assertTrue(match("hell?", "hello"));
    assertFalse(match("hell?", "helloworld"));
    assertFalse(match("hell?", "hell"));

    assertTrue(match("a.b.c", "a.b.c"));
    assertTrue(match("*/a.b.c", "foo/a.b.c"));
    assertFalse(match("*/a.b.c", "foo/aabbc"));

  }
}
