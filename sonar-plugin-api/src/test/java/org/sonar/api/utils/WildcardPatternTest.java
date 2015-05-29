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
package org.sonar.api.utils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
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
  public void examples() {
    assertTrue(match("org/T?st.java", "org/Test.java"));
    assertTrue(match("org/T?st.java", "org/Tost.java"));

    assertTrue(match("org/*.java", "org/Foo.java"));
    assertTrue(match("org/*.java", "org/Bar.java"));

    assertTrue(match("org/**", "org/Foo.java"));
    assertTrue(match("org/**", "org/foo/bar.jsp"));

    assertTrue(match("org/**/Test.java", "org/Test.java"));
    assertTrue(match("org/**/Test.java", "org/foo/Test.java"));
    assertTrue(match("org/**/Test.java", "org/foo/bar/Test.java"));

    assertTrue(match("org/**/*.java", "org/Foo.java"));
    assertTrue(match("org/**/*.java", "org/foo/Bar.java"));
    assertTrue(match("org/**/*.java", "org/foo/bar/Baz.java"));
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

    assertTrue(match("**/Reader", "java.io.Reader", "."));
    assertFalse(match("**/Reader", "org.sonar.channel.CodeReader", "."));

    assertTrue(match("**", "java.io.Reader", "."));
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

    assertTrue(match("**/Reader", "java/io/Reader"));
    assertFalse(match("**/Reader", "org/sonar/channel/CodeReader"));

    assertTrue(match("**", "java/io/Reader"));
  }

  /**
   * See http://jira.sonarsource.com/browse/SONAR-2193
   */
  @Test
  public void issue2193() {
    assertTrue(match("**/app/**", "com.app.Utils", "."));
    assertFalse(match("**/app/**", "com.application.MyService", "."));

    assertTrue(match("**/app/**", "com/app/Utils"));
    assertFalse(match("**/app/**", "com/application/MyService"));
  }

  /**
   * See SONAR-2762
   */
  @Test
  public void shouldEscapeRegexpSpecificCharacters() {
    assertFalse(match("**/*$*", "foo/bar"));
    assertTrue(match("**/*$*", "foo/bar$baz"));

    assertFalse(match("a+", "aa"));
    assertTrue(match("a+", "a+"));

    assertFalse(match("[ab]", "a"));
    assertTrue(match("[ab]", "[ab]"));

    assertTrue("all regexp-specific characters", match("()[]^$.{}+|", "()[]^$.{}+|"));
  }

  @Test
  public void backslash() {
    assertFalse("backslash is not an escape character", match("\\n", "\n"));
    assertTrue("backslash is the same as forward slash", match("foo\\bar", "foo/bar"));
  }

  @Test
  public void shouldIgnoreStartingSlash() {
    assertTrue(match("/foo", "foo"));
    assertTrue(match("\\foo", "foo"));
  }

  /**
   * Godin: in my opinion this is invalid pattern, however it might be constructed by {@link org.sonar.api.resources.JavaFile#matchFilePattern(String)},
   * so it should be supported at least for now for backward compatibility.
   */
  @Test
  public void cornerCase() {
    assertTrue(match("org/**.*", "org.sonar.commons.Foo.java", "."));
  }

  @Test
  public void multiplePatterns() {
    WildcardPattern[] patterns = WildcardPattern.create(new String[] { "Foo", "Bar" });
    assertTrue(WildcardPattern.match(patterns, "Foo"));
    assertTrue(WildcardPattern.match(patterns, "Bar"));
    assertFalse(WildcardPattern.match(patterns, "Other"));

    assertThat(WildcardPattern.create((String[]) null).length, is(0));
  }

  @Test
  public void testToString() {
    assertThat(WildcardPattern.create("foo*").toString(), is("foo*"));
  }
}
