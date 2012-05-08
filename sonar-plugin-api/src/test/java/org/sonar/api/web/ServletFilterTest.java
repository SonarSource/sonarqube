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
package org.sonar.api.web;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ServletFilterTest {
  @Test
  public void matchAll() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/*");
    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/foo/ooo")).isTrue();
  }

  @Test
  public void matchEndOfUrl() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("*foo");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/hello/foo")).isTrue();
    assertThat(pattern.matches("/hello/bar")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo2")).isFalse();
  }

  @Test
  public void matchBeginningOfUrl() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/foo/*");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo/bar")).isTrue();
    assertThat(pattern.matches("/bar")).isFalse();
  }

  @Test
  public void matchExactUrl() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/foo");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo/")).isFalse();
    assertThat(pattern.matches("/bar")).isFalse();
  }
}
