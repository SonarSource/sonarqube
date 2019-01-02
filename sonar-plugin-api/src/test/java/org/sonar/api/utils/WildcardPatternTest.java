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
package org.sonar.api.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WildcardPatternTest {

  private boolean match(String pattern, String value, String separator) {
    return new WildcardPattern(pattern, separator).match(value);
  }

  private boolean match(String pattern, String value) {
    return new WildcardPattern(pattern, "/").match(value);
  }

  @Test
  public void examples() {
    assertThat(match("org/T?st.java", "org/Test.java")).isTrue();
    assertThat(match("org/T?st.java", "org/Tost.java")).isTrue();

    assertThat(match("org/*.java", "org/Foo.java")).isTrue();
    assertThat(match("org/*.java", "org/Bar.java")).isTrue();

    assertThat(match("org/**", "org/Foo.java")).isTrue();
    assertThat(match("org/**", "org/foo/bar.jsp")).isTrue();

    assertThat(match("org/**/Test.java", "org/Test.java")).isTrue();
    assertThat(match("org/**/Test.java", "org/foo/Test.java")).isTrue();
    assertThat(match("org/**/Test.java", "org/foo/bar/Test.java")).isTrue();

    assertThat(match("org/**/*.java", "org/Foo.java")).isTrue();
    assertThat(match("org/**/*.java", "org/foo/Bar.java")).isTrue();
    assertThat(match("org/**/*.java", "org/foo/bar/Baz.java")).isTrue();
  }

  @Test
  public void javaResourcesShouldMatchWildcards() {
    assertThat(match("Foo", "Foo", ".")).isTrue();
    assertThat(match("Foo", "Bar", ".")).isFalse();

    assertThat(match("org/sonar/**", "org.sonar.commons.Foo", ".")).isTrue();
    assertThat(match("org/sonar/**", "org.sonar.Foo", ".")).isTrue();
    assertThat(match("xxx/org/sonar/**", "org.sonar.Foo", ".")).isFalse();

    assertThat(match("org/sonar/**/**", "org.sonar.commons.Foo", ".")).isTrue();
    assertThat(match("org/sonar/**/**", "org.sonar.commons.sub.Foo", ".")).isTrue();
    assertThat(match("org/sonar/**/Foo", "org.sonar.commons.sub.Foo", ".")).isTrue();
    assertThat(match("org/sonar/**/Foo", "org.sonar.Foo", ".")).isTrue();

    assertThat(match("*/foo/*", "org.foo.Bar", ".")).isTrue();
    assertThat(match("*/foo/*", "foo.Bar", ".")).isFalse();
    assertThat(match("*/foo/*", "foo", ".")).isFalse();
    assertThat(match("*/foo/*", "org.foo.bar.Hello", ".")).isFalse();

    assertThat(match("hell?", "hello", ".")).isTrue();
    assertThat(match("hell?", "helloworld", ".")).isFalse();
    assertThat(match("hell?", "hell", ".")).isFalse();

    assertThat(match("a.b.c", "a.b.c", ".")).isTrue();
    assertThat(match("*/a.b.c", "foo.a.b.c", ".")).isTrue();
    assertThat(match("*/a.b.c", "foo/aabbc", ".")).isFalse();

    assertThat(match("**/Reader", "java.io.Reader", ".")).isTrue();
    assertThat(match("**/Reader", "org.sonar.channel.CodeReader", ".")).isFalse();

    assertThat(match("**", "java.io.Reader", ".")).isTrue();
  }

  @Test
  public void directoriesShouldMatchWildcards() {
    assertThat(match("Foo", "Foo")).isTrue();
    assertThat(match("Foo", "Bar")).isFalse();

    assertThat(match("org/sonar/**", "org/sonar/commons/Foo")).isTrue();
    assertThat(match("org/sonar/**", "org/sonar/Foo.java")).isTrue();
    assertThat(match("xxx/org/sonar/**", "org/sonar/Foo")).isFalse();

    assertThat(match("org/sonar/**/**", "org/sonar/commons/Foo")).isTrue();
    assertThat(match("org/sonar/**/**", "org/sonar/commons/sub/Foo.java")).isTrue();
    assertThat(match("org/sonar/**/Foo", "org/sonar/commons/sub/Foo")).isTrue();
    assertThat(match("org/sonar/**/Foo", "org/sonar/Foo")).isTrue();

    assertThat(match("*/foo/*", "org/foo/Bar")).isTrue();
    assertThat(match("*/foo/*", "foo/Bar")).isFalse();
    assertThat(match("*/foo/*", "foo")).isFalse();
    assertThat(match("*/foo/*", "org/foo/bar/Hello")).isFalse();

    assertThat(match("hell?", "hello")).isTrue();
    assertThat(match("hell?", "helloworld")).isFalse();
    assertThat(match("hell?", "hell")).isFalse();

    assertThat(match("a.b.c", "a.b.c")).isTrue();
    assertThat(match("*/a.b.c", "foo/a.b.c")).isTrue();
    assertThat(match("*/a.b.c", "foo/aabbc")).isFalse();

    assertThat(match("**/Reader", "java/io/Reader")).isTrue();
    assertThat(match("**/Reader", "org/sonar/channel/CodeReader")).isFalse();

    assertThat(match("**", "java/io/Reader")).isTrue();
  }

  /**
   * See http://jira.sonarsource.com/browse/SONAR-2193
   */
  @Test
  public void issue2193() {
    assertThat(match("**/app/**", "com.app.Utils", ".")).isTrue();
    assertThat(match("**/app/**", "com.application.MyService", ".")).isFalse();

    assertThat(match("**/app/**", "com/app/Utils")).isTrue();
    assertThat(match("**/app/**", "com/application/MyService")).isFalse();
  }

  /**
   * See SONAR-2762
   */
  @Test
  public void shouldEscapeRegexpSpecificCharacters() {
    assertThat(match("**/*$*", "foo/bar")).isFalse();
    assertThat(match("**/*$*", "foo/bar$baz")).isTrue();

    assertThat(match("a+", "aa")).isFalse();
    assertThat(match("a+", "a+")).isTrue();

    assertThat(match("[ab]", "a")).isFalse();
    assertThat(match("[ab]", "[ab]")).isTrue();

    assertThat(match("()[]^$.{}+|", "()[]^$.{}+|")).as("all regexp-specific characters").isTrue();
  }

  @Test
  public void backslash() {
    assertThat(match("\\n", "\n")).as("backslash is not an escape character").isFalse();
    assertThat(match("foo\\bar", "foo/bar")).as("backslash is the same as forward slash").isTrue();
  }

  @Test
  public void shouldIgnoreStartingSlash() {
    assertThat(match("/foo", "foo")).isTrue();
    assertThat(match("\\foo", "foo")).isTrue();
  }

  /**
   * Godin: in my opinion this is invalid pattern, however it might be constructed by {@link org.sonar.api.resources.JavaFile#matchFilePattern(String)},
   * so it should be supported at least for now for backward compatibility.
   */
  @Test
  public void cornerCase() {
    assertThat(match("org/**.*", "org.sonar.commons.Foo.java", ".")).isTrue();
  }

  @Test
  public void multiplePatterns() {
    WildcardPattern[] patterns = WildcardPattern.create(new String[] {"Foo", "Bar"});
    assertThat(WildcardPattern.match(patterns, "Foo")).isTrue();
    assertThat(WildcardPattern.match(patterns, "Bar")).isTrue();
    assertThat(WildcardPattern.match(patterns, "Other")).isFalse();

    assertThat(WildcardPattern.create((String[]) null)).isEmpty();
  }

  @Test
  public void testToString() {
    assertThat(WildcardPattern.create("foo*").toString()).isEqualTo("foo*");
  }
}
